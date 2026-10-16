package dev.still.focus

import android.app.job.*
import android.content.*
import android.os.Handler
import android.os.Looper
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Optional local hints, independent of saved targets. Never starts a block or displays an overlay. */
object BreakReminders {
    private const val JOB = 140
    const val NOTIFICATION = 140
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("break-reminders", 0)
    fun schedule(ctx: Context) {
        val scheduler = ctx.getSystemService(JobScheduler::class.java)
        if (!Repository.state.breakReminders || !Repository.usageGranted(ctx) || !FocusNotifications.breaksAllowed(ctx)) {
            scheduler.cancel(JOB)
            if (!Repository.state.breakReminders) ctx.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION)
        } else if (scheduler.getPendingJob(JOB) == null) {
            scheduler.schedule(JobInfo.Builder(JOB, ComponentName(ctx, BreakReminderJob::class.java))
                .setPeriodic(15 * 60_000L).setPersisted(true).setRequiresBatteryNotLow(true).build())
        }
    }
    @Synchronized fun evaluate(ctx: Context, usage: Map<String, Long>, now: Long = System.currentTimeMillis(), foreground: String? = null): Boolean {
        if (!FocusNotifications.breaksAllowed(ctx) || !Repository.usageGranted(ctx)) return false
        val pref = prefs(ctx)
        val history = BreakHistory(pref.getString("day", "").orEmpty(), pref.getStringSet("shown", emptySet()).orEmpty().toSet(),
            pref.getInt("count", 0), pref.getLong("last", 0), pref.getLong("quiet-until", 0))
        val ctxManager = ctx.packageManager
        val eligible = usage.filterValues { it >= Repository.state.breakReminderMinutes * 60_000L }.keys.filter { pkg ->
            !FocusAccessibilityService.essential(ctx, pkg) && ctxManager.getLaunchIntentForPackage(pkg) != null
        }.toSet()
        val pkg = BreakPolicy.choose(Repository.state, usage, eligible, history, now, foreground = foreground) ?: return false
        val label = runCatching { ctxManager.getApplicationLabel(ctxManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        if (!FocusNotifications.breakReminder(ctx, label, usage.getValue(pkg), pkg)) return false
        val day = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val sameDay = history.day == day
        pref.edit().putString("day", day).putStringSet("shown", (if (sameDay) history.shown else emptySet()) + pkg)
            .putInt("count", (if (sameDay) history.count else 0) + 1).putLong("last", now).commit()
        return true
    }
    fun quietToday(ctx: Context) {
        val until = java.time.LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        prefs(ctx).edit().putLong("quiet-until", until).apply()
        ctx.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION)
    }
    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        ctx.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION)
    }
}

class BreakReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "dev.still.QUIET_BREAKS") BreakReminders.quietToday(context)
    }
}

class BreakReminderJob : JobService() {
    private val generation = AtomicInteger()
    private val executor = Executors.newSingleThreadExecutor()
    override fun onStartJob(params: JobParameters): Boolean {
        Repository.init(this)
        if (!Repository.state.breakReminders || !Repository.usageGranted(this) || !FocusNotifications.breaksAllowed(this)) return false
        val token = generation.incrementAndGet()
        executor.execute {
            val usage = UsageReader.day(this)
            Handler(Looper.getMainLooper()).post {
                if (generation.get() == token) { BreakReminders.evaluate(this, usage); jobFinished(params, false) }
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { generation.incrementAndGet(); return true }
    override fun onDestroy() { generation.incrementAndGet(); executor.shutdownNow(); super.onDestroy() }
}
