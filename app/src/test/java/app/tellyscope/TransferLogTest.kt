// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.TransferCheck
import app.tellyscope.data.TransferLog
import app.tellyscope.data.TransferRecord
import app.tellyscope.data.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TransferLogTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun rec(name: String = "a.mp4", status: TransferStatus = TransferStatus.DONE, device: String = "tv1") = TransferRecord(
        id = 0, deviceId = device, deviceName = "Living room", toTv = true, folder = "/sdcard/Download", name = name,
        savedAs = null, size = 5_000_000, startedAt = 1_000, finishedAt = 6_000, status = status,
    )

    @Test fun recordsSurviveARestart() {
        val f = tmp.newFile()
        val log = TransferLog(f)
        log.add(rec("فيلم عربي.mp4").copy(savedAs = "فيلم عربي (1).mp4", check = TransferCheck.VERIFIED, sha256 = "ab".repeat(32)))
        log.add(rec("b.txt", TransferStatus.FAILED).copy(error = "the TV went away", size = null))
        val again = TransferLog(f).all()
        assertEquals(listOf("b.txt", "فيلم عربي.mp4"), again.map { it.name })
        assertEquals(TransferCheck.VERIFIED, again[1].check)
        assertEquals("فيلم عربي (1).mp4", again[1].savedAs)
        assertNull(again[0].size)
        assertEquals("the TV went away", again[0].error)
    }

    @Test fun everyRecordGetsItsOwnId() {
        val log = TransferLog()
        val ids = (1..5).map { log.add(rec("f$it")).id }
        assertEquals(5, ids.toSet().size)
    }

    @Test fun removeAndClearOnlyTouchTheLog() {
        val f = tmp.newFile()
        val log = TransferLog(f)
        val a = log.add(rec("a"))
        log.add(rec("b"))
        log.remove(a.id)
        assertEquals(listOf("b"), TransferLog(f).all().map { it.name })
        log.clear()
        assertTrue(TransferLog(f).all().isEmpty())
    }

    @Test fun aDamagedLineCostsOnlyThatLine() {
        val f = tmp.newFile()
        val log = TransferLog(f)
        log.add(rec("a"))
        f.appendText("v1\tnot-a-number\tgarbage\n\nwhat\n")
        log.add(rec("b"))
        assertEquals(listOf("b", "a"), TransferLog(f).all().map { it.name })
    }

    @Test fun tabsAndNewlinesInNamesDoNotBreakTheFile() {
        val f = tmp.newFile()
        TransferLog(f).add(rec("odd\tname\nhere.bin").copy(error = "a\tb"))
        val r = TransferLog(f).all().single()
        assertEquals("odd name here.bin", r.name)
        assertEquals("a b", r.error)
    }

    @Test fun speedIsOnlyClaimedWhenItWasMeasured() {
        assertEquals(1_000_000L, rec().bytesPerSecond)
        assertNull(rec(status = TransferStatus.FAILED).bytesPerSecond)
        assertNull(rec().copy(size = null).bytesPerSecond)
        assertNull(rec().copy(finishedAt = 1_200).bytesPerSecond)
    }
}
