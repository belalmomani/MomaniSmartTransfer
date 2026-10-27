// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.adb.Integrity
import app.tellyscope.adb.TvFolder
import app.tellyscope.ui.Copy
import org.junit.Assert.assertEquals
import org.junit.Test

class CopyTest {
    @Test fun upForSpellsOutTheLargestUnits() {
        assertEquals("3 days, 4 hours", Copy.upFor(3 * 86_400L + 4 * 3_600 + 59))
        assertEquals("1 day", Copy.upFor(86_400L + 120))
        assertEquals("1 hour", Copy.upFor(3_600L + 300))
        assertEquals("12 minutes", Copy.upFor(12 * 60L))
    }

    @Test fun speedUsesTheSameUnitsAsSizes() {
        assertEquals("2.3 MB/s", Copy.speed(2_400_000))
        assertEquals("12 KB/s", Copy.speed(12_500))
        assertEquals("300 bytes/s", Copy.speed(300))
    }

    @Test fun aQueueSummaryNamesEverythingThatWentWrong() {
        assertEquals(
            "Sent 3 files to ${Copy.folder(TvFolder.DOWNLOAD)}. 1 did NOT match after sending: delete and resend them. 1 couldn't be checked. 2 failed: pick them again to retry.",
            Copy.queueDone(3, 2, 1, 1, TvFolder.DOWNLOAD),
        )
        assertEquals("Sent 1 file to ${Copy.folder(TvFolder.MOVIES)}.", Copy.queueDone(1, 0, 0, 0, TvFolder.MOVIES))
    }

    @Test fun cancellingAQueueSaysWhatWasAlreadySent() {
        assertEquals(Copy.CANCELLED_SEND, Copy.cancelledQueue(0))
        assertEquals("Cancelled. 2 files were already sent; the one in progress was removed.", Copy.cancelledQueue(2))
    }

    @Test fun aMismatchIsNeverReportedAsASuccess() {
        val ok = Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, Integrity.Verified("x"))
        val bad = Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, Integrity.Mismatch("x", "y"))
        assertEquals(true, ok.contains("matches"))
        assertEquals(true, bad.contains("does NOT match"))
        assertEquals(Copy.sent("a.mp4", "a.mp4", TvFolder.DOWNLOAD), Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, null))
    }

    @Test fun noRateUntilThereIsSomethingTrueToSay() {
        assertEquals(null, Copy.rate(1_000, 10_000, 0, 5_000))        // start time unknown
        assertEquals(null, Copy.rate(1_000, 10_000, 1_000, 2_000))    // under two seconds
        assertEquals(null, Copy.rate(0, 10_000, 1_000, 9_000))        // nothing moved
        assertEquals(true, Copy.rate(1_000_000, 10_000_000, 1_000, 11_000)!!.endsWith("left"))
        assertEquals(false, Copy.rate(1_000_000, null, 1_000, 11_000)!!.contains("left"))
    }
}
