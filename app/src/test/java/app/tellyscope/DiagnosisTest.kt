// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.adb.Peer
import app.tellyscope.adb.Probe
import app.tellyscope.ui.Diagnosis
import app.tellyscope.ui.DiagnosisUi
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosisTest {
    private lateinit var saved: Locale

    @Before fun remember() { saved = Locale.getDefault(); Locale.setDefault(Locale.US) }

    @After fun restore() { Locale.setDefault(saved) }

    private fun probe(port: Int, peer: Peer, tcp: Long? = 12, reply: Long? = 30) = Probe("192.168.8.14", port, peer, tcp, reply, null)

    @Test fun theVerdictFollowsThePortThatGotClosestToWorking() {
        val v = Diagnosis.verdict(listOf(probe(5555, Peer.NO_TCP, null, null), probe(37123, Peer.ASKS_FOR_KEY)))
        assertTrue(v, v.contains("waiting for this phone's key"))
        assertTrue(v, v.contains("Allow USB debugging?"))
    }

    @Test fun wirelessDebuggingIsToldToPairFirst() {
        assertTrue(Diagnosis.verdict(listOf(probe(40111, Peer.NEEDS_TLS))).contains("pairing"))
    }

    @Test fun everyKindOfAnswerHasAnExplanationAndALabel() {
        Peer.entries.forEach { peer ->
            assertTrue(peer.name, Diagnosis.verdict(listOf(probe(5555, peer))).isNotBlank())
            assertTrue(peer.name, Diagnosis.label(peer).isNotBlank())
        }
    }

    @Test fun nothingToSayWithoutAnyProbe() {
        assertEquals("", Diagnosis.verdict(emptyList()))
    }

    @Test fun linesNameThePortAndHowFastItConnected() {
        val l = Diagnosis.lines(listOf(probe(5555, Peer.ASKS_FOR_KEY, tcp = 12), probe(9, Peer.NO_TCP, tcp = null, reply = null)))
        assertEquals("Port 5555: ADB, waiting for this phone's key · connected in 12 ms", l[0])
        assertEquals("Port 9: refused, or nothing answered", l[1])
    }

    @Test fun theCopiedReportHoldsNoKeysAndNothingPersonal() {
        val r = Diagnosis.report(DiagnosisUi("TV", "192.168.8.14", listOf(probe(5555, Peer.ASKS_FOR_KEY))))
        assertTrue(r.contains("port 5555: ASKS_FOR_KEY"))
        assertTrue(r.contains("192.168.8.14"))
        assertFalse(r.contains("fingerprint", ignoreCase = true))
        assertEquals("", Diagnosis.report(DiagnosisUi("TV", "192.168.8.14", null)))
    }

    @Test fun theVerdictIsArabicWhenTheLanguageIs() {
        Locale.setDefault(Locale("ar"))
        val v = Diagnosis.verdict(listOf(probe(5555, Peer.ASKS_FOR_KEY)))
        assertTrue(v, v.any { it in '\u0600'..'\u06FF' })
        assertTrue(v, v.contains("Allow USB debugging?"))
    }
}
