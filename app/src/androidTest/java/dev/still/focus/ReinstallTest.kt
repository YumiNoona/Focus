package dev.still.focus

import android.app.UiAutomation
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

/** Only the disposable fixture is uninstalled. Never run on a personal device. */
class ReinstallTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    private val fixture = "dev.still.fixture"
    private lateinit var automation: UiAutomation
    private fun shell(command: String) = automation.executeShellCommand(command).use { fd ->
        android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText().trim()
    }
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun waitFor(check: () -> Boolean) {
        val end = SystemClock.elapsedRealtime() + 12_000
        while (!check() && SystemClock.elapsedRealtime() < end) Thread.sleep(100)
        assertTrue("Expected retained rules and real blocking after reinstall", check())
    }
    @Test fun manualDayWeekAndStrictTimerBansSurviveRealReinstallWithOriginalDeadline() {
        automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        Assume.assumeTrue(shell("getprop ro.kernel.qemu") == "1")
        Assume.assumeTrue(shell("ls /data/local/tmp/focus-fixture.apk").endsWith("focus-fixture.apk"))
        main { Repository.init(ctx) }
        Assume.assumeFalse(ManagedProtection.isOwner(ctx))
        val previous = shell("settings get secure enabled_accessibility_services")
        val old = Repository.state
        try {
            shell("input keyevent WAKEUP"); shell("wm dismiss-keyguard")
            main { Repository.update { FocusState(onboarding = true, disclosure = true, breakReminders = false) } }
            shell("settings put secure enabled_accessibility_services null")
            shell("settings put secure enabled_accessibility_services dev.still.focus/.FocusAccessibilityService")
            shell("settings put secure accessibility_enabled 1")
            waitFor { ProtectionRuntime.live() }
            for (period in listOf(BlockPeriod.MANUAL, BlockPeriod.DAY, BlockPeriod.WEEK, BlockPeriod.TIMER)) {
                assertTrue(shell("pm install -r /data/local/tmp/focus-fixture.apk").contains("Success"))
                val now = System.currentTimeMillis()
                val mode = if (period == BlockPeriod.TIMER) FocusMode.STRICT else FocusMode.GENTLE
                val end = if (period == BlockPeriod.TIMER) now + 30_000 else period.deadline(now)
                val plan = BlockPlan(kind = BlockKind.APPS, targets = setOf(fixture), mode = mode, start = now, end = end, period = period)
                main { assertTrue(Repository.update { it.copy(apps = listOf(AppRule(fixture, "Scroll Test")), blocks = listOf(plan), overrides = listOf(OverrideRecord(fixture, now, now + 60_000, "old visit"))) }) }
                val revision = PackageStatus.revision
                assertTrue(shell("pm uninstall $fixture").contains("Success"))
                waitFor { PackageStatus.revision > revision && Repository.state.overrides.single().until <= System.currentTimeMillis() }
                main {
                    assertEquals(listOf(plan), Repository.state.blocks)
                    assertEquals(fixture, Repository.state.apps.single().packageName)
                    assertTrue(Repository.state.overrides.single().until <= System.currentTimeMillis())
                }
                assertTrue(shell("pm install -r /data/local/tmp/focus-fixture.apk").contains("Success"))
                shell("am start -n $fixture/.ScrollActivity")
                waitFor { ProtectionRuntime.blockedPackage == fixture }
                main {
                    assertEquals(plan, Repository.state.blocks.single())
                    val decode = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }
                    val saved = ctx.getSharedPreferences("still", 0).getString("state", "{}")!!
                    assertEquals(plan, (decode.invoke(Repository, saved) as FocusState).blocks.single())
                }
            }
            val ending = Repository.state.blocks.single().end
            Thread.sleep((ending - System.currentTimeMillis()).coerceAtLeast(0) + 100)
            main { Repository.reconcile() }
            waitFor { Repository.state.blocks.isEmpty() && ProtectionRuntime.blockedPackage.isEmpty() }
        } finally {
            val deadline = Repository.state.blocks.filter { it.mode == FocusMode.STRICT }.maxOfOrNull { it.end } ?: 0
            Thread.sleep((deadline - System.currentTimeMillis()).coerceIn(0, 30_000) + 100)
            main { Repository.reconcile(); Repository.update { old }; FocusAccessibilityService.instance?.clearOverlay() }
            shell("pm install -r /data/local/tmp/focus-fixture.apk")
            shell("settings put secure enabled_accessibility_services ${if (previous == "null" || previous.isBlank()) "null" else previous}")
        }
    }
}
