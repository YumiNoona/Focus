package dev.still.focus

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

/** Destructive package operations are limited to the disposable fixture on a managed emulator. */
class ManagedLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    private val fixture = "dev.still.fixture"
    private fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use { fd -> java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText() }
    private fun waitFor(check: () -> Boolean) { val until = SystemClock.elapsedRealtime() + 15_000; while (!check() && SystemClock.elapsedRealtime() < until) Thread.sleep(250); assertTrue(check()) }
    @Test fun cachedSuspensionIsRepairedAndNormalBansProtectFoculyRemoval() {
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        Repository.init(ctx)
        Assume.assumeTrue(ManagedProtection.isOwner(ctx))
        val old = Repository.state
        assertFalse(RulesEngine.strictActive(old, System.currentTimeMillis()))
        val policy = ctx.getSystemService(android.app.admin.DevicePolicyManager::class.java)
        val admin = android.content.ComponentName(ctx, FocusAdminReceiver::class.java)
        try {
            assertTrue(shell("pm install -r /data/local/tmp/focus-fixture.apk").contains("Success"))
            assertTrue(Repository.update { it.copy(uninstallProtection = false, lifecycleRules = listOf(AppLifecycleRule(fixture, "Scroll Test", quarantineInstall = true))) })
            assertTrue(ManagedProtection.sync()); assertTrue(ManagedProtection.appSuspended(fixture))
            assertTrue(policy.setPackagesSuspended(admin, arrayOf(fixture), false).isEmpty())
            assertFalse(ManagedProtection.appSuspended(fixture))
            // Same desired package set and journal: verifying the OS must repair a missing policy.
            assertTrue(ManagedProtection.sync()); assertTrue(ManagedProtection.appSuspended(fixture))
            assertTrue(Repository.update { it.copy(lifecycleRules = emptyList(), blocks = listOf(BlockPlan(kind = BlockKind.APPS,
                targets = setOf(fixture), mode = FocusMode.GENTLE, start = System.currentTimeMillis(), end = System.currentTimeMillis() + 86_400_000, period = BlockPeriod.DAY))) })
            assertTrue(ManagedProtection.sync()); assertTrue(ManagedProtection.uninstallBlocked())
            assertTrue(policy.getUserRestrictions(admin).getBoolean(android.os.UserManager.DISALLOW_CONFIG_DATE_TIME))
            waitFor { ctx.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any {
                it.id == 3 && it.notification.extras.getString(android.app.Notification.EXTRA_TITLE) == "Android blocks FocuLY removal"
            } }
            assertTrue(shell("pm uninstall ${ctx.packageName}").contains("DELETE_FAILED"))
            if (android.os.Build.VERSION.SDK_INT >= 30) assertTrue(ctx.packageName in policy.getUserControlDisabledPackages(admin))
            assertTrue(Repository.update { it.copy(blocks = emptyList()) }); assertTrue(ManagedProtection.sync())
            assertFalse(ManagedProtection.uninstallBlocked())
            assertFalse(policy.getUserRestrictions(admin).getBoolean(android.os.UserManager.DISALLOW_CONFIG_DATE_TIME))
            assertTrue(Repository.update { it.copy(uninstallProtection = true) }); assertTrue(ManagedProtection.sync())
            assertTrue(ManagedProtection.uninstallBlocked())
        } finally { Repository.update { old }; ManagedProtection.invalidate(); ManagedProtection.sync() }
    }
    @Test fun managedPolicyBlocksRemovalQuarantinesFutureInstallsAndReleasesExpiredRules() {
        Assume.assumeTrue(shell("getprop ro.kernel.qemu").trim() == "1")
        Repository.init(ctx)
        Assume.assumeTrue(ManagedProtection.isOwner(ctx))
        val old = Repository.state
        assertFalse(RulesEngine.strictActive(old, System.currentTimeMillis()))
        try {
            assertTrue(shell("pm install -r /data/local/tmp/focus-fixture.apk").contains("Success"))
            assertTrue(Repository.update { it.copy(lifecycleRules = listOf(AppLifecycleRule(fixture, "Scroll Test", lockUninstall = true))) })
            assertTrue(ManagedProtection.sync())
            assertTrue(ManagedProtection.appUninstallBlocked(fixture))
            assertTrue(shell("pm uninstall $fixture").contains("DELETE_FAILED"))
            assertTrue(Repository.update { it.copy(lifecycleRules = emptyList()) }); assertTrue(ManagedProtection.sync())
            assertTrue(shell("pm uninstall $fixture").contains("Success"))
            assertTrue(Repository.update { it.copy(lifecycleRules = listOf(AppLifecycleRule(fixture, "Scroll Test", quarantineInstall = true))) })
            assertTrue(ManagedProtection.sync())
            assertTrue(shell("pm install -r /data/local/tmp/focus-fixture.apk").contains("Success"))
            waitFor { ManagedProtection.appSuspended(fixture) }
            assertTrue(Repository.update { it.copy(lifecycleRules = listOf(AppLifecycleRule(fixture, "Scroll Test", quarantineInstall = true, end = System.currentTimeMillis() + 1200))) })
            Thread.sleep(1500); Repository.reconcile()
            assertFalse(ManagedProtection.appSuspended(fixture))
            assertTrue(Repository.update { it.copy(managedLockdown = false, uninstallProtection = false, session = Session(profile = Profile("strict", "Strict", 1, "Check removal", FocusMode.STRICT), start = System.currentTimeMillis(), end = System.currentTimeMillis() + 1200)) })
            assertTrue(ManagedProtection.sync()); assertTrue(ManagedProtection.uninstallBlocked())
            Thread.sleep(1500); Repository.reconcile()
            assertFalse(ManagedProtection.uninstallBlocked())
        } finally {
            Repository.state.session?.let { if (it.alive(System.currentTimeMillis())) Thread.sleep((it.end - System.currentTimeMillis()).coerceAtLeast(0) + 50) }
            Repository.reconcile(); Repository.update { old }; ManagedProtection.invalidate(); ManagedProtection.sync()
        }
    }
}
