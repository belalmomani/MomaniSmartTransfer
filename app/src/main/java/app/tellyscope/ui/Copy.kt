// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.ui

import app.tellyscope.adb.InstallKind
import app.tellyscope.adb.Integrity
import app.tellyscope.adb.InstallRefusal
import app.tellyscope.adb.Peripheral
import app.tellyscope.adb.TvFolder
import app.tellyscope.data.Action
import app.tellyscope.data.AppNames
import app.tellyscope.data.DeviceState
import app.tellyscope.data.Snapshot
import app.tellyscope.data.TvSettings
import app.tellyscope.data.sourceName
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Every user-facing sentence about actions and time lives here so wording is
 * reviewed in one place: every button says what it does before it runs.
 */
object Copy {
    fun button(action: Action) = when (action) {
        Action.Stop -> L.t("Stop", "إيقاف")
        Action.Disable -> L.t("Disable", "تعطيل")
        Action.Enable -> L.t("Turn on", "تشغيل")
        Action.Open -> L.t("Open", "فتح")
        Action.Uninstall -> L.t("Uninstall", "إزالة التثبيت")
        Action.Change, Action.ClearCaches, Action.Home, Action.HomeBack -> error("$action has its own button")
    }

    fun confirm(action: Action) = when (action) {
        Action.Stop -> L.t("Yes, stop it", "نعم، أوقفه")
        Action.Disable -> L.t("Yes, disable it", "نعم، عطّله")
        Action.Enable -> L.t("Yes, turn it on", "نعم، شغّله")
        Action.Open -> L.t("Yes, open it", "نعم، افتحه")
        Action.Uninstall -> L.t("Yes, uninstall it", "نعم، أزل تثبيته")
        Action.Home -> L.t("Yes, switch home screen", "نعم، بدّل الشاشة الرئيسية")
        Action.HomeBack -> L.t("Yes, switch back", "نعم، أعدها")
        Action.Change, Action.ClearCaches -> error("$action isn't an app action")
    }

    /**
     * Names the package next to the label: the label comes from the app itself, and any app can
     * call itself "Settings". The package id is what actually gets changed.
     */
    fun explain(action: Action, app: String, pkg: String) = "$app ($pkg)".let { who ->
        when (action) {
            Action.Stop -> L.t("Stops $who until you open it again.", "يوقف $who حتى تفتحه مرة أخرى.")
            Action.Disable -> L.t("Hides $who and stops it running. Nothing is deleted; you can turn it back on here.", "يخفي $who ويمنعه من العمل. لا يُحذف شيء؛ يمكنك تشغيله مجدداً من هنا.")
            Action.Enable -> L.t("Turns $who back on.", "يعيد تشغيل $who.")
            Action.Open -> L.t("Opens $who on the TV.", "يفتح $who على التلفزيون.")
            Action.Uninstall -> L.t("Removes $who and everything saved in it. This can't be undone; to get it back, install it again.", "يزيل $who وكل ما حُفظ فيه. لا يمكن التراجع؛ لاستعادته ثبّته من جديد.")
            Action.Change, Action.ClearCaches, Action.Home, Action.HomeBack -> error("$action isn't a one-app action")
        }
    }

    fun done(action: Action, app: String) = when (action) {
        Action.Stop -> L.t("Stopped $app", "تم إيقاف $app")
        Action.Disable -> L.t("Disabled $app", "تم تعطيل $app")
        Action.Enable -> L.t("Turned on $app", "تم تشغيل $app")
        Action.Open -> L.t("Opened $app", "تم فتح $app")
        Action.Uninstall -> L.t("Uninstalled $app", "تمت إزالة تثبيت $app")
        // A setting's label is the whole sentence: "Private DNS set to …".
        Action.Change -> app
        Action.ClearCaches -> L.t("Cleared $app", "تم مسح $app")
        Action.Home -> L.t("Set $app as the default launcher", "تم تعيين $app كشاشة رئيسية افتراضية")
        Action.HomeBack -> L.t("Switched home back to $app", "أُعيدت الشاشة الرئيسية إلى $app")
    }

    /** Said in the confirm before Stop, Disable or Uninstall of an app the TV's list keeps. */
    val KEEP_WARNING get() = L.t("This TV's list says to keep this.", "قائمة هذا التلفزيون توصي بإبقاء هذا.")

    fun homeBack(launcher: String) = L.t("Switch home back to $launcher", "إعادة الشاشة الرئيسية إلى $launcher")

    /** The way back keeps the added launcher, so it can be made Home again or uninstalled. */
    fun explainHomeBack(added: String, back: String, service: Boolean) = buildString {
        append(L.t("Makes $back open when you press Home again, turning it back on if it's off. $added stays on", "يجعل $back يفتح عند الضغط على زر الرئيسية مجدداً، ويعيد تشغيله إن كان متوقفاً. يبقى $added يعمل"))
        append(if (service) L.t(", with its accessibility service turned off. ", "، مع إيقاف خدمة إمكانية الوصول الخاصة به. ") else ". ")
        append(L.t("Undo in History makes $added Home again.", "التراجع من السجل يعيد $added إلى الشاشة الرئيسية."))
    }

    /** Where the lists come from, for the Debloat screen and the snackbar after a change. */
    fun listSource(custom: String?) =
        if (custom == null) L.t("Lists from tellyscope-devices, built from cleanyourtv", "قوائم من tellyscope-devices المبنية على cleanyourtv")
        else L.t("Lists from ${sourceName(custom)}", "قوائم من ${sourceName(custom)}")

    val MAKE_HOME get() = L.t("Set as default launcher", "تعيين كشاشة رئيسية افتراضية")

    /**
     * The home switch touches two apps, so it names both. The old launcher is disabled, not just
     * replaced: otherwise many TVs switch back to it by themselves.
     */
    fun explainHome(app: String, pkg: String, current: String, currentPkg: String, service: Boolean = false) = buildString {
        append(L.t("Makes $app ($pkg) open when you press Home, then disables $current ($currentPkg) so the TV can't switch back by itself. ", "يجعل $app ($pkg) يفتح عند الضغط على زر الرئيسية، ثم يعطّل $current ($currentPkg) حتى لا يعود التلفزيون إليه من تلقاء نفسه. "))
        append(L.t("Undo in History turns $current back on and makes it Home again.", "التراجع من السجل يعيد تشغيل $current ويجعله الشاشة الرئيسية من جديد."))
        if (service) {
            append(L.t("\n\nIt also turns on $app's accessibility service, so the Home button opens $app on TVs that would open their own launcher. ", "\n\nكما يشغّل خدمة إمكانية الوصول الخاصة بـ $app، ليفتح زر الرئيسية $app على التلفزيونات التي تفتح واجهتها الخاصة. "))
            append(L.t("An accessibility service can see what's on screen. Undo and Switch home back turn it off.", "تستطيع خدمة إمكانية الوصول رؤية ما على الشاشة. التراجع وإعادة الشاشة الرئيسية يوقفانها."))
        }
        if (currentPkg in GOOGLE_HOMES) append(L.t("\n\nSome Google TV features, like the Apps row and the Live tab, are part of $current and go away with it.", "\n\nبعض ميزات Google TV، مثل صف التطبيقات وتبويب البث المباشر، جزء من $current وتزول بزواله."))
    }

