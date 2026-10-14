package dev.still.focus

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class EngineTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: String, time: String) = LocalDateTime.parse("${day}T${time}:00").toInstant(zone).toEpochMilli()
    @Test fun overnightUsesPreviousDayAndEndIsExclusive() {
        val s = Schedule(name = "Sleep", profileId = "sleep", days = setOf(1), startMinute = 1320, endMinute = 420)
        assertTrue(s.active(at("2024-01-01", "23:00"), zone))
        assertTrue(s.active(at("2024-01-02", "06:59"), zone))
        assertFalse(s.active(at("2024-01-02", "07:00"), zone))
        assertFalse(s.active(at("2024-01-02", "23:00"), zone))
    }
    @Test fun overlappingAcrossSundayMidnightDetected() {
        val a = Schedule(name = "A", profileId = "deep", days = setOf(7), startMinute = 1380, endMinute = 60)
        val b = Schedule(name = "B", profileId = "deep", days = setOf(1), startMinute = 30, endMinute = 120)
        assertTrue(a.overlaps(b))
        assertFalse(a.overlaps(b.copy(startMinute = 60)))
    }
    @Test fun nextStartUsesSelectedWeekday() {
        val s = Schedule(name = "A", profileId = "deep", days = setOf(1), startMinute = 540, endMinute = 600)
        assertEquals(at("2024-01-08", "09:00"), s.nextStart(at("2024-01-01", "09:01"), zone))
    }
    @Test fun pomodoroBreakDoesNotCountAsFocusAndHasNoFinalBreak() {
        val p = Profile("p", "Study", 55, "Learn", pomodoro = true, rounds = 2)
        val s = Session(profile = p, start = 1000, end = 1000 + 55 * 60_000L)
        assertFalse(s.onBreak(1000 + 24 * 60_000L))
        assertTrue(s.onBreak(1000 + 25 * 60_000L))
        assertFalse(s.onBreak(1000 + 30 * 60_000L))
        assertEquals(50 * 60_000L, s.focusedMillis(s.end))
        assertFalse(s.alive(s.end))
    }
    @Test fun strictestOverlapWinsAndAllowlistAlwaysWins() {
        val now = System.currentTimeMillis()
        val p = Profile("p", "Strict", 50, "Work", FocusMode.STRICT)
        val state = FocusState(apps = listOf(AppRule("social", "Social")), profiles = listOf(p), session = Session(profile = p, start = now, end = now + 60_000))
        assertEquals(FocusMode.STRICT, RulesEngine.restriction(state, "social", now)?.mode)
        assertNull(RulesEngine.restriction(state.copy(allowlist = setOf("social")), "social", now))
        assertNull(RulesEngine.restriction(state, "phone", now))
    }
    @Test fun domainsRespectLabelBoundariesAndNormalizeUrls() {
        assertEquals("reddit.com", RulesEngine.normalizeDomain("https://www.reddit.com/r/study"))
        assertTrue(RulesEngine.domainMatches("old.reddit.com", "reddit.com"))
        assertFalse(RulesEngine.domainMatches("notreddit.com", "reddit.com"))
        assertNull(RulesEngine.normalizeDomain("hello world"))
        assertNull(RulesEngine.normalizeDomain("javascript:alert(1)"))
    }
    @Test fun dailyLimitAppliesOutsideFocusAndDisabledRuleDoesNot() {
        val state = FocusState(apps = listOf(AppRule("social", "Social", dailyMinutes = 10)))
        assertNull(RulesEngine.restriction(state, "social", 100, 9 * 60_000L))
        assertNotNull(RulesEngine.restriction(state, "social", 100, 10 * 60_000L))
        assertNull(RulesEngine.restriction(state.copy(apps = state.apps.map { it.copy(enabled = false) }), "social", 100, 10 * 60_000L))
    }
    @Test fun temporaryAccessExpiresAndCooldownFollowsVisit() {
        val state = FocusState(apps = listOf(AppRule("social", "Social", dailyMinutes = 1)), overrides = listOf(OverrideRecord("social", 100, 1000, "reply")), cooldownMinutes = 5)
        assertNull(RulesEngine.restriction(state, "social", 999, 60_000))
        assertNotNull(RulesEngine.restriction(state, "social", 1000, 60_000))
        assertFalse(RulesEngine.canOverride(state, "social", 1000))
        assertTrue(RulesEngine.canOverride(state, "social", 301_000))
    }
    @Test fun reductionStopsAtTargetAndCanPause() {
        val start = at("2024-01-01", "00:00")
        val rule = AppRule("social", "Social", dailyMinutes = 60, gradualTarget = 20, gradualStart = start)
        assertEquals(45, rule.allowance(at("2024-01-04", "00:00"), zone))
        assertEquals(20, rule.allowance(at("2024-02-01", "00:00"), zone))
        assertEquals(60, rule.copy(gradualPaused = true).allowance(at("2024-01-04", "00:00"), zone))
    }
    @Test fun profileAppScopeLimitsSessionRestrictions() {
        val p = Profile("p", "Work", 50, "Work", apps = setOf("a"))
        val state = FocusState(apps = listOf(AppRule("a", "A"), AppRule("b", "B")), session = Session(profile = p, start = 0, end = 1000))
        assertNotNull(RulesEngine.restriction(state, "a", 100))
        assertNull(RulesEngine.restriction(state, "b", 100))
    }
}
