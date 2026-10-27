// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

enum class LogFilter { ALL, DONE, FAILED }

enum class LogSort { DATE, NAME, SIZE, STATUS }

/** What the transfer log screen shows: search, filter and sort applied to the stored records. */
object TransferLogQuery {
    fun apply(records: List<TransferRecord>, query: String, filter: LogFilter, sort: LogSort): List<TransferRecord> {
        val q = query.trim()
        val shown = records.filter { r ->
            when (filter) {
                LogFilter.ALL -> true
                LogFilter.DONE -> r.status == TransferStatus.DONE
                // A cancelled copy didn't arrive either, but it isn't an error: it stays under All only.
                LogFilter.FAILED -> r.status == TransferStatus.FAILED
            } && (q.isEmpty() || r.name.contains(q, ignoreCase = true) || r.savedAs?.contains(q, ignoreCase = true) == true ||
                r.deviceName.contains(q, ignoreCase = true))
        }
        return when (sort) {
            LogSort.DATE -> shown.sortedByDescending { it.startedAt }
            LogSort.NAME -> shown.sortedBy { it.name.lowercase() }
            // Unknown sizes last, never mixed in as zero.
            LogSort.SIZE -> shown.sortedWith(compareBy<TransferRecord> { it.size == null }.thenByDescending { it.size ?: 0L })
            LogSort.STATUS -> shown.sortedWith(compareBy<TransferRecord> { it.status.ordinal }.thenByDescending { it.startedAt })
        }
    }
}
