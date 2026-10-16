package dev.still.focus

import android.Manifest
import android.app.*
import android.appwidget.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import java.time.*

object FocusNotifications {
    private const val CHANNEL = "focus"
    private fun manager(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).also {
        it.createNotificationChannel(NotificationChannel(CHANNEL, "FocuLY sessions", NotificationManager.IMPORTANCE_LOW).apply { description = "Session status, completion, and schedule reminders" })
    }
    private fun permitted(ctx: Context) = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun breaksAllowed(ctx: Context): Boolean {
        val mgr = ctx.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel("breaks") == null) mgr.createNotificationChannel(NotificationChannel("breaks", "Gentle break reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Optional daytime hints after extended app use" })
        return permitted(ctx) && mgr.areNotificationsEnabled() && mgr.getNotificationChannel("breaks").importance != NotificationManager.IMPORTANCE_NONE
    }
    fun catIcon(ctx: Context, art: CatArt): Bitmap {
        val source = BitmapFactory.decodeResource(ctx.resources, art.drawable, BitmapFactory.Options().apply { inSampleSize = 4 })
        return Bitmap.createScaledBitmap(source, 192, 192, true).also { if (it !== source) source.recycle() }
    }
    private fun catPicture(ctx: Context, art: CatArt): Bitmap {
        val icon = catIcon(ctx, art)
        val picture = Bitmap.createBitmap(480, 240, Bitmap.Config.ARGB_8888)
        Canvas(picture).apply { drawColor(Color.rgb(48, 75, 107)); drawBitmap(icon, null, RectF(128f, 8f, 352f, 232f), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)) }
        icon.recycle()
        return picture
    }
    fun breakReminder(ctx: Context, label: String, millis: Long, pkg: String): Boolean {
        if (!breaksAllowed(ctx)) return false
        val arts = listOf(CatArt.BREATHE, CatArt.TEA, CatArt.CLOUD, CatArt.BREAK, CatArt.PILLOW)
        val art = arts[Math.floorMod(pkg.hashCode(), arts.size)]
        val title = "A little breather from $label?"
        val body = "${millis / 60_000} minutes today. Rest your eyes, stretch, or give yourself a quiet moment."
        val quiet = PendingIntent.getBroadcast(ctx, 140, Intent(ctx, BreakReminderReceiver::class.java).setAction("dev.still.QUIET_BREAKS"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(ctx, "breaks").setSmallIcon(R.drawable.ic_still)
            .setContentTitle(title).setContentText(body).setLargeIcon(catIcon(ctx, art))
            .setStyle(Notification.BigPictureStyle().bigPicture(catPicture(ctx, art)).setBigContentTitle(title).setSummaryText(body))
            .setContentIntent(open(ctx, "insights")).setAutoCancel(true).setCategory(Notification.CATEGORY_RECOMMENDATION)
            .addAction(Notification.Action.Builder(null, "Take a break", open(ctx, "setup")).build())
            .addAction(Notification.Action.Builder(null, "Quiet today", quiet).build())
            .addAction(Notification.Action.Builder(null, "Settings", open(ctx, "settings")).build()).build()
        return runCatching { ctx.getSystemService(NotificationManager::class.java).notify(BreakReminders.NOTIFICATION, notification); true }.getOrDefault(false)
    }
    fun protectionAlert(ctx: Context, title: String, body: String) {
        if (!permitted(ctx)) return
        val mgr = ctx.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(NotificationChannel("protection", "Protection changes", NotificationManager.IMPORTANCE_DEFAULT))
        mgr.notify(141, Notification.Builder(ctx, "protection").setSmallIcon(R.drawable.ic_still).setLargeIcon(catIcon(ctx, CatArt.GUARD))
            .setContentTitle(title).setContentText(body).setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open(ctx, "managed")).setAutoCancel(true).build())
    }
    fun open(ctx: Context, action: String = "home"): PendingIntent = PendingIntent.getActivity(ctx, action.hashCode(), Intent(ctx, MainActivity::class.java).putExtra("screen", action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    fun session(ctx: Context) {
        val mgr = manager(ctx)
        val s = Repository.state.session
        if (s == null) { mgr.cancel(1); return }
        if (!permitted(ctx)) return
        val status = if (!s.timerOnly && Repository.protectionReady(ctx)) "${s.profile.mode.label} · protection connected" else "Blocking unavailable · check protection"
        mgr.notify(1, Notification.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_still)
            .setContentTitle("${s.profile.name} · in progress")
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
        state.blocks.filter { it.end > now }.forEach { times += it.end }
        state.lifecycleRules.filter { it.end > now }.forEach { times += it.end }
        state.session?.let { s -> if (s.end > now) times += s.end }
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
        qsTile?.apply { state = if (Repository.state.session != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE; label = "FocuLY"; if (Build.VERSION.SDK_INT >= 29) subtitle = if (Repository.state.session != null) "View session" else "Start focus"; updateTile() }
    }
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated") // The Intent overload is required by Android 9–13; the API 34+ branch uses PendingIntent.
    override fun onClick() {
        Repository.init(this)
        unlockAndRun {
            val profile = Repository.state.profiles.firstOrNull() ?: FocusState.defaults().first()
            val canStart = Repository.state.session == null && profile.mode != FocusMode.STRICT &&
                Repository.protectionReady(this) && RulesEngine.selectedRuleCount(Repository.state, profile) > 0
            if (canStart && Repository.start(profile, profile.minutes, profile.intention)) refresh()
            else {
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
