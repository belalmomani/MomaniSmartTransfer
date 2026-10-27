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
        title = { Text("Clear the transfer log?") },
        text = { Text("This only empties the list. Files already copied stay on the TV and the phone.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear log") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep it") } },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer log") },
                navigationIcon = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { confirmClear = true }, enabled = records.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear the log")
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
                placeholder = { Text("Search by file or TV") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                listOf(LogFilter.ALL to "All", LogFilter.DONE to "Done", LogFilter.FAILED to "Failed").forEach { (f, label) ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(label) })
                }
                Column {
                    TextButton(onClick = { sortMenu = true }) { Text("Sort: ${sortLabel(sort)}") }
                    DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                        LogSort.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(sortLabel(s)) }, onClick = { sort = s; sortMenu = false })
                        }
                    }
                }
            }
            when {
                records.isEmpty() -> Text(
                    "No transfers yet. Files you send to the TV or save to your phone are listed here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shown.isEmpty() -> Text("Nothing matches.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                "${if (r.toTv) "To" else "From"} ${r.deviceName} · ${r.size?.let { b -> Copy.bytes(b) } ?: "size unknown"} · ${dateTime(r.startedAt)}",
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
                    line("Result", statusLine(r)),
                    line("Saved as", r.savedAs),
                    line("Direction", if (r.toTv) "Phone to TV" else "TV to phone"),
                    line("TV", r.deviceName),
                    line("TV folder", TvFolder.entries.firstOrNull { it.path == r.folder }?.let { f -> Copy.folder(f) } ?: r.folder),
                    line("Type", fileType(r.name)),
                    line("Size", r.size?.let { b -> Copy.bytes(b) }),
                    line("Started", dateTime(r.startedAt)),
                    line("Finished", dateTime(r.finishedAt)),
                    line("Took", duration(r.durationMs)),
                    line("Speed", r.bytesPerSecond?.let { "${Copy.bytes(it)}/s" }),
                    line("Why it failed", r.error),
                    line("SHA-256", r.sha256),
                ).forEach { (label, value) ->
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Removing this entry doesn't delete the file itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Remove entry") } },
    )
}

private fun sortLabel(s: LogSort) = when (s) {
    LogSort.DATE -> "Date"
    LogSort.NAME -> "Name"
    LogSort.SIZE -> "Size"
    LogSort.STATUS -> "Status"
}

/** What happened, in words, including a failed check: a mismatch is never shown as a plain success. */
private fun statusLine(r: TransferRecord): String = when (r.status) {
    TransferStatus.FAILED -> "Failed"
    TransferStatus.CANCELLED -> "Cancelled"
    TransferStatus.DONE -> (if (r.toTv) "Sent" else "Saved") + when (r.check) {
        TransferCheck.VERIFIED -> ", checked: matches"
        TransferCheck.MISMATCH -> ", WARNING: the TV's copy does not match"
        TransferCheck.UNCHECKED -> ", not checked"
        null -> ""
    }
}

@Composable
private fun statusColor(r: TransferRecord): Color = when {
    r.status == TransferStatus.FAILED || r.check == TransferCheck.MISMATCH -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun fileType(name: String): String? =
    name.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 8 }?.uppercase()?.let { "$it file" }

private fun dateTime(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

private fun duration(ms: Long): String? {
    val s = ms / 1000
    return when {
        s < 1 -> null
        s < 60 -> "$s s"
        else -> "${s / 60} min ${s % 60} s"
    }
}