    private val GOOGLE_HOMES = setOf("com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx")

    /**
     * Force-stop has no true undo, and undoing a Turn on disables the app; the button says what it
     * actually does.
     */
    fun undo(undo: Action) = when (undo) {
        Action.Open -> L.t("Open again", "فتح مجدداً")
        Action.Disable -> button(Action.Disable)
        else -> L.t("Undo", "تراجع")
    }

    fun ago(at: Long, now: Long): String {
        val s = ((now - at) / 1000).coerceAtLeast(0)
        return when {
            s < 10 -> L.t("just now", "الآن")
            s < 60 -> L.t("$s s ago", "منذ $s ث")
            s < 3600 -> L.t("${s / 60} min ago", "منذ ${s / 60} د")
            s < 86_400 -> L.t("${s / 3600} h ago", "منذ ${s / 3600} س")
            else -> L.t("${s / 86_400} d ago", "منذ ${s / 86_400} يوماً")
        }
    }

    fun uptime(s: Long): String {
        val d = s / 86_400
        val h = (s % 86_400) / 3600
        val m = (s % 3600) / 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }

    /** Uptime for people: the largest unit or two, spelled out. */
    fun upFor(s: Long): String {
        fun n(v: Long, unit: String): String {
            if (!L.arabic) return "$v $unit${if (v == 1L) "" else "s"}"
            // Arabic: one and two are their own forms, 3-10 take the plural, 11 and up the singular.
            val (one, two, few, many) = when (unit) {
                "day" -> listOf("يوم", "يومان", "أيام", "يوماً")
                "hour" -> listOf("ساعة", "ساعتان", "ساعات", "ساعة")
                else -> listOf("دقيقة", "دقيقتان", "دقائق", "دقيقة")
            }
            return when (v) {
                1L -> one
                2L -> two
                in 3L..10L -> "$v $few"
                else -> "$v $many"
            }
        }
        val d = s / 86_400
        val h = (s % 86_400) / 3600
        val m = (s % 3600) / 60
        return when {
            d > 0 && h > 0 -> "${n(d, "day")}${L.t(", ", "، ")}${n(h, "hour")}"
            d > 0 -> n(d, "day")
            h > 0 -> n(h, "hour")
            else -> n(m, "minute")
        }
    }

    /** "May 2023", in the phone's language. */
    fun month(m: YearMonth): String = m.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    /** Network speed in the same units as [bytes], so a download's size and speed line up. */
    fun speed(bytesPerSec: Long) = bytes(bytesPerSec) + L.t("/s", "/ث")

    fun mb(v: Int) = if (v >= 1024) "%.1f GB".format(v / 1024.0) else "$v MB"

    fun disabledSuggested(done: Int, of: Int) = when {
        done == of -> L.t("Disabled $done ${if (done == 1) "app" else "apps"}. Undo any of them from History.", "تم تعطيل $done من التطبيقات. يمكنك التراجع عن أي منها من السجل.")
        done == 0 -> L.t("The TV didn't disable any of them.", "لم يعطّل التلفزيون أياً منها.")
        else -> L.t("Disabled $done of $of apps; the TV refused the rest. Undo any of them from History.", "تم تعطيل $done من $of تطبيقاً؛ رفض التلفزيون الباقي. يمكنك التراجع عن أي منها من السجل.")
    }

    fun uninstallTitle(app: String) = L.t("Uninstall $app?", "إزالة تثبيت $app؟")

    /** The Apps tabs' header over the apps the TV needs, and why they're apart. */
    val SYSTEM get() = L.t("System", "النظام")
    val SYSTEM_WHY get() = L.t("Needed by the TV · can't be changed", "يحتاجه التلفزيون · لا يمكن تغييره")

    /** Running, with no word from the TV. */
    val TV_OFFLINE get() = L.t("TV offline", "التلفزيون غير متصل")
    val RUNNING_OFFLINE get() = L.t("Running apps show here once the TV is connected.", "تظهر التطبيقات العاملة هنا بعد اتصال التلفزيون.")

    const val ANDROID_SYSTEM = AppNames.SYSTEM

    /** The word next to the status dot, so the state never rests on colour alone. */
    fun state(s: DeviceState) = when (s) {
        DeviceState.Online -> L.t("Connected", "متصل")
        DeviceState.Offline -> L.t("Offline", "غير متصل")
        DeviceState.NotAllowed -> L.t("Not allowed", "غير مسموح")
        DeviceState.Waiting -> L.t("Connecting", "جارٍ الاتصال")
        DeviceState.NeedsPairing -> L.t("Needs the pairing code", "يحتاج رمز الاقتران")
        DeviceState.Found -> L.t("Not connected", "لم يتصل بعد")
    }

    /** Under 10 GB a whole number hides too much: a 1.5 GB TV isn't a 2 GB one. */
    fun gbOf(used: Double, total: Double) =
        if (total < 10) "%.1f of %.1f GB".format(used, total) else "%.1f of %.0f GB".format(used, total)

    /** Free storage in plain words, rounded down so it never promises room that isn't there; null if unknown. */
    fun free(s: Snapshot): String? = free(s.storageUsedGb, s.storageTotalGb)

    /** [free] from the two numbers, for a TV's saved facts as well as a live reading. */
    fun free(used: Double?, total: Double?): String? {
        val gb = used?.let { u -> total?.let { t -> (t - u).coerceAtLeast(0.0) } } ?: return null
        return if (gb < 10) "%.1f GB free".format(Math.floor(gb * 10) / 10) else "${gb.toInt()} GB free"
    }

