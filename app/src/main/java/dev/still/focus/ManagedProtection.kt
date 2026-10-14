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
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Optional uninstall resistance: Android asks you to deactivate this administrator before removing FocuLY. No camera, password, wipe, or screen-lock policy is requested."))
    }
    fun removeAdmin(ctx: Context): Boolean {
        if (!AdminPin.authorized() || RulesEngine.strictActive(Repository.state, System.currentTimeMillis()) || isOwner(ctx)) return false
        manager(ctx).removeActiveAdmin(admin(ctx)); return true
    }
    private var lastDesired: Set<String>? = null
    private var lastProtection: Boolean? = null
    private var lastMessage: BlockMessage? = null
    private var lastStrict: Boolean? = null
    fun invalidate() { lastDesired = null; lastProtection = null; lastMessage = null; lastStrict = null }
    fun installed(ctx: Context, pkg: String) = runCatching { ctx.packageManager.getApplicationInfo(pkg, 0); true }.getOrDefault(false)
    fun appUninstallBlocked(pkg: String, ctx: Context = Repository.context) = isOwner(ctx) && runCatching { manager(ctx).isUninstallBlocked(admin(ctx), pkg) }.getOrDefault(false)
    fun appSuspended(pkg: String, ctx: Context = Repository.context) = isOwner(ctx) && runCatching { manager(ctx).isPackageSuspended(admin(ctx), pkg) }.getOrDefault(false)
    @Synchronized fun sync(ctx: Context = Repository.context): Boolean {
        if (!isOwner(ctx)) return true
        val policy = manager(ctx)
        val component = admin(ctx)
        val state = Repository.state
        val now = System.currentTimeMillis()
        val lifecycle = state.lifecycleRules.filter { it.alive(now) && it.packageName !in state.allowlist && !FocusAccessibilityService.essential(ctx, it.packageName) }
        val strict = if (state.managedLockdown) RulesEngine.strictPackages(state, now) else emptySet()
        val desired = (strict + lifecycle.filter { it.quarantineInstall }.map { it.packageName })
            .filter { !FocusAccessibilityService.essential(ctx, it) && installed(ctx, it) }.toSet()
        val selfProtection = state.uninstallProtection || RulesEngine.strictActive(state, now)
        val journal = ctx.getSharedPreferences("managed-policy", 0)
        val previous = journal.getStringSet("suspended", emptySet()).orEmpty().toSet()
        return runCatching {
            ManagedPolicyService.ensure(ctx, lifecycle.isNotEmpty() || RulesEngine.strictActive(state, now))
            if (lastProtection != selfProtection || policy.isUninstallBlocked(component, ctx.packageName) != selfProtection) {
                policy.setUninstallBlocked(component, ctx.packageName, selfProtection)
                lastProtection = selfProtection
            }
            val uninstallTargets = lifecycle.filter { it.lockUninstall && installed(ctx, it.packageName) }.map { it.packageName }.toSet()
            val oldUninstall = journal.getStringSet("uninstall-targets", emptySet()).orEmpty().toSet()
            if (oldUninstall != uninstallTargets || uninstallTargets.any { !policy.isUninstallBlocked(component, it) }) {
                journal.edit().putStringSet("uninstall-targets", oldUninstall + uninstallTargets).commit()
                (oldUninstall + uninstallTargets).filter { installed(ctx, it) }.forEach { pkg ->
                    val blocked = pkg in uninstallTargets
                    if (policy.isUninstallBlocked(component, pkg) != blocked) policy.setUninstallBlocked(component, pkg, blocked)
                }
                journal.edit().putStringSet("uninstall-targets", uninstallTargets).commit()
            }
            if (lastMessage != state.blockMessage) {
                policy.setShortSupportMessage(component, "${state.blockMessage.emoji} ${state.blockMessage.title}".trim())
                policy.setLongSupportMessage(component, "${state.blockMessage.body}\nOpen FocuLY → Device protection to review app controls. Strict rules unlock at their deadline.")
                lastMessage = state.blockMessage
            }
            if (lastDesired != desired || previous != desired) {
                // Journal before applying so process death cannot orphan suspended apps.
                val attempted = previous + desired
                journal.edit().putStringSet("suspended", attempted).commit()
                val releases = (previous - desired).filter { installed(ctx, it) }.toTypedArray()
                if (releases.isNotEmpty()) {
                    val failed = policy.setPackagesSuspended(component, releases, false)
                    check(failed.isEmpty()) { "Android could not release ${failed.size} managed app(s). Open FocuLY to retry." }
                }
                if (desired.isNotEmpty()) {
                    val failed = policy.setPackagesSuspended(component, desired.toTypedArray(), true)
                    check(failed.isEmpty()) { "Android refused managed blocking for ${failed.size} selected app(s)." }
                }
                journal.edit().putStringSet("suspended", desired).commit()
                lastDesired = desired
            }
            val controlsLocked = strict.isNotEmpty() || lifecycle.isNotEmpty()
            if (lastStrict != controlsLocked || journal.getBoolean("owns-clock-restriction", false) != strict.isNotEmpty()) {
                val clockKey = android.os.UserManager.DISALLOW_CONFIG_DATE_TIME
                if (strict.isNotEmpty() && !policy.getUserRestrictions(component).getBoolean(clockKey)) {
                    journal.edit().putBoolean("owns-clock-restriction", true).commit()
                    policy.addUserRestriction(component, clockKey)
                } else if (strict.isEmpty() && journal.getBoolean("owns-clock-restriction", false)) {
                    policy.clearUserRestriction(component, clockKey)
                    journal.edit().putBoolean("owns-clock-restriction", false).commit()
                }
                if (Build.VERSION.SDK_INT >= 30) policy.setUserControlDisabledPackages(component, if (controlsLocked) listOf(ctx.packageName) else emptyList())
                lastStrict = controlsLocked
            }
            true
        }.getOrElse { ProtectionRuntime.error(it.message ?: "Managed protection could not apply its rules."); false }
    }
}

class AppLifecycleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_REPLACED)) return
        Repository.init(context)
        ManagedProtection.invalidate()
        ManagedProtection.sync(context)
        FocusNotifications.plan(context)
    }
}
