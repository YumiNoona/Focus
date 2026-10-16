package dev.still.focus

import android.app.Notification
import android.app.NotificationManager
import android.app.job.JobScheduler
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

class BreakReminderTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    @Test fun illustratedReminderPostsWithoutSavedRulesDeduplicatesAndCanBeMuted() {
        val automation = instrumentation.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        val emulator = automation.executeShellCommand("getprop ro.kernel.qemu").use { fd -> java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText().trim() }
        Assume.assumeTrue(emulator == "1")
        instrumentation.runOnMainSync { Repository.init(ctx) }
        Assume.assumeTrue(Repository.usageGranted(ctx) && FocusNotifications.breaksAllowed(ctx))
        val old = Repository.state
        val manager = ctx.getSystemService(NotificationManager::class.java)
        try {
            instrumentation.runOnMainSync { Repository.update { FocusState(onboarding = true, breakReminders = true, breakReminderMinutes = 30) } }
            BreakReminders.clear(ctx)
            val fixture = "dev.still.fixture"
            val noon = java.time.LocalDate.now().atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            assertTrue(BreakReminders.evaluate(ctx, mapOf(fixture to 35 * 60_000L), noon, fixture))
            val notice = manager.activeNotifications.single { it.id == BreakReminders.NOTIFICATION }.notification
            assertTrue(notice.extras.getString(Notification.EXTRA_TITLE).orEmpty().contains("Scroll Test"))
            assertNotNull(notice.getLargeIcon())
            assertTrue(notice.extras.getString(Notification.EXTRA_TEMPLATE).orEmpty().contains("BigPictureStyle"))
            assertEquals(3, notice.actions.size)
            if (InstrumentationRegistry.getArguments().getString("gallery") == "true") {
                automation.executeShellCommand("input keyevent WAKEUP").close()
                automation.executeShellCommand("wm dismiss-keyguard").close()
                automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                // SystemUI text is not consistently exposed to UiAutomation across Android builds.
                // Assert the posted notification above and visually review the settled capture.
                Thread.sleep(5000)
                val bitmap = automation.takeScreenshot()!!
                val folder = java.io.File(ctx.getExternalFilesDir(null), "screen-gallery").apply { mkdirs() }
                java.io.File(folder, "break-notification-example.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            }
            assertTrue(Repository.state.apps.isEmpty() && Repository.state.blocks.isEmpty())
            val decode = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }
            val roundTrip = decode.invoke(Repository, Repository.export()) as FocusState
            assertTrue(roundTrip.breakReminders); assertEquals(30, roundTrip.breakReminderMinutes)
            assertFalse(BreakReminders.evaluate(ctx, mapOf(fixture to 90 * 60_000L), noon + 3 * 3_600_000))
            notice.actions.single { it.title == "Quiet today" }.actionIntent.send()
            Thread.sleep(300)
            assertFalse(BreakReminders.evaluate(ctx, mapOf("com.android.chrome" to 120 * 60_000L), noon + 4 * 3_600_000))
            instrumentation.runOnMainSync { Repository.update { it.copy(breakReminders = false) } }
            assertNull(ctx.getSystemService(JobScheduler::class.java).getPendingJob(140))
            assertTrue(manager.activeNotifications.none { it.id == BreakReminders.NOTIFICATION })
        } finally {
            BreakReminders.clear(ctx)
            instrumentation.runOnMainSync { Repository.update { old } }
        }
    }
}