    /** An offline TV that has never sent stats; a refusing TV is on, it just hasn't allowed us. */
    fun noStats(s: DeviceState) =
        if (s == DeviceState.NotAllowed) L.t("Stats show up once the TV allows this phone.", "تظهر الإحصاءات عندما يسمح التلفزيون لهذا الهاتف.")
        else L.t("No stats yet. They show up once the TV is back on.", "لا إحصاءات بعد. تظهر عندما يعمل التلفزيون من جديد.")

    val WRONG_CODE get() = L.t("That code didn't work. Check the 6 digits on the TV and try again.", "لم يعمل هذا الرمز. تحقق من الأرقام الستة على التلفزيون وحاول مجدداً.")
    val UNREACHABLE get() = L.t("The TV didn't answer. Check it's on and on the same Wi-Fi as this phone, then try again.", "لم يرد التلفزيون. تأكد أنه يعمل وأنه على شبكة الواي فاي نفسها مع هذا الهاتف، ثم حاول مجدداً.")
    val PAIR_INSTEAD get() = L.t("The TV didn't let this phone in yet. Pair it once with the code from the TV.", "لم يُدخل التلفزيون هذا الهاتف بعد. اقترن به مرة واحدة بالرمز الذي يظهر على التلفزيون.")

    data class Health(val ok: Boolean, val text: String)

    /**
     * The one line at the top of a TV's page: the most pressing problem, or that all is well.
     * [heaviest] are app names by memory use, largest first; there is no per-app storage size yet.
     */
    fun health(s: Snapshot, heaviest: List<String>): Health {
        val storage = s.storageUsedGb?.let { u -> s.storageTotalGb?.takeIf { it > 0 }?.let { u / it } }
        val ram = if (s.ramTotalMb > 0) s.ramUsedMb.toDouble() / s.ramTotalMb else null
        val most = heaviest.take(2).takeIf { it.isNotEmpty() }?.let { L.t(": ${it.joinToString(" and ")} use the most", "؛ ${it.joinToString(" و ")} الأكثر استهلاكاً") }.orEmpty()
        return when {
            storage != null && storage >= FULL -> Health(false, L.t("Storage almost full", "المساحة شبه ممتلئة"))
            ram != null && ram >= FULL -> Health(false, L.t("Memory almost full$most", "الذاكرة شبه ممتلئة$most"))
            (s.tempC ?: 0) >= HOT_C -> Health(false, L.t("Running hot", "حرارة مرتفعة"))
            else -> Health(true, L.t("Running fine", "يعمل بشكل جيد"))
        }
    }

    const val FULL = 0.9
    private const val HOT_C = 80

    fun bytes(v: Long): String = when {
        v >= 1L shl 30 -> "%.1f GB".format(v / (1L shl 30).toDouble())
        v >= 1L shl 20 -> "%.1f MB".format(v / (1L shl 20).toDouble())
        v >= 1L shl 10 -> "${v shr 10} KB"
        else -> "$v bytes"
    }

    fun folder(f: TvFolder) = when (f) {
        TvFolder.DOWNLOAD -> L.t("Download", "التنزيلات")
        TvFolder.MOVIES -> L.t("Movies", "الأفلام")
    }

    // Sideload

    fun installButton(kind: InstallKind) = when (kind) {
        InstallKind.NEW -> L.t("Install", "تثبيت")
        InstallKind.UPDATE -> L.t("Update", "تحديث")
        InstallKind.REINSTALL -> L.t("Reinstall", "إعادة التثبيت")
        InstallKind.DOWNGRADE -> L.t("Install older version", "تثبيت نسخة أقدم")
    }

    fun installKind(kind: InstallKind, onTv: String?, picked: String?) = when (kind) {
        InstallKind.NEW -> L.t("Not on this TV yet. This installs it.", "غير موجود على هذا التلفزيون بعد. سيثبّته هذا.")
        InstallKind.UPDATE -> "Updates the TV's copy" + versions(onTv, picked) + ". Its settings stay."
        InstallKind.REINSTALL -> "The TV already has this version" + (onTv?.let { " ($it)" } ?: "") + ". This installs it again over itself."
        InstallKind.DOWNGRADE -> "This is older than the one on the TV" +
            (if (onTv != null && picked != null) " ($picked here, $onTv on the TV)" else "") +
            ". Android normally refuses that, and the app's saved data may not work with the older version."
    }

    private fun versions(onTv: String?, picked: String?) =
        if (onTv != null && picked != null) " from $onTv to $picked" else ""

    val ALLOW_DOWNGRADE get() = L.t("Install the older version anyway", "تثبيت النسخة الأقدم رغم ذلك")
    val INSTALL_KEEP_OPEN get() = L.t("Keep Momani Smart Transfer open until it's done.", "أبقِ Momani Smart Transfer مفتوحاً حتى ينتهي.")

    fun installed(kind: InstallKind, app: String) = when (kind) {
        InstallKind.UPDATE -> L.t("Updated $app.", "تم تحديث $app.")
        InstallKind.REINSTALL -> L.t("Reinstalled $app.", "أُعيد تثبيت $app.")
        InstallKind.NEW, InstallKind.DOWNGRADE -> L.t("Installed $app.", "تم تثبيت $app.")
    }

