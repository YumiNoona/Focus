package dev.still.focus

import java.net.URI
import java.time.*
import java.util.UUID

// Keep stored enum names compatible with v1.0.0 profiles.
enum class FocusMode(val label: String, val waitSeconds: Int) {
    GENTLE("Normal", 0), STANDARD("Resistant", 30), STRICT("Strict", 0)
}
enum class BlockKind(val label: String) { APPS("Apps"), WEBSITES("Websites"), FEEDS("Feeds") }
enum class BlockPeriod(val label: String) {
    TODAY("Today"), DAY("1 day"), WEEK("1 week"), MANUAL("Until I unblock"), TIMER("Set minutes");
    fun deadline(now: Long, minutes: Int = 60, zone: ZoneId = ZoneId.systemDefault()): Long {
        val local = Instant.ofEpochMilli(now).atZone(zone)
        return when (this) {
            TODAY -> local.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            DAY -> local.plusDays(1).toInstant().toEpochMilli()
            WEEK -> local.plusWeeks(1).toInstant().toEpochMilli()
            MANUAL -> 0L
            TIMER -> now + minutes.coerceIn(1, 1440) * 60_000L
        }
    }
}
data class BlockPlan(val id: String = UUID.randomUUID().toString(), val kind: BlockKind,
    val targets: Set<String>, val mode: FocusMode = FocusMode.STANDARD,
    val start: Long, val end: Long = 0, val period: BlockPeriod = BlockPeriod.MANUAL) {
    fun alive(now: Long) = end == 0L || now < end
}
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
    val mode: FocusMode = FocusMode.STANDARD,
    val apps: Set<String> = emptySet(), val domains: Set<String> = emptySet(),
    val useAllApps: Boolean = apps.isEmpty(), val useAllDomains: Boolean = domains.isEmpty(),
    val art: String = "FOCUS")
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
    fun nextBoundary(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!active(now, zone)) return nextStart(now, zone)
        val current = Instant.ofEpochMilli(now).atZone(zone)
        val overnight = endMinute <= startMinute
        val endDate = if (overnight && current.hour * 60 + current.minute >= startMinute) current.toLocalDate().plusDays(1) else current.toLocalDate()
        return endDate.atTime(endMinute / 60, endMinute % 60).atZone(zone).toInstant().toEpochMilli()
    }
    fun overlaps(other: Schedule): Boolean = (1..7).any { day -> (0 until 1440).any { minute ->
        val now = LocalDate.of(2024, 1, 1).plusDays((day - 1).toLong()).atTime(minute / 60, minute % 60).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        active(now, ZoneOffset.UTC) && other.active(now, ZoneOffset.UTC)
    } }
}
data class Session(val id: String = UUID.randomUUID().toString(), val profile: Profile, val start: Long,
    val end: Long = 0, val intention: String = profile.intention, val timerOnly: Boolean = false) {
    fun alive(now: Long) = end == 0L || now < end
    fun remaining(now: Long): Long = if (end == 0L) 0 else (end - now).coerceAtLeast(0)
    fun focusedMillis(until: Long): Long = ((if (end > 0) minOf(until, end) else until) - start).coerceAtLeast(0)
}
data class SessionRecord(val name: String, val start: Long, val end: Long, val focusedMillis: Long, val completed: Boolean)
data class OverrideRecord(val packageName: String, val at: Long, val until: Long, val reason: String)
data class AppLifecycleRule(val packageName: String, val label: String,
    val lockUninstall: Boolean = false, val quarantineInstall: Boolean = false,
    val end: Long = 0) {
    fun alive(now: Long) = end == 0L || now < end
}
data class BlockMessage(val title: String = "See you later.",
    val body: String = "Make room for what matters.", val emoji: String = "🌿",
    val art: String = "", val image: String = "")
