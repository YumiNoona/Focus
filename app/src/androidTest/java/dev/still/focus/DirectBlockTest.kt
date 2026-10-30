package dev.still.focus

import android.app.UiAutomation
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

/** The Home shortcuts must save real targets and reach the installed-app overlay. */
class DirectBlockTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var automation: UiAutomation
    private fun shell(command: String) = automation.executeShellCommand(command).use { fd ->
        android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText().trim()
    }
    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 12_000
        while (!condition() && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
        assertTrue("Expected connected protection / fixture overlay", condition())
    }

    @Test fun homeShortcutsSaveNewTargetsAndAppBlockActuallyInterrupts() {
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue("Isolated emulator only", shell("getprop ro.kernel.qemu") == "1")
        Assume.assumeTrue("Install Scroll Test first", shell("pm path dev.still.fixture").startsWith("package:"))
        val previous = shell("settings get secure enabled_accessibility_services")
        try {
            shell("cmd statusbar collapse")
            ui.runOnIdle { assertTrue(Repository.update { FocusState(onboarding = true, disclosure = true) }) }
            shell("settings put secure enabled_accessibility_services null")
            shell("settings put secure enabled_accessibility_services dev.still.focus/.FocusAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            waitFor { Repository.protectionReady() }

            ui.onNodeWithContentDescription("Block apps").performScrollTo().performClick()
            ui.onNodeWithText("Search apps").performTextInput("Scroll")
            ui.waitUntil(5000) { ui.onAllNodesWithText("Scroll Test").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Scroll Test").performClick()
            ui.onNodeWithText("Search apps").performImeAction()
            ui.onNodeWithText("Done · 1 selected").performClick()
            ui.onNodeWithText("Until I unblock").performClick()
            ui.onNodeWithText("Normal").performClick()
            ui.onNodeWithText("Block now").assertIsDisplayed().performClick()
            ui.runOnIdle {
                assertEquals("dev.still.fixture", Repository.state.apps.single().packageName)
                assertEquals(setOf("dev.still.fixture"), Repository.state.blocks.single().targets)
                assertNull(Repository.state.session)
            }

            ui.onNodeWithContentDescription("Block websites").performScrollTo().performClick()
            ui.onNodeWithText("Website or domain").performTextInput("https://www.reddit.com/r/focus")
            ui.onNodeWithText("Add website").performClick()
            ui.onNodeWithText("1 week").performScrollTo().performClick()
            ui.onNodeWithText("Normal").performScrollTo().performClick()
            ui.onNodeWithText("Block now").performClick()
            ui.runOnIdle {
                assertEquals(setOf("reddit.com"), Repository.state.domains)
                assertEquals(BlockPeriod.WEEK, Repository.state.blocks.single { it.kind == BlockKind.WEBSITES }.period)
            }

            ui.onNodeWithText("Shorts & Reels").performScrollClearOfDock(ui).performClick()
            ui.onNodeWithContentDescription("YouTube Shorts").performClick()
            ui.onNodeWithText("Normal").performScrollTo().performClick()
            ui.onNodeWithText("Block now").performClick()
            ui.runOnIdle {
                assertEquals(setOf("YouTube Shorts"), Repository.state.feeds)
                assertEquals(setOf("YouTube Shorts"), Repository.state.blocks.single { it.kind == BlockKind.FEEDS }.targets)
            }

            shell("am start -n dev.still.fixture/.ScrollActivity")
            waitFor {
                ProtectionRuntime.blockedPackage == "dev.still.fixture" && FocusAccessibilityService.instance?.windows
                    ?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }?.root
                    ?.findAccessibilityNodeInfosByText("FocuLY / normal")?.isNotEmpty() == true
            }
        } finally {
            ui.runOnIdle { Repository.update { FocusState(onboarding = true) }; FocusAccessibilityService.instance?.clearOverlay() }
            shell("settings put secure enabled_accessibility_services ${if (previous.isBlank() || previous == "null") "null" else previous}")
            shell("cmd statusbar collapse")
            ui.activityRule.scenario.onActivity {
                it.startActivity(Intent(it.intent).putExtra("screen", "home").setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }
    }
}