    fun installRefused(reason: InstallRefusal, app: String) = when (reason) {
        InstallRefusal.SIGNATURE ->
            L.t("The $app on the TV comes from a different developer, so Android won't replace it. If you're sure, uninstall it first: from Apps here if you installed it, or on the TV.", "نسخة $app على التلفزيون من مطوّر مختلف، فلن يستبدلها Android. إن كنت متأكداً فأزل تثبيتها أولاً: من التطبيقات هنا إن كنت أنت من ثبّتها، أو من التلفزيون.")
        InstallRefusal.DOWNGRADE -> L.t("The TV has a newer $app and won't go back to an older one.", "لدى التلفزيون نسخة أحدث من $app ولن يعود إلى نسخة أقدم.")
        InstallRefusal.NO_SPACE -> L.t("The TV is out of space. Disable or uninstall something there, then try again.", "لا مساحة في التلفزيون. عطّل شيئاً أو أزل تثبيته هناك ثم حاول مجدداً.")
        InstallRefusal.TV_SAID_NO ->
            L.t("The TV blocked the install. Look in the TV's security settings for installing apps from unknown sources, or for app verification, then try again.", "منع التلفزيون التثبيت. ابحث في إعدادات الأمان بالتلفزيون عن تثبيت التطبيقات من مصادر غير معروفة أو عن التحقق من التطبيقات، ثم حاول مجدداً.")
        InstallRefusal.TV_TOO_OLD -> "$app needs a newer version of Android than this TV has."
        InstallRefusal.WRONG_CPU ->
            L.t("This $app is built for a different kind of processor. Look for a version made for this TV, often marked armeabi-v7a or arm64-v8a.", "هذا $app مبني لنوع مختلف من المعالجات. ابحث عن نسخة مناسبة لهذا التلفزيون، وغالباً تحمل الاسم armeabi-v7a أو arm64-v8a.")
        InstallRefusal.APP_TOO_OLD -> "$app was made for a very old version of Android, and this TV won't install it."
        InstallRefusal.MISSING_FEATURE -> "$app needs something this TV doesn't have, like a touchscreen or a camera."
        InstallRefusal.MISSING_PARTS ->
            "$app comes in several parts and one is missing. Pick all of its .apk files together, or its .apks bundle."
        InstallRefusal.TEST_ONLY -> L.t("This is a developer test build of $app, which the TV won't install.", "هذه نسخة اختبار للمطورين من $app ولن يثبّتها التلفزيون.")
        InstallRefusal.CONFLICT -> L.t("Another app on the TV already uses something $app needs, so the TV refused it.", "يستخدم تطبيق آخر على التلفزيون شيئاً يحتاجه $app، فرفضه التلفزيون.")
        InstallRefusal.BROKEN -> L.t("This file isn't a working app. It may be damaged or only half downloaded; try getting it again.", "هذا الملف ليس تطبيقاً سليماً. قد يكون تالفاً أو لم يكتمل تنزيله؛ حاول الحصول عليه مجدداً.")
        InstallRefusal.UNKNOWN -> L.t("The TV didn't install $app.", "لم يثبّت التلفزيون $app.")
    }

    val NOT_AN_APP get() = L.t("That file isn't an Android app. Pick an .apk, several split .apk files, or an .apks or .xapk bundle.", "هذا الملف ليس تطبيق Android. اختر ملف apk. أو عدة ملفات apk. مجزأة أو حزمة apks. أو xapk.")
    val SIDELOAD_LOST get() = L.t("Lost the TV partway through. Check it's still on and connected, then try again.", "انقطع الاتصال بالتلفزيون في منتصف العملية. تأكد أنه يعمل ومتصل ثم حاول مجدداً.")

    // Files

    fun sent(name: String, savedAs: String, f: TvFolder) =
        if (savedAs == name) L.t("Sent $name to ${folder(f)}", "تم إرسال $name إلى ${folder(f)}")
        else L.t("Sent to ${folder(f)} as $savedAs, since $name was already there", "تم الإرسال إلى ${folder(f)} باسم $savedAs لأن $name موجود مسبقاً")

    fun saved(name: String) = L.t("Saved $name to your phone", "تم حفظ $name على هاتفك")

    /** One file, with what the SHA-256 check found. */
    fun sentChecked(name: String, savedAs: String, f: TvFolder, integrity: Integrity?) = sent(name, savedAs, f) + when (integrity) {
        is Integrity.Verified -> L.t(". Checked: the TV's copy matches (SHA-256).", ". تم التحقق: نسخة التلفزيون مطابقة (SHA-256).")
        is Integrity.Mismatch -> L.t(
            ". Warning: the TV's copy does NOT match what was sent. Delete it and send again.",
            ". تحذير: نسخة التلفزيون غير مطابقة لما أُرسل. احذفها وأعد الإرسال.",
        )
        is Integrity.Unchecked -> L.t(". Not checked: this TV can't compute SHA-256.", ". لم يتم التحقق: هذا التلفزيون لا يستطيع حساب SHA-256.")
        null -> ""
    }

    /** Several files. [bad] failed the check, [failed] didn't arrive, [unchecked] arrived without a check. */
    fun queueDone(sent: Int, failed: Int, bad: Int, unchecked: Int, f: TvFolder): String {
        val ar = L.arabic
        val parts = mutableListOf(
            if (ar) "الملفات المرسلة إلى ${folder(f)}: $sent"
            else "Sent $sent ${if (sent == 1) "file" else "files"} to ${folder(f)}",
        )
        if (bad > 0) parts += L.t("$bad did NOT match after sending: delete and resend them", "$bad لم تتطابق بعد الإرسال: احذفها وأعد إرسالها")
        if (unchecked > 0) parts += L.t("$unchecked couldn't be checked", "$unchecked تعذّر فحصها")
        if (failed > 0) parts += L.t("$failed failed: pick them again to retry", "$failed فشلت: اخترها من جديد لإعادة المحاولة")
        return parts.joinToString(". ") + "."
    }

    fun cancelledQueue(alreadySent: Int) =
        if (alreadySent == 0) CANCELLED_SEND
        else L.t(
            "Cancelled. $alreadySent ${if (alreadySent == 1) "file was" else "files were"} already sent; the one in progress was removed.",
            "تم الإلغاء. أُرسل $alreadySent من الملفات بالفعل، وأُزيل الملف الذي كان قيد الإرسال.",
        )

    /** "12 MB/s · about 2 min left", or null before there's enough to say. */
    fun rate(done: Long, total: Long?, startedAt: Long, now: Long): String? {
        val secs = (now - startedAt) / 1000.0
        if (startedAt <= 0 || secs < 2 || done <= 0) return null
        val perSec = done / secs
        val left = total?.takeIf { it > done }?.let { ((it - done) / perSec).toLong() }
        return bytes(perSec.toLong()) + L.t("/s", "/ث") + (left?.let {
            L.t(
                " · about ${if (it < 90) "$it s" else "${(it + 30) / 60} min"} left",
                " · المتبقي نحو ${if (it < 90) "$it ث" else "${(it + 30) / 60} د"}",
            )
        } ?: "")
    }

    fun sendFailed(name: String) = L.t(
        "Couldn't send $name. Check the TV is still connected and has space.",
        "تعذّر إرسال $name. تأكد أن التلفزيون ما زال متصلاً وفيه مساحة.",
    )
    fun saveFailed(name: String) = L.t(
        "Couldn't save $name. Check the TV is still connected and the phone has space.",
        "تعذّر حفظ $name. تأكد أن التلفزيون ما زال متصلاً وأن في الهاتف مساحة.",
    )

