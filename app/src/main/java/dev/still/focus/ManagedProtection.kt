package dev.still.focus

import android.app.AlarmManager
import android.app.admin.*
import android.content.*
import android.os.Build
import android.provider.Settings
import android.net.Uri

class FocusAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) { Repository.init(context); sync(context) }
    override fun onDisabled(context: Context, intent: Intent) { Repository.init(context) }
    private fun sync(context: Context) { ManagedProtection.sync(context) }
}

object ManagedProtection {
    private fun manager(ctx: Context) = ctx.getSystemService(DevicePolicyManager::class.java)
    private fun admin(ctx: Context) = ComponentName(ctx, FocusAdminReceiver::class.java)
    fun isOwner(ctx: Context = Repository.context) = manager(ctx).isDeviceOwnerApp(ctx.packageName)
    fun adminEnabled(ctx: Context = Repository.context) = manager(ctx).isAdminActive(admin(ctx))
    fun uninstallBlocked(ctx: Context = Repository.context) = isOwner(ctx) && runCatching { manager(ctx).isUninstallBlocked(admin(ctx), ctx.packageName) }.getOrDefault(false)
    fun verifiedSuspendedCount(ctx: Context = Repository.context): Int {
        if (!isOwner(ctx)) return 0
        val packages = ctx.getSharedPreferences("managed-policy", 0).getStringSet("suspended", emptySet()).orEmpty()
        return packages.count { pkg -> runCatching { manager(ctx).isPackageSuspended(admin(ctx), pkg) }.getOrDefault(false) }
    }
    fun preciseEnding(ctx: Context = Repository.context) = Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun openPreciseEnding(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
    }
    fun requestAdmin(ctx: Context) {
        ctx.startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(ctx))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Optional uninstall resistance: Android asks you to deactivate this administrator before removing Focus. No camera, password, wipe, or screen-lock policy is requested."))
    }
    fun removeAdmin(ctx: Context): Boolean {
        if (RulesEngine.strictActive(Repository.state, System.currentTimeMillis()) || isOwner(ctx)) return false
        manager(ctx).removeActiveAdmin(admin(ctx)); return true
    }
    private var lastDesired: Set<String>? = null
    private var lastProtection: Boolean? = null
    fun sync(ctx: Context = Repository.context): Boolean {
        if (!isOwner(ctx)) return true
        val policy = manager(ctx)
        val component = admin(ctx)
        val state = Repository.state
        val desired = if (state.managedLockdown) RulesEngine.strictPackages(state, System.currentTimeMillis())
            .filterNot { FocusAccessibilityService.essential(ctx, it) }.toSet() else emptySet()
        val journal = ctx.getSharedPreferences("managed-policy", 0)
        val previous = journal.getStringSet("suspended", emptySet()).orEmpty().toSet()
        return runCatching {
            if (lastProtection != state.uninstallProtection) {
                policy.setUninstallBlocked(component, ctx.packageName, state.uninstallProtection)
                lastProtection = state.uninstallProtection
            }
            if (lastDesired != desired || previous != desired) {
                // Journal before applying so process death cannot orphan suspended apps.
                val attempted = previous + desired
                journal.edit().putStringSet("suspended", attempted).commit()
                val releases = (previous - desired).toTypedArray()
                if (releases.isNotEmpty()) {
                    val failed = policy.setPackagesSuspended(component, releases, false)
                    check(failed.isEmpty()) { "Android could not release ${failed.size} managed app(s). Open Focus to retry." }
                }
                if (desired.isNotEmpty()) {
                    val failed = policy.setPackagesSuspended(component, desired.toTypedArray(), true)
                    check(failed.isEmpty()) { "Android refused managed blocking for ${failed.size} selected app(s)." }
                }
                val clockKey = android.os.UserManager.DISALLOW_CONFIG_DATE_TIME
                if (desired.isNotEmpty() && !policy.getUserRestrictions(component).getBoolean(clockKey)) {
                    journal.edit().putBoolean("owns-clock-restriction", true).commit()
                    policy.addUserRestriction(component, clockKey)
                } else if (desired.isEmpty() && journal.getBoolean("owns-clock-restriction", false)) {
                    policy.clearUserRestriction(component, clockKey)
                    journal.edit().putBoolean("owns-clock-restriction", false).commit()
                }
                if (Build.VERSION.SDK_INT >= 30) policy.setUserControlDisabledPackages(component, if (desired.isNotEmpty()) listOf(ctx.packageName) else emptyList())
                journal.edit().putStringSet("suspended", desired).commit()
                lastDesired = desired
            }
            true
        }.getOrElse { ProtectionRuntime.error(it.message ?: "Managed protection could not apply its rules."); false }
    }
}
