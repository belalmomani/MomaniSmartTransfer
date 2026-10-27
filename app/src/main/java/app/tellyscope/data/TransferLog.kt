// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import java.io.File

enum class TransferStatus { DONE, FAILED, CANCELLED }

/** What the SHA-256 comparison found, kept as text in the log. Null in a record means no check was attempted. */
enum class TransferCheck { VERIFIED, MISMATCH, UNCHECKED }

/**
 * One file copy that ended. [toTv] is the direction; [folder] is the TV folder path either way.
 * Only what was really measured is stored: [size] is null when unknown, [check] null when no check ran.
 */
data class TransferRecord(
    val id: Long,
    val deviceId: String,
    val deviceName: String,
    val toTv: Boolean,
    val folder: String,
    val name: String,
    /** The name on the TV when it differs from [name] (a taken name gets " (1)"). */
    val savedAs: String?,
    val size: Long?,
    val startedAt: Long,
    val finishedAt: Long,
    val status: TransferStatus,
    val error: String? = null,
    val check: TransferCheck? = null,
    val sha256: String? = null,
) {
    val durationMs: Long get() = (finishedAt - startedAt).coerceAtLeast(0)

    /** Bytes per second, only for a finished copy of a known size that took long enough to measure. */
    val bytesPerSecond: Long? get() =
        if (status == TransferStatus.DONE && size != null && size > 0 && durationMs >= 1_000) size * 1_000 / durationMs else null
}

/**
 * Every finished, failed or cancelled copy, kept across restarts in a tab-separated file. Deleting a
 * record never touches the copied file. Writes go to a temp file and are renamed into place; a damaged
 * line costs that line only. With no file (screenshots, tests) it lives in memory.
 */
class TransferLog(private val file: File? = null) {
    private val records = mutableListOf<TransferRecord>()

    init {
        file?.takeIf { it.exists() }?.let { f ->
            runCatching { f.readLines() }.getOrDefault(emptyList()).forEach { line -> parse(line)?.let(records::add) }
        }
    }

    /** Newest first. */
    @Synchronized
    fun all(): List<TransferRecord> = records.sortedByDescending { it.id }

    /** Stores [record] under a fresh id and returns it with that id. */
    @Synchronized
    fun add(record: TransferRecord): TransferRecord {
        val stored = record.copy(id = (records.maxOfOrNull { it.id } ?: 0) + 1)
        records += stored
        save()
        return stored
    }

    @Synchronized
    fun remove(id: Long) {
        if (records.removeAll { it.id == id }) save()
    }

    @Synchronized
    fun clear() {
        if (records.isEmpty()) return
        records.clear()
        save()
    }

    private fun save() {
        val f = file ?: return
        f.parentFile?.mkdirs()
        val tmp = File(f.path + ".tmp")
        tmp.writeText(records.joinToString("") { format(it) + "\n" })
        if (!tmp.renameTo(f)) {
            f.delete()
            tmp.renameTo(f)
        }
    }

    private fun format(r: TransferRecord) = listOf(
        VERSION, r.id.toString(), clean(r.deviceId), clean(r.deviceName), if (r.toTv) "1" else "0", clean(r.folder),
        clean(r.name), clean(r.savedAs), r.size?.toString().orEmpty(), r.startedAt.toString(), r.finishedAt.toString(),
        r.status.name, clean(r.error), r.check?.name.orEmpty(), r.sha256.orEmpty(),
    ).joinToString("\t")

    private fun parse(line: String): TransferRecord? {
        val f = line.split('\t')
        if (f.size != 15 || f[0] != VERSION) return null
        return TransferRecord(
            id = f[1].toLongOrNull() ?: return null,
            deviceId = f[2],
            deviceName = f[3],
            toTv = when (f[4]) { "1" -> true; "0" -> false; else -> return null },
            folder = f[5],
            name = f[6].ifEmpty { return null },
            savedAs = f[7].ifEmpty { null },
            size = f[8].ifEmpty { null }?.let { it.toLongOrNull() ?: return null },
            startedAt = f[9].toLongOrNull() ?: return null,
            finishedAt = f[10].toLongOrNull() ?: return null,
            status = TransferStatus.entries.firstOrNull { it.name == f[11] } ?: return null,
            error = f[12].ifEmpty { null },
            check = f[13].ifEmpty { null }?.let { c -> TransferCheck.entries.firstOrNull { it.name == c } ?: return null },
            sha256 = f[14].ifEmpty { null },
        )
    }

    /** Names and errors come from the TV and the phone's file picker: keep each on one field. */
    private fun clean(s: String?) = s.orEmpty().replace(Regex("[\\t\\r\\n]"), " ")

    private companion object {
        const val VERSION = "v1"
    }
}