data class FocusState(val name: String = "", val intention: String = "Make room for what matters.",
    val goalMinutes: Int = 120, val onboarding: Boolean = false, val disclosure: Boolean = false,
    val theme: String = "Light", val haptics: Boolean = true, val reminders: Boolean = true,
    val hideInsights: Boolean = false, val reduceMotion: Boolean = false, val askReason: Boolean = false,
    val overrideMinutes: Int = 5, val cooldownMinutes: Int = 10,
    val managedLockdown: Boolean = false, val uninstallProtection: Boolean = false,
    val breakReminders: Boolean = true, val breakReminderMinutes: Int = 45,
    val blockMessage: BlockMessage = BlockMessage(), val lifecycleRules: List<AppLifecycleRule> = emptyList(),
    val apps: List<AppRule> = emptyList(), val domains: Set<String> = emptySet(),
    val feeds: Set<String> = emptySet(), val allowlist: Set<String> = emptySet(),
    val profiles: List<Profile> = defaults(), val schedules: List<Schedule> = emptyList(),
    val session: Session? = null, val blocks: List<BlockPlan> = emptyList(), val history: List<SessionRecord> = emptyList(),
    val overrides: List<OverrideRecord> = emptyList(), val avatarImage: String = "") {
    companion object { fun defaults() = listOf(
        Profile("deep", "Deep work", 50, "One thing at a time."),
        Profile("study", "Study", 50, "Make space to learn.", art = "STUDY"),
        Profile("sleep", "Wind down", 60, "Let the day settle.", art = "BREAK"),
        Profile("personal", "Personal time", 30, "Be here, now.", FocusMode.GENTLE, art = "PAUSE")) }
}
data class Restriction(val label: String, val explanation: String, val mode: FocusMode, val intention: String, val until: Long? = null)
object RulesEngine {
    fun hasActiveProtection(state: FocusState, now: Long): Boolean = state.blocks.any { it.alive(now) } ||
        state.lifecycleRules.any { it.alive(now) } || activeProfiles(state, now).any { selectedRuleCount(state, it) > 0 }

