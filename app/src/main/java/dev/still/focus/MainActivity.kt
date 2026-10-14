package dev.still.focus

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainActivity : ComponentActivity() {
    var requestedScreen by mutableStateOf("home")
    var navigationSequence by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        requestedScreen = intent.getStringExtra("screen") ?: "home"
        enableEdgeToEdge()
        setContent { StillTheme(Repository.state.theme) { StillApp(requestedScreen, navigationSequence) } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); requestedScreen = intent.getStringExtra("screen") ?: "home"; navigationSequence++ }
    override fun onResume() { super.onResume(); Repository.reconcile(); FocusNotifications.session(this); FocusNotifications.plan(this) }
}

@Composable fun StillApp(requested: String, navigationSequence: Int = 0) {
    val activity = LocalActivity.current
    val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
    SideEffect { activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView).apply { isAppearanceLightStatusBars = lightBars; isAppearanceLightNavigationBars = lightBars } } }
    val s = Repository.state
    var route by rememberSaveable { mutableStateOf("home") }
    var tab by rememberSaveable { mutableStateOf("Apps") }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var selectedProfile by remember { mutableStateOf(s.profiles.firstOrNull() ?: FocusState.defaults().first()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var usageReady by remember { mutableStateOf(false) }
    LaunchedEffect(requested, navigationSequence) { route = requested }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); Repository.reconcile(); delay(1000) } }
    LaunchedEffect(route, now / 15_000) { usage = withContext(Dispatchers.IO) { UsageReader.day(Repository.context) }; usageReady = true }
    val mainRoutes = listOf("home", "rules", "insights")
    BackHandler(route !in mainRoutes) { route = if (route in listOf("picker", "allowlist", "schedule", "appRule")) "rules" else "home" }
    var appRule by remember { mutableStateOf<AppRule?>(null) }
    var stopDialog by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<(() -> Unit)?>(null) }
    val nav: (String) -> Unit = { route = it }
    if (!s.onboarding) { Onboarding { Repository.update { it.copy(onboarding = true) }; route = "settings" }; return }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header(route, s.name, { route = "profile" }, { route = "settings" }, { route = if (route == "schedule" || route == "appRule" || route == "picker" || route == "allowlist") "rules" else "home" }) },
        bottomBar = { if (route in mainRoutes) Dock(route, nav) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (route) {
                "home" -> Home(s, now, usage, usageReady, nav, { profile -> selectedProfile = profile; route = "setup" }, { stopDialog = true })
                "rules" -> Rules(s, tab, { tab = it }, nav, { appRule = it; route = "appRule" }, { editingSchedule = it; route = "schedule" })
                "insights" -> Insights(s, usage, usageReady)
                "profile" -> ProfileScreen(s, { editingProfile = it; route = "profileEdit" }, { editingProfile = null; route = "profileEdit" })
                "settings" -> SettingsScreen(s, nav)
                "picker", "allowlist" -> AppPicker(s, route == "allowlist") { route = "rules" }
                "appRule" -> appRule?.let { AppRuleEditor(it) { updated -> Repository.update { state -> state.copy(apps = state.apps.map { if (it.packageName == updated.packageName) updated else it }) }; route = "rules" } }
                "schedule" -> ScheduleEditor(s, editingSchedule) { schedule -> Repository.update { state -> state.copy(schedules = state.schedules.filterNot { it.id == schedule.id } + schedule) }; FocusNotifications.plan(Repository.context); route = "rules" }
                "setup", "profileEdit" -> SessionEditor(s, if (route == "profileEdit") editingProfile else selectedProfile, route == "profileEdit", onSave = { p -> Repository.update { state -> state.copy(profiles = state.profiles.filterNot { it.id == p.id } + p) }; route = "profile" }, onStart = { p, duration, intention ->
                    val action = { Repository.start(p, duration, intention); route = "home"; Unit }
                    if (!Repository.protectionGranted() || !s.disclosure) { pendingStart = action; timerDialog = true } else action()
                })
                else -> Home(s, now, usage, usageReady, nav, { selectedProfile = it; route = "setup" }, { stopDialog = true })
            }
        }
    }
    if (timerDialog) AlertDialog(onDismissRequest = { timerDialog = false }, title = { Text("Enable protection", fontFamily = Mono) }, text = { Text("Blocking needs your consent and Android accessibility access. You can enable it now, or start a timer without blocking.") }, confirmButton = { TextButton({ timerDialog = false; route = "settings" }) { Text("Enable protection") } }, dismissButton = { TextButton({ timerDialog = false; pendingStart?.invoke(); pendingStart = null }) { Text("Timer only") } })
    if (stopDialog) EndSessionDialog(s.session?.profile?.mode ?: FocusMode.STANDARD, { stopDialog = false }) { Repository.stop(); stopDialog = false }
}

