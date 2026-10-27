// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.LogFilter
import app.tellyscope.data.LogSort
import app.tellyscope.data.TransferLogQuery
import app.tellyscope.data.TransferRecord
import app.tellyscope.data.TransferStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferLogQueryTest {
    private fun rec(name: String, start: Long, size: Long?, status: TransferStatus = TransferStatus.DONE, tv: String = "Den") =
        TransferRecord(start, "id", tv, true, "/sdcard/Download", name, null, size, start, start + 2_000, status)

    private val a = rec("Beta.mp4", 100, 50)
    private val b = rec("alpha.mkv", 300, null, TransferStatus.FAILED)
    private val c = rec("Gamma.apk", 200, 900, TransferStatus.CANCELLED, tv = "Bedroom")
    private val all = listOf(a, b, c)

    private fun names(l: List<TransferRecord>) = l.map { it.name }

    @Test fun searchIgnoresCaseAndAlsoMatchesTheTvName() {
        assertEquals(listOf("alpha.mkv"), names(TransferLogQuery.apply(all, " ALPHA ", LogFilter.ALL, LogSort.DATE)))
        assertEquals(listOf("Gamma.apk"), names(TransferLogQuery.apply(all, "bedroom", LogFilter.ALL, LogSort.DATE)))
    }

    @Test fun filtersSplitSuccessFromFailureAndCancelledIsNeither() {
        assertEquals(listOf("Beta.mp4"), names(TransferLogQuery.apply(all, "", LogFilter.DONE, LogSort.DATE)))
        assertEquals(listOf("alpha.mkv"), names(TransferLogQuery.apply(all, "", LogFilter.FAILED, LogSort.DATE)))
    }

    @Test fun sortsByDateNameSizeAndStatus() {
        assertEquals(listOf("alpha.mkv", "Gamma.apk", "Beta.mp4"), names(TransferLogQuery.apply(all, "", LogFilter.ALL, LogSort.DATE)))
        assertEquals(listOf("alpha.mkv", "Beta.mp4", "Gamma.apk"), names(TransferLogQuery.apply(all, "", LogFilter.ALL, LogSort.NAME)))
        assertEquals(listOf("Gamma.apk", "Beta.mp4", "alpha.mkv"), names(TransferLogQuery.apply(all, "", LogFilter.ALL, LogSort.SIZE)))
        assertEquals(listOf("Beta.mp4", "alpha.mkv", "Gamma.apk"), names(TransferLogQuery.apply(all, "", LogFilter.ALL, LogSort.STATUS)))
    }
}
