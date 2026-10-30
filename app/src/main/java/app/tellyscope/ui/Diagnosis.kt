// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.tellyscope.adb.Peer
import app.tellyscope.adb.Probe

/** [probes] is null while the check runs. */
data class DiagnosisUi(val tv: String, val host: String, val probes: List<Probe>? = null)

/** Turns what the TV's ports said into plain sentences, and a plain-text report to copy. */
object Diagnosis {
    fun label(p: Peer): String = when (p) {
        Peer.OPEN -> L.t("ADB, and it already trusts this phone", "ADB، ويثق بهذا الهاتف مسبقاً")
        Peer.ASKS_FOR_KEY -> L.t("ADB, waiting for this phone's key", "ADB، ينتظر مفتاح هذا الهاتف")
        Peer.NEEDS_TLS -> L.t("ADB over TLS (wireless debugging), needs pairing", "ADB عبر TLS (التصحيح اللاسلكي)، يحتاج اقتراناً")
        Peer.CLOSED_AT_ONCE -> L.t("closed the connection at once", "أغلق الاتصال فوراً")
        Peer.NOT_ADB -> L.t("answered, but not with ADB", "ردّ، لكن ليس بـ ADB")
        Peer.SILENT -> L.t("open, but never answered", "مفتوح، لكنه لم يرد أبداً")
        Peer.NO_TCP -> L.t("refused, or nothing answered", "رفض الاتصال، أو لم يرد شيء")
    }

    /** One line per port: "Port 5555: waiting for this phone's key · connected in 12 ms". */
    fun lines(probes: List<Probe>): List<String> = probes.map { p ->
        val ms = p.tcpMs?.let { L.t(" · connected in $it ms", " · اتصل خلال $it مللي ثانية") }.orEmpty()
        L.t("Port ${p.port}: ", "المنفذ ${p.port}: ") + label(p.peer) + ms
    }

    /** What it most likely means and what to do, judged by the port that got closest to working. */
    fun verdict(probes: List<Probe>): String = when (probes.minByOrNull { it.peer.ordinal }?.peer) {
        null -> ""
        Peer.OPEN -> L.t(
            "The TV already trusts this phone, so the connection itself should work. If the app still fails, tell the developer what the app said.",
            "التلفزيون يثق بهذا الهاتف مسبقاً، فالاتصال نفسه يجب أن يعمل. إن بقي التطبيق يفشل فأخبر المطوّر بالرسالة التي يعرضها.",
        )
        Peer.ASKS_FOR_KEY -> L.t(
            "The TV speaks ADB and is waiting for this phone's key. Press Try again and watch the TV: an \"Allow USB debugging?\" box should appear. If nothing appears, this TV's menu is hiding or blocking that box. Look in Developer options for a \"Revoke USB debugging authorisations\" item and use it, then try again.",
            "التلفزيون يتحدث ADB وينتظر مفتاح هذا الهاتف. اضغط «حاول مرة أخرى» وراقب التلفزيون: يجب أن يظهر مربع «Allow USB debugging?». إن لم يظهر شيء فواجهة هذا التلفزيون تخفي المربع أو تمنعه. ابحث في خيارات المطوّر عن بند «Revoke USB debugging authorisations» واستخدمه ثم حاول مجدداً.",
        )
        Peer.NEEDS_TLS -> L.t(
            "This is wireless debugging, which needs pairing first. On the TV open Developer options → Wireless debugging → Pair device with pairing code, then enter the 6 digits in this app.",
            "هذا هو التصحيح اللاسلكي، ويحتاج اقتراناً أولاً. على التلفزيون افتح خيارات المطوّر ← التصحيح اللاسلكي ← الاقتران برمز، ثم أدخل الأرقام الستة في هذا التطبيق.",
        )
        Peer.CLOSED_AT_ONCE -> L.t(
            "The TV accepts the connection and drops it before asking for a key. Either this port isn't the debugging port, or this TV refuses new computers. Turn wireless debugging off and on again, then check again.",
            "يقبل التلفزيون الاتصال ثم يقطعه قبل أن يطلب المفتاح. إما أن هذا المنفذ ليس منفذ التصحيح، أو أن هذا التلفزيون يرفض الحواسيب الجديدة. أطفئ التصحيح اللاسلكي وشغّله من جديد ثم افحص مجدداً.",
        )
        Peer.NOT_ADB -> L.t(
            "Something answers on this port, but it isn't ADB. The TV's debugging port may be a different one.",
            "هناك ما يرد على هذا المنفذ لكنه ليس ADB. ربما منفذ التصحيح في التلفزيون مختلف.",
        )
        Peer.SILENT -> L.t(
            "The port is open but never answers like ADB. The TV may be busy or asleep, or something is filtering the traffic. Wake the TV and check again.",
            "المنفذ مفتوح لكنه لا يرد مثل ADB. ربما التلفزيون مشغول أو في وضع السكون، أو شيء يعترض الاتصال. أيقظ التلفزيون وافحص مجدداً.",
        )
        Peer.NO_TCP -> L.t(
            "Nothing on the TV accepts a connection on these ports. Check that wireless debugging is on, that the TV is awake, and that the phone and the TV are on the same Wi-Fi.",
            "لا شيء في التلفزيون يقبل الاتصال على هذه المنافذ. تأكد أن التصحيح اللاسلكي مفعّل، وأن التلفزيون يعمل، وأن الهاتف والتلفزيون على شبكة الواي فاي نفسها.",
        )
    }

    /** What the copy button puts on the clipboard: no keys, no personal data. */
    fun report(d: DiagnosisUi): String {
        val probes = d.probes ?: return ""
        return buildString {
            appendLine("Momani Smart Transfer: connection check")
            appendLine("TV: ${d.tv} (${d.host})")
            probes.forEach { p -> appendLine("port ${p.port}: ${p.peer}${p.detail?.let { " ($it)" }.orEmpty()}, tcp ${p.tcpMs ?: "-"} ms, reply ${p.replyMs ?: "-"} ms") }
        }
    }
}

@Composable
fun DiagnosisDialog(d: DiagnosisUi, onClose: () -> Unit) {
    val context = LocalContext.current
    val probes = d.probes
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(L.t("Why can't it connect?", "لماذا لا يتصل؟")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${d.tv} · ${d.host}", style = MaterialTheme.typography.labelLarge)
                if (probes == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.padding(4.dp))
                        Text(L.t("Asking the TV…", "جارٍ سؤال التلفزيون…"))
                    }
                } else {
                    Diagnosis.lines(probes).forEach { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()) }
                    Text(Diagnosis.verdict(probes), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        L.t("This check sends no key and changes nothing on the TV.", "هذا الفحص لا يرسل أي مفتاح ولا يغيّر شيئاً في التلفزيون."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(L.t("Close", "إغلاق")) } },
        dismissButton = if (probes == null) null else {
            {
                TextButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Momani Smart Transfer", Diagnosis.report(d)))
                }) { Text(L.t("Copy report", "نسخ التقرير")) }
            }
        },
    )
}