@Composable fun Header(route: String, name: String, profile: () -> Unit, settings: () -> Unit, back: () -> Unit) {
    val root = route in listOf("home", "rules", "insights")
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (root) IconButton(profile) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                if (name.isBlank()) Icon(Icons.Outlined.Person, "Profile", Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                else Text(name.first().uppercase(), fontFamily = Mono, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { contentDescription = "Profile" })
            }
        } else IconButton(back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Text(if (root) "focus" else when (route) { "profile" -> "your space"; "settings" -> "preferences"; "setup" -> "make room"; "profileEdit" -> "focus profile"; "schedule" -> "schedule"; "allowlist" -> "always allowed"; "appRule" -> "app rules"; else -> "choose apps" }, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = Mono, fontSize = 18.sp)
        if (root) IconButton(settings) { Icon(Icons.Outlined.Settings, "Settings", Modifier.size(24.dp)) } else Spacer(Modifier.width(48.dp))
    }
}
@Composable fun Dock(route: String, nav: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 36.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("rules", "Rules", Icons.Outlined.Tune), Triple("insights", "Insights", Icons.Outlined.BarChart)).forEach { (key, label, icon) ->
                Box(Modifier.size(width = 68.dp, height = 52.dp).clip(RoundedCornerShape(20.dp)).background(if (route == key) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { nav(key) }.semantics { contentDescription = label; selected = route == key }, contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(25.dp), tint = if (route == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable fun Page(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(24.dp), content = content) }
@Composable fun Caption(text: String) { Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Heading(title: String, subtitle: String? = null) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge); subtitle?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable fun Primary(text: String, enabled: Boolean = true, icon: ImageVector? = Icons.AutoMirrored.Outlined.ArrowForward, action: () -> Unit) { val haptic = LocalHapticFeedback.current; Button({ if (Repository.state.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); action() }, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) { Text(text, Modifier.weight(1f)); icon?.let { Icon(it, null, Modifier.size(24.dp)) } } }
@Composable fun SoftCard(content: @Composable ColumnScope.() -> Unit) { Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) } }
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, onChange, Modifier.semantics { contentDescription = title }) } }
@Composable fun LinkRow(title: String, detail: String, icon: ImageVector = Icons.Outlined.ChevronRight, action: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = action).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun Input(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, supporting: String? = null) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, shape = RoundedCornerShape(16.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text), supportingText = supporting?.let { { Text(it) } }) }
@Composable fun ChoiceRow(options: List<String>, selected: String, choose: (String) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { FilterChip(selected == it, { choose(it) }, label = { Text(it) }) } } }
fun clock(millis: Long): String { val sec = (millis.coerceAtLeast(0) + 999) / 1000; return if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60) else "%02d:%02d".format(sec / 60, sec % 60) }
fun duration(millis: Long): String { val m = millis / 60_000; return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }
fun minuteLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

@Composable fun Onboarding(done: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("focus / a little more room", fontFamily = Mono, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            OrbitArt(Modifier.fillMaxWidth().height(210.dp))
            Text("Your attention.\nBack with you.", style = MaterialTheme.typography.headlineLarge)
            Text("Pause the scroll. Protect your time. Make space for what you actually want to do.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("No account. No ads. Your usage stays on this device.", style = MaterialTheme.typography.bodyMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Primary("Make room", action = done); Text("You choose the rules. You can always recover access.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
@Composable fun OrbitArt(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val radius = minOf(size.width, size.height) * .40f
        drawCircle(line, radius, style = Stroke(1.dp.toPx()))
        drawCircle(line, radius * .74f, style = Stroke(1.dp.toPx()))
        drawCircle(accent.copy(alpha = .08f), radius * .49f)
        val x = center.x
        listOf(-14f, 0f, 14f).forEachIndexed { i, offset -> drawLine(accent, androidx.compose.ui.geometry.Offset(x + offset.dp.toPx(), center.y - (if (i == 1) 27 else 18).dp.toPx()), androidx.compose.ui.geometry.Offset(x + offset.dp.toPx(), center.y + (if (i == 1) 27 else 18).dp.toPx()), 4.dp.toPx(), StrokeCap.Round) }
        drawCircle(accent, 5.dp.toPx(), center + androidx.compose.ui.geometry.Offset(radius * .71f, -radius * .71f))
    }
}

@Composable fun Home(s: FocusState, now: Long, usage: Map<String, Long>, usageReady: Boolean, nav: (String) -> Unit, start: (Profile) -> Unit, stop: () -> Unit) {
    Page {
        val hour = LocalTime.now().hour
        Caption("${when (hour) { in 5..11 -> "GOOD MORNING"; in 12..17 -> "GOOD AFTERNOON"; else -> "GOOD EVENING" }}${if (s.name.isBlank()) "" else ", ${s.name.uppercase()}"}")
        val session = s.session
        if (session == null) {
            Heading("A little less scroll.\nA little more you.", "Give the next moment your full attention.")
            OrbitArt(Modifier.fillMaxWidth().height(190.dp))
            Primary("Start focus", icon = Icons.Outlined.PlayArrow) { start(s.profiles.firstOrNull() ?: FocusState.defaults().first()) }
            if (!Repository.protectionGranted() || !s.disclosure) LinkRow("Set up protection", "Enable blocking when you’re ready.", Icons.Outlined.Shield) { nav("settings") }
            else Caption("PROTECTION CONNECTED · RULES APPLY DURING FOCUS")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Find your rhythm", style = MaterialTheme.typography.titleMedium)
                s.profiles.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { p -> Surface(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable { start(p) }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(when (p.id) { "study" -> Icons.AutoMirrored.Outlined.MenuBook; "sleep" -> Icons.Outlined.NightsStay; "personal" -> Icons.Outlined.FavoriteBorder; else -> Icons.Outlined.Bolt }, null, tint = MaterialTheme.colorScheme.primary)
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Caption(if (p.pomodoro) "${p.workMinutes}/${p.breakMinutes} · ${p.rounds} rounds" else "${p.minutes} minutes")
                        }
                    } }; if (pair.size == 1) Spacer(Modifier.weight(1f))
                } }
            }
        } else {
            Caption(if (session.onBreak(now)) "A MOMENT TO RESET" else "ONE THING AT A TIME")
            Text(session.intention, style = MaterialTheme.typography.headlineLarge)
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                val accent = MaterialTheme.colorScheme.primary
                val line = MaterialTheme.colorScheme.outlineVariant
                Canvas(Modifier.size(230.dp)) { drawCircle(line, style = Stroke(2.dp.toPx())); val progress = if (session.end == 0L) .85f else ((session.end - now).toFloat() / (session.end - session.start)).coerceIn(0f, 1f); drawArc(accent, -90f, 360 * progress, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (session.end == 0L) clock(now - session.start) else clock(session.phaseRemaining(now)), style = MaterialTheme.typography.displayLarge)
                    Caption(if (session.onBreak(now)) "BREAK TIME" else if (session.end == 0L) "TIME IN FOCUS" else "REMAINING")
                }
            }
            SoftCard { Caption(session.profile.name.uppercase()); Text(if (Repository.protectionGranted() && s.disclosure) "${s.apps.count { it.enabled }} ${if (s.apps.count { it.enabled } == 1) "selected app" else "selected apps"} · ${s.domains.size} ${if (s.domains.size == 1) "website" else "websites"}" else "Timer only · protection is off", style = MaterialTheme.typography.bodyLarge); Text(if (session.onBreak(now)) "Session restrictions are paused. Daily limits and schedules still apply." else "${session.profile.mode.name.lowercase().replaceFirstChar { it.uppercase() }} mode · your essential apps remain available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedButton(stop, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) { Text("End session") }
            LinkRow("Protection health", if (Repository.protectionGranted()) "Accessibility connected" else "Blocking is unavailable", Icons.Outlined.Shield) { nav("settings") }
        }
        val next = s.schedules.mapNotNull { schedule -> schedule.nextStart(now)?.let { it to schedule } }.minByOrNull { it.first }
        val active = s.schedules.filter { it.active(now) }
        if (active.isNotEmpty()) SoftCard { Caption("SCHEDULE ACTIVE"); Text(active.joinToString { it.name }, style = MaterialTheme.typography.titleMedium); Text("Overlapping rules use the strictest applicable mode.", style = MaterialTheme.typography.bodyMedium) }
        else if (next != null) LinkRow("Up next · ${next.second.name}", Instant.ofEpochMilli(next.first).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, HH:mm")), Icons.Outlined.CalendarMonth) { nav("rules") }
        val limited = s.apps.filter { it.enabled && it.dailyMinutes > 0 }.take(3)
        if (limited.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Room for balance", style = MaterialTheme.typography.titleMedium)
            limited.forEach { rule -> val used = usage[rule.packageName] ?: 0; val limit = rule.allowance(now); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(rule.label); Caption(if (Repository.usageGranted() && usageReady) "${used / 60_000} / ${limit}m" else "Usage access needed") }; LinearProgressIndicator(progress = { (used.toFloat() / (limit * 60_000L)).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant) }
        }
        if (session == null && s.history.isNotEmpty()) s.history.last().let { record -> SoftCard { Caption(if (record.completed) "LAST SESSION COMPLETE" else "LAST SESSION"); Text("${record.name} · ${duration(record.focusedMillis)} in focus", style = MaterialTheme.typography.bodyLarge) } }
    }
}

