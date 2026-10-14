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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.selection.toggleable
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
    var blockKind by rememberSaveable { mutableStateOf(BlockKind.APPS) }
    var blockTargets by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var blockParent by rememberSaveable { mutableStateOf("rules") }
    var settingsParent by rememberSaveable { mutableStateOf("home") }
    var profileParent by rememberSaveable { mutableStateOf("home") }
    var pinParent by rememberSaveable { mutableStateOf("settings") }
    val screenState = rememberSaveableStateHolder()
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
    val back: () -> Unit = { route = when (route) { "block" -> blockParent; "settings" -> settingsParent; "profile" -> profileParent; "pin" -> pinParent; "profileEdit" -> "profile"; "managed", "messages", "permissions", "welcome" -> "settings"; "lifecycle" -> "managed"; "picker", "allowlist", "schedule", "appRule" -> "rules"; else -> "home" } }
    BackHandler(route !in mainRoutes, onBack = back)
    var appRule by remember { mutableStateOf<AppRule?>(null) }
    var stopDialog by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var strictRequested by remember { mutableStateOf(false) }
    val startBlock: (BlockKind, Set<String>?) -> Unit = { kind, targets -> screenState.removeState("block"); blockKind = kind; blockTargets = targets; blockParent = if (route == "appRule") "rules" else route; route = "block" }
    val startSession: (Profile) -> Unit = { screenState.removeState("setup"); selectedProfile = it; route = "setup" }
    val nav: (String) -> Unit = { when (it) {
        "blockApps" -> if (s.apps.none { it.enabled && it.packageName !in s.allowlist }) { tab = "Apps"; route = "picker" } else startBlock(BlockKind.APPS, null)
        "blockWebsites" -> if (s.domains.isEmpty()) { tab = "Websites"; route = "rules" } else startBlock(BlockKind.WEBSITES, null)
        "settings" -> { settingsParent = route; route = "settings" }
        "pin" -> { pinParent = route; route = "pin" }
        else -> route = it
    } }
    if (!s.onboarding) {
        FoculyOnboarding { Repository.update { it.copy(onboarding = true) }; route = "home" }
        return
    }
    if (route == "welcome") { FoculyOnboarding { route = "settings" }; return }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header(route, s.name, { profileParent = route; route = "profile" }, { settingsParent = route; route = "settings" }, back) },
        bottomBar = { if (route in mainRoutes) Dock(route, nav) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            screenState.SaveableStateProvider(route) {
            when (route) {
                "home" -> Home(s, now, usage, usageReady, nav, startSession, { stopDialog = true })
                "rules" -> Rules(s, tab, { tab = it }, nav, { appRule = it; route = "appRule" }, { screenState.removeState("schedule"); editingSchedule = it; route = "schedule" }, startBlock)
                "block" -> BlockEditor(s, blockKind, blockTargets, { route = "home" }, { settingsParent = "block"; route = "settings" })
                "insights" -> Insights(s, usage, usageReady)
                "profile" -> ProfileScreen(s, { screenState.removeState("profileEdit"); editingProfile = it; route = "profileEdit" }, { screenState.removeState("profileEdit"); editingProfile = null; route = "profileEdit" })
                "settings" -> SettingsScreen(s, nav)
                "managed" -> DeviceControlsGate { ManagedScreen(s, nav) }
                "lifecycle" -> DeviceControlsGate { LifecycleScreen(s) }
                "pin" -> PinScreen(s)
                "messages" -> MessageScreen(s)
                "permissions" -> SetupChecklist { route = "settings" }
                "picker", "allowlist" -> AppPicker(s, route == "allowlist") { route = "rules" }
                "appRule" -> appRule?.let { app -> AppRuleEditor(app, { startBlock(BlockKind.APPS, setOf(app.packageName)) }) { updated -> Repository.update { state -> state.copy(apps = state.apps.map { if (it.packageName == updated.packageName) updated else it }) }; route = "rules" } }
                "schedule" -> ScheduleEditor(s, editingSchedule) { schedule -> Repository.update { state -> state.copy(schedules = state.schedules.filterNot { it.id == schedule.id } + schedule) }; FocusNotifications.plan(Repository.context); route = "rules" }
                "setup", "profileEdit" -> SessionEditor(s, if (route == "profileEdit") editingProfile else selectedProfile, route == "profileEdit", onSave = { p -> Repository.update { state -> state.copy(profiles = state.profiles.filterNot { it.id == p.id } + p) }; route = "profile" }, onStart = { p, duration, intention ->
                    val action: (Boolean) -> Unit = { timerOnly -> route = if (Repository.start(p, duration, intention, timerOnly)) "home" else "settings" }
                    if (!Repository.protectionReady()) { strictRequested = p.mode == FocusMode.STRICT; pendingStart = action; timerDialog = true } else action(false)
                })
                else -> Home(s, now, usage, usageReady, nav, startSession, { stopDialog = true })
            }
            }
        }
    }
    if (timerDialog) AlertDialog(onDismissRequest = { timerDialog = false }, title = { Text("Enable protection", fontFamily = Mono) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DoodleBadge(CatArt.REMINDER, Modifier.fillMaxWidth().height(88.dp))
        Text(if (strictRequested) "Strict requires a connected protection service. Enable it and allow background activity first." else "Blocking needs your consent and a connected Android accessibility service. You can enable it now, or explicitly start a timer without blocking.")
    } }, confirmButton = { TextButton({ timerDialog = false; settingsParent = "setup"; route = "settings" }) { Text("Enable protection") } }, dismissButton = { if (!strictRequested) TextButton({ timerDialog = false; pendingStart?.invoke(true); pendingStart = null }) { Text("Timer only") } })
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
        if (root) BrandWordmark(Modifier.weight(1f))
        else Text(when (route) { "block" -> "block setup"; "profile" -> "your space"; "settings" -> "preferences"; "managed" -> "device protection"; "lifecycle" -> "app boundaries"; "pin" -> "admin PIN"; "messages" -> "blocking screen"; "permissions" -> "setup checklist"; "setup" -> "make room"; "profileEdit" -> "focus profile"; "schedule" -> "schedule"; "allowlist" -> "always allowed"; "appRule" -> "app rules"; else -> "choose apps" }, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontFamily = Mono, fontSize = 18.sp)
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
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange).semantics { contentDescription = title }, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.bodyLarge); if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, null, enabled = enabled) } }
@Composable fun LinkRow(title: String, detail: String, icon: ImageVector = Icons.Outlined.ChevronRight, action: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = action).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun Input(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, supporting: String? = null) { val keyboard = LocalSoftwareKeyboardController.current; val focus = LocalFocusManager.current; OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, shape = RoundedCornerShape(16.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focus.clearFocus() }), supportingText = supporting?.let { { Text(it) } }) }
@Composable fun ChoiceRow(options: List<String>, selected: String, choose: (String) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { FilterChip(selected == it, { choose(it) }, label = { Text(it) }) } } }
fun clock(millis: Long): String { val sec = (millis.coerceAtLeast(0) + 999) / 1000; return if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60) else "%02d:%02d".format(sec / 60, sec % 60) }
fun duration(millis: Long): String { val m = millis / 60_000; return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }
fun minuteLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