    val CANCELLED_SEND get() = L.t("Cancelled. Nothing was left on the TV.", "تم الإلغاء. لم يُترك أي شيء على التلفزيون.")
    val CANCELLED_SAVE get() = L.t("Cancelled. Nothing was saved.", "تم الإلغاء. لم يُحفظ أي شيء.")
    val CANT_READ get() = L.t("Couldn't read that file on your phone.", "تعذّرت قراءة هذا الملف على هاتفك.")
    val CANT_UNDO get() = L.t("This version of Momani Smart Transfer can't undo that setting.", "هذا الإصدار من Momani Smart Transfer لا يستطيع التراجع عن هذا الإعداد.")
    val FILES_LOST get() = L.t("Couldn't read this folder. Check the TV is still connected.", "تعذّرت قراءة هذا المجلد. تأكد أن التلفزيون ما زال متصلاً.")

    // Remote

    val KEY_REFUSED get() = L.t("The TV didn't take that key.", "لم يقبل التلفزيون هذا الزر.")
    val TYPE_REFUSED get() = L.t("The TV didn't take the text. Select a text box on the TV first, then send again.", "لم يقبل التلفزيون النص. اختر مربع نص على التلفزيون أولاً ثم أرسل مجدداً.")
    val REMOTE_LOST get() = L.t("Lost the TV. Check it's still on and connected.", "فُقد الاتصال بالتلفزيون. تأكد أنه يعمل ومتصل.")
    val NO_UPDATE_SCREEN get() = L.t("This TV has no update screen Momani Smart Transfer can open. Look in its Settings, under System or About.", "لا توجد في هذا التلفزيون شاشة تحديث يستطيع Momani Smart Transfer فتحها. ابحث في إعداداته ضمن النظام أو حول.")
    val UPDATES_OPENED get() = L.t("Opened the update screen on the TV. Look at the TV to check for updates.", "فُتحت شاشة التحديث على التلفزيون. انظر إلى التلفزيون للتحقق من التحديثات.")
    val REBOOT_REFUSED get() = L.t("The TV said no to restarting. Use its own remote or power button instead.", "رفض التلفزيون إعادة التشغيل. استخدم جهاز التحكم الخاص به أو زر التشغيل.")
    val REBOOT_IGNORED get() = L.t("The TV didn't restart. Use its own remote or power button instead.", "لم يعد التلفزيون التشغيل. استخدم جهاز التحكم الخاص به أو زر التشغيل.")
    val REMOTE_OFFLINE get() = L.t("The TV is offline. The remote works again once it reconnects.", "التلفزيون غير متصل. يعمل جهاز التحكم من جديد عند عودة الاتصال.")
    val TYPE_HINT get() = L.t("Select a text box on the TV, like a search box, then type here.", "اختر مربع نص على التلفزيون، مثل مربع البحث، ثم اكتب هنا.")
    val CLEAR_TV_TEXT get() = L.t("Clear the TV's text box", "مسح مربع النص في التلفزيون")
    val CLEAR_REFUSED get() = L.t("The TV's text box didn't clear. Select it on the TV first, then try again.", "لم يُمسح مربع النص في التلفزيون. اخترْه على التلفزيون أولاً ثم حاول مجدداً.")
    val SCREEN_OFF get() = L.t("Screen off", "إطفاء الشاشة")
    val SCREEN_ON get() = L.t("Turn the screen back on", "إعادة تشغيل الشاشة")
    val SCREEN_OFF_WAKE get() = L.t("tap any key to wake", "المس أي زر للإيقاظ")
    val SCREEN_OFF_TIP get() = L.t("Turns off the TV's picture. The app stays connected; any key wakes it.", "يطفئ صورة التلفزيون. يبقى التطبيق متصلاً؛ وأي زر يوقظه.")
    val SCREEN_OFF_REFUSED get() = L.t("This TV can't turn its picture off from Momani Smart Transfer.", "لا يستطيع هذا التلفزيون إطفاء صورته من Momani Smart Transfer.")
    val SCREEN_ON_REFUSED get() = L.t("The TV's picture didn't come back on. Try again, or restart the TV.", "لم تعد صورة التلفزيون. حاول مجدداً أو أعد تشغيل التلفزيون.")

    fun cantType(text: String): String {
        val odd = text.firstOrNull { it !in ' '..'~' }
        return when {
            text.isEmpty() -> L.t("Type something first.", "اكتب شيئاً أولاً.")
            text.length > app.tellyscope.adb.Remote.MAX_TEXT -> L.t("That's too long to send in one go. Send up to ${app.tellyscope.adb.Remote.MAX_TEXT} characters at a time.", "هذا النص أطول من أن يُرسل دفعة واحدة. أرسل حتى ${app.tellyscope.adb.Remote.MAX_TEXT} حرفاً في المرة.")
            odd == '\n' || odd == '\t' -> L.t("The TV can only take one line at a time.", "يستطيع التلفزيون استقبال سطر واحد فقط في المرة.")
            odd != null -> L.t("The TV can only type plain letters, numbers and symbols, not \u201c$odd\u201d.", "يستطيع التلفزيون كتابة الحروف والأرقام والرموز العادية فقط، وليس «$odd».")
            else -> L.t("The TV reads \u201c%s\u201d as a space, so it can't type that.", "يقرأ التلفزيون «%s» كمسافة، فلا يستطيع كتابتها.")
        }
    }

    val REBOOT_TITLE get() = L.t("Restart the TV?", "إعادة تشغيل التلفزيون؟")
    val REBOOT_TEXT get() = L.t("The TV turns off and on again. Momani Smart Transfer reconnects by itself when it's back, usually within a minute.", "يُطفأ التلفزيون ثم يعمل من جديد. يعيد Momani Smart Transfer الاتصال تلقائياً عند عودته، عادةً خلال دقيقة.")
    val RESTARTING_TITLE get() = L.t("Restarting…", "جارٍ إعادة التشغيل…")
    val RESTARTING_TEXT get() = L.t("The TV is restarting. Momani Smart Transfer will reconnect by itself.", "التلفزيون يعيد التشغيل. سيعيد Momani Smart Transfer الاتصال تلقائياً.")
    val RESTART_LATE_TITLE get() = L.t("The TV hasn't come back yet", "لم يعد التلفزيون بعد")
    val RESTART_LATE_STATUS get() = L.t("Not back yet", "لم يعد بعد")
    val RESTART_LATE_TEXT get() = L.t("It's been a minute. Some TVs take longer to start; Retry waits another minute.", "مرت دقيقة. بعض التلفزيونات تحتاج وقتاً أطول؛ «إعادة المحاولة» تنتظر دقيقة أخرى.")
    val RESTART_LOST_TEXT get() = L.t("Use the TV's own remote to turn it on, then tap Retry.", "استخدم جهاز التحكم الخاص بالتلفزيون لتشغيله، ثم اضغط «إعادة المحاولة».")

