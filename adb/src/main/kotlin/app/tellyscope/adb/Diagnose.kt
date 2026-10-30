// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.adb

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

/** What a port said to a bare ADB greeting, ordered from "closest to working" to "nothing there". */
enum class Peer {
    /** Answered with its own greeting: it already trusts this phone. */
    OPEN,

    /** Asked for a key: an "Allow USB debugging?" prompt is expected on the TV. */
    ASKS_FOR_KEY,

    /** Asked to switch to TLS: wireless debugging, which needs pairing with the 6-digit code. */
    NEEDS_TLS,

    /** Accepted the connection, then closed it before saying anything. */
    CLOSED_AT_ONCE,

    /** Answered with something that isn't ADB. */
    NOT_ADB,

    /** Accepted the connection and never answered. */
    SILENT,

    /** Refused, or nothing answered at all. */
    NO_TCP,
}

/** One port's answer. [tcpMs] and [replyMs] are null where that step didn't happen. */
data class Probe(val host: String, val port: Int, val peer: Peer, val tcpMs: Long?, val replyMs: Long?, val detail: String?)

/**
 * Says what is listening on a TV's port without logging in. It sends the greeting every ADB client
 * sends first and reads the header of the reply, then hangs up. It never sends a key, so it can't make
 * the TV show an Allow prompt and can't change anything on the TV.
 */
object Diagnose {
    private const val A_CNXN = 0x4e584e43
    private const val A_AUTH = 0x48545541
    private const val A_STLS = 0x534c5453
    private const val HEADER = 24

    suspend fun probe(host: String, port: Int, timeoutMs: Int = 4_000): Probe =
        runInterruptible(Dispatchers.IO) { probeBlocking(host, port, timeoutMs) }

    private fun probeBlocking(host: String, port: Int, timeoutMs: Int): Probe {
        if (port !in 1..65535) return Probe(host, port, Peer.NO_TCP, null, null, "port $port is not between 1 and 65535")
        var tcpMs: Long? = null
        val socket = Socket()
        try {
            val t0 = System.nanoTime()
            try {
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            } catch (e: IOException) {
                return Probe(host, port, Peer.NO_TCP, null, null, e.message ?: e.javaClass.simpleName)
            }
            tcpMs = (System.nanoTime() - t0) / 1_000_000
            socket.soTimeout = timeoutMs
            val payload = "host::\u0000".toByteArray()
            val header = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(A_CNXN)
                putInt(0x01000001) // protocol version
                putInt(256 * 1024) // largest message we'd take
                putInt(payload.size)
                putInt(payload.sumOf { it.toInt() and 0xff })
                putInt(A_CNXN.inv())
            }.array()
            socket.getOutputStream().apply {
                write(header)
                write(payload)
                flush()
            }
            val t1 = System.nanoTime()
            val reply = ByteArray(HEADER)
            var got = 0
            val input = socket.getInputStream()
            while (got < HEADER) {
                val n = try {
                    input.read(reply, got, HEADER - got)
                } catch (_: SocketTimeoutException) {
                    return Probe(host, port, Peer.SILENT, tcpMs, null, null)
                }
                if (n < 0) break
                got += n
            }
            val replyMs = (System.nanoTime() - t1) / 1_000_000
            if (got == 0) return Probe(host, port, Peer.CLOSED_AT_ONCE, tcpMs, replyMs, null)
            if (got < HEADER) return Probe(host, port, Peer.NOT_ADB, tcpMs, replyMs, "only $got bytes came back")
            val bb = ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN)
            val command = bb.getInt(0)
            // Every ADB header ends with the command inverted; anything else isn't ADB.
            if (bb.getInt(20) != command.inv()) return Probe(host, port, Peer.NOT_ADB, tcpMs, replyMs, "not an ADB header")
            return when (command) {
                A_CNXN -> Probe(host, port, Peer.OPEN, tcpMs, replyMs, null)
                A_AUTH -> Probe(host, port, Peer.ASKS_FOR_KEY, tcpMs, replyMs, null)
                A_STLS -> Probe(host, port, Peer.NEEDS_TLS, tcpMs, replyMs, null)
                else -> Probe(host, port, Peer.NOT_ADB, tcpMs, replyMs, "ADB command 0x%08x".format(command))
            }
        } catch (e: IOException) {
            // A reset mid-way is the TV hanging up on us.
            return Probe(host, port, Peer.CLOSED_AT_ONCE, tcpMs, null, e.message ?: e.javaClass.simpleName)
        } finally {
            runCatching { socket.close() }
        }
    }
}
