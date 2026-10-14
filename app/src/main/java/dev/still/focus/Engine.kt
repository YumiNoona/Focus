package dev.still.focus

import java.net.URI
import java.time.*
import java.util.UUID

enum class FocusMode { GENTLE, STANDARD, STRICT }
data class AppRule(val packageName: String, val label: String, val enabled: Boolean = true,
    val dailyMinutes: Int = 0, val openingMinutes: Int = 0, val gradualTarget: Int = 0,
    val gradualStart: Long = 0, val gradualPaused: Boolean = false) {
    fun allowance(now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (dailyMinutes == 0 || gradualTarget == 0 || gradualPaused || gradualStart == 0L) return dailyMinutes
        val days = java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochMilli(gradualStart).atZone(zone).toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate()).coerceAtLeast(0)
        return (dailyMinutes - days.toInt() * 5).coerceAtLeast(gradualTarget.coerceAtMost(dailyMinutes))
    }
}
data class Profile(val id: String, val name: String, val minutes: Int, val intention: String,
    val mode: FocusMode = FocusMode.STANDARD, val pomodoro: Boolean = false,
    val workMinutes: Int = 25, val breakMinutes: Int = 5, val rounds: Int = 4,
    val apps: Set<String> = emptySet(), val domains: Set<String> = emptySet())
