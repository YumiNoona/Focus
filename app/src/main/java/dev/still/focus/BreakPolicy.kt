package dev.still.focus

import java.time.Instant
import java.time.ZoneId

data class BreakHistory(val day: String = "", val shown: Set<String> = emptySet(),
    val count: Int = 0, val lastAt: Long = 0, val quietUntil: Long = 0)

object BreakPolicy {
    fun choose(state: FocusState, usage: Map<String, Long>, eligible: Set<String>, history: BreakHistory,
        now: Long, zone: ZoneId = ZoneId.systemDefault(), foreground: String? = null): String? {
        val local = Instant.ofEpochMilli(now).atZone(zone)
        if (!state.breakReminders || local.hour !in 8..21 || now < history.quietUntil) return null
        val today = history.day == local.toLocalDate().toString()
        if (today && (history.count >= 3 || history.lastAt > 0 && now - history.lastAt < 2 * 3_600_000L)) return null
        return usage.filter { (pkg, millis) -> pkg in eligible && (!today || pkg !in history.shown) &&
            millis >= state.breakReminderMinutes.coerceIn(15, 180) * 60_000L &&
            RulesEngine.restriction(state, pkg, now, millis) == null }
            .keys.sortedWith(compareByDescending<String> { it == foreground }.thenByDescending { usage[it] }.thenBy { it }).firstOrNull()
    }
}
