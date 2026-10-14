package dev.still.focus

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Opt-in gallery from real emulator UI. Never run against a personal phone. */
class ScreenshotGalleryTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var automation: android.app.UiAutomation
    private fun shell(command: String) = automation.executeShellCommand(command).use { fd -> java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText() }
    private fun shot(name: String) {
        shell("cmd statusbar collapse")
        ui.waitForIdle(); Thread.sleep(700)
        val folder = File(ui.activity.getExternalFilesDir(null), "screen-gallery").also { it.mkdirs() }
        val bitmap = automation.takeScreenshot() ?: error("No screenshot")
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    private fun open(route: String) {
        shell("cmd statusbar collapse")
        shell("am start -n dev.still.focus/.MainActivity --es screen $route")
        ui.waitForIdle(); Thread.sleep(900)
        scrollPage(-100_000f)
    }
    private fun scrollPage(pixels: Float) {
        val pages = ui.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        if (pages.fetchSemanticsNodes().isNotEmpty()) pages.onFirst().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, pixels) }
        ui.waitForIdle(); Thread.sleep(250)
    }
    private fun lower(name: String) { scrollPage(ui.activity.resources.displayMetrics.heightPixels * .65f); shot(name) }
    private fun captureCompletion() {
        ui.onNodeWithText("A little win for you.").performScrollTo()
        scrollPage(800f)
        shot("36-session-complete")
    }
    private fun captureBreak(demo: FocusState) {
        val breakProfile = Profile("break-preview", "Quiet rhythm", 3, "Give yourself a little room.", FocusMode.GENTLE, pomodoro = true, workMinutes = 1, breakMinutes = 1, rounds = 2)
        ui.runOnIdle { Repository.update { demo }; assertTrue(Repository.start(breakProfile, 3, breakProfile.intention, timerOnly = true)) }
        // Wall time advances while the test clock is idle. Resume its coroutine tick before capture.
        Thread.sleep(61_000)
        ui.mainClock.advanceTimeBy(1_100)
        open("home")
        ui.onNodeWithText("BREAK TIME").assertIsDisplayed()
        assertTrue(Repository.state.session!!.onBreak(System.currentTimeMillis()))
        shot("39-pomodoro-break")
        ui.runOnIdle { Repository.stop(); Repository.update { demo.copy(theme = "Dark", session = Session(profile = breakProfile.copy(pomodoro = false), start = System.currentTimeMillis(), end = System.currentTimeMillis() + 60_000, timerOnly = true)) } }
        open("home"); shot("40-dark-focus")
    }
    @Test fun captureSessionMilestones() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        val original = Repository.state
        try {
            val demo = FocusState(onboarding = true)
            val now = System.currentTimeMillis()
            ui.runOnIdle { Repository.update { demo.copy(session = Session(profile = Profile("completion-preview", "Quiet moment", 1, "One small step.", FocusMode.GENTLE), start = now, end = now + 20_000, timerOnly = true)) } }
            open("home"); Thread.sleep(20_100)
            ui.mainClock.advanceTimeBy(1_100)
            ui.runOnIdle { Repository.reconcile() }
            open("home"); captureCompletion()
            captureBreak(demo)
        } finally { ui.runOnIdle { Repository.update { original } }; open("home") }
    }
    @Test fun captureAllPages() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        val original = Repository.state
        try {
            ui.runOnIdle { Repository.update { FocusState() } }
            shot("01-welcome")
            ui.onNodeWithText("Next").performClick(); shot("01b-onboarding-rhythm")
            ui.onNodeWithText("Next").performClick(); shot("01c-onboarding-boundary")
            ui.onNodeWithText("Set up protection").performClick(); shot("02-onboarding-permissions")
            ui.onNodeWithText("App & feed protection").performScrollTo().performClick(); shot("03-protection-disclosure")
            ui.onNodeWithText("Not now").performClick()
            shell("settings put secure enabled_accessibility_services dev.still.focus/.FocusAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            shell("appops set dev.still.focus GET_USAGE_STATS allow")
            shell("pm grant dev.still.focus android.permission.POST_NOTIFICATIONS")
            val demo = FocusState(onboarding = true, disclosure = true,
                apps = listOf(AppRule("dev.still.fixture", "Scroll Test")), domains = setOf("reddit.com"),
                feeds = setOf("YouTube Shorts", "Instagram Reels", "Facebook Reels"),
                schedules = listOf(Schedule(name = "Study hours", profileId = "study", days = setOf(1, 2, 3, 4, 5), startMinute = 540, endMinute = 600, enabled = false)),
                lifecycleRules = listOf(AppLifecycleRule("com.instagram.android", "Instagram", quarantineInstall = true)))
            ui.runOnIdle { Repository.update { demo } }
            open("home"); shot("04-home")
            open("rules"); ui.onNodeWithText("Apps", useUnmergedTree = true).performClick(); shot("05-rules-apps")
            ui.onNodeWithContentDescription("Edit Scroll Test").performClick(); shot("06-app-rule")
            open("picker"); shot("07-app-picker")
            open("allowlist"); shot("08-always-allowed")
            open("rules"); ui.onNodeWithText("Websites").performClick(); shot("09-rules-websites")
            ui.onNodeWithText("Block selected websites").performScrollTo().performClick(); shot("10-block-websites"); lower("10b-block-websites-modes")
            open("rules"); ui.onNodeWithText("Feeds").performClick(); shot("11-rules-feeds"); lower("11b-reels-cards")
            ui.onNodeWithText("Block Instagram Reels").performScrollTo().performClick(); shot("12-block-reels")
            open("rules"); ui.onNodeWithText("Schedules").performClick(); shot("13-rules-schedules")
            ui.onNodeWithText("Add a routine").performClick(); shot("14-schedule-editor")
            open("setup"); shot("15-session-setup"); lower("15b-session-mode")
            open("profile"); shot("16-profile")
            open("profileEdit"); shot("17-profile-editor")
            open("insights"); shot("18-insights")
            open("settings"); shot("19-settings-health"); lower("19b-settings-cards")
            ui.onNodeWithText("Appearance").performScrollTo().performClick(); shot("19c-appearance"); ui.onNodeWithText("Appearance").performClick()
            ui.onNodeWithText("Intentional visits").performScrollTo().performClick(); shot("19d-intentional-visits"); ui.onNodeWithText("Intentional visits").performClick()
            ui.onNodeWithText("Privacy & data").performScrollTo().performClick(); shot("19e-privacy-data"); ui.onNodeWithText("Privacy & data").performClick()
            open("managed"); shot("20-device-protection"); lower("20b-device-controls")
            open("lifecycle"); shot("21-individual-app-controls"); lower("21b-app-policy-editor"); lower("21c-app-policy-options")
            open("pin"); shot("22-admin-pin")
            ui.onNodeWithText("Set FocuLY PIN").performClick(); shot("22b-pin-setup"); ui.onNodeWithText("Cancel").performClick()
            open("messages"); shot("23-custom-blocking-screen"); lower("23b-message-editor")
            open("permissions"); shot("24-permissions-checklist"); lower("24b-permissions-options")
            open("rules"); ui.onNodeWithText("Apps", useUnmergedTree = true).performClick(); ui.onNodeWithText("Block selected apps").performClick(); shot("25-block-apps"); lower("25b-block-app-modes")
            ui.runOnIdle { Repository.update { demo.copy(blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf("dev.still.fixture"), mode = FocusMode.GENTLE, start = System.currentTimeMillis(), period = BlockPeriod.MANUAL))) } }
            open("home"); shot("26-active-blocks")
            automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            shell("settings put secure enabled_accessibility_services null")
            shell("settings put secure enabled_accessibility_services dev.still.focus/.FocusAccessibilityService")
            val connectionDeadline = SystemClock.elapsedRealtime() + 12_000
            while (!ProtectionRuntime.live() && SystemClock.elapsedRealtime() < connectionDeadline) Thread.sleep(100)
            assertTrue("Protection must be connected before capturing overlays", ProtectionRuntime.live())
            for ((mode, name) in listOf(FocusMode.GENTLE to "27-normal-popup", FocusMode.STANDARD to "28-resistant-screen", FocusMode.STRICT to "29-strict-screen")) {
                val now = System.currentTimeMillis()
                ui.runOnIdle { Repository.update { demo.copy(session = Session(profile = Profile("preview", "Deep work", 1, "Make room for what matters.", mode), start = now, end = now + 20_000)) } }
                open("home"); if (mode == FocusMode.STANDARD) {
                    shot("30-active-session")
                    ui.onNodeWithText("End session").performScrollTo().performClick(); shot("35-end-session-warning")
                    ui.onNodeWithText("Keep focusing").performClick()
                }
                shell("am start -n dev.still.fixture/.ScrollActivity")
                val deadline = SystemClock.elapsedRealtime() + 10_000
                while (ProtectionRuntime.blockedPackage != "dev.still.fixture" && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
                assertEquals("dev.still.fixture", ProtectionRuntime.blockedPackage)
                shot(name)
                Thread.sleep((now + 20_100 - System.currentTimeMillis()).coerceAtLeast(0))
                ui.runOnIdle { Repository.reconcile() }; open("home")
            }
            captureCompletion()
            ui.runOnIdle { Repository.update { demo.copy(blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf("dev.still.fixture"), mode = FocusMode.STANDARD, start = System.currentTimeMillis(), period = BlockPeriod.MANUAL))) } }
            open("home"); ui.onNodeWithText("Unblock").performScrollTo().performClick(); shot("37-unblock-warning")
            ui.onNodeWithText("Keep block").performClick()
            ui.runOnIdle { Repository.update { demo.copy(disclosure = false) } }
            open("setup"); ui.onNodeWithText("Start focus").performScrollTo().performClick(); shot("38-protection-warning")
            ui.onNodeWithText("Timer only").assertExists()
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            ui.runOnIdle { Repository.update { demo } }
            open("home")
            captureBreak(demo)
            ui.runOnIdle { Repository.update { demo.copy(theme = "Dark") } }; open("home"); shot("31-dark-home")
        } finally { ui.runOnIdle { Repository.update { original } }; open("home") }
    }
}
