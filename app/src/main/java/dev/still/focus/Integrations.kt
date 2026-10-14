package dev.still.focus

import android.Manifest
import android.app.*
import android.appwidget.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import java.time.*

object FocusNotifications {
    private const val CHANNEL = "focus"
    private fun manager(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).also {
        it.createNotificationChannel(NotificationChannel(CHANNEL, "Focus sessions", NotificationManager.IMPORTANCE_LOW).apply { description = "Session status, completion, and schedule reminders" })
    }
    private fun permitted(ctx: Context) = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun open(ctx: Context, action: String = "home"): PendingIntent = PendingIntent.getActivity(ctx, action.hashCode(), Intent(ctx, MainActivity::class.java).putExtra("screen", action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun session(ctx: Context) {
        val mgr = manager(ctx)
        val s = Repository.state.session
        if (s == null) { mgr.cancel(1); return }
        if (!permitted(ctx)) return
        val status = if (!s.timerOnly && Repository.protectionReady(ctx)) "${s.profile.mode.label} · protection connected" else "Blocking unavailable · check protection"
        mgr.notify(1, Notification.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_still)
            .setContentTitle("${s.profile.name} · ${if (s.onBreak(System.currentTimeMillis())) "break" else "in progress"}")
            .setContentText(status).setContentIntent(open(ctx)).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "View session", open(ctx)).build())
            .addAction(Notification.Action.Builder(null, "Manage protection", open(ctx, "settings")).build()).build())
    }
    fun message(ctx: Context, title: String, body: String) {
        if (permitted(ctx)) manager(ctx).notify(2, Notification.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_still).setContentTitle(title).setContentText(body).setContentIntent(open(ctx)).setAutoCancel(true).build())
    }
    fun plan(ctx: Context) {
        val now = System.currentTimeMillis()
        val state = Repository.state
        val times = mutableListOf<Long>()
        state.session?.let { s ->
            if (s.end > now) times += s.end
            if (s.profile.pomodoro && s.alive(now)) times += now + s.phaseRemaining(now) + 1000
        }
        state.schedules.filter { it.enabled }.forEach { schedule ->
            schedule.nextBoundary(now)?.let { times += it }
            schedule.nextStart(now)?.let { next ->
                times += next
                if (schedule.reminder && state.reminders && next - 300_000 > now) times += next - 300_000
            }
        }
        val alarm = ctx.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, FocusReceiver::class.java).setAction("dev.still.TICK"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarm.cancel(pending)
        times.minOrNull()?.let {
            if (state.managedLockdown && ManagedProtection.isOwner(ctx) && ManagedProtection.preciseEnding(ctx)) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pending)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it, pending)
        }
    }
}

class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf("dev.still.TICK", Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED)) return
        Repository.init(context)
        Repository.reconcile()
        val now = System.currentTimeMillis()
        if (Repository.state.reminders) Repository.state.schedules.filter { it.enabled && it.reminder }.forEach { schedule ->
            val next = schedule.nextStart(now) ?: return@forEach
            if (next - now in 0..300_000) {
                val pref = context.getSharedPreferences("reminders", 0)
                val key = "${schedule.id}:$next"
                if (!pref.getBoolean(key, false)) {
                    pref.edit().putBoolean(key, true).apply()
                    FocusNotifications.message(context, "A moment to prepare.", "${schedule.name} starts in ${(next - now) / 60_000 + 1} minutes.")
                }
            }
        }
        FocusNotifications.session(context)
        FocusWidget.refresh(context)
        FocusNotifications.plan(context)
    }
}

class FocusTile : TileService() {
    override fun onStartListening() { Repository.init(this); Repository.reconcile(); refresh() }
    private fun refresh() {
        qsTile?.apply { state = if (Repository.state.session != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; label = "Focus"; if (Build.VERSION.SDK_INT >= 29) subtitle = if (Repository.state.session != null) "View session" else "Start focus"; updateTile() }
    }
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // The Intent overload is required by Android 9–13; the API 34+ branch uses PendingIntent.
    override fun onClick() {
        Repository.init(this)
        unlockAndRun {
            if (Repository.state.session == null && Repository.protectionReady(this) && (Repository.state.apps.any { it.enabled } || Repository.state.domains.isNotEmpty() || Repository.state.feeds.isNotEmpty())) {
                val p = Repository.state.profiles.firstOrNull() ?: FocusState.defaults().first()
                Repository.start(p, p.minutes, p.intention)
                refresh()
            } else {
                val pending = FocusNotifications.open(this)
                if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(pending)
                else { @Suppress("DEPRECATION") startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
}

class FocusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { Repository.init(context); refresh(context) }
    companion object {
        fun refresh(ctx: Context) {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, FocusWidget::class.java))
            if (ids.isEmpty()) return
            val s = Repository.state.session
            val views = RemoteViews(ctx.packageName, R.layout.focus_widget)
            views.setTextViewText(R.id.widget_status, s?.let { "${it.profile.name} · ${if (it.end == 0L) "open-ended" else "${((it.end - System.currentTimeMillis()).coerceAtLeast(0) + 59_999) / 60_000} min left"}" } ?: "Make room for what matters.")
            views.setTextViewText(R.id.widget_action, if (s == null) "Start focus" else "View session")
            views.setOnClickPendingIntent(R.id.widget_action, FocusNotifications.open(ctx, if (s == null) "setup" else "home"))
            manager.updateAppWidget(ids, views)
        }
    }
}
