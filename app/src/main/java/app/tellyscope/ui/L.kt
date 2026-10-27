// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import java.util.Locale

/**
 * Arabic next to the English, at the place a string is written. English stays the default everywhere
 * else, and in every test, so adding an Arabic line can't change what the English tests see.
 * The language is read at each call because the app's language can change while it runs.
 */
object L {
    val arabic: Boolean get() = Locale.getDefault().language == "ar"

    fun t(en: String, ar: String): String = if (arabic) ar else en
}
