// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.adb.Integrity
import app.tellyscope.adb.TvFolder
import app.tellyscope.ui.Copy
import app.tellyscope.ui.L
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LanguageTest {
    private lateinit var saved: Locale

    @Before fun remember() { saved = Locale.getDefault() }

    @After fun restore() { Locale.setDefault(saved) }

    private fun hasArabic(s: String) = s.any { it in '\u0600'..'\u06FF' }

    private fun hasLatinWords(s: String) = Regex("[A-Za-z]{3,}").containsMatchIn(s.replace("SHA-256", ""))

    @Test fun englishIsTheDefaultAnywhereButArabic() {
        Locale.setDefault(Locale.US)
        assertEquals("Cancel", L.t("Cancel", "إلغاء"))
        Locale.setDefault(Locale.GERMANY)
        assertEquals("Cancel", L.t("Cancel", "إلغاء"))
    }

    @Test fun arabicIsUsedWhenTheLanguageIsArabic() {
        Locale.setDefault(Locale("ar"))
        assertEquals("إلغاء", L.t("Cancel", "إلغاء"))
        Locale.setDefault(Locale("ar", "JO"))
        assertEquals("إلغاء", L.t("Cancel", "إلغاء"))
    }

    @Test fun theTransferMessagesAreFullyArabicWithNoEnglishLeftOver() {
        Locale.setDefault(Locale("ar"))
        val lines = listOf(
            Copy.sent("a.mp4", "a.mp4", TvFolder.DOWNLOAD),
            Copy.sent("a.mp4", "a (1).mp4", TvFolder.MOVIES),
            Copy.saved("a.mp4"),
            Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, Integrity.Verified("x")),
            Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, Integrity.Mismatch("x", "y")),
            Copy.sentChecked("a.mp4", "a.mp4", TvFolder.DOWNLOAD, Integrity.Unchecked("x")),
            Copy.queueDone(3, 2, 1, 1, TvFolder.DOWNLOAD),
            Copy.cancelledQueue(0),
            Copy.cancelledQueue(2),
            Copy.sendFailed("a.mp4"),
            Copy.saveFailed("a.mp4"),
            Copy.CANCELLED_SAVE,
            Copy.CANT_READ,
            Copy.rate(1_000_000, 10_000_000, 1_000, 11_000)!!,
        )
        lines.forEach { l ->
            assertTrue(l, hasArabic(l))
            // File names like a.mp4 are short; any longer Latin word means an untranslated English phrase.
            assertFalse(l, hasLatinWords(l.replace(Regex("\\b[a-z]\\.?[a-z0-9 ()]*mp4\\b"), "").replace(Regex("\\b[A-Z]{1,2}B/?\\b"), "")))
        }
    }

    @Test fun aMismatchIsStillAWarningInArabic() {
        Locale.setDefault(Locale("ar"))
        assertTrue(Copy.sentChecked("a", "a", TvFolder.DOWNLOAD, Integrity.Mismatch("x", "y")).contains("تحذير"))
    }
}