@Composable fun Home(s: FocusState, now: Long, usage: Map<String, Long>, usageReady: Boolean, nav: (String) -> Unit, start: (Profile) -> Unit, stop: () -> Unit) {
    Page {
        val hour = LocalTime.now().hour
        Caption("${when (hour) { in 5..11 -> "GOOD MORNING"; in 12..17 -> "GOOD AFTERNOON"; else -> "GOOD EVENING" }}${if (s.name.isBlank()) "" else ", ${s.name.uppercase()}"}")
        val session = s.session
        if (session == null) {
            DoodleCard(CatArt.PAUSE, "A little less scroll.\nA little more you.", "Give the next moment your full attention.", FoculyBrand.blush)
            ActiveBlocks(s, now)
            Primary("Start focus", icon = Icons.Outlined.PlayArrow) { start(s.profiles.firstOrNull() ?: FocusState.defaults().first()) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Triple("Block apps", "blockApps", Icons.Outlined.Apps), Triple("Block websites", "blockWebsites", Icons.Outlined.Public)).forEach { (title, target, icon) ->
                    Surface(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable { nav(target) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleSmall) }
                    }
                }
            }
            if (!Repository.protectionReady()) LinkRow("Set up protection", "Enable blocking when you’re ready.", Icons.Outlined.Shield) { nav("settings") }
            else Caption("PROTECTION CONNECTED")
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { Caption(if (session.onBreak(now)) "A MOMENT TO RESET" else "ONE THING AT A TIME") }; DoodleBadge(if (session.onBreak(now)) CatArt.BREAK else CatArt.FOCUS, Modifier.size(88.dp)) }
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
            SoftCard { Caption(session.profile.name.uppercase()); Text(if (s.managedLockdown && ManagedProtection.verifiedSuspendedCount() > 0) "${ManagedProtection.verifiedSuspendedCount()} apps locked by Android" else if (!session.timerOnly && Repository.protectionReady()) "${s.apps.count { it.enabled }} ${if (s.apps.count { it.enabled } == 1) "selected app" else "selected apps"} · ${s.domains.size} ${if (s.domains.size == 1) "website" else "websites"}" else if (session.timerOnly) "Timer only · protection is off" else "Blocking interrupted · check protection health", style = MaterialTheme.typography.bodyLarge); Text(if (session.onBreak(now)) "Session restrictions are paused. Daily limits and schedules still apply." else "${session.profile.mode.label} mode · your essential apps remain available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedButton(stop, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = session.profile.mode != FocusMode.STRICT, shape = RoundedCornerShape(18.dp)) { Text(if (session.profile.mode == FocusMode.STRICT) "Locked until the timer ends" else "End session") }
            LinkRow("Protection health", if (Repository.protectionReady()) "Service connected and responsive" else "Blocking is unavailable", Icons.Outlined.Shield) { nav("settings") }
        }
        if (session != null) ActiveBlocks(s, now)
        val next = s.schedules.mapNotNull { schedule -> schedule.nextStart(now)?.let { it to schedule } }.minByOrNull { it.first }
        val active = s.schedules.filter { it.active(now) }
        if (active.isNotEmpty()) SoftCard { Caption("SCHEDULE ACTIVE"); Text(active.joinToString { it.name }, style = MaterialTheme.typography.titleMedium); Text("Overlapping rules use the strictest applicable mode.", style = MaterialTheme.typography.bodyMedium) }
        else if (next != null) LinkRow("Up next · ${next.second.name}", Instant.ofEpochMilli(next.first).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, HH:mm")), Icons.Outlined.CalendarMonth) { nav("rules") }
        val limited = s.apps.filter { it.enabled && it.dailyMinutes > 0 }.take(3)
        if (limited.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Room for balance", style = MaterialTheme.typography.titleMedium)
            limited.forEach { rule -> val used = usage[rule.packageName] ?: 0; val limit = rule.allowance(now); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(rule.label); Caption(if (Repository.usageGranted() && usageReady) "${used / 60_000} / ${limit}m" else "Usage access needed") }; LinearProgressIndicator(progress = { (used.toFloat() / (limit * 60_000L)).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(3.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant) }
        }
        if (session == null && s.history.isNotEmpty()) s.history.last().let { record ->
            DoodleCard(if (record.completed) CatArt.COMPLETE else CatArt.PAUSE,
                if (record.completed) "A little win for you." else "Every minute counts.",
                "${record.name} · ${duration(record.focusedMillis)} in focus", if (record.completed) FoculyBrand.peach else FoculyBrand.lilac)
        }
    }
}

@Composable fun Rules(s: FocusState, tab: String, selectTab: (String) -> Unit, nav: (String) -> Unit, editApp: (AppRule) -> Unit, editSchedule: (Schedule?) -> Unit, block: (BlockKind, Set<String>?) -> Unit) {
    if (RulesEngine.strictActive(s, System.currentTimeMillis())) { LockedRules(s); return }
    var domain by rememberSaveable { mutableStateOf("") }
    var domainError by remember { mutableStateOf(false) }
    Page {
        Heading("Your time.\nYour rules.")
        ChoiceRow(listOf("Apps", "Websites", "Feeds", "Schedules"), tab, selectTab)
        when (tab) {
            "Apps" -> {
                Primary("Choose distracting apps", icon = Icons.Outlined.Add) { nav("picker") }
                if (s.apps.isNotEmpty()) Primary("Block selected apps", enabled = s.apps.any { it.enabled && it.packageName !in s.allowlist }, icon = Icons.Outlined.Lock) { block(BlockKind.APPS, null) }
                if (s.apps.isEmpty()) DoodleCard(CatArt.PAUSE, "Choose your first app.", "A small pause can make a big difference.", FoculyBrand.blush)
                s.apps.forEach { app -> Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppBadge(app.packageName)
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { editApp(app) }.padding(horizontal = 12.dp, vertical = 10.dp)) { Text(app.label, style = MaterialTheme.typography.titleSmall); Text(buildList { add("Session selection"); if (app.dailyMinutes > 0) add("${app.allowance(System.currentTimeMillis())}m/day"); if (app.openingMinutes > 0) add("${app.openingMinutes}m/visit") }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton({ block(BlockKind.APPS, setOf(app.packageName)) }, enabled = app.enabled && app.packageName !in s.allowlist) { Icon(Icons.Outlined.LockClock, "Block ${app.label}", Modifier.size(20.dp)) }
                    IconButton({ editApp(app) }) { Icon(Icons.Outlined.Edit, "Edit ${app.label}", Modifier.size(20.dp)) }
                } } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow("Always allowed", "${s.allowlist.size} additional exceptions · phone and settings stay available", Icons.Outlined.LockOpen) { nav("allowlist") }
                LinkRow("Daily allowances & gradual reduction", "Tap an app above to set its limits.", Icons.Outlined.Timer) { if (s.apps.isNotEmpty()) editApp(s.apps.first()) else nav("picker") }
            }
            "Websites" -> {
                Input("Website or domain", domain, { domain = it; domainError = false }, supporting = if (domainError) "Enter a domain, such as reddit.com" else null)
                Primary("Add website", enabled = domain.isNotBlank(), icon = Icons.Outlined.Add) {
                    val normalized = RulesEngine.normalizeDomain(domain)
                    if (normalized == null) domainError = true else { Repository.update { it.copy(domains = it.domains + normalized) }; domain = "" }
                }
                if (s.domains.isNotEmpty()) Primary("Block selected websites", icon = Icons.Outlined.Lock) { block(BlockKind.WEBSITES, null) }
                else DoodleCard(CatArt.PAUSE, "Less rabbit holes.", "Add a website above to give browsing a boundary.", FoculyBrand.peach)
                s.domains.sorted().forEach { host -> SoftCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Public, null, tint = MaterialTheme.colorScheme.primary); Text(host, Modifier.weight(1f).padding(start = 14.dp), fontFamily = Mono, fontSize = 14.sp); IconButton({ block(BlockKind.WEBSITES, setOf(host)) }) { Icon(Icons.Outlined.LockClock, "Block $host") }; IconButton({ Repository.update { it.copy(domains = it.domains - host) } }) { Icon(Icons.Outlined.Close, "Remove $host from focus list") } } } }
                DetailsCard("Browser coverage", "Chrome · Edge · Brave · Firefox", Icons.Outlined.Info) { Text("Experimental address-field detection. Hidden fields, private browsing, embedded browsers, and app updates can miss pages. Add the whole browser for a stronger boundary.", style = MaterialTheme.typography.bodyMedium) }
            }
            "Feeds" -> {
                Caption("SELECTIVE DETECTION · EXPERIMENTAL")
                listOf("YouTube Shorts", "Instagram Reels", "Facebook Reels").forEach { feed -> SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Icon(if (feed == "YouTube Shorts") Icons.Outlined.SmartDisplay else Icons.Outlined.Movie, null, tint = MaterialTheme.colorScheme.primary); Text(feed, style = MaterialTheme.typography.titleMedium) }
                    Primary(if (feed == "YouTube Shorts") "Block Shorts" else "Block ${feed.substringBefore(' ')} Reels", icon = Icons.Outlined.Lock) { if (Repository.update { it.copy(feeds = it.feeds + feed) }) block(BlockKind.FEEDS, setOf(feed)) }
                    ToggleRow("Use in focus sessions", "", feed in s.feeds) { enabled -> Repository.update { it.copy(feeds = if (enabled) it.feeds + feed else it.feeds - feed) } }
                } }
                if (s.feeds.isNotEmpty()) Primary("Block selected feeds", icon = Icons.Outlined.Lock) { block(BlockKind.FEEDS, null) }
                DetailsCard("Feed coverage", "Whole-app fallback available", Icons.Outlined.Info) { Text("Selective detection depends on each app's exposed controls. Use whole-app blocking if a feed is missed. Other screens remain accessible with selective rules.") }
                Primary("Choose whole-app fallback", icon = Icons.Outlined.Apps) { nav("picker") }
            }
            "Schedules" -> {
                Primary("Add a routine", icon = Icons.Outlined.Add) { editSchedule(null) }
                if (s.schedules.isEmpty()) DoodleCard(CatArt.STUDY, "Give focus a regular place.", "A quiet morning, study hours, or time to wind down.", FoculyBrand.lime)
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

@Composable fun AppRuleEditor(app: AppRule, block: () -> Unit, save: (AppRule) -> Unit) {
    var daily by rememberSaveable(app.packageName) { mutableStateOf(app.dailyMinutes.toString()) }
    var visit by rememberSaveable(app.packageName) { mutableStateOf(app.openingMinutes.toString()) }
    var target by rememberSaveable(app.packageName) { mutableStateOf(app.gradualTarget.toString()) }
    var paused by rememberSaveable(app.packageName) { mutableStateOf(app.gradualPaused) }
    var enabled by rememberSaveable(app.packageName) { mutableStateOf(app.enabled) }
    val d = daily.toIntOrNull(); val v = visit.toIntOrNull(); val t = target.toIntOrNull()
    val valid = d != null && d in 0..1440 && v != null && v in 0..1440 && t != null && t in 0..(d ?: 0)
    Page {
        Heading(app.label)
        Primary("Block now", app.enabled && app.packageName !in Repository.state.allowlist, Icons.Outlined.LockClock, block)
        ToggleRow("Use during focus", "", enabled) { enabled = it }
        DetailsCard("Daily & visit limits", "${daily}m/day · ${visit}m/visit", Icons.Outlined.Timer, initiallyOpen = app.dailyMinutes > 0 || app.openingMinutes > 0) {
        Input("Daily allowance (minutes)", daily, { daily = it.filter(Char::isDigit) }, true, "0 = no limit · up to 1440")
        Input("Time per visit (minutes)", visit, { visit = it.filter(Char::isDigit) }, true, "0 = no limit")
        }
        DetailsCard("Gradual reduction", "5 minutes less each day", Icons.AutoMirrored.Outlined.TrendingDown) {
        Input("Target allowance (minutes)", target, { target = it.filter(Char::isDigit) }, true, "Reduce by 5 minutes per day. 0 disables the plan.")
        ToggleRow("Pause reduction", "Keep the current allowance until you resume.", paused) { paused = it }
        }
        if (app.gradualTarget > 0) Caption("CURRENT ALLOWANCE · ${app.allowance(System.currentTimeMillis())} MINUTES")
        if (!Repository.usageGranted()) SoftCard { Text("Daily limits need usage access.", style = MaterialTheme.typography.titleSmall); TextButton({ Repository.openUsage() }) { Text("Enable usage access") } }
        if (app.packageName in Repository.state.allowlist) Text("This app is always allowed. Remove it from your allowlist to enforce these limits.", color = MaterialTheme.colorScheme.error)
        if (!valid) Text("Use minutes between 0 and 1440. The reduction target must not exceed your daily allowance.", color = MaterialTheme.colorScheme.error)
        Primary("Save app rules", valid, Icons.Outlined.Check) {
            val changed = d != app.dailyMinutes || t != app.gradualTarget
            val pauseChanged = paused != app.gradualPaused
            save(app.copy(enabled = enabled, dailyMinutes = if (pauseChanged && !changed) app.allowance(System.currentTimeMillis()) else d!!, openingMinutes = v!!, gradualTarget = t!!,
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
    var scopedApps by rememberSaveable { mutableStateOf(initial.apps) }
    var scopedDomains by rememberSaveable { mutableStateOf(initial.domains) }
    var allApps by rememberSaveable { mutableStateOf(initial.useAllApps) }
    var allDomains by rememberSaveable { mutableStateOf(initial.useAllDomains) }
    var strictAcknowledged by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mode) { if (mode == "STRICT") pomo = false; strictAcknowledged = false }
    val duplicateName = editing && s.profiles.any { it.id != profileId && it.name.equals(name.trim(), true) }
    val valid = name.isNotBlank() && !duplicateName && minutes.toIntOrNull()?.let { it in (if (mode == "STRICT") 1 else 0)..1440 } == true && (!pomo || (work.toIntOrNull()?.let { it in 1..240 } == true && pause.toIntOrNull()?.let { it in 1..60 } == true && rounds.toIntOrNull()?.let { it in 1..12 } == true))
    fun configured() = Profile(profileId, name.trim(), minutes.toIntOrNull() ?: 50, intention.trim(), FocusMode.valueOf(mode), pomo, work.toIntOrNull() ?: 25, pause.toIntOrNull() ?: 5, rounds.toIntOrNull() ?: 4, scopedApps, scopedDomains, allApps, allDomains)
    Page {
        Heading(if (editing) "Build your rhythm." else "Make room.")
        if (!editing) ChoiceRow(s.profiles.map { it.name }, name) { choice -> s.profiles.find { it.name == choice }?.let { p -> profileId = p.id; name = p.name; minutes = p.minutes.toString(); intention = p.intention; mode = p.mode.name; pomo = p.pomodoro; work = p.workMinutes.toString(); pause = p.breakMinutes.toString(); rounds = p.rounds.toString(); scopedApps = p.apps; scopedDomains = p.domains; allApps = p.useAllApps; allDomains = p.useAllDomains } }
        if (editing) Input("Profile name", name, { name = it.take(40) })
        Input("Your intention", intention, { intention = it.take(120) })
        if (!pomo) { ChoiceRow(listOf("25m", "50m", "90m", "Open end"), if (minutes == "0") "Open end" else "${minutes}m") { minutes = if (it == "Open end") "0" else it.removeSuffix("m") }; Input("Duration (minutes)", minutes, { minutes = it.filter(Char::isDigit).take(4) }, true, if (mode == "STRICT") "1–1440 minutes" else "0 = open end · up to 1440") }
        if (mode != "STRICT") ToggleRow("Pomodoro rhythm", "", pomo) { pomo = it }
        if (pomo) { Input("Work minutes", work, { work = it.filter(Char::isDigit) }, true, "1–240 minutes"); Input("Break minutes", pause, { pause = it.filter(Char::isDigit) }, true, "1–60 minutes"); Input("Rounds", rounds, { rounds = it.filter(Char::isDigit) }, true, "1–12 rounds · no break after the last round") }
        Caption("RESISTANCE")
        ModeCards(FocusMode.valueOf(mode), manual = minutes == "0" && !pomo) { mode = it.name }
        if (mode == "STRICT" && !editing) {
            ToggleRow("Lock until the timer ends", "I understand that FocuLY has no early end or override in Strict mode.", strictAcknowledged) { strictAcknowledged = it }
            Caption(if (ManagedProtection.isOwner()) "Android blocks removing FocuLY during Strict." else "Android can still disable or remove FocuLY on this phone. Admin access adds a deactivation step.")
        }
        DetailsCard("What gets paused", "${s.apps.count { it.enabled }} apps · ${s.domains.size} websites", Icons.Outlined.Tune) {
        ToggleRow("Use all selected app rules", "Or choose a specific subset below.", allApps) { allApps = it }
        if (s.apps.isEmpty() && s.domains.isEmpty() && s.feeds.isEmpty()) Text("You haven’t selected distractions yet. Add them in Rules to make blocking useful.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!allApps) s.apps.filter { it.enabled }.forEach { rule -> ToggleRow(rule.label, "Include in this profile", rule.packageName in scopedApps) { enabled -> scopedApps = if (enabled) scopedApps + rule.packageName else scopedApps - rule.packageName } }
        ToggleRow("Use all website rules", "An empty custom subset means no websites.", allDomains) { allDomains = it }
        if (!allDomains) s.domains.forEach { host -> ToggleRow(host, "Website rule", host in scopedDomains) { enabled -> scopedDomains = if (enabled) scopedDomains + host else scopedDomains - host } }
        if (s.feeds.isNotEmpty()) Text("Selected feed rules apply to every focus profile.", style = MaterialTheme.typography.bodyMedium)
        }
        if (!valid) Text(if (duplicateName) "Choose a unique profile name." else "Check the name and minute ranges above.", color = MaterialTheme.colorScheme.error)
        Primary(if (editing) "Save profile" else if (mode == "STRICT") "Start strict session" else "Start focus", valid && (editing || s.session == null) && (editing || mode != "STRICT" || strictAcknowledged), if (editing) Icons.Outlined.Check else Icons.Outlined.PlayArrow) { if (editing) onSave(configured()) else onStart(configured(), minutes.toInt(), intention) }
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
        Text("Session profile", style = MaterialTheme.typography.titleMedium)
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
        s.profiles.forEach { p -> LinkRow(p.name, "${if (p.pomodoro) "${p.workMinutes}/${p.breakMinutes} rhythm" else "${p.minutes} minutes"} · ${p.mode.label}", Icons.Outlined.Edit) { if (!RulesEngine.strictActive(s, System.currentTimeMillis())) edit(p) } }
        Primary("Create focus profile", enabled = !RulesEngine.strictActive(s, System.currentTimeMillis()), icon = Icons.Outlined.Add, action = add)
        Text("Gradual reduction plans live with each app’s daily allowance in Rules.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun SettingsScreen(s: FocusState, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf(Repository.usageGranted()) }
    var accessibility by remember { mutableStateOf(Repository.protectionGranted()) }
    var connected by remember { mutableStateOf(ProtectionRuntime.live()) }
    var batteryExempt by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            exportStatus = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(Repository.export().toByteArray()) } ?: error("Could not open the destination") }.fold({ "Your data was exported." }, { "Export failed. Choose a different destination." }) }
        }
    }
    LaunchedEffect(Unit) { while (true) { usage = Repository.usageGranted(); accessibility = Repository.protectionGranted(); connected = ProtectionRuntime.live(); batteryExempt = ctx.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName); delay(1000) } }
    Page {
        Heading("Keep it yours.")
        SoftCard {
            Caption("PROTECTION HEALTH")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(if (connected && s.disclosure) Icons.Outlined.VerifiedUser else Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Text(if (connected && s.disclosure) "Protection connected" else "Connect protection", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CatDoodle(CatArt.GUARD, Modifier.size(60.dp))
            }
            if (accessibility && !connected) Text("Service disconnected. Toggle FocuLY off and on in Android Accessibility settings.", style = MaterialTheme.typography.bodyMedium)
            if (ProtectionRuntime.lastError.isNotBlank()) Text(ProtectionRuntime.lastError, color = MaterialTheme.colorScheme.error)
            ActionCard("FocuLY protection", if (connected && s.disclosure) "Connected" else "Required for blocking", Icons.Outlined.Shield, connected && s.disclosure) { if (s.disclosure) Repository.openAccessibility() else consent = true }
            ActionCard("Usage access", if (usage) "Connected" else "Daily limits & recovery", Icons.Outlined.Timer, usage) { Repository.openUsage() }
            ActionCard("Background activity", if (batteryExempt) "Unrestricted" else "Check battery settings", Icons.Outlined.BatteryChargingFull, batteryExempt) { Repository.openBatterySettings() }
            Primary("Test selected app for 1 minute", enabled = connected && s.disclosure && s.session == null && !RulesEngine.strictActive(s, System.currentTimeMillis()) && s.apps.any { it.enabled && it.packageName !in s.allowlist }, icon = Icons.Outlined.PlayArrow) {
                val app = s.apps.first { it.enabled && it.packageName !in s.allowlist }
                val test = Profile("diagnostic", "Protection test", 1, "Checking that this app is locked.", FocusMode.STRICT)
                if (Repository.start(test, 1, test.intention)) { nav("home"); ctx.packageManager.getLaunchIntentForPackage(app.packageName)?.let { ctx.startActivity(it) } }
            }
        }
        DetailsCard("Setup details", "Permissions & recovery", Icons.Outlined.Info) {
            LinkRow("App information", "Restricted settings & background access") { Repository.openAppInfo() }
            LinkRow("Notification permission", "Session updates", Icons.Outlined.NotificationsNone) {
                if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
            if (android.os.Build.MANUFACTURER.equals("OnePlus", true) || android.os.Build.MANUFACTURER.equals("OPPO", true)) Text("OxygenOS: allow background activity in FocuLY's App Info → Battery settings. Menu names vary.", style = MaterialTheme.typography.bodyMedium)
            if (ProtectionRuntime.observedPackage.isNotBlank()) Caption("LAST APP · ${runCatching { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(ProtectionRuntime.observedPackage, 0)).toString() }.getOrDefault(ProtectionRuntime.observedPackage)}")
            TextButton({ consent = true }) { Text("Read protection disclosure") }
            if (s.disclosure) TextButton({ if (Repository.update { it.copy(disclosure = false) }) { FocusAccessibilityService.instance?.clearOverlay(); FocusNotifications.session(ctx) } }, enabled = !RulesEngine.strictActive(s, System.currentTimeMillis())) { Text("Withdraw consent") }
        }
        ActionCard("Uninstall & managed protection", "Optional Android administration", Icons.Outlined.Lock) { nav("managed") }
        ActionCard("FocuLY admin PIN", if (AdminPin.configured()) "PIN is set" else "Protect device controls", Icons.Outlined.Password, AdminPin.configured()) { nav("pin") }
        ActionCard("Your blocking screen", "Message, emoji & live preview", Icons.Outlined.EmojiEmotions) { nav("messages") }
        ActionCard("Setup checklist", "Review all permissions in one place", Icons.Outlined.FactCheck) { nav("permissions") }
        ActionCard("Welcome to FocuLY", "Meet your little focus companions", Icons.Outlined.AutoAwesome) { nav("welcome") }
        DetailsCard("Appearance", s.theme, Icons.Outlined.Palette) {
            ChoiceRow(listOf("System", "Light", "Dark"), s.theme) { value -> Repository.update { it.copy(theme = value) } }
            ToggleRow("Reduce motion", "", s.reduceMotion) { value -> Repository.update { it.copy(reduceMotion = value) } }
            ToggleRow("Haptic feedback", "", s.haptics) { value -> Repository.update { it.copy(haptics = value) } }
            ToggleRow("Completion & routine reminders", "", s.reminders) { value -> Repository.update { it.copy(reminders = value) }; FocusNotifications.plan(ctx) }
            ToggleRow("Hide insights", "", s.hideInsights) { value -> Repository.update { it.copy(hideInsights = value) } }
        }
        DetailsCard("Intentional visits", "Normal preferences · Resistant uses a pause", Icons.Outlined.Timer) {
            ToggleRow("Ask for an intention", "Normal only", s.askReason) { value -> Repository.update { it.copy(askReason = value) } }
            Caption("NORMAL VISIT · MINUTES")
            ChoiceRow(listOf("1", "5", "10"), s.overrideMinutes.toString()) { value -> Repository.update { it.copy(overrideMinutes = value.toInt()) } }
            Caption("COOLDOWN · MINUTES")
            ChoiceRow(listOf("0", "5", "10", "30"), s.cooldownMinutes.toString()) { value -> Repository.update { it.copy(cooldownMinutes = value.toInt()) } }
            Text("Resistant: 30-second wait, reason, one-minute visit. Strict: no visits.", style = MaterialTheme.typography.bodyMedium)
        }
        DetailsCard("Privacy & data", "Stored on this device", Icons.Outlined.PrivacyTip) {
            Text("No accounts, ads, analytics, or network access. Screen content is checked in memory and never recorded.", style = MaterialTheme.typography.bodyMedium)
            LinkRow("Export my data", "Rules, blocks & history", Icons.Outlined.Download) { export.launch("focus-data-${LocalDate.now()}.json") }
            if (exportStatus.isNotEmpty()) Text(exportStatus, style = MaterialTheme.typography.bodyMedium)
            LinkRow("Delete my data", "Available outside Strict", Icons.Outlined.Delete) { delete = true }
        }
        DetailsCard("Essential access", "Phone, home & settings stay available", Icons.Outlined.HealthAndSafety) { Text("Android settings control accessibility on ordinary phones. Device-owner management adds OS app suspension and uninstall policies. Strict has no in-app early exit.", style = MaterialTheme.typography.bodyMedium) }
        Caption("FOCULY 1.2 · DEVELOPMENT · ANDROID 9+")
    }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("How protection works", fontFamily = Mono) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("FocuLY uses Android Accessibility access to observe which app is open, check selected feed controls and supported browser address fields, and display a blocking screen for rules you choose.")
        Text("This access can expose on-screen text. FocuLY checks it only in memory on your device. It does not store screen content, send it anywhere, or use it for advertising.")
        Text("Access is optional. You can withdraw consent inside FocuLY when Strict is inactive, or turn accessibility protection off in Android settings at any time. Without access, focus timers still work. Managed whole-app suspension is controlled separately by Android device-owner policies.")
    } }, confirmButton = { TextButton({ Repository.update { it.copy(disclosure = true) }; consent = false; Repository.openAccessibility() }) { Text("Agree & open settings") } }, dismissButton = { TextButton({ consent = false }) { Text("Not now") } })
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Clear your space?", fontFamily = Mono) }, text = { Text("This deletes FocuLY’s rules, profiles, sessions, and override history and ends any active session. It does not delete Android’s system usage history. This cannot be undone.") }, confirmButton = { TextButton({ Repository.deleteAll(); delete = false; nav("home") }) { Text("Delete local data", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ delete = false }) { Text("Keep data") } })
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
            Surface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Caption("COMPLETED"); Text(records.count { it.completed && it.end in dayStart until dayEnd }.toString(), style = MaterialTheme.typography.headlineMedium); Text("FocuLY sessions", style = MaterialTheme.typography.bodySmall) } }
        }
        val progress = (focused.toFloat() / (s.goalMinutes * 60_000L)).coerceIn(0f, 1f)
        Caption("YOUR PERSONAL GUIDE · ${s.goalMinutes} MINUTES")
        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), trackColor = MaterialTheme.colorScheme.outlineVariant)
        if (focused == 0L) DoodleCard(CatArt.STUDY, "Start small. Stay curious.", "Your first session is a good place to begin.", FoculyBrand.lime)
        else Text("You made room for ${duration(focused)} today. Keep a rhythm that feels sustainable.", style = MaterialTheme.typography.bodyLarge)
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
    if (mode == FocusMode.STRICT) { AlertDialog(onDismissRequest = dismiss, title = { Text("Strict stays locked", fontFamily = Mono) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DoodleBadge(CatArt.BOUNDARY, Modifier.fillMaxWidth().height(88.dp))
        Text("The timer must finish. There is no early end in FocuLY. Phone and emergency access stay available.")
    } }, confirmButton = { TextButton(dismiss) { Text("Keep focusing") } }); return }
    var remaining by remember { mutableIntStateOf(mode.waitSeconds) }
    var reason by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { while (remaining > 0) { delay(1000); remaining-- } }
    AlertDialog(onDismissRequest = dismiss, title = { Text("A conscious pause.", fontFamily = Mono) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DoodleBadge(mode.catArt(), Modifier.fillMaxWidth().height(88.dp))
        Text(if (mode == FocusMode.STANDARD) "Resistant mode asks for a pause and a reason before ending early." else "End this session now? The time you spent focusing will still be recorded.")
        if (mode == FocusMode.STANDARD) Input("Why are you ending early?", reason, { reason = it.take(120) })
        if (remaining > 0) Text("Ready in ${remaining}s", fontFamily = Mono)
    } }, confirmButton = { TextButton(stop, enabled = remaining == 0 && (mode != FocusMode.STANDARD || reason.isNotBlank())) { Text("End session") } }, dismissButton = { TextButton(dismiss) { Text("Keep focusing") } })
}

