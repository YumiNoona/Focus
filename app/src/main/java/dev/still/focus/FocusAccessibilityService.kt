package dev.still.focus

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import java.time.LocalDate

class FocusAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = ""
    private var openedAt = 0L
    private var openingElapsed = 0L
    private var lastPoll = System.currentTimeMillis()
    private var website: String? = null
    private var feed: String? = null
    private var overlay: View? = null
    private var overlayKey = ""
    private var usage = emptyMap<String, Long>()
    private var usageAt = 0L
    private var readingUsage = false
    private var usageDate: LocalDate = LocalDate.now()
    private var screenSeenAt = 0L
    private var overridePending = false
    private var generation = 0
    private var lastPhase = ""
    private var eventPackage: String? = null
    private var fallbackAt = 0L
    private var fallbackReading = false
    private fun refreshForeground(event: AccessibilityEvent? = null) {
        val eventPkg = event?.packageName?.toString()
        val imePackages = windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            .mapNotNull { it.root?.packageName?.toString() }.toSet()
        if (eventPkg != null &&
            eventPkg !in imePackages && !eventPkg.contains("inputmethod") && eventPkg != "com.android.systemui" &&
            (eventPkg != packageName || event.className?.toString() == MainActivity::class.java.name)) eventPackage = eventPkg
        val appWindows = windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { window -> window.root?.packageName?.toString()?.let { AppWindow(it, window.layer, window.isFocused) } }
            .filterNot { it.packageName in imePackages }
        val rootPackage = if (overlay == null) rootInActiveWindow?.packageName?.toString()?.takeUnless { it in imePackages || it == "com.android.systemui" } else null
        if (appWindows.isEmpty() && Repository.usageGranted(this) && !fallbackReading && System.currentTimeMillis() - fallbackAt > 2000) {
            fallbackReading = true; fallbackAt = System.currentTimeMillis()
            Thread {
                val recent = UsageReader.foreground(this)
                handler.post { if (instance === this@FocusAccessibilityService) { fallbackReading = false; recent?.let { eventPackage = it }; refreshForeground(); evaluate() } }
            }.start()
        }
        val resolved = ForegroundResolver.choose(appWindows, rootPackage ?: eventPackage) ?: return
        if (resolved != foreground) {
            clearOverlay(); foreground = resolved; openedAt = System.currentTimeMillis(); openingElapsed = 0L
            website = null; feed = null
        }
        ProtectionRuntime.observed(resolved)
    }
    private val poll = object : Runnable {
        override fun run() {
            val tickNow = System.currentTimeMillis()
            val interactive = getSystemService(android.os.PowerManager::class.java).isInteractive
            if (interactive) refreshForeground()
            if (interactive && overlay == null && foreground.isNotBlank()) openingElapsed += (tickNow - lastPoll).coerceIn(0, 5000)
            lastPoll = tickNow
            if (!interactive) clearOverlay()
            ProtectionRuntime.tick()
            Repository.reconcile()
            val s = Repository.state.session
            val phase = "${s?.id}:${s?.onBreak(System.currentTimeMillis())}"
            if (phase != lastPhase) { lastPhase = phase; FocusNotifications.session(this@FocusAccessibilityService); FocusNotifications.plan(this@FocusAccessibilityService) }
            if (Repository.state.apps.any { it.enabled && it.dailyMinutes > 0 } && System.currentTimeMillis() - usageAt > 10_000 && !readingUsage && Repository.usageGranted(this@FocusAccessibilityService)) {
                readingUsage = true
                Thread {
                    val data = UsageReader.day(this@FocusAccessibilityService)
                    handler.post { if (instance === this@FocusAccessibilityService) { usage = data; usageAt = System.currentTimeMillis(); usageDate = LocalDate.now(); readingUsage = false; evaluate() } }
                }.start()
            }
            evaluate()
            handler.postDelayed(this, 500)
        }
    }
    override fun onServiceConnected() {
        Repository.init(this)
        instance = this
        ProtectionRuntime.connect()
        handler.removeCallbacks(poll)
        handler.post(poll)
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        refreshForeground(event)
        val pkg = foreground
        if (pkg.isBlank() || essential(this, pkg)) { clearOverlay(); return }
        val now = System.currentTimeMillis()
        val state = Repository.state
        val needsScreen = (pkg in browserIds && (state.domains.isNotEmpty() || state.profiles.any { it.domains.isNotEmpty() } || state.blocks.any { it.kind == BlockKind.WEBSITES && it.alive(now) })) ||
            ((state.feeds.isNotEmpty() || state.blocks.any { it.kind == BlockKind.FEEDS && it.alive(now) }) && pkg in setOf("com.google.android.youtube", "com.instagram.android", "com.facebook.katana"))
        if (needsScreen && now - screenSeenAt > 200 && overlay == null) {
            screenSeenAt = now
            val root = windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }.maxByOrNull { it.layer }?.root ?: rootInActiveWindow
            if (root != null && root.packageName?.toString() == pkg) inspect(root, pkg)
        }
        evaluate()
    }
    private fun inspect(root: AccessibilityNodeInfo, pkg: String) {
        var address: String? = null
        var detectedFeed: String? = null
        val browser = browserIds[pkg]
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count++ < 700) {
            val node = queue.removeFirst()
            val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
            if (browser != null && id in browser) node.text?.toString()?.let { if (RulesEngine.normalizeDomain(it) != null) address = it }
            if (pkg == "com.google.android.youtube" && (id in setOf("reel_recycler", "reel_watch_fragment_root", "shorts_player") || (node.isSelected && node.contentDescription?.toString()?.equals("Shorts", true) == true))) detectedFeed = "YouTube Shorts"
            if (pkg == "com.instagram.android" && (id in setOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container") || (node.isSelected && node.contentDescription?.toString()?.equals("Reels", true) == true))) detectedFeed = "Instagram Reels"
            if (pkg == "com.facebook.katana" && node.isSelected && node.contentDescription?.toString()?.equals("Reels", true) == true) detectedFeed = "Facebook Reels"
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        website = address
        feed = detectedFeed
    }
    private fun evaluate() {
        if (!getSystemService(android.os.PowerManager::class.java).isInteractive) { clearOverlay(); return }
        if (!Repository.state.disclosure || foreground.isBlank() || essential(this, foreground)) { clearOverlay(); return }
        if (usageDate != LocalDate.now()) { usage = emptyMap(); usageAt = 0L }
        val now = System.currentTimeMillis()
        val used = if (Repository.usageGranted(this)) (usage[foreground] ?: 0L) + if (usageAt > 0 && overlay == null) (now - maxOf(usageAt, openedAt)).coerceAtLeast(0) else 0 else 0
        val reason = RulesEngine.restriction(Repository.state, foreground, now, used, openingElapsed, website, feed)
        if (reason == null) { clearOverlay(); return }
        if (overlay == null) showOverlay(reason)
        else if (overlayKey != "$foreground:${reason.explanation}:${reason.mode}:${reason.until}") { clearOverlay(); showOverlay(reason) }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun background(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun showOverlay(restriction: Restriction) {
        if (overlay != null) return
        val blockedPackage = foreground
        val state = Repository.state
        val dark = state.theme == "Dark" || (state.theme == "System" && resources.configuration.uiMode and 0x30 == 0x20)
        val bg = Color.parseColor(if (dark) "#19181C" else "#F9F8F4")
        val ink = Color.parseColor(if (dark) "#F4F1F6" else "#25232A")
        val secondary = Color.parseColor(if (dark) "#B3ACBA" else "#77737D")
        val violet = Color.parseColor(if (dark) "#C7B5F2" else "#7760BA")
        val mono = resources.getFont(R.font.jetbrains_mono)
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(56), dp(28), dp(40)); setBackgroundColor(bg) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(container) }
        fun text(value: String, size: Float, color: Int = ink, useMono: Boolean = false): TextView = TextView(this).apply {
            this.text = value; textSize = size; setTextColor(color); if (useMono) typeface = mono
            setLineSpacing(dp(5).toFloat(), 1f); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) }
        }.also(container::addView)
        fun button(value: String, primary: Boolean = false, click: () -> Unit): Button = Button(this).apply {
            this.text = value; isAllCaps = false; typeface = mono; textSize = 14f
            setTextColor(if (primary) bg else violet)
            background = background(if (primary) violet else Color.TRANSPARENT)
            elevation = 0f; stateListAnimator = null
            minHeight = dp(56)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            setPadding(dp(16), dp(16), dp(16), dp(16)); setOnClickListener { if (Repository.state.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); click() }
        }.also(container::addView)
        text("focus / ${restriction.mode.label.lowercase()}", 14f, violet, true)
        text(if (restriction.mode == FocusMode.STRICT) "This app stays locked." else "Take a moment.", if (restriction.mode == FocusMode.GENTLE) 22f else 28f, ink, true)
        text(restriction.label, 18f, ink, true)
        text(restriction.explanation, 16f, secondary)
        val until = restriction.until
        if (until != null && until > 0) {
            text("Unlocks ${java.time.Instant.ofEpochMilli(until).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))}", 14f, secondary, true)
            val counter = text("", 14f, secondary, true)
            val myGeneration = generation
            handler.post(object : Runnable {
                override fun run() {
                    if (generation != myGeneration || overlay == null) return
                    val remaining = (until - System.currentTimeMillis()).coerceAtLeast(0)
                    counter.text = if (remaining >= 86_400_000) "${remaining / 86_400_000}d ${remaining / 3_600_000 % 24}h left" else "${(remaining + 59_999) / 60_000} min left"
                    handler.postDelayed(this, 1000)
                }
            })
        } else if (until == 0L) text("No automatic ending", 14f, secondary, true)
        button("Return to focus", true) { overridePending = false; performGlobalAction(GLOBAL_ACTION_HOME); foreground = ""; clearOverlay() }
        val reason = EditText(this).apply {
            hint = "What are you opening this for?"; setTextColor(ink); setHintTextColor(secondary); textSize = 16f
            setSingleLine(false); maxLines = 3
            setPadding(dp(12), dp(16), dp(12), dp(16)); visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
        }
        container.addView(reason)
        val canOverride = RulesEngine.canOverride(state, blockedPackage, System.currentTimeMillis(), restriction.mode)
        if (canOverride) {
            var remaining = restriction.mode.waitSeconds
            val ask = restriction.mode == FocusMode.STANDARD || state.askReason
            val visitMinutes = if (restriction.mode == FocusMode.STANDARD) 1 else state.overrideMinutes
            val visitLabel = "$visitMinutes ${if (visitMinutes == 1) "minute" else "minutes"}"
            lateinit var visit: Button
            visit = button(if (restriction.mode == FocusMode.GENTLE && !ask) "Continue for $visitLabel" else "Take an intentional visit") {
                if (restriction.mode == FocusMode.GENTLE && !ask) {
                    if (Repository.allow(blockedPackage, "", restriction.mode)) clearOverlay()
                    return@button
                }
                if (!overridePending) {
                    overridePending = true
                    if (ask) reason.visibility = View.VISIBLE
                    val myGeneration = generation
                    val wait = object : Runnable {
                        override fun run() {
                            if (generation != myGeneration || overlay == null) return
                            visit.text = if (remaining > 0) "Pause for ${remaining}s" else "Allow for $visitLabel"
                            visit.isEnabled = remaining == 0
                            if (remaining > 0) { remaining--; handler.postDelayed(this, 1000) }
                        }
                    }
                    handler.post(wait)
                } else {
                    if (ask && reason.text.toString().isBlank()) { reason.error = "Add a short intention first"; return@button }
                    if (Repository.allow(blockedPackage, reason.text.toString().take(120), restriction.mode)) { overridePending = false; clearOverlay() }
                    else visit.text = "Cooldown is still active"
                }
            }
        } else text(if (restriction.mode == FocusMode.STRICT) "No early unblock. Essential apps stay available." else "Cooldown active. Essential apps stay available.", 14f, secondary)
        button("Emergency & system access") { overridePending = false; performGlobalAction(GLOBAL_ACTION_HOME); foreground = ""; clearOverlay() }
        val normal = restriction.mode == FocusMode.GENTLE
        val params = WindowManager.LayoutParams(if (normal) (resources.displayMetrics.widthPixels * .92).toInt() else -1, if (normal) -2 else -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, android.graphics.PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.CENTER
        if (normal) { params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND; params.dimAmount = .45f }
        if (normal) { container.setPadding(dp(24), dp(24), dp(24), dp(24)); container.background = background(bg, 24) }
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        overlayKey = "$foreground:${restriction.explanation}:${restriction.mode}:${restriction.until}"
        overlay = scroll
        runCatching { getSystemService(WindowManager::class.java).addView(scroll, params); ProtectionRuntime.blocked(blockedPackage) }
            .onFailure { overlay = null; overlayKey = ""; ProtectionRuntime.error("Android could not show the blocking screen. Re-enable Focus protection and check background access.") }
    }
    fun clearOverlay() {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null; overlayKey = ""; generation++; overridePending = false
        ProtectionRuntime.cleared()
    }
    override fun onInterrupt() { clearOverlay() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); clearOverlay(); instance = null; ProtectionRuntime.disconnect(); super.onDestroy() }
    companion object {
        private var serviceReference = java.lang.ref.WeakReference<FocusAccessibilityService>(null)
        var instance: FocusAccessibilityService?
            get() = serviceReference.get()
            private set(value) { serviceReference = java.lang.ref.WeakReference(value) }
        private val browserIds = mapOf(
            "com.android.chrome" to setOf("url_bar"), "com.chrome.beta" to setOf("url_bar"),
            "com.microsoft.emmx" to setOf("url_bar"), "com.brave.browser" to setOf("url_bar"),
            "org.mozilla.firefox" to setOf("mozac_browser_toolbar_url_view", "mozac_browser_toolbar_edit_url_view"))
        fun essential(ctx: Context, pkg: String): Boolean {
            if (pkg == ctx.packageName || pkg in setOf("android", "com.android.settings", "com.android.systemui", "com.android.permissioncontroller", "com.google.android.permissioncontroller", "com.android.phone", "com.android.server.telecom", "com.android.emergency", "com.google.android.apps.safetyhub", "com.android.incallui")) return true
            val launcher = ctx.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
            val dialer = ctx.getSystemService(android.telecom.TelecomManager::class.java)?.defaultDialerPackage
            return pkg == launcher || pkg == dialer
        }
    }
}
