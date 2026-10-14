package dev.still.focus

import android.app.UiAutomation
import android.os.Bundle
import android.view.accessibility.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

/** Real installed-app / accessibility-window regression; isolated emulators only. */
class ProtectionScenarioTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var automation: UiAutomation
    private val fixture = "dev.still.fixture"
    private fun shell(command: String): String = automation.executeShellCommand(command).use { descriptor ->
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
    }
    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun waitFor(timeout: Long = 8000, condition: () -> Boolean) {
        val end = android.os.SystemClock.elapsedRealtime() + timeout
        while (!condition() && android.os.SystemClock.elapsedRealtime() < end) Thread.sleep(100)
        assertTrue("Expected protection state: connected=${ProtectionRuntime.connected}, observed=${ProtectionRuntime.observedPackage}, blocked=${ProtectionRuntime.blockedPackage}, error=${ProtectionRuntime.lastError}", condition())
    }
    private fun overlayRoot(): AccessibilityNodeInfo? = FocusAccessibilityService.instance?.windows
        ?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }?.root
    private fun find(text: String) = overlayRoot()?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
    private fun click(text: String) { waitFor { find(text) != null }; assertTrue(find(text)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)) }

    @Test fun popupResistanceStrictAndTransientWindowsWorkAgainstAnInstalledApp() {
        // Do not let test automation suppress the service it is checking.
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue("Do not change a personal phone's permissions or data", shell("getprop ro.kernel.qemu") == "1")
        Assume.assumeTrue("Install the optional test-fixture first", shell("pm path $fixture").startsWith("package:"))
        shell("input keyevent WAKEUP")
        shell("wm dismiss-keyguard")
        val previous = shell("settings get secure enabled_accessibility_services")
        try {
            onMain { Repository.init(context); Repository.update { FocusState(onboarding = true, disclosure = true, apps = listOf(AppRule(fixture, "Scroll Test"))) } }
            shell("appops set ${context.packageName} GET_USAGE_STATS allow")
            shell("settings put secure enabled_accessibility_services null")
            shell("settings put secure enabled_accessibility_services ${context.packageName}/${FocusAccessibilityService::class.java.name}")
            shell("settings put secure accessibility_enabled 1")
            waitFor(12_000) { ProtectionRuntime.live() }

            onMain { assertTrue(Repository.start(Profile("normal", "Normal test", 10, "Work", FocusMode.GENTLE), 10, "Work")) }
            shell("am start -n $fixture/.ScrollActivity")
            waitFor { ProtectionRuntime.blockedPackage == fixture && overlayRoot() != null }
            click("Continue for 5 minutes")
            waitFor { ProtectionRuntime.blockedPackage.isEmpty() }
            onMain { assertTrue(Repository.state.overrides.any { it.packageName == fixture }); assertTrue(Repository.stop()); Repository.update { it.copy(overrides = emptyList()) } }

            onMain { assertTrue(Repository.start(Profile("resistant", "Resistant test", 10, "Work", FocusMode.STANDARD), 10, "Work")) }
            onMain {
                assertTrue(Repository.block(BlockKind.APPS, setOf(fixture), BlockPeriod.MANUAL, FocusMode.STANDARD))
                Repository.beginUnblock(Repository.state.blocks.single().id)
                assertFalse(Repository.unblock(Repository.state.blocks.single().id, "too fast"))
            }
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            click("Take an intentional visit")
            waitFor { find("Pause for") != null }
            assertFalse(find("Pause for")!!.isEnabled)
            waitFor(35_000) { find("Allow for 1 minute")?.isEnabled == true }
            val root = overlayRoot()!!
            fun edit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.className == "android.widget.EditText") return node
                for (index in 0 until node.childCount) node.getChild(index)?.let { edit(it)?.let { result -> return result } }
                return null
            }
            val input = edit(root)!!
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "An intentional reply") }))
            click("Allow for 1 minute")
            waitFor { ProtectionRuntime.blockedPackage.isEmpty() }
            onMain { val visit = Repository.state.overrides.last(); assertEquals(60_000L, visit.until - visit.at); assertTrue(Repository.unblock(Repository.state.blocks.single().id, "An intentional ending")) }

            // Keep that existing visit: Strict must ignore it and must recover after shade/home transitions.
            onMain {
                val now = System.currentTimeMillis()
                Repository.update { it.copy(session = Session(profile = Profile("strict", "Strict test", 1, "Work", FocusMode.STRICT), start = now, end = now + 18_000)) }
            }
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            assertNull(find("Take an intentional visit"))
            assertNull(find("Continue for"))
            shell("cmd statusbar expand-notifications")
            shell("cmd statusbar collapse")
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            shell("input keyevent HOME")
            waitFor { ProtectionRuntime.blockedPackage.isEmpty() }
            shell("am start -n $fixture/.ScrollActivity")
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            waitFor(25_000) { Repository.state.session == null && ProtectionRuntime.blockedPackage.isEmpty() }
            onMain {
                Repository.update { it.copy(overrides = emptyList()) }
                assertTrue(Repository.block(BlockKind.APPS, setOf(fixture), BlockPeriod.MANUAL, FocusMode.GENTLE))
                assertTrue(Repository.block(BlockKind.APPS, setOf(fixture), BlockPeriod.MANUAL, FocusMode.GENTLE))
                assertEquals(1, Repository.state.blocks.size)
                assertNull(Repository.state.session)
            }
            shell("am start -n $fixture/.ScrollActivity")
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            onMain { assertTrue(Repository.unblock(Repository.state.blocks.single().id)) }
            waitFor { ProtectionRuntime.blockedPackage.isEmpty() }
            onMain {
                val now = System.currentTimeMillis()
                Repository.update { it.copy(blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf(fixture), mode = FocusMode.STRICT, start = now, end = now + 5000))) }
            }
            waitFor { ProtectionRuntime.blockedPackage == fixture }
            waitFor { Repository.state.blocks.isEmpty() && ProtectionRuntime.blockedPackage.isEmpty() }
        } finally {
            waitFor(25_000) { !RulesEngine.strictActive(Repository.state, System.currentTimeMillis()) }
            onMain { Repository.stop(); Repository.update { FocusState(onboarding = true) } }
            shell("settings put secure enabled_accessibility_services ${if (previous.isBlank() || previous == "null") "null" else previous}")
        }
    }
}
