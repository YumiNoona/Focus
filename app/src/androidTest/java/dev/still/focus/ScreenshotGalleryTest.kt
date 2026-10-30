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
        // Preserve the launch intent filter so ActivityScenario follows recreation and cleanup.
        ui.activityRule.scenario.onActivity {
            it.startActivity(Intent(it.intent).putExtra("screen", route).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
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
    private fun captureDarkFocus(demo: FocusState) {
        val profile = Profile("dark-preview", "Quiet focus", 3, "Give yourself a little room.", FocusMode.GENTLE)
        ui.runOnIdle { Repository.update { demo.copy(theme = "Dark", session = Session(profile = profile, start = System.currentTimeMillis(), end = System.currentTimeMillis() + 60_000, timerOnly = true)) } }
        open("home"); shot("40-dark-focus")
    }
    @Test fun captureSimplifiedFlows() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        val original = Repository.state
        val fontScale = shell("settings get system font_scale").trim().toFloatOrNull() ?: 1f
        try {
            val demo = FocusState(onboarding = true, apps = listOf(AppRule("dev.still.fixture", "Scroll Test")), domains = setOf("reddit.com"))
            ui.runOnIdle { Repository.update { demo } }
            open("home"); shot("simple-01-home"); lower("simple-02-home-feeds")
            ui.onNodeWithContentDescription("Block apps").performScrollTo().performClick(); shot("simple-03-block-apps")
            ui.onNodeWithText("Apps").performClick(); shot("simple-04-choose-apps")
            ui.onNodeWithText("Search apps").performTextInput("Scroll"); shot("simple-05-search-keyboard")
            ui.onNodeWithText("Search apps").performImeAction()
            ui.onNodeWithText("Done · 1 selected").performClick()
            open("home"); ui.onNodeWithContentDescription("Block websites").performScrollTo().performClick(); shot("simple-06-block-websites")
            open("home"); ui.onNodeWithText("Shorts & Reels").performScrollTo().performClick(); shot("simple-07-block-feeds")
            open("setup"); shot("simple-08-focus"); lower("simple-09-focus-targets")
            ui.onNodeWithText("Websites").performScrollTo().performClick(); shot("simple-10-choose-websites")
            ui.onNodeWithText("Done · 1 selected").performClick()
            open("rules"); ui.onNodeWithText("Schedules").performClick(); shot("simple-11-routines")
            ui.onNodeWithText("Add a routine").performClick(); shot("simple-12-routine-editor")
            open("settings"); shot("simple-13-settings"); ui.onNodeWithText("Appearance").performScrollTo(); lower("simple-14-appearance")
            ui.runOnIdle { Repository.update { demo.copy(theme = "Dark") } }; open("home"); shot("simple-15-dark-home")
            ui.onNodeWithText("Shorts & Reels").performScrollTo().performClick(); shot("simple-16-dark-feeds")
            shell("settings put system font_scale 1.3"); Thread.sleep(1500)
            ui.runOnIdle { Repository.update { demo } }; open("setup"); shot("simple-17-large-focus")
            ui.onNodeWithText("Apps").performScrollTo().performClick(); shot("simple-18-large-apps")
            ui.onNodeWithText("Search apps").performTextInput("Scroll"); shot("simple-19-large-keyboard")
            ui.onNodeWithText("Search apps").performImeAction()
            ui.onNodeWithText("Done · 1 selected").performClick()
        } finally { shell("settings put system font_scale $fontScale"); ui.runOnIdle { Repository.update { original } }; open("home") }
    }
    @Test fun captureFloatingDock() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        val original = Repository.state
        val fontScale = shell("settings get system font_scale").trim().toFloatOrNull() ?: 1f
        val demo = FocusState(onboarding = true, apps = listOf(AppRule("dev.still.fixture", "Scroll Test")))
        try {
            ui.runOnIdle { Repository.update { demo.copy(theme = "Light") } }
            open("home"); shot("dock-01-home")
            scrollPage(350f); shot("dock-02-scroll-through")
            ui.onNodeWithContentDescription("Rules").performClick()
            ui.onNodeWithContentDescription("Rules").assertIsSelected(); shot("dock-03-rules")
            ui.onNodeWithContentDescription("Insights").performClick()
            ui.onNodeWithContentDescription("Insights").assertIsSelected(); shot("dock-04-insights")
            ui.runOnIdle { Repository.update { demo.copy(theme = "Dark", reduceMotion = true) } }
            open("home"); shot("dock-05-dark-home")
            scrollPage(350f); shot("dock-06-dark-scroll")
            shell("settings put system font_scale 1.3"); Thread.sleep(1500)
            ui.runOnIdle { Repository.update { demo.copy(theme = "Light") } }
            open("home"); shot("dock-07-large-text")
            ui.onNodeWithText("Personal time").performScrollClearOfDock(ui); shot("dock-08-last-actions")
            ui.onNodeWithText("Personal time").performClick()
            ui.onNodeWithContentDescription("Rules").assertDoesNotExist()
        } finally {
            shell("settings put system font_scale $fontScale")
            ui.runOnIdle { Repository.update { original } }; open("home")
        }
    }
    @Test fun captureBreakSettings() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        open("settings")
        ui.onNodeWithText("Gentle break reminders").performScrollTo(); scrollPage(420f)
        shot("break-settings")
        open("home")
    }
    @Test fun capturePersonalization() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("gallery") == "true")
        automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        val original = Repository.state
        val previous = shell("settings get secure enabled_accessibility_services").trim()
        val fontScale = shell("settings get system font_scale").trim()
        try {
            ui.runOnIdle { Repository.update { FocusState() } }
            open("home"); shot("personal-01-onboarding")
            ui.onNodeWithContentDescription("Next").performClick(); shot("personal-01b-onboarding-rhythm")
            ui.onNodeWithContentDescription("Next").performClick(); shot("personal-01c-onboarding-boundary")
            ui.onNodeWithContentDescription("Set up protection").performClick(); shot("personal-02-onboarding-done")
            ui.onNodeWithText("More options").performScrollTo().performClick(); shot("personal-02b-onboarding-options")
            ui.onNodeWithContentDescription("Finish onboarding").performClick()
            open("picker"); shot("personal-03-app-picker")
            open("messages"); shot("personal-04-message-preview")
            ui.onNodeWithText("ILLUSTRATION").performScrollTo(); scrollPage(950f)
            ui.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(3)
            shot("personal-05-cat-choices")
            ui.onNodeWithText("EMOJI & SYMBOLS").performScrollTo(); scrollPage(900f); shot("personal-06-emoji")
            val image = BlockingArtwork.importImage(ui.activity, android.net.Uri.parse("android.resource://${ui.activity.packageName}/${R.drawable.cat_tea}"))
            ui.runOnIdle { Repository.update { it.copy(blockMessage = BlockMessage(art = "TEA", image = image)) } }
            open("home"); open("messages"); shot("personal-07-custom-image")
            shell("settings put system font_scale 1.3"); Thread.sleep(1500)
            open("picker"); shot("personal-08-large-app-picker")
            shell("settings put system font_scale $fontScale"); Thread.sleep(1000)
            shell("settings put secure enabled_accessibility_services null")
            shell("settings put secure enabled_accessibility_services dev.still.focus/.FocusAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            ui.waitUntil(12_000) { ProtectionRuntime.live() }
            ui.runOnIdle { Repository.update { it.copy(disclosure = true, blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf("dev.still.fixture"), mode = FocusMode.GENTLE, start = System.currentTimeMillis(), period = BlockPeriod.MANUAL))) } }
            shell("am start -n dev.still.fixture/.ScrollActivity")
            ui.waitUntil(10_000) { ProtectionRuntime.blockedPackage == "dev.still.fixture" }
            shot("personal-09-native-custom-image")
        } finally {
            shell("settings put system font_scale $fontScale")
            ui.runOnIdle { Repository.update { original }; FocusAccessibilityService.instance?.clearOverlay() }
            BlockingArtwork.prune(ui.activity, setOf(original.blockMessage.image))
            shell("settings put secure enabled_accessibility_services ${if (previous.isBlank() || previous == "null") "null" else previous}")
            open("home")
        }
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
            captureDarkFocus(demo)
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
            ui.onNodeWithContentDescription("Next").performClick(); shot("01b-onboarding-rhythm")
            ui.onNodeWithContentDescription("Next").performClick(); shot("01c-onboarding-boundary")
            ui.onNodeWithContentDescription("Set up protection").performClick(); shot("02-onboarding-permissions")
            ui.onNodeWithText("Connect protection").performScrollTo().performClick(); shot("03-protection-disclosure")
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
            ui.onNodeWithText("Appearance").performScrollTo(); shot("19c-appearance")
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
            open("setup"); ui.onNodeWithText("Start focus").performClick(); shot("38-protection-warning")
            ui.onNodeWithText("Timer only").assertExists()
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            ui.runOnIdle { Repository.update { demo } }
            open("home")
            ui.runOnIdle { Repository.update { demo.copy(theme = "Dark") } }; open("home"); shot("31-dark-home")
        } finally { ui.runOnIdle { Repository.update { original } }; open("home") }
    }
}