@Composable fun LockedRules(s: FocusState) {
    Page { Heading("Rules are locked.", "Available again when Strict ends.")
        DoodleCard(CatArt.BOUNDARY, "Your boundary is holding.", "These rules become editable when Strict finishes.", FoculyBrand.blush)
        ActiveBlocks(s, System.currentTimeMillis())
        s.session?.let { Text("${it.profile.name} · ${if (it.end > 0) clock(it.end - System.currentTimeMillis()) else "schedule active"}", fontFamily = Mono) }
        s.apps.filter { it.enabled }.forEach { Text(it.label) }
        Text("You can still view insights and change appearance. Phone and emergency access remain available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun ManagedScreen(s: FocusState, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    var owner by remember { mutableStateOf(ManagedProtection.isOwner(ctx)) }
    var admin by remember { mutableStateOf(ManagedProtection.adminEnabled(ctx)) }
    var protected by remember { mutableStateOf(ManagedProtection.uninstallBlocked(ctx)) }
    var exact by remember { mutableStateOf(ManagedProtection.preciseEnding(ctx)) }
    val locked = RulesEngine.strictActive(s, System.currentTimeMillis())
    LaunchedEffect(Unit) { while (true) { owner = ManagedProtection.isOwner(ctx); admin = ManagedProtection.adminEnabled(ctx); protected = ManagedProtection.uninstallBlocked(ctx); exact = ManagedProtection.preciseEnding(ctx); delay(1000) } }
    Page {
        Heading("Keep the boundary.")
        SoftCard { Caption(if (owner) "MANAGED DEVICE" else "ORDINARY ANDROID INSTALLATION")
            Text(if (owner) "Android device-owner management is active." else "Android settings still control this app.", style = MaterialTheme.typography.titleMedium)
            Text(if (owner) "FocuLY removal is automatically blocked while Strict is active." else "Your FocuLY PIN protects in-app controls. Android still permits deactivation and removal.", style = MaterialTheme.typography.bodyMedium)
        }
        if (!owner) {
            SoftCard { Icon(Icons.Outlined.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary); Text("Uninstall resistance", style = MaterialTheme.typography.titleMedium); Text("Adds an Android administrator deactivation step before removal. No wipe, password, camera, or screen-lock policy is requested.", style = MaterialTheme.typography.bodyMedium) }
            if (!admin) Primary("Enable uninstall resistance", enabled = !locked, icon = Icons.Outlined.Lock) { ManagedProtection.requestAdmin(ctx) }
            else { Caption("ADMINISTRATOR ACTIVE · DEACTIVATE BEFORE UNINSTALLING"); Primary("Disable uninstall resistance", enabled = !locked, icon = Icons.Outlined.LockOpen) { ManagedProtection.removeAdmin(ctx) } }
        } else {
            SoftCard {
            ToggleRow("Managed uninstall protection", if (protected) "Verified: Android blocks FocuLY removal." else "Keep FocuLY installed outside Strict too.", s.uninstallProtection, enabled = !locked) { enabled -> if (Repository.update { it.copy(uninstallProtection = enabled) }) ManagedProtection.sync(ctx) }
            ToggleRow("OS-enforced Strict app blocking", "Suspend selected whole apps until Strict ends. Essential apps stay available.", s.managedLockdown, enabled = !locked) { enabled ->
                if (!enabled || exact) { if (Repository.update { it.copy(managedLockdown = enabled) }) { ManagedProtection.sync(ctx); FocusNotifications.plan(ctx) } }
                else ManagedProtection.openPreciseEnding(ctx)
            }
            LinkRow("Reliable session ending", if (exact) "Exact deadline alarm available" else "Enable before managed Strict can start") { ManagedProtection.openPreciseEnding(ctx) }
            }
        }
        ActionCard("Individual app controls", "Pause new installs · keep selected apps installed", Icons.Outlined.AppBlocking) { nav("lifecycle") }
        ActionCard("FocuLY admin PIN", if (AdminPin.configured()) "Set · unlock lasts 90 seconds" else "Add a PIN for device controls", Icons.Outlined.Password, AdminPin.configured()) { nav("pin") }
        DetailsCard("Stronger managed protection", "Device-owner setup & coverage", Icons.Outlined.Info) {
            Text("Android reserves OS uninstall blocking and app suspension for a device-owner administrator. A normal install on your OnePlus cannot grant itself that role. It requires dedicated-device or enterprise setup, typically before personal accounts are configured. See the project's managed-device guide.")
            Text("Websites and feeds need their browser or social app selected as a whole-app rule for OS enforcement. After reboot, exact ending may wait for Android to unlock app storage. Opening FocuLY releases expired rules.")
            Text("Android shows its own administration dialogs. Your custom FocuLY message is supplied as administrator support text where Android displays it. Device resets and administrator tools remain outside this boundary.")
        }
        if (locked) Text("Protection preferences stay locked until Strict ends.", color = MaterialTheme.colorScheme.primary)
    }
}
