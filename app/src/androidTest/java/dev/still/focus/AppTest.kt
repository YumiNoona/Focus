package dev.still.focus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*

class AppTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun reset() { ui.runOnIdle { Repository.update { FocusState(onboarding = true) } } }
    @Test fun chosenIllustrationSurvivesRoundTripAndLegacyProfilesKeepTheirDefault() {
        ui.runOnIdle {
            Repository.update { it.copy(profiles = listOf(Profile("study", "Study", 50, "Learn", art = "FOCUS"))) }
            val decode = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }
            val data = Repository.export()
            assertEquals("FOCUS", (decode.invoke(Repository, data) as FocusState).profiles.single().art)
            val legacy = org.json.JSONObject(data)
            legacy.getJSONArray("profiles").getJSONObject(0).remove("art")
            assertEquals("STUDY", (decode.invoke(Repository, legacy.toString()) as FocusState).profiles.single().art)
        }
    }
    @Test fun appRuleDraftAndNavigationSurviveActivityRecreation() {
        ui.runOnIdle { Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test"))) } }
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithContentDescription("Edit Scroll Test").performClick()
        ui.onNodeWithText("Daily & visit limits").performScrollTo().performClick()
        ui.onNodeWithText("Daily allowance (minutes)").performScrollTo().performTextReplacement("42")
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Daily allowance (minutes)").performScrollTo().assertTextContains("42")
        ui.onNodeWithText("Save app rules").performScrollTo().performClick()
        ui.runOnIdle { assertEquals(42, Repository.state.apps.single().dailyMinutes) }
    }
    @Test fun routineDraftRetainsIdentityAndDaysAfterRecreation() {
        val saved = Schedule(id = "existing-routine", name = "Study hours", profileId = "study", days = setOf(1, 2, 3), startMinute = 540, endMinute = 600, enabled = false)
        ui.runOnIdle { Repository.update { it.copy(schedules = listOf(saved)) } }
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithText("Schedules").performClick()
        ui.onNodeWithText("Edit routine").performScrollTo().performClick()
        ui.onNodeWithText("Routine name").performTextReplacement("Weekend reading")
        ui.onNodeWithText("Weekend").performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Weekend").assertIsSelected()
        ui.onNodeWithText("Save routine").performScrollTo().performClick()
        ui.runOnIdle {
            val routine = Repository.state.schedules.single()
            assertEquals(saved.id, routine.id)
            assertEquals("Weekend reading", routine.name)
            assertEquals(setOf(6, 7), routine.days)
            assertFalse(routine.enabled)
        }
    }
    @Test fun emptyProfilesOfferRecoveryInsteadOfCrashingRoutineSetup() {
        ui.runOnIdle { Repository.update { it.copy(profiles = emptyList()) } }
        ui.onNodeWithContentDescription("Rules").performClick()
        ui.onNodeWithText("Schedules").performClick()
        ui.onNodeWithText("Add a routine").performClick()
        ui.onNodeWithText("Save routine").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("Create a profile").performScrollTo().performClick()
        ui.onNodeWithText("Your profiles").assertIsDisplayed()
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithText("Routine name").assertExists()
    }
    @Test fun editingProfilePreservesItsIdentityAndArtAfterRecreation() {
        ui.onNodeWithContentDescription("Profile").performClick()
        ui.onNodeWithContentDescription("Profile options for Study").performScrollTo().performClick()
        ui.onNodeWithText("Edit").performClick()
        ui.onNodeWithText("Name").performScrollTo().performTextReplacement("Quiet study")
        ui.onNodeWithContentDescription("focus illustration").performScrollTo().performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Name").performScrollTo().assertTextContains("Quiet study")
        ui.onNodeWithContentDescription("focus illustration").assertIsSelected()
        ui.onNodeWithText("Save profile").assertIsDisplayed().performClick()
        ui.runOnIdle {
            assertEquals(4, Repository.state.profiles.size)
            val edited = Repository.state.profiles.single { it.id == "study" }
            assertEquals("Quiet study", edited.name)
            assertEquals("FOCUS", edited.art)
        }
        // Preserve the launcher intent filter so ActivityScenario can follow the next recreation.
        ui.activityRule.scenario.onActivity { it.startActivity(android.content.Intent(it.intent).putExtra("screen", "settings").setFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
        ui.waitUntil(5000) { ui.onAllNodesWithText("PROTECTION HEALTH").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("PROTECTION HEALTH").assertExists()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("PROTECTION HEALTH").assertExists()
    }
    @Test fun onboardingHasFourStepsAndCleanFinish() {
        ui.runOnIdle { Repository.update { FocusState() } }
        ui.onNodeWithText("Less scroll.\nMore life.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Onboarding step 1 of 4").assertExists()
        ui.onNodeWithText("Skip intro").assertDoesNotExist()
        ui.onNodeWithText("Back").assertDoesNotExist()
        ui.onNodeWithContentDescription("Next").performClick()
        ui.onNodeWithText("Find your\nrhythm.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Next").performClick()
        ui.onNodeWithText("Keep your\nboundary.").assertIsDisplayed()
        ui.onNodeWithContentDescription("Set up protection").performClick()
        ui.onNodeWithText("Make space for focus.").assertExists()
        ui.onNodeWithContentDescription("Onboarding step 4 of 4").assertExists()
        ui.runOnIdle { assertFalse(Repository.state.onboarding) }
        ui.onNodeWithText("Continue with timers").assertDoesNotExist()
        ui.onNodeWithContentDescription("Finish onboarding").performClick()
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
        ui.onNodeWithText("Your profiles").assertIsDisplayed()
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
        ui.onNodeWithText("Deep work").performScrollTo().performClick()
        ui.onNodeWithText("Start focus").assertIsDisplayed().performClick()
        ui.onNodeWithText("Blocking needs your consent and a connected Android accessibility service. You can enable it now, or explicitly start a timer without blocking.").assertExists()
        ui.onNodeWithText("Timer only").performClick()
        ui.onNodeWithText("Timer only · protection is off").assertExists()
        ui.runOnIdle { assertNotNull(Repository.state.session); Repository.stop() }
    }
    @Test fun profileCanBeCreatedWithWeekAndTargetsThenDeleted() {
        ui.runOnIdle { Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test")), domains = setOf("example.com")) } }
        ui.onNodeWithContentDescription("Profile").performClick()
        ui.onNodeWithText("Create profile").performClick()
        ui.onNodeWithText("Name").performTextReplacement("Week pause")
        ui.onNodeWithText("Name").performImeAction()
        ui.onNode(hasText("1 week") and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        ui.onNodeWithText("Apps").performScrollTo().performClick()
        ui.onNodeWithContentDescription("All selected apps").performClick()
        ui.onNodeWithText("Done · 1 selected").performClick()
        ui.onNodeWithText("Websites").performScrollTo().performClick()
        ui.onNodeWithContentDescription("All saved websites").performClick()
        ui.onNodeWithText("Done · 1 selected").performClick()
        ui.onNodeWithText("Save profile").assertIsDisplayed().performClick()
        ui.runOnIdle {
            val profile = Repository.state.profiles.single { it.name == "Week pause" }
            assertEquals(10_080, profile.minutes)
            assertFalse(profile.useAllApps)
            assertEquals(setOf("dev.still.fixture"), profile.apps)
            assertFalse(profile.useAllDomains)
            assertEquals(setOf("example.com"), profile.domains)
            Repository.update { it.copy(schedules = it.schedules + Schedule(name = "Week routine", profileId = profile.id, days = setOf(1), startMinute = 540, endMinute = 600)) }
        }
        ui.onNodeWithContentDescription("Profile options for Week pause").performScrollTo().performClick()
        ui.onNodeWithText("Delete").performClick()
        ui.onNodeWithText("Delete profile").performClick()
        ui.runOnIdle {
            assertTrue(Repository.state.profiles.none { it.name == "Week pause" })
            assertTrue(Repository.state.schedules.none { it.name == "Week routine" })
        }
    }
    @Test fun rulesAndSessionSurviveSerialization() {
        ui.runOnIdle {
            Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test", dailyMinutes = 10)), domains = setOf("reddit.com"), blocks = listOf(BlockPlan(kind = BlockKind.WEBSITES, targets = setOf("reddit.com"), start = 0)), schedules = listOf(Schedule(name = "Night", profileId = "sleep", days = setOf(1, 2), startMinute = 1320, endMinute = 420)), profiles = it.profiles + Profile("custom", "Week pause", 10_080, "Work", FocusMode.STRICT, art = "GUARD"), blockMessage = BlockMessage("Don't open", "Be here", "✨"), lifecycleRules = listOf(AppLifecycleRule("com.instagram.android", "Instagram", quarantineInstall = true))) }
            val data = Repository.export()
            val decoded = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }.invoke(Repository, data) as FocusState
            assertEquals(Repository.state.apps, decoded.apps)
            assertEquals(Repository.state.domains, decoded.domains)
            assertEquals(Repository.state.schedules, decoded.schedules)
            assertEquals(Repository.state.blocks, decoded.blocks)
            assertEquals(Repository.state.blockMessage, decoded.blockMessage)
            assertEquals(Repository.state.lifecycleRules, decoded.lifecycleRules)
            assertEquals(Repository.state.profiles, decoded.profiles)
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
        ui.onNodeWithText("Apps").performClick()
        ui.onNodeWithText("Clear").performClick()
        ui.onNodeWithText("Search apps").performTextInput("Scroll")
        ui.waitUntil(5000) { ui.onAllNodesWithText("Scroll Test").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Scroll Test").performClick()
        ui.onNodeWithText("Done · 1 selected").performClick()
        ui.onNodeWithText("Connect protection").assertIsDisplayed().performClick()
        ui.onNodeWithText("Back to block").performScrollTo().assertExists()
        ui.onNodeWithContentDescription("Back").performClick()
        ui.onNodeWithText("1 week").assertIsSelected()
        ui.onNodeWithText("Until I unblock").performScrollTo().performClick()
        ui.onNodeWithText("Strict").performScrollTo().assertIsNotEnabled()
    }
    @Test fun homeAppShortcutOpensPickerAndRestoresSelectionWithoutSavingABlock() {
        ui.onNodeWithContentDescription("Block apps").performScrollTo().performClick()
        ui.onNodeWithText("Search apps").assertIsDisplayed().performTextInput("Scroll")
        ui.waitUntil(5000) { ui.onAllNodesWithText("Scroll Test").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Scroll Test").performClick()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Done · 1 selected").assertIsDisplayed().performClick()
        ui.onNodeWithText("Block apps.").assertIsDisplayed()
        ui.onNodeWithText("Connect protection").assertIsDisplayed()
        ui.runOnIdle { assertTrue(Repository.state.apps.isEmpty()); assertTrue(Repository.state.blocks.isEmpty()) }
    }
    @Test fun homeWebsiteShortcutAddsAndNormalizesDomainInOneScreen() {
        ui.onNodeWithContentDescription("Block websites").performScrollTo().performClick()
        ui.onNodeWithText("Website or domain").performTextInput("https://www.reddit.com/r/focus")
        ui.onNodeWithText("Add website").performClick()
        ui.onNodeWithText("reddit.com").assertExists()
        ui.onNodeWithText("1 week").performClick().assertIsSelected()
        ui.onNodeWithText("Connect protection").assertIsDisplayed()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("reddit.com").assertExists()
        ui.onNodeWithText("1 week").assertIsSelected()
        ui.runOnIdle { assertTrue(Repository.state.domains.isEmpty()); assertTrue(Repository.state.blocks.isEmpty()) }
    }
    @Test fun homeFeedsShortcutShowsAllFeedsAndKeepsManualStrictGuard() {
        ui.onNodeWithText("Shorts & Reels").performScrollTo().performClick()
        ui.onNodeWithContentDescription("YouTube Shorts").performClick()
        ui.onNodeWithContentDescription("Instagram Reels").assertExists()
        ui.onNodeWithContentDescription("Facebook Reels").assertExists()
        ui.onNodeWithText("Until I unblock").performScrollTo().performClick()
        ui.onNodeWithText("Strict").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("Connect protection").assertIsDisplayed()
        ui.runOnIdle { assertTrue(Repository.state.feeds.isEmpty()); assertTrue(Repository.state.blocks.isEmpty()) }
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