    fun waitingFor(name: String, ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return L.t("Waiting for $name · ${s / 60}:${(s % 60).toString().padStart(2, '0')}", "بانتظار $name · ${s / 60}:${(s % 60).toString().padStart(2, '0')}")
    }

    fun restartLost(name: String) = L.t("Still can't reach $name", "ما زال الوصول إلى $name متعذراً")
    fun backOnline(name: String) = L.t("Back online. $name reconnected.", "عاد الاتصال. أُعيد الاتصال بـ $name.")

    // Dock

    val ADD_TO_DOCK get() = L.t("Add to dock", "إضافة إلى الشريط")
    val DOCK_PICK_TITLE get() = L.t("Add an app to the dock", "إضافة تطبيق إلى الشريط")
    val DOCK_PICK_HINT get() = L.t("Tap an app to add it.", "المس تطبيقاً لإضافته.")
    val DOCK_ADDED get() = L.t("Added", "تمت الإضافة")
    val DOCK_NO_APPS get() = L.t("Still reading the TV's apps. Open this again in a moment.", "ما زلت أقرأ تطبيقات التلفزيون. افتح هذا مجدداً بعد لحظة.")
    val DOCK_CHANGE get() = L.t("Change app", "تغيير التطبيق")
    val DOCK_REMOVE get() = L.t("Remove from dock", "إزالة من الشريط")
    val DOCK_REORDER get() = L.t("Keep holding and move to reorder", "استمر بالضغط وحرّك لإعادة الترتيب")

    fun dockPickFrom(name: String) = L.t("Apps installed on $name", "التطبيقات المثبّتة على $name")
    fun dockSwap(label: String) = L.t("Put another app in $label's place", "ضع تطبيقاً آخر مكان $label")
    fun cantOpen(label: String) = L.t("The TV couldn't open $label. If it's turned off, turn it on in Apps.", "تعذّر على التلفزيون فتح $label. إن كان معطّلاً فشغّله من التطبيقات.")

    fun leftDock(labels: List<String>) =
        if (labels.size == 1) "${labels[0]} was uninstalled, so it left the dock."
        else "${labels.size} apps were uninstalled, so they left the dock."

    fun restarting(minutes: Long) =
        if (minutes < 3) L.t("Momani Smart Transfer reconnects on its own when the TV is back, usually within a couple of minutes.", "يعيد Momani Smart Transfer الاتصال تلقائياً عند عودة التلفزيون، عادةً خلال دقائق.")
        else L.t("Taking a while. Some TVs turn network debugging off when they restart. If it doesn't come back, turn it on again in the TV's Developer options.", "الأمر يطول. بعض التلفزيونات توقف تصحيح الأخطاء عبر الشبكة عند إعادة التشغيل. إن لم يعد الاتصال فشغّله مجدداً من خيارات المطوّر في التلفزيون.")

