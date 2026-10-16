package dev.still.focus

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class BreakPolicyTest {
    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
    private val usage = mapOf("social" to 50 * 60_000L, "video" to 90 * 60_000L)
    @Test fun remindsAboutUnconfiguredAppsWithoutCreatingRules() {
        val state = FocusState()
        assertEquals("social", BreakPolicy.choose(state, usage, usage.keys, BreakHistory(), now, zone, "social"))
        assertTrue(state.apps.isEmpty()); assertTrue(state.blocks.isEmpty())
        assertNull(BreakPolicy.choose(state.copy(breakReminders = false), usage, usage.keys, BreakHistory(), now, zone))
        assertNull(BreakPolicy.choose(state.copy(breakReminderMinutes = 120), usage, usage.keys, BreakHistory(), now, zone))
    }
    @Test fun respectsQuietHoursSnoozeAndEligibility() {
        assertNull(BreakPolicy.choose(FocusState(), usage, emptySet(), BreakHistory(), now, zone))
        for (hour in listOf(0, 7, 22, 23)) assertNull(BreakPolicy.choose(FocusState(), usage, usage.keys, BreakHistory(),
            Instant.parse("2026-10-16T%02d:00:00Z".format(hour)).toEpochMilli(), zone))
        assertNull(BreakPolicy.choose(FocusState(), usage, usage.keys, BreakHistory(quietUntil = now + 1), now, zone))
    }
    @Test fun limitsPerAppAndAcrossAppsAndResetsNextDay() {
        val history = BreakHistory("2026-10-16", setOf("video"), 1, now - 3 * 3_600_000L)
        assertEquals("social", BreakPolicy.choose(FocusState(), usage, usage.keys, history, now, zone))
        assertNull(BreakPolicy.choose(FocusState(), usage, usage.keys, history.copy(count = 3), now, zone))
        assertNull(BreakPolicy.choose(FocusState(), usage, usage.keys, history.copy(lastAt = now - 60_000), now, zone))
        assertEquals("video", BreakPolicy.choose(FocusState(), usage, usage.keys, history.copy(count = 3), now + 86_400_000, zone))
    }
    @Test fun skipsAppsAlreadyCoveredByABlock() {
        val state = FocusState(blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf("video"), start = now - 1)))
        assertEquals("social", BreakPolicy.choose(state, usage, usage.keys, BreakHistory(), now, zone))
    }
    @Test fun reinstallEndsVisitButRetainsBansDeadlinesAndCooldown() {
        val day = BlockPlan(kind = BlockKind.APPS, targets = setOf("social"), start = now - 1, end = now + 86_400_000, period = BlockPeriod.DAY)
        val state = FocusState(apps = listOf(AppRule("social", "Social", dailyMinutes = 20)), blocks = listOf(day),
            overrides = listOf(OverrideRecord("social", now - 10, now + 60_000, "reply")))
        val next = RulesEngine.afterPackageChange(state, "social", now)
        assertEquals(state.apps, next.apps); assertEquals(state.blocks, next.blocks)
        assertEquals(now, next.overrides.single().until)
        assertNotNull(RulesEngine.restriction(next, "social", now))
        assertFalse(RulesEngine.canOverride(next, "social", now, FocusMode.STANDARD))
        assertTrue(RulesEngine.hasActiveProtection(next, now))
        assertFalse(RulesEngine.hasActiveProtection(next.copy(blocks = emptyList()), now))
    }
}
