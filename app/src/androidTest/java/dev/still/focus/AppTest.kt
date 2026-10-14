package dev.still.focus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*

class AppTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun reset() { ui.runOnIdle { Repository.update { FocusState(onboarding = true) } } }
    @Test fun onboardingHasFourStepsAndSkipKeepsPermissionChecklist() {
        ui.runOnIdle { Repository.update { FocusState() } }
        ui.onNodeWithText("Less scroll.\nMore life.").assertIsDisplayed()
        ui.onNodeWithText("Next").performClick()
        ui.onNodeWithText("Find your\nrhythm.").assertIsDisplayed()
        ui.onNodeWithText("Back").performClick()
        ui.onNodeWithText("Skip intro").performClick()
        ui.onNodeWithText("Ready when you are.").assertExists()
        ui.onNodeWithContentDescription("Onboarding step 4 of 4").assertExists()
        ui.runOnIdle { assertFalse(Repository.state.onboarding) }
        ui.onNodeWithText("Continue with timers").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Home").assertExists()
        ui.runOnIdle { assertTrue(Repository.state.onboarding) }
    }
    @Test fun navigationHasExactlyThreeDockDestinationsAndHeaderActions() {
        ui.onNodeWithContentDescription("Home").assertExists()
        ui.onNodeWithContentDescription("Rules").assertExists().performClick()
        ui.onNodeWithText("Your time.\nYour rules.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Insights").performClick()
        ui.onNodeWithText("Notice your rhythm.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Profile").performClick()
        ui.onNodeWithText("Your space.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithContentDescription("Settings").performClick()
        ui.onNodeWithText("PROTECTION HEALTH").assertExists()
        ui.onNodeWithContentDescription("Rules").assertDoesNotExist()
    }
    @Test fun websiteEntryNormalizesAndPersists() {
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithText("Websites").performClick()
        ui.onNodeWithText("Website or domain").performTextInput("https://www.reddit.com/r/focus")
        ui.onNodeWithText("Add website").performClick()
        ui.onNodeWithText("reddit.com").assertExists()
        ui.runOnIdle { assertEquals(setOf("reddit.com"), Repository.state.domains) }
    }
    @Test fun timerOnlyWarningIsExplicitAndSessionStarts() {
        ui.onNodeWithText("Start focus").performClick()
        ui.onNodeWithText("Start focus").performScrollTo().performClick()
        ui.onNodeWithText("Blocking needs your consent and a connected Android accessibility service. You can enable it now, or explicitly start a timer without blocking.").assertExists()
        ui.onNodeWithText("Timer only").performClick()
        ui.onNodeWithText("Timer only · protection is off").assertExists()
        ui.runOnIdle { assertNotNull(Repository.state.session); Repository.stop() }
    }
    @Test fun rulesAndSessionSurviveSerialization() {
        ui.runOnIdle {
            Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test", dailyMinutes = 10)), domains = setOf("reddit.com"), blocks = listOf(BlockPlan(kind = BlockKind.WEBSITES, targets = setOf("reddit.com"), start = 0)), schedules = listOf(Schedule(name = "Night", profileId = "sleep", days = setOf(1, 2), startMinute = 1320, endMinute = 420)), blockMessage = BlockMessage("Don't open", "Be here", "✨"), lifecycleRules = listOf(AppLifecycleRule("com.instagram.android", "Instagram", quarantineInstall = true))) }
            val data = Repository.export()
            val decoded = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }.invoke(Repository, data) as FocusState
            assertEquals(Repository.state.apps, decoded.apps)
            assertEquals(Repository.state.domains, decoded.domains)
            assertEquals(Repository.state.schedules, decoded.schedules)
            assertEquals(Repository.state.blocks, decoded.blocks)
            assertEquals(Repository.state.blockMessage, decoded.blockMessage)
            assertEquals(Repository.state.lifecycleRules, decoded.lifecycleRules)
        }
    }
    @Test fun shortsShortcutOpensSingleFeedWithDurationChoices() {
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithText("Feeds").performClick()
        ui.onNodeWithText("Block Shorts").performScrollTo().performClick()
        ui.onNodeWithText("YouTube Shorts").assertExists()
        ui.onNodeWithText("1 week").assertExists()
        ui.onNodeWithText("Until I unblock").assertExists()
        ui.runOnIdle { assertEquals(setOf("YouTube Shorts"), Repository.state.feeds) }
    }
    @Test fun strictRejectsEarlyStopVisitsDeletionAndRuleWeakening() {
        ui.runOnIdle {
            val now = System.currentTimeMillis()
            val p = Profile("s", "Strict", 1, "Work", FocusMode.STRICT)
            Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Fixture")), session = Session(profile = p, start = now, end = now + 1800)) }
            assertFalse(Repository.stop())
            assertFalse(Repository.stop(completed = true))
            assertFalse(Repository.allow("dev.still.fixture", "escape", FocusMode.STRICT))
            assertFalse(Repository.deleteAll())
            assertFalse(Repository.update { it.copy(apps = emptyList(), disclosure = false) })
            assertEquals(1, Repository.state.apps.size)
        }
        Thread.sleep(2000)
        ui.runOnIdle { Repository.reconcile(); assertNull(Repository.state.session); assertTrue(Repository.update { it.copy(apps = emptyList()) }) }
    }
    @Test fun strictCannotStartAsOpenEndedOrTimerOnly() {
        ui.runOnIdle {
            val p = Profile("s", "Strict", 1, "Work", FocusMode.STRICT)
            assertFalse(Repository.start(p, 0, "Work"))
            assertFalse(Repository.start(p, 1, "Work", timerOnly = true))
            assertNull(Repository.state.session)
        }
    }
    @Test fun blockDraftSurvivesPermissionSetupAndManualCannotBeStrict() {
        ui.runOnIdle { Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test"))) } }
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithText("Block selected apps").performClick()
        ui.onNodeWithText("1 week").performClick().assertIsSelected()
        ui.onNodeWithText("Choose targets").performClick()
        ui.onNodeWithText("Clear").performClick()
        ui.onNodeWithText("Select all").performClick()
        ui.onNodeWithText("Connect protection").performScrollTo().performClick()
        ui.onNodeWithText("PROTECTION HEALTH").assertExists()
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithText("1 week").assertIsSelected()
        ui.onNodeWithText("Until I unblock").performScrollTo().performClick()
        ui.onNodeWithText("Strict").performScrollTo().assertIsNotEnabled()
    }
    @Test fun strictBlockRejectsDeletionAndRecoversAtDeadline() {
        ui.runOnIdle {
            val now = System.currentTimeMillis()
            val plan = BlockPlan(kind = BlockKind.APPS, targets = setOf("dev.still.fixture"), mode = FocusMode.STRICT, start = now, end = now + 1800)
            Repository.update { it.copy(blocks = listOf(plan)) }
            assertFalse(Repository.unblock(plan.id))
            assertFalse(Repository.deleteAll())
            assertFalse(Repository.update { it.copy(blocks = emptyList()) })
            assertFalse(Repository.allow("dev.still.fixture", "escape", FocusMode.STRICT))
        }
        Thread.sleep(2000)
        ui.runOnIdle { Repository.reconcile(); assertTrue(Repository.state.blocks.isEmpty()) }
    }
}