    const val TWEAKS = "TV tweaks"
    val DNS_TITLE get() = L.t("Block ads with Private DNS", "حظر الإعلانات بـ Private DNS")
    fun dnsState(host: String?) = host?.let { L.t("On: $it", "مفعّل: $it") } ?: L.t("Off. Turn on to block ads and trackers in every app on the TV.", "متوقف. شغّله لحظر الإعلانات والمتتبعات في كل تطبيق على التلفزيون.")
    val DNS_TOO_OLD get() = L.t("Needs Android 9 or newer. This TV is older.", "يحتاج Android 9 أو أحدث. هذا التلفزيون أقدم.")
    val DNS_DIALOG get() = L.t("Block ads on the whole TV?", "حظر الإعلانات على التلفزيون كله؟")
    const val DNS_TEXT = "The TV will look up every address through this ad-blocking DNS provider. AdGuard is free and needs no account. " +
        "Momani Smart Transfer then checks the TV can still look addresses up, and puts it back if not. Undo is in History."
    const val DNS_FIELD = "DNS provider"
    val DNS_BAD get() = L.t("Enter a hostname like dns.adguard-dns.com. An IP address won't work here.", "أدخل اسم مضيف مثل dns.adguard-dns.com. عنوان IP لا يصلح هنا.")
    val DNS_CONFIRM get() = L.t("Turn on", "تشغيل")
    val ANIMATIONS_TITLE get() = L.t("Faster animations", "حركات أسرع")
    fun animationsState(scale: String?) =
        if (scale == TvSettings.FAST) L.t("On: menus move at twice the speed", "مفعّل: القوائم تتحرك بضعف السرعة")
        else L.t("Off. Turn on to make menus feel snappier.", "متوقف. شغّله لتصبح القوائم أسرع استجابة.")
    val CACHES_TITLE get() = L.t("Clear all app caches", "مسح ذاكرة التخزين المؤقت لكل التطبيقات")
    val CACHES_TEXT get() = L.t("Frees space by clearing every app's temporary files. App data and sign-ins stay.", "يوفّر مساحة بمسح الملفات المؤقتة لكل تطبيق. تبقى بيانات التطبيقات وتسجيلات الدخول.")
    val CACHES_BUTTON get() = L.t("Clear caches", "مسح الذاكرة المؤقتة")
    fun cachesCleared(freedKb: Long?) =
        if (freedKb == null || freedKb <= 0) L.t("Cleared app caches", "تم مسح الذاكرة المؤقتة للتطبيقات") else L.t("Cleared app caches, freed ${bytes(freedKb * 1024)}", "تم مسح الذاكرة المؤقتة للتطبيقات، وتحرّر ${bytes(freedKb * 1024)}")
    val SCREENSHOT_TITLE get() = L.t("Screenshot", "لقطة شاشة")
    val SCREENSHOT_TEXT get() = L.t("Saves what the TV shows to your phone. Protected video, like most streaming apps, shows as black.", "يحفظ على هاتفك ما يعرضه التلفزيون. الفيديو المحمي، مثل أغلب تطبيقات البث، يظهر أسود.")
    val SCREENSHOT_BUTTON get() = L.t("Take screenshot", "التقاط لقطة")
    fun screenshotName(at: Long) =
        "TV screenshot " + SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.ROOT).format(Date(at)) + ".png"
    val SCREENSHOT_SAVED get() = L.t("Screenshot saved to your phone", "حُفظت لقطة الشاشة على هاتفك")
    val SCREENSHOT_FAILED get() = L.t("Couldn't take a screenshot. Check the TV is still connected.", "تعذّر التقاط لقطة الشاشة. تأكد أن التلفزيون ما زال متصلاً.")
    val REQUEST_TITLE get() = L.t("Another device wants to control this TV", "جهاز آخر يريد التحكم بهذا التلفزيون")
    const val REQUEST_TEXT = "The TV is asking whether to allow debugging from a device with this key fingerprint. " +
        "Allow it only if you started this yourself and the fingerprint matches the one that device shows."
    val REQUEST_ALWAYS get() = L.t("Always allow this device", "السماح دائماً لهذا الجهاز")
    val REQUEST_ALLOW get() = L.t("Allow", "سماح")
    val REQUEST_DENY get() = L.t("Deny", "رفض")
    val REQUEST_ALLOWED get() = L.t("Allowed. That device can control the TV now.", "تم السماح. يستطيع ذلك الجهاز التحكم بالتلفزيون الآن.")
    val REQUEST_DENIED get() = L.t("Denied. That device can't connect.", "تم الرفض. لا يستطيع ذلك الجهاز الاتصال.")
    val REQUEST_GONE get() = L.t("The TV's prompt had closed or changed, so nothing was pressed.", "أُغلقت رسالة التلفزيون أو تغيّرت، فلم يُضغط شيء.")

    // Who has access
    val WARNING_TITLE get() = L.t("Another computer is connected to this TV", "حاسوب آخر متصل بهذا التلفزيون")
    val WARNING_TEXT get() = L.t("It can do everything Momani Smart Transfer can: install or remove apps, read files and control the screen.", "يستطيع فعل كل ما يفعله Momani Smart Transfer: تثبيت التطبيقات أو إزالتها وقراءة الملفات والتحكم بالشاشة.")
    val ITS_MINE get() = L.t("It's mine", "هذا جهازي")
    val REMOVE_ACCESS get() = L.t("Remove all other access…", "إزالة كل وصول آخر…")
    val CONNECTED_NOW get() = L.t("Connected now", "متصل الآن")
    val WHO_HAS_ACCESS get() = L.t("Who has access", "من لديه وصول")
    fun othersConnected(n: Int) = if (n == 1) "1 other connected now" else "$n others connected now"
    fun connections(n: Int) = if (n == 1) "1 connection" else "$n connections"
    fun connectedFrom(address: String, n: Int) = "From $address · ${connections(n)}"
    val ON_TV get() = L.t("An app on this TV", "تطبيق على هذا التلفزيون")
    val ACCESS_INTRO get() = L.t("Each computer allowed to use ADB can install or remove apps, read files and control the screen.", "كل حاسوب مسموح له باستخدام ADB يستطيع تثبيت التطبيقات أو إزالتها وقراءة الملفات والتحكم بالشاشة.")
    val THIS_PHONE get() = L.t("This phone", "هذا الهاتف")
    const val TELLYSCOPE = "Momani Smart Transfer"
    val NOT_THIS_PHONE get() = L.t("Not this phone", "ليس هذا الهاتف")
    val YOURS get() = L.t("Yours", "جهازك")
    val ALLOWED_COMPUTERS get() = L.t("Allowed computers", "الحواسيب المسموح لها")
    val UNNAMED_COMPUTER get() = L.t("Unnamed computer", "حاسوب بلا اسم")
    val CANT_CHECK_CONNECTED get() = L.t("This TV doesn't say who is connected.", "هذا التلفزيون لا يذكر من المتصل به.")
    val CANT_CHECK_ALLOWED get() = L.t("This TV doesn't list its allowed computers.", "هذا التلفزيون لا يعرض قائمة الحواسيب المسموح لها.")
    val NOBODY_ALLOWED get() = L.t("No computer is allowed. The next one to connect has to be allowed on the TV.", "لا يوجد حاسوب مسموح. أول حاسوب يتصل لاحقاً يجب السماح له على التلفزيون.")
    val ACCESS_FOOT_LINK get() = L.t("The TV doesn't say which allowed computer is behind a connection. If you don't recognise an address, remove all other access.", "لا يذكر التلفزيون أي حاسوب مسموح وراء كل اتصال. إن لم تعرف عنواناً فأزل كل وصول آخر.")
    val ACCESS_FOOT_ALL get() = L.t("Android can't remove one computer on its own: this clears every computer, then you allow Momani Smart Transfer again.", "لا يستطيع Android إزالة حاسوب واحد وحده: هذا يمسح كل الحواسيب، ثم تسمح لـ Momani Smart Transfer من جديد.")
    /** Rough on purpose: the TV stamps a key when any connection uses it, not per session. */
    fun lastSeen(at: Long?, now: Long): String {
        if (at == null) return L.t("Never seen", "لم يُرَ قط")
        val days = ((now - at) / 86_400_000).coerceAtLeast(0)
        return when {
            now - at < 5 * 60_000 -> L.t("Last seen just now", "آخر ظهور: الآن")
            days == 0L -> L.t("Last seen today, ", "آخر ظهور اليوم، ") + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
            days == 1L -> L.t("Last seen yesterday", "آخر ظهور أمس")
            days < 30 -> L.t("Last seen $days days ago", "آخر ظهور منذ $days يوماً")
            else -> L.t("Last seen over a month ago", "آخر ظهور منذ أكثر من شهر")
        }
    }
    /** Enough of a fingerprint to tell keys apart: the first three and last two pairs. */
    fun shortFingerprint(fp: String): String {
        val parts = fp.split(':')
        return if (parts.size <= 5) fp else (parts.take(3) + "…" + parts.takeLast(2)).joinToString(":")
    }

    // Remove all other access
    val REMOVE_TITLE get() = L.t("Remove all other access", "إزالة كل وصول آخر")
    val CONFIRM_TITLE get() = L.t("Remove all other access?", "إزالة كل وصول آخر؟")
    val CONFIRM_TEXT get() = L.t("Every computer this TV has allowed loses access, your own computers too: they'll need allowing again.", "كل حاسوب سمح له هذا التلفزيون يفقد الوصول، حتى حواسيبك: ستحتاج السماح لها من جديد.")
    val CONFIRM_REMOTE get() = L.t("Momani Smart Transfer reconnects straight away. Keep the TV remote handy: the TV asks you to allow this phone.", "يعيد Momani Smart Transfer الاتصال فوراً. أبقِ جهاز التحكم قريباً: يطلب التلفزيون منك السماح لهذا الهاتف.")
    val CONFIRM_BUTTON get() = L.t("Remove access", "إزالة الوصول")
    val CANCEL get() = L.t("Cancel", "إلغاء")
    val REVOKING get() = L.t("Removing access…", "جارٍ إزالة الوصول…")
    val ALLOW_TITLE get() = L.t("Allow this phone on the TV", "السماح لهذا الهاتف على التلفزيون")
    const val ALLOW_TEXT_A = "Other access is removed. The TV now asks “Allow USB debugging?”. Tick "
    const val ALLOW_ALWAYS = "Always allow from this computer"
    const val ALLOW_TEXT_B = ", then choose "
    const val ALLOW_BUTTON = "Allow"
    val FINGERPRINT_LABEL get() = L.t("The TV should show this fingerprint", "يجب أن يعرض التلفزيون هذه البصمة")
    val FINGERPRINT_HINT get() = L.t("If it shows a different one, choose Cancel on the TV.", "إن عرض بصمة مختلفة فاختر «Cancel» على التلفزيون.")
    val STOP_WAITING get() = L.t("Stop waiting", "إيقاف الانتظار")
    val REFUSED get() = L.t("The TV didn't allow this phone. It may have timed out, or Cancel was chosen.", "لم يسمح التلفزيون لهذا الهاتف. ربما انتهت المهلة أو اختير «Cancel».")
    val ASK_AGAIN get() = L.t("Ask the TV again", "اسأل التلفزيون مرة أخرى")
    val CHECKING get() = L.t("Checking the TV…", "جارٍ فحص التلفزيون…")
    val DONE_TITLE get() = L.t("Only this phone has access", "هذا الهاتف وحده لديه وصول")
    val DONE_TEXT get() = L.t("Momani Smart Transfer checked the TV again: this phone is the only allowed computer, and nothing else is connected.", "فحص Momani Smart Transfer التلفزيون مجدداً: هذا الهاتف هو الحاسوب الوحيد المسموح، ولا شيء آخر متصل.")
    val TRUST_WARNING get() = L.t("Only allow computers and apps you trust. Anything you allow can install apps, read files and control the TV.", "اسمح فقط للحواسيب والتطبيقات التي تثق بها. أي شيء تسمح له يستطيع تثبيت التطبيقات وقراءة الملفات والتحكم بالتلفزيون.")
    fun backTo(tv: String) = L.t("Back to $tv", "العودة إلى $tv")
    val STILL_TITLE get() = L.t("Another computer is still connected", "ما زال حاسوب آخر متصلاً")
    fun stillText(who: String, tv: String) =
        L.t("Momani Smart Transfer still sees $who on $tv. It may have connected again straight away. Turn it off or unplug it from the network, then try again.", "ما زال Momani Smart Transfer يرى $who على $tv. ربما اتصل مجدداً فوراً. أطفئه أو افصله عن الشبكة ثم حاول مجدداً.")
    fun connectionFrom(address: String) = L.t("a connection from $address", "اتصال من $address")
    val TRY_AGAIN get() = L.t("Try again", "حاول مرة أخرى")
    val UNCONFIRMED_TITLE get() = L.t("Revoked on the TV", "أُلغي الوصول على التلفزيون")
    val UNCONFIRMED_TEXT get() = L.t("Momani Smart Transfer can't check the list on this TV. Look under Developer options to confirm.", "لا يستطيع Momani Smart Transfer فحص القائمة على هذا التلفزيون. راجع خيارات المطوّر للتأكد.")
    val DONE get() = L.t("Done", "تم")
    val MANUAL_INTRO get() = L.t("This TV didn't take Momani Smart Transfer's request, so it's two steps on the TV. Afterwards only this phone can use ADB on it; your own computers will need allowing again.", "لم يقبل هذا التلفزيون طلب Momani Smart Transfer، لذا هما خطوتان على التلفزيون. بعدها يستطيع هذا الهاتف وحده استخدام ADB عليه؛ وستحتاج حواسيبك السماح من جديد.")
    val MANUAL_STEP_1 get() = L.t("Open Developer options on the TV", "افتح خيارات المطوّر على التلفزيون")
    val MANUAL_STEP_1_TEXT get() = L.t("Momani Smart Transfer opens it for you. If nothing happens on the TV, go to Settings → Device Preferences → Developer options.", "يفتحها Momani Smart Transfer لك. إن لم يحدث شيء على التلفزيون فاذهب إلى الإعدادات ← تفضيلات الجهاز ← خيارات المطوّر.")
    val OPEN_ON_TV get() = L.t("Open on TV", "فتح على التلفزيون")
    const val MANUAL_STEP_2 = "Choose “Revoke USB debugging authorisations”"
    val MANUAL_STEP_2_TEXT get() = L.t("Then come back here, and Momani Smart Transfer asks the TV to allow this phone again.", "ثم عد إلى هنا ويطلب Momani Smart Transfer من التلفزيون السماح لهذا الهاتف من جديد.")
    val MANUAL_DONE get() = L.t("I've revoked it", "أزلتُ الوصول")
    val DEV_OPTIONS_FAILED get() = L.t("Couldn't open Developer options on the TV. Go to Settings → Device Preferences → Developer options.", "تعذّر فتح خيارات المطوّر على التلفزيون. اذهب إلى الإعدادات ← تفضيلات الجهاز ← خيارات المطوّر.")

    // Connected devices
    val DEVICES_TITLE get() = L.t("Connected devices", "الأجهزة المتصلة")
    fun showAll(n: Int) = L.t("Show all ($n)", "عرض الكل ($n)")
    val SHOW_LESS get() = L.t("Show less", "عرض أقل")
    val NO_DEVICES get() = L.t("Nothing plugged in or paired.", "لا شيء موصول أو مقترن.")
    fun group(g: Peripheral.Group) = when (g) {
        Peripheral.Group.Bluetooth -> "Bluetooth"
        Peripheral.Group.Input -> "Input"
        Peripheral.Group.Storage -> "USB storage"
        Peripheral.Group.Hdmi -> "HDMI-CEC"
        Peripheral.Group.Audio -> "Audio output"
    }
    /** The row's right-hand words. Collapsed rows have no group label, so Bluetooth says so there. */
    fun peripheralStatus(p: Peripheral, grouped: Boolean): String {
        val status = p.free?.let { "${bytes(it).replace(".0 ", " ")} free" } ?: p.status
        return if (!grouped && p.group == Peripheral.Group.Bluetooth) "Bluetooth · ${status.lowercase()}" else status
    }
}