    /** Package removal never changes target IDs, deadlines, profiles, or cooldown history. */
    fun afterPackageChange(state: FocusState, pkg: String, now: Long): FocusState = state.copy(
        overrides = state.overrides.map { if (it.packageName == pkg && it.until > now) it.copy(until = now) else it })
    private fun profileDeadline(state: FocusState, profile: Profile, now: Long): Long? {
        val ends = mutableListOf<Long>()
        state.session?.takeIf { !it.timerOnly && it.alive(now) && it.profile.id == profile.id && it.profile.mode == profile.mode }?.let { ends += it.end }
        state.schedules.filter { it.active(now) && it.profileId == profile.id && state.profiles.any { p -> p.id == it.profileId && p.mode == profile.mode } }.forEach { it.nextBoundary(now)?.let(ends::add) }
        return if (0L in ends) 0L else ends.maxOrNull()
    }
    fun strictActive(state: FocusState, now: Long): Boolean =
        state.session?.let { !it.timerOnly && it.alive(now) && it.profile.mode == FocusMode.STRICT } == true ||
            state.blocks.any { it.mode == FocusMode.STRICT && it.alive(now) } ||
            state.schedules.any { s -> s.active(now) && state.profiles.any { it.id == s.profileId && it.mode == FocusMode.STRICT } }
    fun strictPackages(state: FocusState, now: Long): Set<String> = state.apps.filter { rule ->
        rule.enabled && rule.packageName !in state.allowlist && activeProfiles(state, now).any { it.mode == FocusMode.STRICT && (it.useAllApps || rule.packageName in it.apps) }
    }.map { it.packageName }.toSet() + state.blocks.filter { it.kind == BlockKind.APPS && it.mode == FocusMode.STRICT && it.alive(now) }
        .flatMap { it.targets }.filterNot { it in state.allowlist }
    fun selectedRuleCount(state: FocusState, profile: Profile): Int =
        selectedAppCount(state, profile) + selectedDomainCount(state, profile) + state.feeds.size
    fun selectedAppCount(state: FocusState, profile: Profile): Int =
        state.apps.count { it.enabled && it.packageName !in state.allowlist && (profile.useAllApps || it.packageName in profile.apps) }
    fun selectedDomainCount(state: FocusState, profile: Profile): Int =
        if (profile.useAllDomains) state.domains.size else profile.domains.count { it in state.domains }
    fun normalizeDomain(raw: String): String? = runCatching {
        val value = raw.trim().lowercase()
        if (value.isBlank() || value.any { it.isWhitespace() }) return null
        val uri = URI(if (value.contains("://")) value else "https://$value")
        val host = uri.host?.removePrefix("www.")?.trimEnd('.') ?: return null
        if (uri.scheme !in listOf("https", "http") || !host.contains('.') || !host.matches(Regex("[a-z0-9.-]+"))) null else host
    }.getOrNull()
    fun domainMatches(host: String, blocked: String): Boolean = host == blocked || host.endsWith(".$blocked")
    fun activeProfiles(state: FocusState, now: Long): List<Profile> = buildList {
        state.session?.takeIf { !it.timerOnly && it.alive(now) }?.let { add(it.profile) }
        state.schedules.filter { it.active(now) }.forEach { s -> state.profiles.find { it.id == s.profileId }?.let(::add) }
    }
    fun restriction(state: FocusState, pkg: String, now: Long, usedMillis: Long = 0, openingMillis: Long = 0,
        website: String? = null, feed: String? = null): Restriction? {
        if (pkg in state.allowlist) return null
        val candidates = mutableListOf<Restriction>()
        val profiles = activeProfiles(state, now)
        val rule = state.apps.find { it.packageName == pkg && it.enabled }
        val eligible = profiles.filter { it.useAllApps || pkg in it.apps }
        if (rule != null) {
            eligible.forEach { p -> candidates += Restriction(rule.label, "Protected by ${p.name}", p.mode, p.intention, profileDeadline(state, p, now)) }
            val allowance = rule.allowance(now)
            if (allowance > 0 && usedMillis >= allowance * 60_000L) candidates += Restriction(rule.label, "Daily allowance reached", FocusMode.STANDARD, state.intention, BlockPeriod.TODAY.deadline(now))
            if (rule.openingMinutes > 0 && openingMillis >= rule.openingMinutes * 60_000L) candidates += Restriction(rule.label, "Visit limit reached", FocusMode.STANDARD, state.intention)
        }
        if (website != null) {
            val host = normalizeDomain(website)
            val covering = profiles.filter { p -> (if (p.useAllDomains) state.domains else p.domains).any { host != null && domainMatches(host, it) } }
            covering.forEach { candidates += Restriction(host!!, "Website paused", it.mode, it.intention, profileDeadline(state, it, now)) }
        }
        if (feed != null && feed in state.feeds && profiles.isNotEmpty()) {
            profiles.forEach { p -> candidates += Restriction(feed, "Feed paused", p.mode, p.intention, profileDeadline(state, p, now)) }
        }
        val host = website?.let(::normalizeDomain)
        state.blocks.filter { it.alive(now) }.forEach { plan ->
            val label = when (plan.kind) {
                BlockKind.APPS -> if (pkg in plan.targets) rule?.label ?: pkg else null
                BlockKind.WEBSITES -> host?.takeIf { value -> plan.targets.any { domainMatches(value, it) } }
                BlockKind.FEEDS -> feed?.takeIf { it in plan.targets }
            }
            if (label != null) candidates += Restriction(label, if (plan.end == 0L) "Until you unblock" else "${plan.period.label} block", plan.mode, "Make room for what matters.", plan.end)
        }
        return candidates.maxWithOrNull(compareBy<Restriction> { it.mode.ordinal }.thenBy { if (it.until == null || it.until == 0L) Long.MAX_VALUE else it.until })?.let { overrideAware(state, pkg, now, it) }
    }
    private fun overrideAware(state: FocusState, pkg: String, now: Long, restriction: Restriction): Restriction? =
        if (restriction.mode != FocusMode.STRICT && state.overrides.any { it.packageName == pkg && now < it.until }) null else restriction
    fun canOverride(state: FocusState, pkg: String, now: Long, mode: FocusMode = FocusMode.STANDARD): Boolean {
        if (mode == FocusMode.STRICT || strictActive(state, now)) return false
        val cooldown = if (mode == FocusMode.STANDARD) maxOf(10, state.cooldownMinutes) else state.cooldownMinutes
        return state.overrides.filter { it.packageName == pkg }.maxOfOrNull { it.until }?.let { now >= it + cooldown * 60_000L } ?: true
    }
}

data class AppWindow(val packageName: String, val layer: Int, val focused: Boolean = false, val application: Boolean = true)
object ForegroundResolver {
    // Callers pass application windows only; IME, shade and accessibility overlays must not win.
    fun choose(windows: List<AppWindow>, fallback: String?): String? =
        windows.filter { it.application && it.packageName.isNotBlank() }.maxWithOrNull(compareBy<AppWindow> { it.focused }.thenBy { it.layer })?.packageName ?: fallback
}