data class Schedule(val id: String = UUID.randomUUID().toString(), val name: String,
    val profileId: String, val days: Set<Int>, val startMinute: Int, val endMinute: Int,
    val enabled: Boolean = true, val reminder: Boolean = false) {
    fun active(now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!enabled) return false
        val date = Instant.ofEpochMilli(now).atZone(zone)
        val minute = date.hour * 60 + date.minute
        return if (endMinute > startMinute) date.dayOfWeek.value in days && minute in startMinute until endMinute
        else (date.dayOfWeek.value in days && minute >= startMinute) || (date.minusDays(1).dayOfWeek.value in days && minute < endMinute)
    }
    fun nextStart(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!enabled || days.isEmpty()) return null
        val current = Instant.ofEpochMilli(now).atZone(zone)
        return (0..7).map { offset -> current.toLocalDate().plusDays(offset.toLong()).atTime(startMinute / 60, startMinute % 60).atZone(zone) }
            .firstOrNull { it.dayOfWeek.value in days && it.toInstant().toEpochMilli() > now }?.toInstant()?.toEpochMilli()
    }
    fun overlaps(other: Schedule): Boolean = (1..7).any { day -> (0 until 1440).any { minute ->
        val now = LocalDate.of(2024, 1, 1).plusDays((day - 1).toLong()).atTime(minute / 60, minute % 60).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        active(now, ZoneOffset.UTC) && other.active(now, ZoneOffset.UTC)
    } }
}
data class Session(val id: String = UUID.randomUUID().toString(), val profile: Profile, val start: Long,
    val end: Long = 0, val intention: String = profile.intention) {
    fun alive(now: Long) = end == 0L || now < end
    fun onBreak(now: Long): Boolean {
        if (!profile.pomodoro || !alive(now)) return false
        val cycle = (profile.workMinutes + profile.breakMinutes) * 60_000L
        return ((now - start).coerceAtLeast(0) % cycle) >= profile.workMinutes * 60_000L
    }
    fun phaseRemaining(now: Long): Long {
        if (!profile.pomodoro) return if (end == 0L) 0 else (end - now).coerceAtLeast(0)
        val cycle = (profile.workMinutes + profile.breakMinutes) * 60_000L
        val elapsed = (now - start).coerceAtLeast(0) % cycle
        val boundary = if (elapsed < profile.workMinutes * 60_000L) profile.workMinutes * 60_000L else cycle
        return (boundary - elapsed).coerceAtMost((end - now).coerceAtLeast(0))
    }
    fun focusedMillis(until: Long): Long {
        val elapsed = ((if (end > 0) minOf(until, end) else until) - start).coerceAtLeast(0)
        if (!profile.pomodoro) return elapsed
        val work = profile.workMinutes * 60_000L
        val cycle = work + profile.breakMinutes * 60_000L
        return elapsed / cycle * work + minOf(elapsed % cycle, work)
    }
}
data class SessionRecord(val name: String, val start: Long, val end: Long, val focusedMillis: Long, val completed: Boolean)
data class OverrideRecord(val packageName: String, val at: Long, val until: Long, val reason: String)
data class FocusState(val name: String = "", val intention: String = "Make room for what matters.",
    val goalMinutes: Int = 120, val onboarding: Boolean = false, val disclosure: Boolean = false,
    val theme: String = "System", val haptics: Boolean = true, val reminders: Boolean = true,
    val hideInsights: Boolean = false, val reduceMotion: Boolean = false, val askReason: Boolean = true,
    val overrideMinutes: Int = 5, val cooldownMinutes: Int = 10,
    val apps: List<AppRule> = emptyList(), val domains: Set<String> = emptySet(),
    val feeds: Set<String> = emptySet(), val allowlist: Set<String> = emptySet(),
    val profiles: List<Profile> = defaults(), val schedules: List<Schedule> = emptyList(),
    val session: Session? = null, val history: List<SessionRecord> = emptyList(),
    val overrides: List<OverrideRecord> = emptyList()) {
    companion object { fun defaults() = listOf(
        Profile("deep", "Deep work", 50, "One thing at a time."),
        Profile("study", "Study", 100, "Make space to learn.", pomodoro = true),
        Profile("sleep", "Wind down", 60, "Let the day settle."),
        Profile("personal", "Personal time", 30, "Be here, now.", FocusMode.GENTLE)) }
}
data class Restriction(val label: String, val explanation: String, val mode: FocusMode, val intention: String)
object RulesEngine {
    fun normalizeDomain(raw: String): String? = runCatching {
        val value = raw.trim().lowercase()
        if (value.isBlank() || value.any { it.isWhitespace() }) return null
        val uri = URI(if (value.contains("://")) value else "https://$value")
        val host = uri.host?.removePrefix("www.")?.trimEnd('.') ?: return null
        if (uri.scheme !in listOf("https", "http") || !host.contains('.') || !host.matches(Regex("[a-z0-9.-]+"))) null else host
    }.getOrNull()
    fun domainMatches(host: String, blocked: String): Boolean = host == blocked || host.endsWith(".$blocked")
    fun activeProfiles(state: FocusState, now: Long): List<Profile> = buildList {
        state.session?.takeIf { it.alive(now) && !it.onBreak(now) }?.let { add(it.profile) }
        state.schedules.filter { it.active(now) }.forEach { s -> state.profiles.find { it.id == s.profileId }?.let(::add) }
    }
    fun restriction(state: FocusState, pkg: String, now: Long, usedMillis: Long = 0, openingMillis: Long = 0,
        website: String? = null, feed: String? = null): Restriction? {
        if (pkg in state.allowlist) return null
        if (state.overrides.any { it.packageName == pkg && now < it.until }) return null
        val profiles = activeProfiles(state, now)
        val rule = state.apps.find { it.packageName == pkg && it.enabled }
        val eligible = profiles.filter { it.apps.isEmpty() || pkg in it.apps }
        val effective = eligible.maxByOrNull { it.mode.ordinal }
        if (rule != null) {
            if (effective != null) return Restriction(rule.label, "Protected by ${effective.name}", effective.mode, state.session?.intention ?: effective.intention)
            val allowance = rule.allowance(now)
            if (allowance > 0 && usedMillis >= allowance * 60_000L) return Restriction(rule.label, "Your $allowance minute daily allowance is complete.", FocusMode.STANDARD, state.intention)
            if (rule.openingMinutes > 0 && openingMillis >= rule.openingMinutes * 60_000L) return Restriction(rule.label, "Your ${rule.openingMinutes} minute visit is complete.", FocusMode.STANDARD, state.intention)
        }
        if (website != null) {
            val host = normalizeDomain(website)
            val covering = profiles.filter { p -> (if (p.domains.isEmpty()) state.domains else p.domains).any { host != null && domainMatches(host, it) } }
            covering.maxByOrNull { it.mode.ordinal }?.let { return Restriction(host!!, "This website is on your focus blocklist.", it.mode, state.session?.intention ?: it.intention) }
        }
        if (feed != null && feed in state.feeds && profiles.isNotEmpty()) {
            val p = profiles.maxBy { it.mode.ordinal }
            return Restriction(feed, "This feed is paused during focus time.", p.mode, state.session?.intention ?: p.intention)
        }
        return null
    }
    fun canOverride(state: FocusState, pkg: String, now: Long): Boolean = state.overrides.filter { it.packageName == pkg }.maxOfOrNull { it.until }?.let { now >= it + state.cooldownMinutes * 60_000L } ?: true
}
