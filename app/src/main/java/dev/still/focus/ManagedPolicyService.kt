package dev.still.focus

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*

/** Visible, owner-only monitor: Android 8+ restricts manifest package-added broadcasts. */
class ManagedPolicyService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val reconcile = object : Runnable {
        override fun run() { if (!running) return; Repository.reconcile(); if (running) handler.postDelayed(this, 2000) }
    }
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        if (!ManagedProtection.isOwner(this)) { stopSelf(); return }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("managed-policies", "Managed app controls", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, "managed-policies").setSmallIcon(R.drawable.ic_still)
            .setContentTitle("Focus app controls are active")
            .setContentText("Watching installations and policy deadlines on this managed device.")
            .setContentIntent(FocusNotifications.open(this, "managed")).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(3, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(3, notification)
        running = true; requested = true
        handler.post(reconcile)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() { handler.removeCallbacks(reconcile); running = false; requested = false; super.onDestroy() }
    override fun onBind(intent: Intent?) = null
    companion object {
        private var running = false
        private var requested = false
        fun ensure(ctx: Context, needed: Boolean) {
            if (needed && !running && !requested) {
                requested = true
                try { ctx.startForegroundService(Intent(ctx, ManagedPolicyService::class.java)) }
                catch (error: RuntimeException) { requested = false; throw error }
            } else if (!needed && (running || requested)) { ctx.stopService(Intent(ctx, ManagedPolicyService::class.java)); requested = false }
        }
    }
}
