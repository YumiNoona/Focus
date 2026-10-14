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
        assertFalse(RulesEngine.canOverride(state, "social", 301_000))
        assertTrue(RulesEngine.canOverride(state, "social", 601_000))
        assertTrue(RulesEngine.canOverride(state, "social", 301_000, FocusMode.GENTLE))
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
    @Test fun selectedCountsMatchTheEffectiveProfileScope() {
        val profile = Profile("p", "Work", 50, "Work", apps = setOf("a", "allowed"), domains = setOf("work.example", "missing.example"), useAllApps = false, useAllDomains = false)
        val state = FocusState(
            apps = listOf(AppRule("a", "A"), AppRule("disabled", "Disabled", enabled = false), AppRule("allowed", "Allowed")),
            domains = setOf("work.example", "other.example"), allowlist = setOf("allowed"), feeds = setOf("YouTube Shorts")
        )
        assertEquals(1, RulesEngine.selectedAppCount(state, profile))
        assertEquals(1, RulesEngine.selectedDomainCount(state, profile))
        assertEquals(3, RulesEngine.selectedRuleCount(state, profile))
    }
    @Test fun strictIgnoresAnExistingVisitAndNeverOffersAnother() {
        val profile = Profile("strict", "Strict", 10, "Work", FocusMode.STRICT)
        val state = FocusState(apps = listOf(AppRule("social", "Social")), session = Session(profile = profile, start = 0, end = 1000), overrides = listOf(OverrideRecord("social", 0, 900, "earlier visit")))
        assertEquals(FocusMode.STRICT, RulesEngine.restriction(state, "social", 100)?.mode)
        assertFalse(RulesEngine.canOverride(state, "social", 100, FocusMode.STRICT))
        assertTrue(RulesEngine.strictActive(state, 999))
        assertFalse(RulesEngine.strictActive(state, 1000))
    }
    @Test fun explicitEmptySubsetDoesNotSilentlyBecomeAllApps() {
        val profile = Profile("p", "Custom", 10, "Work", useAllApps = false, useAllDomains = false)
        val state = FocusState(apps = listOf(AppRule("social", "Social")), domains = setOf("example.com"), session = Session(profile = profile, start = 0, end = 1000))
        assertEquals(0, RulesEngine.selectedRuleCount(state, profile))
        assertNull(RulesEngine.restriction(state, "social", 100))
        assertNull(RulesEngine.restriction(state, "browser", 100, website = "example.com"))
    }
    @Test fun timerOnlyNeverActivatesAppBlocking() {
        val profile = FocusState.defaults().first()
        val state = FocusState(apps = listOf(AppRule("social", "Social")), session = Session(profile = profile, start = 0, end = 1000, timerOnly = true))
        assertNull(RulesEngine.restriction(state, "social", 100))
    }
    @Test fun keyboardAndOverlayWindowsDoNotReplaceTheForegroundApp() {
        val windows = listOf(AppWindow("social", 1), AppWindow("keyboard", 10, true, false), AppWindow("focus", 20, true, false))
        assertEquals("social", ForegroundResolver.choose(windows, "focus"))
        assertEquals("browser", ForegroundResolver.choose(listOf(AppWindow("browser", 2, true)), "social"))
        assertEquals("social", ForegroundResolver.choose(emptyList(), "social"))
    }
    @Test fun schedulePlansItsEndRatherThanOnlyTheNextStart() {
        val schedule = Schedule(name = "Night", profileId = "sleep", days = setOf(1), startMinute = 1320, endMinute = 420)
        assertEquals(at("2024-01-02", "07:00"), schedule.nextBoundary(at("2024-01-02", "06:00"), zone))
        assertEquals(at("2024-01-08", "22:00"), schedule.nextBoundary(at("2024-01-02", "07:00"), zone))
    }
    @Test fun strictDoesNotEnterPomodoroBreaks() {
        val profile = Profile("s", "Strict", 55, "Work", FocusMode.STRICT, pomodoro = true)
        val session = Session(profile = profile, start = 0, end = 3_300_000)
        assertFalse(session.onBreak(1_500_000))
        assertEquals(1_800_000L, session.phaseRemaining(1_500_000))
    }
    @Test fun independentAppBlocksWorkWithoutSessionAndExpireExactly() {
        val plan = BlockPlan(kind = BlockKind.APPS, targets = setOf("social"), start = 0, end = 1000, period = BlockPeriod.DAY)
        val state = FocusState(blocks = listOf(plan))
        assertNotNull(RulesEngine.restriction(state, "social", 999))
        assertNull(RulesEngine.restriction(state, "social", 1000))
        assertNull(RulesEngine.restriction(state, "other", 999))
        assertNotNull(RulesEngine.restriction(state.copy(blocks = listOf(plan.copy(end = 0, period = BlockPeriod.MANUAL))), "social", Long.MAX_VALUE))
    }
    @Test fun independentWebsiteAndFeedBlocksHavePreciseScope() {
        val website = BlockPlan(kind = BlockKind.WEBSITES, targets = setOf("example.com"), start = 0)
        val feed = BlockPlan(kind = BlockKind.FEEDS, targets = setOf("YouTube Shorts"), start = 0)
        val state = FocusState(blocks = listOf(website, feed))
        assertNotNull(RulesEngine.restriction(state, "browser", 100, website = "https://video.example.com/watch"))
        assertNull(RulesEngine.restriction(state, "browser", 100, website = "notexample.com"))
        assertNotNull(RulesEngine.restriction(state, "youtube", 100, feed = "YouTube Shorts"))
        assertNull(RulesEngine.restriction(state, "youtube", 100, feed = "Instagram Reels"))
    }
    @Test fun strictIndependentBlockWinsOverSessionAndEarlierVisit() {
        val plan = BlockPlan(kind = BlockKind.APPS, targets = setOf("social"), mode = FocusMode.STRICT, start = 0, end = 1000)
        val state = FocusState(apps = listOf(AppRule("social", "Social")), blocks = listOf(plan), session = Session(profile = Profile("normal", "Normal", 10, "Work", FocusMode.GENTLE), start = 0, end = 2000), overrides = listOf(OverrideRecord("social", 0, 1500, "old visit")))
        assertEquals(FocusMode.STRICT, RulesEngine.restriction(state, "social", 100)?.mode)
        assertTrue(RulesEngine.strictActive(state, 999))
        assertFalse(RulesEngine.strictActive(state, 1000))
        assertEquals(setOf("social"), RulesEngine.strictPackages(state, 999))
        assertEquals(1000L, RulesEngine.restriction(state, "social", 100)?.until)
        assertEquals(2000L, RulesEngine.restriction(state.copy(blocks = state.blocks + plan.copy(id = "later", end = 2000)), "social", 100)?.until)
        assertTrue(RulesEngine.strictPackages(state.copy(allowlist = setOf("social")), 999).isEmpty())
    }
    @Test fun periodsFollowCalendarDaysAndDaylightSaving() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2024, 3, 9, 12, 0, 0, 0, ny).toInstant().toEpochMilli()
        assertEquals(23 * 3_600_000L, BlockPeriod.DAY.deadline(now, zone = ny) - now)
        assertEquals(167 * 3_600_000L, BlockPeriod.WEEK.deadline(now, zone = ny) - now)
        assertEquals(ZonedDateTime.of(2024, 3, 10, 0, 0, 0, 0, ny).toInstant().toEpochMilli(), BlockPeriod.TODAY.deadline(now, zone = ny))
        assertEquals(0L, BlockPeriod.MANUAL.deadline(now))
        assertEquals(now + 15 * 60_000L, BlockPeriod.TIMER.deadline(now, 15))
    }
    @Test fun ordinaryBlocksHonorVisitsButCannotWeakenDailyLimit() {
        val plan = BlockPlan(kind = BlockKind.APPS, targets = setOf("social"), mode = FocusMode.GENTLE, start = 0)
        val state = FocusState(apps = listOf(AppRule("social", "Social", dailyMinutes = 1)), blocks = listOf(plan))
        assertEquals(FocusMode.STANDARD, RulesEngine.restriction(state, "social", 100, 60_000)?.mode)
        assertNull(RulesEngine.restriction(state.copy(overrides = listOf(OverrideRecord("social", 0, 200, "reply"))), "social", 100, 60_000))
    }
}
