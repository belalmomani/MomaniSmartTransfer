// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.adb

import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnoseTest {
    private fun header(command: Int, magic: Int = command.inv()): ByteArray =
        ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putInt(command).putInt(0).putInt(0).putInt(0).putInt(0).putInt(magic).array()

    /** A fake TV: reads the greeting, then lets [respond] decide what to do with the connection. */
    private fun withTv(respond: (java.net.Socket) -> Unit, test: (Int) -> Unit) {
        ServerSocket(0).use { server ->
            val t = thread(isDaemon = true) {
                runCatching { server.accept().use { s -> s.getInputStream().readNBytes(24 + 7); respond(s) } }
            }
            test(server.localPort)
            t.join(2_000)
        }
    }

    private fun probe(port: Int, timeoutMs: Int = 1_500) = runBlocking { Diagnose.probe("127.0.0.1", port, timeoutMs) }

    @Test fun aTvThatAsksForAKeyIsReportedAsWaitingForOne() = withTv({ it.getOutputStream().write(header(0x48545541)) }) { port ->
        val p = probe(port)
        assertEquals(Peer.ASKS_FOR_KEY, p.peer)
        assertNotNull(p.tcpMs)
        assertNotNull(p.replyMs)
    }

    @Test fun aTvThatWantsTlsNeedsPairing() = withTv({ it.getOutputStream().write(header(0x534c5453)) }) { port ->
        assertEquals(Peer.NEEDS_TLS, probe(port).peer)
    }

    @Test fun aTvThatAlreadyTrustsUsSaysHello() = withTv({ it.getOutputStream().write(header(0x4e584e43)) }) { port ->
        assertEquals(Peer.OPEN, probe(port).peer)
    }

    @Test fun aTvThatHangsUpAtOnceIsReportedAsClosing() = withTv({ /* closes without a word */ }) { port ->
        assertEquals(Peer.CLOSED_AT_ONCE, probe(port).peer)
    }

    @Test fun aPortThatAnswersWithSomethingElseIsNotAdb() = withTv({ it.getOutputStream().write(ByteArray(24) { 7 }) }) { port ->
        assertEquals(Peer.NOT_ADB, probe(port).peer)
    }

    @Test fun anAdbShapedHeaderWithABadMagicIsNotAdb() = withTv({ it.getOutputStream().write(header(0x48545541, magic = 1)) }) { port ->
        assertEquals(Peer.NOT_ADB, probe(port).peer)
    }

    @Test fun aPortThatNeverAnswersIsSilent() = withTv({ Thread.sleep(1_500) }) { port ->
        val p = probe(port, timeoutMs = 300)
        assertEquals(Peer.SILENT, p.peer)
        assertNull(p.replyMs)
    }

    @Test fun aClosedPortIsReportedAsNothingThere() {
        val port = ServerSocket(0).use { it.localPort }
        val p = probe(port)
        assertEquals(Peer.NO_TCP, p.peer)
        assertNull(p.tcpMs)
    }

    @Test fun anImpossiblePortIsNotProbed() {
        assertEquals(Peer.NO_TCP, probe(70_000).peer)
    }
}