@Composable fun Rules(s: FocusState, tab: String, selectTab: (String) -> Unit, nav: (String) -> Unit, editApp: (AppRule) -> Unit, editSchedule: (Schedule?) -> Unit) {
    var domain by rememberSaveable { mutableStateOf("") }
    var domainError by remember { mutableStateOf(false) }
    Page {
        Heading("Your time.\nYour rules.", "A few boundaries. A lot more space.")
        ChoiceRow(listOf("Apps", "Websites", "Feeds", "Schedules"), tab, selectTab)
        when (tab) {
            "Apps" -> {
                Primary("Choose distracting apps", icon = Icons.Outlined.Add) { nav("picker") }
                Text("Selected apps are blocked during focus. Daily and per-visit limits also work outside sessions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.apps.isEmpty()) SoftCard { Icon(Icons.Outlined.Apps, null, tint = MaterialTheme.colorScheme.primary); Text("Start with one distraction.", style = MaterialTheme.typography.titleMedium); Text("Choose an app you often open without thinking. You can add more later.") }
                s.apps.forEach { app -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { editApp(app) }.padding(vertical = 10.dp)) { Text(app.label, style = MaterialTheme.typography.titleSmall); Text(buildList { add("During focus"); if (app.dailyMinutes > 0) add("${app.allowance(System.currentTimeMillis())}m/day"); if (app.openingMinutes > 0) add("${app.openingMinutes}m/visit") }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton({ editApp(app) }) { Icon(Icons.Outlined.Edit, "Edit ${app.label}", Modifier.size(20.dp)) }
                    Switch(app.enabled, { enabled -> Repository.update { state -> state.copy(apps = state.apps.map { if (it.packageName == app.packageName) it.copy(enabled = enabled) else it }) } }, Modifier.semantics { contentDescription = "Restrict ${app.label}" })
                } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow("Always allowed", "${s.allowlist.size} additional exceptions · phone and settings stay available", Icons.Outlined.LockOpen) { nav("allowlist") }
                LinkRow("Daily allowances & gradual reduction", "Tap an app above to set its limits.", Icons.Outlined.Timer) { if (s.apps.isNotEmpty()) editApp(s.apps.first()) else nav("picker") }
            }
            "Websites" -> {
                Input("Website or domain", domain, { domain = it; domainError = false }, supporting = if (domainError) "Enter a domain, such as reddit.com" else "Includes subdomains. Applies during focus.")
                Primary("Add website", enabled = domain.isNotBlank(), icon = Icons.Outlined.Add) {
                    val normalized = RulesEngine.normalizeDomain(domain)
                    if (normalized == null) domainError = true else { Repository.update { it.copy(domains = it.domains + normalized) }; domain = "" }
                }
                if (s.domains.isEmpty()) Text("Your website list is empty. Add the places you want to pause during focus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.domains.sorted().forEach { host -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Public, null, tint = MaterialTheme.colorScheme.primary); Text(host, Modifier.weight(1f).padding(start = 14.dp), fontFamily = Mono, fontSize = 14.sp); IconButton({ Repository.update { it.copy(domains = it.domains - host) } }) { Icon(Icons.Outlined.Close, "Remove $host") } } }
                SoftCard { Caption("BROWSER COVERAGE"); Text("Chrome, Edge, Brave & Firefox", style = MaterialTheme.typography.titleSmall); Text("Experimental address-field detection. It can interrupt matching pages when the browser exposes a readable address. Hidden address bars, incognito behavior, other browsers, and in-app browsers may bypass it. This is not network-level filtering.", style = MaterialTheme.typography.bodyMedium); Text("For stronger boundaries, add your browser to the app blocklist.", style = MaterialTheme.typography.bodyMedium) }
            }
            "Feeds" -> {
                SoftCard { Caption("EXPERIMENTAL"); Text("Less feed. More intention.", style = MaterialTheme.typography.titleMedium); Text("Pauses detected short-video screens during focus. Detection depends on each app’s current accessibility controls and language. Keep whole-app blocking available if a feed is missed.", style = MaterialTheme.typography.bodyMedium) }
                listOf("YouTube Shorts", "Instagram Reels", "Facebook Reels").forEach { feed -> ToggleRow(feed, "Selective detection · not yet verified on your device", feed in s.feeds) { enabled -> Repository.update { it.copy(feeds = if (enabled) it.feeds + feed else it.feeds - feed) } } }
                Text("Messaging and other screens remain available when a feed rule is the only restriction. TikTok and unsupported feeds can be blocked as whole apps.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Primary("Choose whole-app fallback", icon = Icons.Outlined.Apps) { nav("picker") }
            }
            "Schedules" -> {
                Primary("Add a routine", icon = Icons.Outlined.Add) { editSchedule(null) }
                if (s.schedules.isEmpty()) SoftCard { Text("Give focus a regular place.", style = MaterialTheme.typography.titleMedium); Text("Schedule study hours, a quiet morning, or time to wind down. Overnight schedules are supported.") }
                s.schedules.forEach { schedule -> SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(schedule.name, style = MaterialTheme.typography.titleMedium); Caption("${minuteLabel(schedule.startMinute)}–${minuteLabel(schedule.endMinute)}${if (schedule.endMinute <= schedule.startMinute) " · next day" else ""}") }; Switch(schedule.enabled, { enabled -> Repository.update { it.copy(schedules = it.schedules.map { item -> if (item.id == schedule.id) item.copy(enabled = enabled) else item }) }; FocusNotifications.plan(Repository.context) }, Modifier.semantics { contentDescription = "Enable ${schedule.name}" }) }
                    Text(schedule.days.sorted().joinToString(" · ") { DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar(Char::uppercase) }, style = MaterialTheme.typography.bodyMedium)
                    Row { TextButton({ editSchedule(schedule) }) { Text("Edit routine") }; Spacer(Modifier.weight(1f)); IconButton({ Repository.update { it.copy(schedules = it.schedules.filterNot { item -> item.id == schedule.id }) }; FocusNotifications.plan(Repository.context) }) { Icon(Icons.Outlined.Delete, "Delete ${schedule.name}") } }
                } }
                Text("Schedules apply their profile’s rules automatically while protection is connected. Notifications may arrive late under Android’s battery restrictions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

data class InstalledApp(val pkg: String, val label: String, val icon: Drawable)
@Composable fun AppPicker(s: FocusState, allow: Boolean, done: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) {
        ctx.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ctx.packageName && !FocusAccessibilityService.essential(ctx, it.activityInfo.packageName) }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(ctx.packageManager).toString(), it.loadIcon(ctx.packageManager)) }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    } }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (allow) "These apps stay available even when a rule would block them." else "Choose what you want to pause. Phone, emergency access, your launcher, and settings are always available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Input("Search installed apps", query, { query = it })
        if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        val visible = apps?.filter { it.label.contains(query, true) }.orEmpty()
        if (apps != null && visible.isEmpty()) Text("No matching apps on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(visible, key = { it.pkg }) { app ->
                val selected = if (allow) app.pkg in s.allowlist else s.apps.any { it.packageName == app.pkg }
                val change: () -> Unit = { Repository.update { state -> if (allow) state.copy(allowlist = if (selected) state.allowlist - app.pkg else state.allowlist + app.pkg) else state.copy(apps = if (selected) state.apps.filterNot { it.packageName == app.pkg } else state.apps + AppRule(app.pkg, app.label)) } }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = change).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val bitmap = remember(app.pkg) { android.graphics.Bitmap.createBitmap(72, 72, android.graphics.Bitmap.Config.ARGB_8888).also { app.icon.setBounds(0, 0, 72, 72); app.icon.draw(android.graphics.Canvas(it)) }.asImageBitmap() }
                    Image(bitmap, null, Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(app.label); if (listOf("instagram", "youtube", "tiktok", "facebook", "reddit").any { app.label.contains(it, true) }) Caption("COMMON DISTRACTION") }
                    Checkbox(selected, { change() }, Modifier.semantics { contentDescription = "Select ${app.label}" })
                }
            }
        }
        Primary("Done · ${if (allow) s.allowlist.size else s.apps.size} selected", icon = Icons.Outlined.Check, action = done)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable fun AppRuleEditor(app: AppRule, save: (AppRule) -> Unit) {
    var daily by rememberSaveable(app.packageName) { mutableStateOf(app.dailyMinutes.toString()) }
    var visit by rememberSaveable(app.packageName) { mutableStateOf(app.openingMinutes.toString()) }
    var target by rememberSaveable(app.packageName) { mutableStateOf(app.gradualTarget.toString()) }
    var paused by rememberSaveable(app.packageName) { mutableStateOf(app.gradualPaused) }
    val d = daily.toIntOrNull(); val v = visit.toIntOrNull(); val t = target.toIntOrNull()
    val valid = d != null && d in 0..1440 && v != null && v in 0..1440 && t != null && t in 0..(d ?: 0)
    Page {
        Heading(app.label, "Small boundaries that fit your day.")
        Input("Daily allowance (minutes)", daily, { daily = it.filter(Char::isDigit) }, true, "0 means no daily limit. Maximum 1440.")
        Input("Time per visit (minutes)", visit, { visit = it.filter(Char::isDigit) }, true, "0 means no visit limit. Resets after leaving this app.")
        Text("Gradual reduction", style = MaterialTheme.typography.titleMedium)
        Input("Target allowance (minutes)", target, { target = it.filter(Char::isDigit) }, true, "Reduce by 5 minutes per day. 0 disables the plan.")
        ToggleRow("Pause reduction", "Keep the current allowance until you resume.", paused) { paused = it }
        if (app.gradualTarget > 0) Caption("CURRENT ALLOWANCE · ${app.allowance(System.currentTimeMillis())} MINUTES")
        if (!Repository.usageGranted()) SoftCard { Text("Daily limits need usage access.", style = MaterialTheme.typography.titleSmall); TextButton({ Repository.openUsage() }) { Text("Enable usage access") } }
        if (app.packageName in Repository.state.allowlist) Text("This app is always allowed. Remove it from your allowlist to enforce these limits.", color = MaterialTheme.colorScheme.error)
        if (!valid) Text("Use minutes between 0 and 1440. The reduction target must not exceed your daily allowance.", color = MaterialTheme.colorScheme.error)
        Primary("Save app rules", valid, Icons.Outlined.Check) {
            val changed = d != app.dailyMinutes || t != app.gradualTarget
            val pauseChanged = paused != app.gradualPaused
            save(app.copy(dailyMinutes = if (pauseChanged && !changed) app.allowance(System.currentTimeMillis()) else d!!, openingMinutes = v!!, gradualTarget = t!!,
                gradualStart = if (changed || pauseChanged || app.gradualStart == 0L) System.currentTimeMillis() else app.gradualStart, gradualPaused = paused))
        }
        TextButton({ Repository.update { it.copy(apps = it.apps.filterNot { rule -> rule.packageName == app.packageName }) }; save(app.copy(enabled = false)) }) { Text("Remove from blocklist", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable fun SessionEditor(s: FocusState, original: Profile?, editing: Boolean, onSave: (Profile) -> Unit, onStart: (Profile, Int, String) -> Unit) {
    val initial = original ?: Profile(UUID.randomUUID().toString(), "My focus", 50, s.intention)
    var profileId by rememberSaveable { mutableStateOf(initial.id) }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var minutes by rememberSaveable { mutableStateOf(initial.minutes.toString()) }
    var intention by rememberSaveable { mutableStateOf(if (s.intention == "Make room for what matters.") initial.intention else s.intention) }
    var mode by rememberSaveable { mutableStateOf(initial.mode.name) }
    var pomo by rememberSaveable { mutableStateOf(initial.pomodoro) }
    var work by rememberSaveable { mutableStateOf(initial.workMinutes.toString()) }
    var pause by rememberSaveable { mutableStateOf(initial.breakMinutes.toString()) }
    var rounds by rememberSaveable { mutableStateOf(initial.rounds.toString()) }
    var scopedApps by remember { mutableStateOf(initial.apps) }
    var scopedDomains by remember { mutableStateOf(initial.domains) }
    val duplicateName = editing && s.profiles.any { it.id != profileId && it.name.equals(name.trim(), true) }
    val valid = name.isNotBlank() && !duplicateName && minutes.toIntOrNull()?.let { it in 0..1440 } == true && (!pomo || (work.toIntOrNull()?.let { it in 1..240 } == true && pause.toIntOrNull()?.let { it in 1..60 } == true && rounds.toIntOrNull()?.let { it in 1..12 } == true))
    fun configured() = Profile(profileId, name.trim(), minutes.toIntOrNull() ?: 50, intention.trim(), FocusMode.valueOf(mode), pomo, work.toIntOrNull() ?: 25, pause.toIntOrNull() ?: 5, rounds.toIntOrNull() ?: 4, scopedApps, scopedDomains)
    Page {
        Heading(if (editing) "Build your rhythm." else "What deserves\nyour attention?", if (editing) "Save a profile for sessions and schedules." else "Choose a little space for yourself.")
        if (!editing) ChoiceRow(s.profiles.map { it.name }, name) { choice -> s.profiles.find { it.name == choice }?.let { p -> profileId = p.id; name = p.name; minutes = p.minutes.toString(); intention = p.intention; mode = p.mode.name; pomo = p.pomodoro; work = p.workMinutes.toString(); pause = p.breakMinutes.toString(); rounds = p.rounds.toString(); scopedApps = p.apps; scopedDomains = p.domains } }
        if (editing) Input("Profile name", name, { name = it.take(40) })
        Input("Your intention", intention, { intention = it.take(120) })
        if (!pomo) { ChoiceRow(listOf("25", "50", "90", "0"), minutes) { minutes = it }; Input("Duration (minutes)", minutes, { minutes = it.filter(Char::isDigit) }, true, "0 for an open-ended session. Up to 1440 minutes.") }
        ToggleRow("Pomodoro rhythm", "Alternate focus and restorative breaks.", pomo) { pomo = it }
        if (pomo) { Input("Focus minutes", work, { work = it.filter(Char::isDigit) }, true, "1–240 minutes"); Input("Break minutes", pause, { pause = it.filter(Char::isDigit) }, true, "1–60 minutes"); Input("Rounds", rounds, { rounds = it.filter(Char::isDigit) }, true, "1–12 rounds · no break after the last round") }
        Text("How firm should the boundary be?", style = MaterialTheme.typography.titleMedium)
        ChoiceRow(listOf("GENTLE", "STANDARD", "STRICT"), mode) { mode = it }
        SoftCard { Text(when (mode) { "GENTLE" -> "A pause before you continue. You can take a short intentional visit."; "STRICT" -> "A 30-second pause and a reason before overriding or ending early. Emergency and system access stay available. Android settings can always disable protection."; else -> "Blocks distractions. A short wait and temporary access are available when needed." }, style = MaterialTheme.typography.bodyMedium) }
        Text("What gets paused", style = MaterialTheme.typography.titleMedium)
        Caption("NO SPECIFIC SELECTION = ALL ENABLED GLOBAL RULES")
        if (s.apps.isEmpty() && s.domains.isEmpty() && s.feeds.isEmpty()) Text("You haven’t selected distractions yet. Add them in Rules to make blocking useful.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.apps.filter { it.enabled }.forEach { rule -> ToggleRow(rule.label, if (scopedApps.isEmpty()) "Uses all enabled apps" else "Include in this profile", rule.packageName in scopedApps) { enabled -> scopedApps = if (enabled) scopedApps + rule.packageName else scopedApps - rule.packageName } }
        s.domains.forEach { host -> ToggleRow(host, "Website rule", host in scopedDomains) { enabled -> scopedDomains = if (enabled) scopedDomains + host else scopedDomains - host } }
        if (s.feeds.isNotEmpty()) Text("Selected feed rules apply to every focus profile.", style = MaterialTheme.typography.bodyMedium)
        if (!valid) Text(if (duplicateName) "Choose a unique profile name." else "Check the name and minute ranges above.", color = MaterialTheme.colorScheme.error)
        Primary(if (editing) "Save profile" else if (mode == "STRICT") "Start strict session" else "Start focus", valid && (editing || s.session == null), if (editing) Icons.Outlined.Check else Icons.Outlined.PlayArrow) { if (editing) onSave(configured()) else onStart(configured(), minutes.toInt(), intention) }
        if (!editing && s.session != null) Text("End your current session before starting another.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable fun ScheduleEditor(s: FocusState, original: Schedule?, save: (Schedule) -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name ?: "Quiet hours") }
    var start by rememberSaveable { mutableStateOf(minuteLabel(original?.startMinute ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(original?.endMinute ?: 660)) }
    var days by remember { mutableStateOf(original?.days ?: setOf(1, 2, 3, 4, 5)) }
    var profile by rememberSaveable { mutableStateOf(original?.profileId ?: s.profiles.first().id) }
    var reminder by rememberSaveable { mutableStateOf(original?.reminder ?: false) }
    var conflict by remember { mutableStateOf<Schedule?>(null) }
    fun time(raw: String): Int? { val pieces = raw.split(':'); if (pieces.size != 2) return null; val h = pieces[0].toIntOrNull() ?: return null; val m = pieces[1].toIntOrNull() ?: return null; return if (h in 0..23 && m in 0..59) h * 60 + m else null }
    val valid = name.isNotBlank() && days.isNotEmpty() && time(start) != null && time(end) != null && time(start) != time(end)
    Page {
        Heading("A place for focus.", "Build a routine around your real life.")
        Input("Routine name", name, { name = it.take(40) })
        ChoiceRow(listOf("Weekdays", "Weekend", "Every day"), when (days) { setOf(1, 2, 3, 4, 5) -> "Weekdays"; setOf(6, 7) -> "Weekend"; (1..7).toSet() -> "Every day"; else -> "Custom" }) { days = when (it) { "Weekdays" -> setOf(1, 2, 3, 4, 5); "Weekend" -> setOf(6, 7); else -> (1..7).toSet() } }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..7).forEach { d -> FilterChip(d in days, { days = if (d in days) days - d else days + d }, label = { Text(DayOfWeek.of(d).name.take(3)) }) } }
        Input("Starts (24-hour HH:mm)", start, { start = it.take(5) })
        Input("Ends (24-hour HH:mm)", end, { end = it.take(5) }, supporting = "An earlier end time continues into the following day.")
        Text("Focus profile", style = MaterialTheme.typography.titleMedium)
        ChoiceRow(s.profiles.map { it.name }, s.profiles.find { it.id == profile }?.name ?: "") { choice -> profile = s.profiles.first { it.name == choice }.id }
        ToggleRow("A gentle heads-up", "Remind me about five minutes before.", reminder) { reminder = it }
        if (!valid) Text("Choose at least one day and two different valid times.", color = MaterialTheme.colorScheme.error)
        Primary("Save routine", valid, Icons.Outlined.Check) {
            val candidate = Schedule(original?.id ?: UUID.randomUUID().toString(), name.trim(), profile, days, time(start)!!, time(end)!!, original?.enabled ?: true, reminder)
            if (s.schedules.any { it.id != candidate.id && candidate.overlaps(it) }) conflict = candidate else save(candidate)
        }
    }
    conflict?.let { candidate -> AlertDialog(onDismissRequest = { conflict = null }, title = { Text("Routines overlap", fontFamily = Mono) }, text = { Text("Both routines will apply. Their restrictions combine, and the strictest applicable mode wins. Allowlisted apps stay available.") }, confirmButton = { TextButton({ conflict = null; save(candidate) }) { Text("Save overlapping routine") } }, dismissButton = { TextButton({ conflict = null }) { Text("Adjust times") } }) }
}

@Composable fun ProfileScreen(s: FocusState, edit: (Profile) -> Unit, add: () -> Unit) {
    var goal by rememberSaveable { mutableStateOf(s.goalMinutes.toString()) }
    Page {
        Heading("Your space.", "Make focus fit the way you live.")
        Input("What should we call you?", s.name, { value -> Repository.update { it.copy(name = value.take(30)) } })
        Input("Default focus intention", s.intention, { value -> Repository.update { it.copy(intention = value.take(120)) } })
        Input("Daily focus goal (minutes)", goal, { goal = it.filter(Char::isDigit); goal.toIntOrNull()?.takeIf { it in 1..1440 }?.let { minutes -> Repository.update { it.copy(goalMinutes = minutes) } } }, true, "Choose 1–1440 minutes. This is a personal guide, not a score.")
        Text("Your rhythms", style = MaterialTheme.typography.titleMedium)
        s.profiles.forEach { p -> LinkRow(p.name, "${if (p.pomodoro) "${p.workMinutes}/${p.breakMinutes} rhythm" else "${p.minutes} minutes"} · ${p.mode.name.lowercase()}", Icons.Outlined.Edit) { edit(p) } }
        Primary("Create focus profile", icon = Icons.Outlined.Add, action = add)
        Text("Gradual reduction plans live with each app’s daily allowance in Rules.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun SettingsScreen(s: FocusState, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf(Repository.usageGranted()) }
    var accessibility by remember { mutableStateOf(Repository.protectionGranted()) }
    var connected by remember { mutableStateOf(FocusAccessibilityService.instance != null) }
    var consent by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            exportStatus = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(Repository.export().toByteArray()) } ?: error("Could not open the destination") }.fold({ "Your data was exported." }, { "Export failed. Choose a different destination." }) }
        }
    }
    LaunchedEffect(Unit) { while (true) { usage = Repository.usageGranted(); accessibility = Repository.protectionGranted(); connected = FocusAccessibilityService.instance != null; delay(1000) } }
    Page {
        Heading("Keep it yours.", "Permissions, privacy, and the little details.")
        SoftCard {
            Caption("PROTECTION HEALTH")
            Text(if (accessibility && connected && s.disclosure) "Ready to protect your focus." else "Blocking needs setup.", style = MaterialTheme.typography.titleMedium)
            Text(if (accessibility && !connected) "Android enabled the service, but it has not connected. Try turning Focus off and on in Accessibility settings." else "App and visit limits need protection. Daily allowances and usage charts also need usage access.", style = MaterialTheme.typography.bodyMedium)
            LinkRow("Focus protection", if (accessibility && s.disclosure) "Enabled · service ${if (connected) "connected" else "disconnected"}" else "Off · accessibility access", Icons.Outlined.Shield) { if (s.disclosure) Repository.openAccessibility() else consent = true }
            LinkRow("Usage access", if (usage) "Enabled · daily allowances available" else "Off · needed for daily usage", Icons.Outlined.Timer) { Repository.openUsage() }
            LinkRow("Notification permission", "Session status and optional reminders", Icons.Outlined.NotificationsNone) {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
            if (!s.disclosure) TextButton({ consent = true }) { Text("Read protection disclosure") }
            else TextButton({ Repository.update { it.copy(disclosure = false) }; FocusAccessibilityService.instance?.clearOverlay(); FocusNotifications.session(ctx) }) { Text("Withdraw protection consent") }
        }
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ChoiceRow(listOf("System", "Light", "Dark"), s.theme) { value -> Repository.update { it.copy(theme = value) } }
        ToggleRow("Reduce motion", "Keep transitions quiet and immediate.", s.reduceMotion) { value -> Repository.update { it.copy(reduceMotion = value) } }
        ToggleRow("Haptic feedback", "A subtle response to primary actions.", s.haptics) { value -> Repository.update { it.copy(haptics = value) } }
        ToggleRow("Completion & routine reminders", "Optional heads-ups, without nagging.", s.reminders) { value -> Repository.update { it.copy(reminders = value) }; FocusNotifications.plan(ctx) }
        ToggleRow("Hide insights", "Keep your numbers out of view.", s.hideInsights) { value -> Repository.update { it.copy(hideInsights = value) } }
        Text("Intentional visits", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Ask for an intention", "A reason before a temporary visit. Strict mode always asks.", s.askReason) { value -> Repository.update { it.copy(askReason = value) } }
        Caption("TEMPORARY ACCESS · MINUTES")
        ChoiceRow(listOf("1", "5", "10"), s.overrideMinutes.toString()) { value -> Repository.update { it.copy(overrideMinutes = value.toInt()) } }
        Caption("COOLDOWN AFTER A VISIT · MINUTES")
        ChoiceRow(listOf("0", "5", "10", "30"), s.cooldownMinutes.toString()) { value -> Repository.update { it.copy(cooldownMinutes = value.toInt()) } }
        Text("Privacy & data", style = MaterialTheme.typography.titleMedium)
        Text("Everything stays on this device. No advertising, accounts, trackers, or analytics. Screen content is checked in memory for your rules and is not recorded. Only your chosen rules, session history, and override reasons are saved. Android usage events are read for allowances and insights.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinkRow("Export my data", "Rules, profiles, sessions, and override history", Icons.Outlined.Download) { export.launch("focus-data-${LocalDate.now()}.json") }
        if (exportStatus.isNotEmpty()) Text(exportStatus, style = MaterialTheme.typography.bodyMedium)
        LinkRow("Delete my data", "Stops the session and clears Focus’s local records", Icons.Outlined.Delete) { delete = true }
        Text("Emergency access", style = MaterialTheme.typography.titleMedium)
        Text("Phone, your launcher, system settings, and Focus are never blocked. You can disable the service in Android Accessibility settings. Strict mode adds friction, but cannot prevent you from changing system settings or uninstalling the app.", style = MaterialTheme.typography.bodyMedium)
        Caption("FOCUS 1.0.0 · ANDROID 9+")
    }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("How protection works", fontFamily = Mono) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Focus uses Android Accessibility access to observe which app is open, check selected feed controls and supported browser address fields, and display a blocking screen for rules you choose.")
        Text("This access can expose on-screen text. Focus checks it only in memory on your device. It does not store screen content, send it anywhere, or use it for advertising.")
        Text("Access is optional. You can withdraw consent or turn protection off in Android settings at any time. Without access, focus timers still work.")
    } }, confirmButton = { TextButton({ Repository.update { it.copy(disclosure = true) }; consent = false; Repository.openAccessibility() }) { Text("Agree & open settings") } }, dismissButton = { TextButton({ consent = false }) { Text("Not now") } })
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Clear your space?", fontFamily = Mono) }, text = { Text("This deletes Focus’s rules, profiles, sessions, and override history and ends any active session. It does not delete Android’s system usage history. This cannot be undone.") }, confirmButton = { TextButton({ Repository.deleteAll(); delete = false; nav("home") }) { Text("Delete local data", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ delete = false }) { Text("Keep data") } })
}

@Composable fun Insights(s: FocusState, usage: Map<String, Long>, usageReady: Boolean) {
    var week by remember { mutableStateOf<List<Pair<LocalDate, Map<String, Long>>>>(emptyList()) }
    var previousWeek by remember { mutableStateOf<List<Map<String, Long>>>(emptyList()) }
    LaunchedEffect(s.apps, LocalDate.now(), Repository.usageGranted()) {
        val data = withContext(Dispatchers.IO) { (13 downTo 0).map { day -> LocalDate.now().minusDays(day.toLong()).let { it to UsageReader.day(Repository.context, it) } } }
        previousWeek = data.take(7).map { it.second }; week = data.takeLast(7)
    }
    Page {
        Heading("Notice your rhythm.", "Small changes deserve a little space.")
        if (s.hideInsights) {
            SoftCard { Text("Your numbers are tucked away.", style = MaterialTheme.typography.titleMedium); Text("You can focus without keeping score."); TextButton({ Repository.update { it.copy(hideInsights = false) } }) { Text("Show insights") } }
            return@Page
        }
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val records = s.history.filter { it.end > dayStart && it.start < dayEnd }
        val focused = records.sumOf { record ->
            // Session records carry total focused time; apportion cross-midnight sessions by elapsed overlap.
            val overlap = (minOf(record.end, dayEnd) - maxOf(record.start, dayStart)).coerceAtLeast(0)
            if (record.end > record.start) record.focusedMillis * overlap / (record.end - record.start) else 0L
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Caption("FOCUS TODAY"); Text(duration(focused), style = MaterialTheme.typography.headlineMedium); Text("Logged sessions", style = MaterialTheme.typography.bodySmall) } }
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Caption("COMPLETED"); Text(records.count { it.completed && it.end in dayStart until dayEnd }.toString(), style = MaterialTheme.typography.headlineMedium); Text("Focus sessions", style = MaterialTheme.typography.bodySmall) } }
        }
        val progress = (focused.toFloat() / (s.goalMinutes * 60_000L)).coerceIn(0f, 1f)
        Caption("YOUR PERSONAL GUIDE · ${s.goalMinutes} MINUTES")
        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.outlineVariant)
        Text(if (focused == 0L) "Your first session is a good place to begin." else "You made room for ${duration(focused)} today. Keep a rhythm that feels sustainable.", style = MaterialTheme.typography.bodyLarge)
        if (!Repository.usageGranted()) SoftCard { Text("See your everyday patterns.", style = MaterialTheme.typography.titleMedium); Text("Usage access enables Android usage measurements and daily allowance enforcement."); TextButton({ Repository.openUsage() }) { Text("Enable usage access") } }
        else if (!usageReady || week.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
        else {
            val selected = s.apps.filter { it.enabled }.map { it.packageName }.toSet()
            val values = week.map { (_, data) -> data.filterKeys { it in selected }.values.sum() }
            val lastTotal = previousWeek.sumOf { data -> data.filterKeys { it in selected }.values.sum() }
            if (selected.isNotEmpty() && lastTotal > 0) {
                val difference = values.sum() - lastTotal
                Text("${duration(kotlin.math.abs(difference))} ${if (difference <= 0) "less" else "more"} selected-app time than the previous seven days. The current day is still in progress.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Selected app usage", style = MaterialTheme.typography.titleMedium)
            val max = values.maxOrNull()?.coerceAtLeast(60_000L) ?: 60_000L
            Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                values.forEachIndexed { i, value -> Column(Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = "${week[i].first.dayOfWeek.name}: ${value / 60_000} minutes" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text("${value / 60_000}", fontFamily = Mono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height((value.toFloat() / max * 100).coerceAtLeast(3f).dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(if (i == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer))
                    Spacer(Modifier.height(8.dp)); Caption(week[i].first.dayOfWeek.name.take(1))
                } }
            }
            Text("Android foreground activity time · minutes. Split-screen and device behavior can affect measurements. Cross-midnight focus totals are apportioned estimates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (selected.isEmpty()) Text("Choose apps in Rules to see their usage here.", style = MaterialTheme.typography.bodyMedium)
            s.apps.filter { it.enabled }.sortedByDescending { usage[it.packageName] ?: 0 }.forEach { rule ->
                val used = usage[rule.packageName] ?: 0; val budget = rule.allowance(System.currentTimeMillis())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(rule.label); Caption(if (budget == 0) "NO DAILY LIMIT" else if (used < budget * 60_000L) "WITHIN ${budget}M ALLOWANCE" else "ALLOWANCE COMPLETE") }; Text(duration(used), fontFamily = Mono, fontSize = 14.sp) }
            }
        }
        val visits = s.overrides.count { it.at in dayStart until dayEnd }
        SoftCard { Caption("INTENTIONAL VISITS TODAY"); Text("$visits temporary overrides", style = MaterialTheme.typography.bodyLarge) }
        Text("Recent focus", style = MaterialTheme.typography.titleMedium)
        if (s.history.isEmpty()) Text("Your completed and ended sessions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.history.takeLast(8).reversed().forEach { record -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(record.name); Caption(Instant.ofEpochMilli(record.start).atZone(zone).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm")) + if (record.completed) " · complete" else " · ended early") }; Text(duration(record.focusedMillis), fontFamily = Mono, fontSize = 14.sp) } }
    }
}

@Composable fun EndSessionDialog(mode: FocusMode, dismiss: () -> Unit, stop: () -> Unit) {
    var remaining by remember { mutableIntStateOf(if (mode == FocusMode.STRICT) 30 else 0) }
    var reason by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (remaining > 0) { delay(1000); remaining-- } }
    AlertDialog(onDismissRequest = dismiss, title = { Text("A conscious pause.", fontFamily = Mono) }, text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (mode == FocusMode.STRICT) "Strict mode asks for a short pause and an intention before ending early. Essential apps remain available." else "End this session now? The time you spent focusing will still be recorded.")
        if (mode == FocusMode.STRICT) Input("Why are you ending early?", reason, { reason = it.take(120) })
        if (remaining > 0) Text("Ready in ${remaining}s", fontFamily = Mono)
    } }, confirmButton = { TextButton(stop, enabled = remaining == 0 && (mode != FocusMode.STRICT || reason.isNotBlank())) { Text("End session") } }, dismissButton = { TextButton(dismiss) { Text("Keep focusing") } })
}
