// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tellyscope.adb.TvFolder
import app.tellyscope.data.LogFilter
import app.tellyscope.data.LogSort
import app.tellyscope.data.TransferCheck
import app.tellyscope.data.TransferLogQuery
import app.tellyscope.data.TransferRecord
import app.tellyscope.data.TransferStatus
import java.text.DateFormat
import java.util.Date

/**
 * Every copy that ended, kept across restarts. Removing a record or clearing the list never deletes
 * the copied file, on the TV or the phone, and the screen says so.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferLogScreen(
    records: List<TransferRecord>,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    back: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(LogSort.DATE) }
    var sortMenu by remember { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val shown = remember(records, query, filter, sort) { TransferLogQuery.apply(records, query, filter, sort) }
    val open = records.firstOrNull { it.id == openId }

    if (open != null) DetailsDialog(open, onClose = { openId = null }, onDelete = { onDelete(open.id); openId = null })
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text(L.t("Clear the transfer log?", "مسح سجل النقل؟")) },
        text = { Text(L.t("This only empties the list. Files already copied stay on the TV and the phone.", "هذا يفرغ القائمة فقط. الملفات المنسوخة تبقى على التلفزيون والهاتف.")) },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text(L.t("Clear log", "مسح السجل")) } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(L.t("Keep it", "إبقاء السجل")) } },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(L.t("Transfer log", "سجل النقل")) },
                navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.t("Back", "رجوع")) } },
                actions = {
                    IconButton(onClick = { confirmClear = true }, enabled = records.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = L.t("Clear the log", "مسح السجل"))
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(L.t("Search by file or TV", "ابحث باسم الملف أو التلفزيون")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                listOf(LogFilter.ALL to L.t("All", "الكل"), LogFilter.DONE to L.t("Done", "تم"), LogFilter.FAILED to L.t("Failed", "فشل")).forEach { (f, label) ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label) })
                }
                Column {
                    TextButton(onClick = { sortMenu = true }) { Text(L.t("Sort: ${sortLabel(sort)}", "الترتيب: ${sortLabel(sort)}")) }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        LogSort.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(sortLabel(s)) }, onClick = { sort = s; sortMenu = false })
                        }
                    }
                }
            }
            when {
                records.isEmpty() -> Text(
                    L.t("No transfers yet. Files you send to the TV or save to your phone are listed here.", "لا توجد عمليات نقل بعد. الملفات التي ترسلها إلى التلفزيون أو تحفظها على هاتفك تظهر هنا."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shown.isEmpty() -> Text(L.t("Nothing matches.", "لا نتائج مطابقة."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shown, key = { it.id }) { r -> RecordRow(r) { openId = r.id } }
                }
            }
        }
    }
}

@Composable
private fun RecordRow(r: TransferRecord, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(r.savedAs ?: r.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            Text(
                L.t(if (r.toTv) "To" else "From", if (r.toTv) "إلى" else "من") + " ${r.deviceName} · ${r.size?.let { b -> Copy.bytes(b) } ?: L.t("size unknown", "الحجم غير معروف")} · ${dateTime(r.startedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(statusLine(r), style = MaterialTheme.typography.labelMedium, color = statusColor(r))
        }
    }
}

@Composable
private fun DetailsDialog(r: TransferRecord, onClose: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(r.name, maxLines = 2, overflow = TextOverflow.MiddleEllipsis) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fun line(label: String, value: String?) = value?.let { label to it }
                listOfNotNull(
                    line(L.t("Result", "النتيجة"), statusLine(r)),
                    line(L.t("Saved as", "حُفظ باسم"), r.savedAs),
                    line(L.t("Direction", "الاتجاه"), if (r.toTv) L.t("Phone to TV", "من الهاتف إلى التلفزيون") else L.t("TV to phone", "من التلفزيون إلى الهاتف")),
                    line(L.t("TV", "التلفزيون"), r.deviceName),
                    line(L.t("TV folder", "مجلد التلفزيون"), TvFolder.entries.firstOrNull { it.path == r.folder }?.let { f -> Copy.folder(f) } ?: r.folder),
                    line(L.t("Type", "النوع"), fileType(r.name)),
                    line(L.t("Size", "الحجم"), r.size?.let { b -> Copy.bytes(b) }),
                    line(L.t("Started", "البداية"), dateTime(r.startedAt)),
                    line(L.t("Finished", "النهاية"), dateTime(r.finishedAt)),
                    line(L.t("Took", "المدة"), duration(r.durationMs)),
                    line(L.t("Speed", "السرعة"), r.bytesPerSecond?.let { Copy.bytes(it) + L.t("/s", "/ث") }),
                    line(L.t("Why it failed", "سبب الفشل"), r.error),
                    line("SHA-256", r.sha256),
                ).forEach { (label, value) ->
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    L.t("Removing this entry doesn't delete the file itself.", "حذف هذا السجل لا يحذف الملف نفسه."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(L.t("Close", "إغلاق")) } },
        dismissButton = { TextButton(onClick = onDelete) { Text(L.t("Remove entry", "حذف السجل")) } },
    )
}

private fun sortLabel(s: LogSort) = when (s) {
    LogSort.DATE -> L.t("Date", "التاريخ")
    LogSort.NAME -> L.t("Name", "الاسم")
    LogSort.SIZE -> L.t("Size", "الحجم")
    LogSort.STATUS -> L.t("Status", "الحالة")
}

/** What happened, in words, including a failed check: a mismatch is never shown as a plain success. */
private fun statusLine(r: TransferRecord): String = when (r.status) {
    TransferStatus.FAILED -> L.t("Failed", "فشل")
    TransferStatus.CANCELLED -> L.t("Cancelled", "أُلغي")
    TransferStatus.DONE -> (if (r.toTv) L.t("Sent", "تم الإرسال") else L.t("Saved", "تم الحفظ")) + when (r.check) {
        TransferCheck.VERIFIED -> L.t(", checked: matches", "، تم التحقق: مطابق")
        TransferCheck.MISMATCH -> L.t(", WARNING: the TV's copy does not match", "، تحذير: نسخة التلفزيون غير مطابقة")
        TransferCheck.UNCHECKED -> L.t(", not checked", "، لم يتم التحقق")
        null -> ""
    }
}

@Composable
private fun statusColor(r: TransferRecord): Color = when {
    r.status == TransferStatus.FAILED || r.check == TransferCheck.MISMATCH -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun fileType(name: String): String? =
    name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 8 }?.uppercase()?.let { L.t("$it file", "ملف $it") }

private fun dateTime(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

private fun duration(ms: Long): String? {
    val s = ms / 1000
    return when {
        s < 1 -> null
        s < 60 -> L.t("$s s", "$s ث")
        else -> L.t("${s / 60} min ${s % 60} s", "${s / 60} د ${s % 60} ث")
    }
}
