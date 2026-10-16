package dev.still.focus

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import androidx.compose.ui.graphics.toArgb
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
    private var pendingInspection: Runnable? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }
    private val feedAudioFocus by lazy { AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setOnAudioFocusChangeListener(audioFocusListener).build() }
    private var pausedFeedAudio = false
    private var feedOverlayOnHome = false
    private val launcherPackage by lazy {
        packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
    }
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
        // A feed block parks the video app behind Home but keeps the accessibility overlay visible.
        // Calls and Android settings still replace the overlay normally.
        if (feedOverlayOnHome && overlay != null && resolved == launcherPackage) return
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
            val phase = s?.id.orEmpty()
            if (phase != lastPhase) { lastPhase = phase; FocusNotifications.session(this@FocusAccessibilityService); FocusNotifications.plan(this@FocusAccessibilityService) }
            if (Repository.state.apps.any { it.enabled && it.dailyMinutes > 0 } && System.currentTimeMillis() - usageAt > 10_000 && !readingUsage && Repository.usageGranted(this@FocusAccessibilityService)) {
                readingUsage = true
                Thread {
                    val data = UsageReader.day(this@FocusAccessibilityService)
                    handler.post { if (instance === this@FocusAccessibilityService) { usage = data; usageAt = System.currentTimeMillis(); usageDate = LocalDate.now(); readingUsage = false; evaluate() } }
                }.start()
            }
            evaluate()
            handler.postDelayed(this, 250)
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
        // Content events are frequent on video feeds; their package is already known.
        // Window transitions still take the full resolver path.
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.packageName?.toString() != foreground) refreshForeground(event)
        val pkg = foreground
        if (pkg.isBlank() || essential(this, pkg)) { clearOverlay(); return }
        val now = System.currentTimeMillis()
        val state = Repository.state
        val needsScreen = (pkg in browserIds && (state.domains.isNotEmpty() || state.profiles.any { it.domains.isNotEmpty() } || state.blocks.any { it.kind == BlockKind.WEBSITES && it.alive(now) })) ||
            ((state.feeds.isNotEmpty() || state.blocks.any { it.kind == BlockKind.FEEDS && it.alive(now) }) && pkg in setOf("com.google.android.youtube", "com.instagram.android", "com.facebook.katana"))
        if (needsScreen && overlay == null) {
            if (now - screenSeenAt >= 75 || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) inspectCurrentScreen(pkg)
            else if (pendingInspection == null) {
                pendingInspection = Runnable { pendingInspection = null; if (foreground == pkg && overlay == null) { inspectCurrentScreen(pkg); evaluate() } }
                handler.postDelayed(pendingInspection!!, 75)
            }
        }
        evaluate()
    }
    private fun inspectCurrentScreen(pkg: String) {
        screenSeenAt = System.currentTimeMillis()
        val root = rootInActiveWindow ?: windows.filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
            .maxByOrNull { it.layer }?.root
        if (root?.packageName?.toString() == pkg) inspect(root, pkg)
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
        // Parked feed overlays use the same consent, allowance and mode checks as every other block.
        // Returning early here would leave an old Normal visit button after a mode change.
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
    private fun background(color: Int, radius: Int = 24) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.parseColor("#66FFFFFF")) }
    private fun showOverlay(restriction: Restriction) {
        if (overlay != null) return
        val blockedPackage = foreground
        val state = Repository.state
        val dark = state.theme == "Dark" || (state.theme == "System" && resources.configuration.uiMode and 0x30 == 0x20)
        val bg = Color.parseColor("#204875")
        val ink = Color.WHITE
        val secondary = Color.parseColor("#F1F5FC")
        val violet = Color.parseColor("#D2E6FF")
        val mono = resources.getFont(R.font.jetbrains_mono)
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28), dp(56), dp(28), dp(40)); setBackgroundColor(Color.TRANSPARENT) }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = nativeGlassScene(this@FocusAccessibilityService, dark, if (restriction.mode == FocusMode.GENTLE) dp(24).toFloat() else 0f)
            addView(container)
        }
        fun text(value: String, size: Float, color: Int = ink): TextView = TextView(this).apply {
            this.text = value; textSize = size; setTextColor(color); typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(dp(5).toFloat(), 1f); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) }
        }.also(container::addView)
        fun button(value: String, primary: Boolean = false, click: () -> Unit): Button = Button(this).apply {
            this.text = value; isAllCaps = false; typeface = mono; textSize = 14f
            setTextColor(if (primary) bg else violet)
            background = background(Color.parseColor(if (primary) "#E6F2F7FF" else "#335A7DA3"))
            elevation = 0f; stateListAnimator = null
            minHeight = dp(56)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
            setPadding(dp(16), dp(16), dp(16), dp(16)); setOnClickListener { if (Repository.state.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); click() }
        }.also(container::addView)
        text("FocuLY / ${restriction.mode.label.lowercase()}", 14f, violet)
        if (state.blockMessage.art != "NONE" || state.blockMessage.image.isNotEmpty()) ImageView(this).apply {
            val custom = BlockingArtwork.bitmap(this@FocusAccessibilityService, state.blockMessage.image)
            if (custom != null) setImageBitmap(custom) else setImageResource(BlockingArtwork.art(state.blockMessage, restriction.mode).drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = background(Color.parseColor("#33FFFFFF"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = LinearLayout.LayoutParams(-1, dp(if (restriction.mode == FocusMode.GENTLE) 86 else 120)).apply { bottomMargin = dp(16) }
        }.also(container::addView)
        if (state.blockMessage.emoji.isNotBlank()) text(state.blockMessage.emoji, 44f)
        text(state.blockMessage.title.ifBlank { "See you later." }, if (restriction.mode == FocusMode.GENTLE) 22f else 28f, ink)
        if (state.blockMessage.body.isNotBlank()) text(state.blockMessage.body, 16f, secondary)
        text(restriction.label, 18f, ink)
        text(restriction.explanation, 16f, secondary)
        val until = restriction.until
        if (until != null && until > 0) {
            text("Unlocks ${java.time.Instant.ofEpochMilli(until).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))}", 14f, secondary)
            val counter = text("", 14f, secondary)
            val myGeneration = generation
            handler.post(object : Runnable {
                override fun run() {
                    if (generation != myGeneration || overlay == null) return
                    val remaining = (until - System.currentTimeMillis()).coerceAtLeast(0)
                    counter.text = if (remaining >= 86_400_000) "${remaining / 86_400_000}d ${remaining / 3_600_000 % 24}h left" else "${(remaining + 59_999) / 60_000} min left"
                    handler.postDelayed(this, 1000)
                }
            })
        } else if (until == 0L) text("No automatic ending", 14f, secondary)
        button("Return to focus", true) { overridePending = false; performGlobalAction(GLOBAL_ACTION_HOME); foreground = ""; clearOverlay() }
        val reason = EditText(this).apply {
            hint = "What are you opening this for?"; setTextColor(ink); setHintTextColor(secondary); textSize = 16f
            typeface = mono
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
        } else TextView(this).apply {
            text = if (restriction.mode == FocusMode.STRICT) "No early unblock. Essential apps stay available." else "Cooldown active. Essential apps stay available."
            textSize = 14f; setTextColor(secondary); typeface = mono
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24); bottomMargin = dp(20) }
        }.also(container::addView)
        button("Emergency & system access") { overridePending = false; performGlobalAction(GLOBAL_ACTION_HOME); foreground = ""; clearOverlay() }
        val normal = restriction.mode == FocusMode.GENTLE
        val params = WindowManager.LayoutParams(if (normal) (resources.displayMetrics.widthPixels * .92).toInt() else -1, if (normal) -2 else -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, android.graphics.PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.CENTER
        if (normal) { params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND; params.dimAmount = .45f }
        if (normal) { container.setPadding(dp(24), dp(24), dp(24), dp(24)) }
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        overlayKey = "$foreground:${restriction.explanation}:${restriction.mode}:${restriction.until}"
        overlay = scroll
        runCatching { getSystemService(WindowManager::class.java).addView(scroll, params) }
            .onSuccess {
                if (feed != null && restriction.label == feed) runCatching {
                    pauseFeedPlayback()
                    feedOverlayOnHome = true
                    if (!performGlobalAction(GLOBAL_ACTION_HOME)) feedOverlayOnHome = false
                }
                ProtectionRuntime.blocked(blockedPackage)
            }
            .onFailure { overlay = null; overlayKey = ""; ProtectionRuntime.error("Android could not show the blocking screen. Re-enable FocuLY protection and check background access.") }
    }
    private fun pauseFeedPlayback() {
        val audio = getSystemService(AudioManager::class.java)
        if (!pausedFeedAudio) { audio.requestAudioFocus(feedAudioFocus); pausedFeedAudio = true }
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }
    fun clearOverlay() {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null; overlayKey = ""; generation++; overridePending = false
        feedOverlayOnHome = false
        pendingInspection?.let(handler::removeCallbacks); pendingInspection = null
        if (pausedFeedAudio) { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(feedAudioFocus); pausedFeedAudio = false }
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
