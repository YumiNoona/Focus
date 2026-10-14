package dev.still.focus

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
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
        ready = true
    }
    @Synchronized fun update(transform: (FocusState) -> FocusState) {
        state = transform(state)
        context.getSharedPreferences("still", 0).edit().putString("state", encode(state).toString()).apply()
        FocusWidget.refresh(context)
    }
    fun start(profile: Profile, minutes: Int, intention: String) {
        reconcile()
        if (state.session != null) return
        val now = System.currentTimeMillis()
        val duration = if (profile.pomodoro) (profile.workMinutes * profile.rounds + profile.breakMinutes * (profile.rounds - 1)) * 60_000L else minutes * 60_000L
        update { it.copy(session = Session(profile = profile, start = now, end = if (duration == 0L) 0 else now + duration, intention = intention.ifBlank { profile.intention })) }
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
    }
    fun stop(completed: Boolean = false) {
        val current = state.session ?: return
        val now = if (completed && current.end > 0) current.end else System.currentTimeMillis()
        update { it.copy(session = null, history = (it.history + SessionRecord(current.profile.name, current.start, now, current.focusedMillis(now), completed)).takeLast(1000)) }
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
    }
    fun reconcile() {
        val s = state.session ?: return
        if (!s.alive(System.currentTimeMillis())) {
            stop(true)
            if (state.reminders) FocusNotifications.message(context, "A little more room.", "Your ${s.profile.name.lowercase()} session is complete.")
        }
    }
    fun allow(pkg: String, reason: String): Boolean {
        val now = System.currentTimeMillis()
        if (!RulesEngine.canOverride(state, pkg, now)) return false
        update { it.copy(overrides = (it.overrides + OverrideRecord(pkg, now, now + it.overrideMinutes * 60_000L, reason.trim())).takeLast(1000)) }
        return true
    }
    fun deleteAll() {
        update { FocusState(onboarding = true) }
        FocusAccessibilityService.instance?.clearOverlay()
        FocusNotifications.session(context)
        FocusNotifications.plan(context)
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
    fun export(): String = encode(state).put("formatVersion", 1).put("exportedAt", System.currentTimeMillis()).toString(2)
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }.toSet()
    private fun <T> JSONArray.items(parse: (JSONObject) -> T) = (0 until length()).map { parse(getJSONObject(it)) }
    private fun JSONObject.array(key: String) = optJSONArray(key) ?: JSONArray()
    private fun JSONObject.mode() = runCatching { FocusMode.valueOf(optString("mode", "STANDARD")) }.getOrDefault(FocusMode.STANDARD)
    private fun profile(p: Profile) = JSONObject().put("id", p.id).put("name", p.name).put("minutes", p.minutes).put("intention", p.intention).put("mode", p.mode.name).put("pomodoro", p.pomodoro).put("workMinutes", p.workMinutes).put("breakMinutes", p.breakMinutes).put("rounds", p.rounds).put("apps", JSONArray(p.apps.toList())).put("domains", JSONArray(p.domains.toList()))
    private fun parseProfile(j: JSONObject) = Profile(j.getString("id"), j.getString("name"), j.optInt("minutes", 50), j.optString("intention"), j.mode(), j.optBoolean("pomodoro"), j.optInt("workMinutes", 25), j.optInt("breakMinutes", 5), j.optInt("rounds", 4), j.array("apps").strings(), j.array("domains").strings())
    private fun encode(s: FocusState): JSONObject = JSONObject()
        .put("name", s.name).put("intention", s.intention).put("goalMinutes", s.goalMinutes).put("onboarding", s.onboarding).put("disclosure", s.disclosure)
        .put("theme", s.theme).put("haptics", s.haptics).put("reminders", s.reminders).put("hideInsights", s.hideInsights).put("reduceMotion", s.reduceMotion).put("askReason", s.askReason)
        .put("overrideMinutes", s.overrideMinutes).put("cooldownMinutes", s.cooldownMinutes)
        .put("apps", JSONArray(s.apps.map { JSONObject().put("packageName", it.packageName).put("label", it.label).put("enabled", it.enabled).put("dailyMinutes", it.dailyMinutes).put("openingMinutes", it.openingMinutes).put("gradualTarget", it.gradualTarget).put("gradualStart", it.gradualStart).put("gradualPaused", it.gradualPaused) }))
        .put("domains", JSONArray(s.domains.toList())).put("feeds", JSONArray(s.feeds.toList())).put("allowlist", JSONArray(s.allowlist.toList()))
        .put("profiles", JSONArray(s.profiles.map(::profile)))
        .put("schedules", JSONArray(s.schedules.map { JSONObject().put("id", it.id).put("name", it.name).put("profileId", it.profileId).put("days", JSONArray(it.days.toList())).put("startMinute", it.startMinute).put("endMinute", it.endMinute).put("enabled", it.enabled).put("reminder", it.reminder) }))
        .put("session", s.session?.let { JSONObject().put("id", it.id).put("profile", profile(it.profile)).put("start", it.start).put("end", it.end).put("intention", it.intention) } ?: JSONObject.NULL)
        .put("history", JSONArray(s.history.map { JSONObject().put("name", it.name).put("start", it.start).put("end", it.end).put("focusedMillis", it.focusedMillis).put("completed", it.completed) }))
        .put("overrides", JSONArray(s.overrides.map { JSONObject().put("packageName", it.packageName).put("at", it.at).put("until", it.until).put("reason", it.reason) }))
    private fun decode(raw: String): FocusState {
        val j = JSONObject(raw)
        return FocusState(name = j.optString("name"), intention = j.optString("intention", "Make room for what matters."), goalMinutes = j.optInt("goalMinutes", 120), onboarding = j.optBoolean("onboarding"), disclosure = j.optBoolean("disclosure"),
            theme = j.optString("theme", "System"), haptics = j.optBoolean("haptics", true), reminders = j.optBoolean("reminders", true), hideInsights = j.optBoolean("hideInsights"), reduceMotion = j.optBoolean("reduceMotion"), askReason = j.optBoolean("askReason", true), overrideMinutes = j.optInt("overrideMinutes", 5), cooldownMinutes = j.optInt("cooldownMinutes", 10),
            apps = j.array("apps").items { AppRule(it.getString("packageName"), it.getString("label"), it.optBoolean("enabled", true), it.optInt("dailyMinutes"), it.optInt("openingMinutes"), it.optInt("gradualTarget"), it.optLong("gradualStart"), it.optBoolean("gradualPaused")) },
            domains = j.array("domains").strings(), feeds = j.array("feeds").strings(), allowlist = j.array("allowlist").strings(),
            profiles = if (j.has("profiles")) j.array("profiles").items(::parseProfile) else FocusState.defaults(),
            schedules = j.array("schedules").items { Schedule(it.getString("id"), it.getString("name"), it.getString("profileId"), it.array("days").stringsOrInts(), it.getInt("startMinute"), it.getInt("endMinute"), it.optBoolean("enabled", true), it.optBoolean("reminder")) },
            session = j.optJSONObject("session")?.let { Session(it.getString("id"), parseProfile(it.getJSONObject("profile")), it.getLong("start"), it.getLong("end"), it.optString("intention")) },
            history = j.array("history").items { SessionRecord(it.getString("name"), it.getLong("start"), it.getLong("end"), it.getLong("focusedMillis"), it.getBoolean("completed")) },
            overrides = j.array("overrides").items { OverrideRecord(it.getString("packageName"), it.getLong("at"), it.getLong("until"), it.optString("reason")) })
    }
    private fun JSONArray.stringsOrInts() = (0 until length()).map { getInt(it) }.toSet()
}

object UsageReader {
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
