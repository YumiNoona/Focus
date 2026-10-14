package dev.still.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.*

object Repository {
    lateinit var context: Context
    var state by mutableStateOf(FocusState()); private set
    private var ready = false
    fun init(ctx: Context) {
        if (ready) return
        context = ctx.applicationContext
        state = runCatching { decode(context.getSharedPreferences("still", 0).getString("state", "{}")!!) }.getOrDefault(FocusState())
        state.session?.takeIf { it.profile.mode == FocusMode.STRICT && RulesEngine.selectedRuleCount(state, it.profile) == 0 }?.let { legacy ->
            state = state.copy(session = legacy.copy(profile = legacy.profile.copy(mode = FocusMode.STANDARD)))
            context.getSharedPreferences("still", 0).edit().putString("state", encode(state).toString()).apply()
        }
        ready = true
        androidx.core.content.ContextCompat.registerReceiver(context, AppLifecycleReceiver(), android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
        }, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    @Synchronized fun update(transform: (FocusState) -> FocusState): Boolean {
        val next = transform(state)
        if (AdminPin.configured() && !AdminPin.authorized() &&
            (next.managedLockdown != state.managedLockdown || next.uninstallProtection != state.uninstallProtection ||
                next.lifecycleRules != state.lifecycleRules && next.lifecycleRules != state.lifecycleRules.filter { it.alive(System.currentTimeMillis()) } ||
                state.disclosure && !next.disclosure)) return fail("Unlock device controls with your FocuLY PIN first.")
        if (RulesEngine.strictActive(state, System.currentTimeMillis())) {
            val protected = state.copy(name = next.name, intention = next.intention, goalMinutes = next.goalMinutes,
                theme = next.theme, haptics = next.haptics, reminders = next.reminders, hideInsights = next.hideInsights, reduceMotion = next.reduceMotion,
                history = next.history, session = if (state.session?.alive(System.currentTimeMillis()) == false) next.session else state.session,
                lifecycleRules = if (next.lifecycleRules == state.lifecycleRules.filter { it.alive(System.currentTimeMillis()) }) next.lifecycleRules else state.lifecycleRules,
                blocks = if (next.blocks.filter { it.alive(System.currentTimeMillis()) } == state.blocks.filter { it.alive(System.currentTimeMillis()) }) next.blocks else state.blocks)
            if (protected != next) return fail("Strict rules stay locked until their timer or schedule ends.")
        }
        state = next
        context.getSharedPreferences("still", 0).edit().putString("state", encode(state).toString()).apply()
        FocusWidget.refresh(context)
        return true
    }
    fun block(kind: BlockKind, targets: Set<String>, period: BlockPeriod, mode: FocusMode, minutes: Int = 60): Boolean {
        reconcile()
        if (!protectionReady()) return fail("Enable FocuLY protection before blocking.")
        val eligible = when (kind) {
            BlockKind.APPS -> state.apps.filter { it.enabled && it.packageName !in state.allowlist && !FocusAccessibilityService.essential(context, it.packageName) }.map { it.packageName }.toSet()
            BlockKind.WEBSITES -> state.domains
            BlockKind.FEEDS -> state.feeds
        }
        if (targets.isEmpty() || !eligible.containsAll(targets)) return fail("Choose at least one available target.")
        if (period == BlockPeriod.TIMER && minutes !in 1..1440) return fail("Choose 1–1440 minutes.")
        if (mode == FocusMode.STRICT && period == BlockPeriod.MANUAL) return fail("Strict needs an automatic ending. Choose a day, week, or timer.")
        if (mode == FocusMode.STRICT && state.managedLockdown && (kind != BlockKind.APPS || !ManagedProtection.isOwner() || !ManagedProtection.preciseEnding())) return fail("Managed Strict needs whole-app targets and precise ending permission.")
        val now = System.currentTimeMillis()
        val existing = state.blocks.find { it.kind == kind && it.targets == targets && it.mode == mode && it.period == period }
        if (existing == null && state.blocks.size >= 100) return fail("Unblock an existing group before adding another.")
        val plan = BlockPlan(id = existing?.id ?: java.util.UUID.randomUUID().toString(), kind = kind, targets = targets.toSet(), mode = mode, start = now, end = period.deadline(now, minutes), period = period)
        val previousBlocks = state.blocks
        if (!update { it.copy(blocks = it.blocks.filterNot { item -> item.id == plan.id } + plan) }) return false
        if (!ManagedProtection.sync()) {
            state = state.copy(blocks = previousBlocks)
            context.getSharedPreferences("still", 0).edit().putString("state", encode(state).toString()).commit()
            ManagedProtection.sync()
            return fail("Android could not apply managed blocking. Check protection health.")
        }
        FocusNotifications.plan(context)
        return true
    }
    private val unblockRequests = mutableMapOf<String, Long>()
    fun beginUnblock(id: String) { unblockRequests[id] = android.os.SystemClock.elapsedRealtime() }
    fun unblock(id: String, reason: String = ""): Boolean {
        val plan = state.blocks.find { it.id == id } ?: return true
        if (plan.mode == FocusMode.STRICT && plan.alive(System.currentTimeMillis())) return fail("Strict stays locked until its deadline.")
        if (plan.mode == FocusMode.STANDARD && plan.alive(System.currentTimeMillis()) && (reason.isBlank() || android.os.SystemClock.elapsedRealtime() - (unblockRequests[id] ?: android.os.SystemClock.elapsedRealtime()) < 30_000)) return fail("Pause for 30 seconds and add a reason before unblocking.")
        if (!update { it.copy(blocks = it.blocks.filterNot { item -> item.id == id }) }) return false
        unblockRequests.remove(id)
        ManagedProtection.sync(); FocusNotifications.plan(context)
        return true
    }
    private fun fail(message: String): Boolean { android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }; return false }
    fun protectionReady(ctx: Context = context) = state.disclosure && protectionGranted(ctx) && ProtectionRuntime.live()
    fun openAppInfo() { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun openBatterySettings() { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun start(profile: Profile, minutes: Int, intention: String, timerOnly: Boolean = false): Boolean {
        reconcile()
        if (state.session != null) return fail("End your existing session first.")
        if (minutes !in 0..1440) return fail("Choose a duration from 0 to 1440 minutes.")
        if (profile.mode == FocusMode.STRICT && (minutes == 0 || timerOnly || profile.pomodoro)) return fail("Strict needs a finite timer and has no breaks or timer-only option.")
        if (!timerOnly && !protectionReady()) return fail("Protection is not connected. Enable accessibility and allow background activity before starting blocking.")
        if (!timerOnly && RulesEngine.selectedRuleCount(state, profile) == 0) return fail("Choose at least one effective app, website, or feed rule first.")
        if (profile.mode == FocusMode.STRICT && state.managedLockdown && (!ManagedProtection.isOwner() || !ManagedProtection.preciseEnding())) return fail("Managed lockdown needs device-owner setup and reliable ending permission.")
        val now = System.currentTimeMillis()
        val duration = if (profile.pomodoro) (profile.workMinutes * profile.rounds + profile.breakMinutes * (profile.rounds - 1)) * 60_000L else minutes * 60_000L
        if (profile.mode == FocusMode.STRICT && state.managedLockdown && RulesEngine.strictPackages(state.copy(session = Session(profile = profile, start = now, end = now + duration)), now).none { !FocusAccessibilityService.essential(context, it) }) return fail("Managed Strict needs at least one whole-app rule. Website and feed detection alone are not OS-enforced.")
        if (!update { it.copy(session = Session(profile = profile, start = now, end = if (duration == 0L) 0 else now + duration, intention = intention.ifBlank { profile.intention }, timerOnly = timerOnly)) }) return false
        if (!ManagedProtection.sync()) {
            // A failed policy application must not masquerade as a successful lockdown.
            state = state.copy(session = null)
            context.getSharedPreferences("still", 0).edit().putString("state", encode(state).toString()).commit()
            ManagedProtection.sync()
            return fail("Managed rules could not be applied. Check protection health.")
        }
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
        return true
    }
    fun stop(completed: Boolean = false): Boolean {
        val current = state.session ?: return true
        if (current.profile.mode == FocusMode.STRICT && current.alive(System.currentTimeMillis())) return fail("Strict sessions cannot end early.")
        val now = if (completed && current.end > 0) current.end else System.currentTimeMillis()
        if (!update { it.copy(session = null, history = (it.history + SessionRecord(current.profile.name, current.start, now, current.focusedMillis(now), completed)).takeLast(1000)) }) return false
        ManagedProtection.sync()
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
        return true
    }
    fun reconcile() {
        val livingPolicies = state.lifecycleRules.filter { it.alive(System.currentTimeMillis()) }
        if (livingPolicies != state.lifecycleRules && update { it.copy(lifecycleRules = livingPolicies) }) FocusNotifications.plan(context)
        val expired = state.blocks.filterNot { it.alive(System.currentTimeMillis()) }
        if (expired.isNotEmpty() && update { it.copy(blocks = it.blocks - expired.toSet()) }) {
            expired.forEach { unblockRequests.remove(it.id) }
            FocusNotifications.plan(context)
        }
        val s = state.session
        if (s != null && !s.alive(System.currentTimeMillis())) {
            stop(true)
            if (state.reminders) FocusNotifications.message(context, "A little more room.", "Your ${s.profile.name.lowercase()} session is complete.")
        }
        ManagedProtection.sync()
    }
    fun allow(pkg: String, reason: String, mode: FocusMode = FocusMode.STANDARD): Boolean {
        val now = System.currentTimeMillis()
        if (!RulesEngine.canOverride(state, pkg, now, mode) || (mode == FocusMode.STANDARD && reason.isBlank())) return false
        val minutes = if (mode == FocusMode.STANDARD) 1 else state.overrideMinutes
        return update { it.copy(overrides = (it.overrides + OverrideRecord(pkg, now, now + minutes * 60_000L, reason.trim())).takeLast(1000)) }
    }
    fun deleteAll(): Boolean {
        if (AdminPin.configured() && !AdminPin.authorized()) return fail("Unlock device controls before deleting data.")
        if (RulesEngine.strictActive(state, System.currentTimeMillis())) return fail("Local rules cannot be cleared during Strict mode.")
        update { FocusState(onboarding = true) }; ManagedProtection.sync()
        FocusAccessibilityService.instance?.clearOverlay()
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
        return true
    }
    fun usageGranted(ctx: Context = context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) == AppOpsManager.MODE_ALLOWED
    }
    fun protectionGranted(ctx: Context = context): Boolean {
        val manager = ctx.getSystemService(AccessibilityManager::class.java)
        return manager.getEnabledAccessibilityServiceList(-1).any { it.resolveInfo.serviceInfo.packageName == ctx.packageName }
    }
    fun openAccessibility() { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun openUsage() { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun export(): String = encode(state).put("formatVersion", 2).put("exportedAt", System.currentTimeMillis()).toString(2)
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }.toSet()
    private fun <T> JSONArray.items(parse: (JSONObject) -> T) = (0 until length()).map { parse(getJSONObject(it)) }
    private fun JSONObject.array(key: String) = optJSONArray(key) ?: JSONArray()
    private fun JSONObject.mode() = runCatching { FocusMode.valueOf(optString("mode", "STANDARD")) }.getOrDefault(FocusMode.STANDARD)
    private fun profile(p: Profile) = JSONObject().put("id", p.id).put("name", p.name).put("minutes", p.minutes).put("intention", p.intention).put("mode", p.mode.name).put("pomodoro", p.pomodoro).put("workMinutes", p.workMinutes).put("breakMinutes", p.breakMinutes).put("rounds", p.rounds).put("apps", JSONArray(p.apps.toList())).put("domains", JSONArray(p.domains.toList())).put("useAllApps", p.useAllApps).put("useAllDomains", p.useAllDomains)
    private fun parseProfile(j: JSONObject) = Profile(j.getString("id"), j.getString("name"), j.optInt("minutes", 50), j.optString("intention"), j.mode(), j.optBoolean("pomodoro"), j.optInt("workMinutes", 25), j.optInt("breakMinutes", 5), j.optInt("rounds", 4), j.array("apps").strings(), j.array("domains").strings(), j.optBoolean("useAllApps", j.array("apps").length() == 0), j.optBoolean("useAllDomains", j.array("domains").length() == 0))
    private fun encode(s: FocusState): JSONObject = JSONObject()
        .put("name", s.name).put("intention", s.intention).put("goalMinutes", s.goalMinutes).put("onboarding", s.onboarding).put("disclosure", s.disclosure)
        .put("theme", s.theme).put("haptics", s.haptics).put("reminders", s.reminders).put("hideInsights", s.hideInsights).put("reduceMotion", s.reduceMotion).put("askReason", s.askReason)
        .put("overrideMinutes", s.overrideMinutes).put("cooldownMinutes", s.cooldownMinutes)
        .put("managedLockdown", s.managedLockdown).put("uninstallProtection", s.uninstallProtection)
        .put("blockMessage", JSONObject().put("title", s.blockMessage.title).put("body", s.blockMessage.body).put("emoji", s.blockMessage.emoji))
        .put("lifecycleRules", JSONArray(s.lifecycleRules.map { JSONObject().put("packageName", it.packageName).put("label", it.label).put("lockUninstall", it.lockUninstall).put("quarantineInstall", it.quarantineInstall).put("end", it.end) }))
        .put("apps", JSONArray(s.apps.map { JSONObject().put("packageName", it.packageName).put("label", it.label).put("enabled", it.enabled).put("dailyMinutes", it.dailyMinutes).put("openingMinutes", it.openingMinutes).put("gradualTarget", it.gradualTarget).put("gradualStart", it.gradualStart).put("gradualPaused", it.gradualPaused) }))
        .put("domains", JSONArray(s.domains.toList())).put("feeds", JSONArray(s.feeds.toList())).put("allowlist", JSONArray(s.allowlist.toList()))
        .put("profiles", JSONArray(s.profiles.map(::profile)))
        .put("schedules", JSONArray(s.schedules.map { JSONObject().put("id", it.id).put("name", it.name).put("profileId", it.profileId).put("days", JSONArray(it.days.toList())).put("startMinute", it.startMinute).put("endMinute", it.endMinute).put("enabled", it.enabled).put("reminder", it.reminder) }))
        .put("session", s.session?.let { JSONObject().put("id", it.id).put("profile", profile(it.profile)).put("start", it.start).put("end", it.end).put("intention", it.intention).put("timerOnly", it.timerOnly) } ?: JSONObject.NULL)
        .put("blocks", JSONArray(s.blocks.map { JSONObject().put("id", it.id).put("kind", it.kind.name).put("targets", JSONArray(it.targets.toList())).put("mode", it.mode.name).put("start", it.start).put("end", it.end).put("period", it.period.name) }))
        .put("history", JSONArray(s.history.map { JSONObject().put("name", it.name).put("start", it.start).put("end", it.end).put("focusedMillis", it.focusedMillis).put("completed", it.completed) }))
        .put("overrides", JSONArray(s.overrides.map { JSONObject().put("packageName", it.packageName).put("at", it.at).put("until", it.until).put("reason", it.reason) }))
    private fun decode(raw: String): FocusState {
        val j = JSONObject(raw)
        return FocusState(name = j.optString("name"), intention = j.optString("intention", "Make room for what matters."), goalMinutes = j.optInt("goalMinutes", 120), onboarding = j.optBoolean("onboarding"), disclosure = j.optBoolean("disclosure"),
            theme = j.optString("theme", "System"), haptics = j.optBoolean("haptics", true), reminders = j.optBoolean("reminders", true), hideInsights = j.optBoolean("hideInsights"), reduceMotion = j.optBoolean("reduceMotion"), askReason = j.optBoolean("askReason", false), overrideMinutes = j.optInt("overrideMinutes", 5), cooldownMinutes = j.optInt("cooldownMinutes", 10),
            managedLockdown = j.optBoolean("managedLockdown"), uninstallProtection = j.optBoolean("uninstallProtection"),
            blockMessage = j.optJSONObject("blockMessage")?.let { BlockMessage(it.optString("title", "See you later.").take(80), it.optString("body", "Make room for what matters.").take(180), it.optString("emoji", "🌿").take(16)) } ?: BlockMessage(),
            lifecycleRules = j.array("lifecycleRules").items { AppLifecycleRule(it.getString("packageName"), it.getString("label"), it.optBoolean("lockUninstall"), it.optBoolean("quarantineInstall"), it.optLong("end")) },
            apps = j.array("apps").items { AppRule(it.getString("packageName"), it.getString("label"), it.optBoolean("enabled", true), it.optInt("dailyMinutes"), it.optInt("openingMinutes"), it.optInt("gradualTarget"), it.optLong("gradualStart"), it.optBoolean("gradualPaused")) },
            domains = j.array("domains").strings(), feeds = j.array("feeds").strings(), allowlist = j.array("allowlist").strings(),
            blocks = j.array("blocks").items {
                val end = it.optLong("end")
                val mode = it.mode().let { mode -> if (mode == FocusMode.STRICT && end == 0L) FocusMode.STANDARD else mode }
                BlockPlan(it.getString("id"), BlockKind.valueOf(it.getString("kind")), it.array("targets").strings(), mode, it.getLong("start"), end, BlockPeriod.valueOf(it.optString("period", "MANUAL")))
            },
            profiles = if (j.has("profiles")) j.array("profiles").items(::parseProfile) else FocusState.defaults(),
            schedules = j.array("schedules").items { Schedule(it.getString("id"), it.getString("name"), it.getString("profileId"), it.array("days").stringsOrInts(), it.getInt("startMinute"), it.getInt("endMinute"), it.optBoolean("enabled", true), it.optBoolean("reminder")) },
            session = j.optJSONObject("session")?.let {
                val original = parseProfile(it.getJSONObject("profile"))
                // v1.0 allowed open-ended Strict sessions with an exit. Do not migrate them into permanent locks.
                val profile = if (it.getLong("end") == 0L && original.mode == FocusMode.STRICT) original.copy(mode = FocusMode.STANDARD) else original
                Session(it.getString("id"), profile, it.getLong("start"), it.getLong("end"), it.optString("intention"), it.optBoolean("timerOnly"))
            },
            history = j.array("history").items { SessionRecord(it.getString("name"), it.getLong("start"), it.getLong("end"), it.getLong("focusedMillis"), it.getBoolean("completed")) },
            overrides = j.array("overrides").items { OverrideRecord(it.getString("packageName"), it.getLong("at"), it.getLong("until"), it.optString("reason")) })
    }
    private fun JSONArray.stringsOrInts() = (0 until length()).map { getInt(it) }.toSet()
}

object UsageReader {
    fun foreground(ctx: Context): String? = runCatching {
        if (!Repository.usageGranted(ctx)) return null
        val now = System.currentTimeMillis()
        val events = ctx.getSystemService(UsageStatsManager::class.java).queryEvents(now - 30_000, now)
        val event = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) { events.getNextEvent(event); if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) pkg = event.packageName }
        pkg
    }.getOrNull()
    fun day(ctx: Context, date: LocalDate = LocalDate.now()): Map<String, Long> {
        if (!Repository.usageGranted(ctx)) return emptyMap()
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), System.currentTimeMillis())
        if (end <= start) return emptyMap()
        return runCatching {
            val events = ctx.getSystemService(UsageStatsManager::class.java).queryEvents(start - 86_400_000L, end)
            val event = UsageEvents.Event()
            val totals = mutableMapOf<String, Long>()
            var foreground: String? = null
            var since = start
            fun close(time: Long) {
                foreground?.let { pkg -> totals[pkg] = (totals[pkg] ?: 0L) + (time - maxOf(since, start)).coerceAtLeast(0) }
                foreground = null
            }
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> { close(event.timeStamp); foreground = event.packageName; since = event.timeStamp }
                    UsageEvents.Event.ACTIVITY_PAUSED -> if (event.packageName == foreground) close(event.timeStamp)
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> close(event.timeStamp)
                }
            }
            close(end)
            totals.filterValues { it > 0 }
        }.getOrDefault(emptyMap())
    }
}
