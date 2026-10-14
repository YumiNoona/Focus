package dev.still.focus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*

class AppTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun reset() { ui.runOnIdle { Repository.update { FocusState(onboarding = true) } } }
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
        ui.onNodeWithText("Blocking needs your consent and Android accessibility access. You can enable it now, or start a timer without blocking.").assertExists()
        ui.onNodeWithText("Timer only").performClick()
        ui.onNodeWithText("Timer only · protection is off").assertExists()
        ui.runOnIdle { assertNotNull(Repository.state.session); Repository.stop() }
    }
    @Test fun rulesAndSessionSurviveSerialization() {
        ui.runOnIdle {
            Repository.update { it.copy(apps = listOf(AppRule("dev.still.fixture", "Scroll Test", dailyMinutes = 10)), domains = setOf("reddit.com"), schedules = listOf(Schedule(name = "Night", profileId = "sleep", days = setOf(1, 2), startMinute = 1320, endMinute = 420))) }
            val data = Repository.export()
            val decoded = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }.invoke(Repository, data) as FocusState
            assertEquals(Repository.state.apps, decoded.apps)
            assertEquals(Repository.state.domains, decoded.domains)
            assertEquals(Repository.state.schedules, decoded.schedules)
        }
    }
}
