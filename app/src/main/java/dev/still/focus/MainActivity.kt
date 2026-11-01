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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.selection.toggleable
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {
    var requestedScreen by mutableStateOf("home")
    var navigationSequence by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repository.init(this)
        requestedScreen = savedInstanceState?.getString("requestedScreen") ?: intent.getStringExtra("screen") ?: "home"
        navigationSequence = savedInstanceState?.getInt("navigationSequence") ?: 0
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent { StillTheme(Repository.state.theme, Repository.state.reduceMotion) { StillApp(requestedScreen, navigationSequence) } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); requestedScreen = intent.getStringExtra("screen") ?: "home"; navigationSequence++ }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("requestedScreen", requestedScreen)
        outState.putInt("navigationSequence", navigationSequence)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() { super.onResume(); Repository.reconcile(); FocusNotifications.session(this); FocusNotifications.plan(this); BreakReminders.schedule(this) }
}

@Composable fun StillApp(requested: String, navigationSequence: Int = 0) {
    val activity = LocalActivity.current
    val lightBars = MaterialTheme.colorScheme.background.luminance() > .5f
    SideEffect { activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView).apply { isAppearanceLightStatusBars = lightBars; isAppearanceLightNavigationBars = lightBars } } }
    val s = Repository.state
    var route by rememberSaveable { mutableStateOf(requested) }
    var appliedNavigation by rememberSaveable { mutableIntStateOf(navigationSequence) }
    var tab by rememberSaveable { mutableStateOf("Apps") }
    var blockKind by rememberSaveable { mutableStateOf(BlockKind.APPS) }
    var blockTargets by rememberSaveable { mutableStateOf<Set<String>?>(null) }
    var blockParent by rememberSaveable { mutableStateOf("rules") }
    var settingsParent by rememberSaveable { mutableStateOf("home") }
    var permissionParent by rememberSaveable { mutableStateOf("settings") }
    var profileParent by rememberSaveable { mutableStateOf("home") }
    var pinParent by rememberSaveable { mutableStateOf("settings") }
    val screenState = rememberSaveableStateHolder()
    var editingProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingScheduleId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProfileId by rememberSaveable { mutableStateOf(s.profiles.firstOrNull()?.id) }
    val editingProfile = s.profiles.find { it.id == editingProfileId }
    val editingSchedule = s.schedules.find { it.id == editingScheduleId }
    val selectedProfile = s.profiles.find { it.id == selectedProfileId } ?: s.profiles.firstOrNull() ?: FocusState.defaults().first()
    var sessionParent by rememberSaveable { mutableStateOf("home") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var usageReady by remember { mutableStateOf(false) }
    LaunchedEffect(requested, navigationSequence) {
        if (appliedNavigation != navigationSequence) { route = requested; appliedNavigation = navigationSequence }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); Repository.reconcile(); delay(1000) } }
    // Share one usage snapshot across tabs instead of querying Android on every dock tap.
    LaunchedEffect(now / 15_000) { usage = withContext(Dispatchers.IO) { UsageReader.day(Repository.context) }; usageReady = true }
    val mainRoutes = listOf("home", "rules", "insights")
    val back: () -> Unit = { route = when (route) { "block" -> blockParent; "setup" -> sessionParent; "settings" -> settingsParent; "permissions" -> permissionParent; "profile" -> profileParent; "pin" -> pinParent; "profileEdit" -> "profile"; "managed", "messages", "welcome" -> "settings"; "lifecycle" -> "managed"; "picker", "allowlist", "schedule", "appRule" -> "rules"; else -> "home" } }
    BackHandler(route !in mainRoutes, onBack = back)
    var appRulePackage by rememberSaveable { mutableStateOf<String?>(null) }
    val appRule = s.apps.find { it.packageName == appRulePackage }
    var stopDialog by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var strictRequested by remember { mutableStateOf(false) }
    val startBlock: (BlockKind, Set<String>?) -> Unit = { kind, targets -> screenState.removeState("block"); blockKind = kind; blockTargets = targets; blockParent = if (route == "appRule") "rules" else route; route = "block" }
    val startSession: (Profile) -> Unit = { screenState.removeState("setup"); selectedProfileId = it.id; sessionParent = route; route = "setup" }
    val nav: (String) -> Unit = { when (it) {
        "blockApps" -> startBlock(BlockKind.APPS, null)
        "blockWebsites" -> startBlock(BlockKind.WEBSITES, null)
        "blockFeeds" -> startBlock(BlockKind.FEEDS, null)
        "permissions" -> { permissionParent = route; route = "permissions" }
        "settings" -> { settingsParent = route; route = "settings" }
        "pin" -> { pinParent = route; route = "pin" }
        "profile" -> { profileParent = route; route = "profile" }
        else -> route = it
    } }
    if (!s.onboarding) {
        FoculyOnboarding { Repository.update { it.copy(onboarding = true) }; route = "home" }
        return
    }
    if (route == "welcome") { FoculyOnboarding { route = "settings" }; return }
    val floatingHeader = route in mainRoutes || route in listOf("settings", "setup", "block")
    Box(Modifier.fillMaxSize()) {
    Scaffold(containerColor = Color.Transparent,
        topBar = { if (!floatingHeader) Header(route, s.name, { profileParent = route; route = "profile" }, { settingsParent = route; route = "settings" }, back) }
    ) { padding ->
        val pageInsets = PaddingValues(top = if (floatingHeader) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else padding.calculateTopPadding(), bottom = if (route in mainRoutes) 0.dp else padding.calculateBottomPadding())
        val headerClearance = if (floatingHeader) 80.dp else 0.dp
        val dockClearance = if (route in mainRoutes) 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
        CompositionLocalProvider(LocalPageTopInset provides headerClearance, LocalPageBottomInset provides dockClearance) {
        Box(Modifier.fillMaxSize().padding(pageInsets).consumeWindowInsets(pageInsets)) {
            screenState.SaveableStateProvider(route) {
            when (route) {
                "home" -> Home(s, now, usage, usageReady, nav, startSession, { stopDialog = true })
                "rules" -> Rules(s, tab, { tab = it }, nav, { appRulePackage = it.packageName; route = "appRule" }, { screenState.removeState("schedule"); editingScheduleId = it?.id; route = "schedule" }, startBlock)
                "block" -> BlockEditor(s, blockKind, blockTargets, { route = "home" }, { permissionParent = "block"; route = "permissions" })
                "insights" -> Insights(s, usage, usageReady)
                "profile" -> ProfileScreen(s,
                    { screenState.removeState("profileEdit"); editingProfileId = it.id; route = "profileEdit" },
                    { screenState.removeState("profileEdit"); editingProfileId = null; route = "profileEdit" },
                    { profile -> Repository.update { state -> state.copy(profiles = state.profiles.filterNot { it.id == profile.id }, schedules = state.schedules.filterNot { it.profileId == profile.id }) } })
                "settings" -> SettingsScreen(s, nav)
                "managed" -> DeviceControlsGate { ManagedScreen(s, nav) }
                "lifecycle" -> DeviceControlsGate { LifecycleScreen(s) }
                "pin" -> PinScreen(s)
                "messages" -> MessageScreen(s)
                "permissions" -> SetupChecklist(doneLabel = if (permissionParent == "block") "Back to block" else "Done") { route = permissionParent }
                "picker", "allowlist" -> AppPicker(s, route == "allowlist") { route = "rules" }
                "appRule" -> appRule?.let { app -> AppRuleEditor(app, { startBlock(BlockKind.APPS, setOf(app.packageName)) }) { updated -> Repository.update { state -> state.copy(apps = state.apps.map { if (it.packageName == updated.packageName) updated else it }) }; route = "rules" } }
                "schedule" -> ScheduleEditor(s, editingSchedule, { nav("profile") }) { schedule -> if (Repository.update { state -> state.copy(schedules = state.schedules.filterNot { it.id == schedule.id } + schedule) }) { FocusNotifications.plan(Repository.context); route = "rules" } }
                "setup", "profileEdit" -> SessionEditor(s, if (route == "profileEdit") editingProfile else selectedProfile, route == "profileEdit", onSave = { p -> if (Repository.update { state -> state.copy(profiles = state.profiles.filterNot { it.id == p.id } + p) }) route = "profile" }, onStart = { p, duration, intention ->
                    val action: (Boolean) -> Unit = { timerOnly -> if (Repository.start(p, duration, intention, timerOnly)) route = "home" }
                    if (!Repository.protectionReady()) { strictRequested = p.mode == FocusMode.STRICT; pendingStart = action; timerDialog = true } else action(false)
                })
                else -> Home(s, now, usage, usageReady, nav, startSession, { stopDialog = true })
            }
            }
        }
        }
    }
    if (route in mainRoutes) Dock(route, nav, Modifier.align(Alignment.BottomCenter))
    if (floatingHeader) Header(route, s.name, { profileParent = route; route = "profile" }, { settingsParent = route; route = "settings" }, back)
    }
    if (timerDialog) AlertDialog(onDismissRequest = { timerDialog = false }, title = { Text("Enable protection", fontFamily = Mono) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DoodleBadge(CatArt.REMINDER, Modifier.fillMaxWidth().height(88.dp))
        Text(if (strictRequested) "Strict requires a connected protection service. Enable it and allow background activity first." else "Blocking needs your consent and a connected Android accessibility service. You can enable it now, or explicitly start a timer without blocking.")
    } }, confirmButton = { TextButton({ timerDialog = false; permissionParent = "setup"; route = "permissions" }) { Text("Enable protection") } }, dismissButton = { if (!strictRequested) TextButton({ timerDialog = false; pendingStart?.invoke(true); pendingStart = null }) { Text("Timer only") } })
    if (stopDialog) EndSessionDialog(s.session?.profile?.mode ?: FocusMode.STANDARD, { stopDialog = false }) { Repository.stop(); stopDialog = false }
}

@Composable fun Header(route: String, name: String, profile: () -> Unit, settings: () -> Unit, back: () -> Unit) {
    val root = route in listOf("home", "rules", "insights")
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
    FloatingGlassChrome(Modifier.widthIn(max = 360.dp).fillMaxWidth().height(64.dp), highContrast = route !in listOf("setup", "block")) {
    Row(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (root) IconButton(profile, Modifier.size(48.dp)) {
                ProfileAvatar(name, Repository.state.avatarImage, Modifier.size(28.dp).semantics { contentDescription = "Profile" })
        } else IconButton(back, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") }
        Text(if (root) "" else when (route) { "block" -> "Block setup"; "profile" -> "Your space"; "settings" -> "Settings"; "managed" -> "Device protection"; "lifecycle" -> "App boundaries"; "pin" -> "Admin PIN"; "messages" -> "Blocking screen"; "permissions" -> "Permissions"; "setup" -> "Setup"; "profileEdit" -> "Focus profile"; "schedule" -> "Schedule"; "allowlist" -> "Always allowed"; "appRule" -> "App rules"; else -> "Choose apps" }, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        when {
            root -> IconButton(settings, Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings, "Settings", Modifier.size(24.dp)) }
            route == "settings" -> AppearanceToggle()
            else -> Spacer(Modifier.width(48.dp))
        }
    }
    }
    }
}
@Composable private fun AppearanceToggle() {
    val dark = LocalGlassDark.current
    val haptic = LocalHapticFeedback.current
    Box(Modifier.size(48.dp).clip(CircleShape)
        .toggleable(value = dark, role = Role.Switch, onValueChange = { nextDark ->
            if (Repository.state.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            Repository.update { it.copy(theme = if (nextDark) "Dark" else "Light") }
        }).semantics { contentDescription = "Dark mode"; stateDescription = if (dark) "Dark" else "Light" }, contentAlignment = Alignment.Center) {
        Crossfade(dark, animationSpec = tween(if (Repository.state.reduceMotion) 0 else 180), label = "Appearance icon") { enabled ->
            Icon(if (enabled) Icons.Outlined.DarkMode else Icons.Outlined.LightMode, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}
private val LocalPageTopInset = staticCompositionLocalOf { 0.dp }
private val LocalPageBottomInset = staticCompositionLocalOf { 0.dp }

/** A separate transparent overlay lets the page scroll behind the capsule, like Stash. */
@Composable fun Dock(route: String, nav: (String) -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalGlassDark.current
    val destinations = remember { listOf(Triple("home", "Home", Icons.Outlined.Home), Triple("rules", "Rules", Icons.Outlined.Tune), Triple("insights", "Insights", Icons.Outlined.BarChart)) }
    val selectedIndex = destinations.indexOfFirst { it.first == route }.coerceAtLeast(0)
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(36.dp)
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        FloatingGlassChrome(Modifier.widthIn(max = 360.dp).fillMaxWidth().height(72.dp), shape) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(8.dp)) {
            val itemWidth = maxWidth / destinations.size
            val selectionOffset = animateDpAsState(itemWidth * selectedIndex,
                animationSpec = if (Repository.state.reduceMotion) snap() else tween(180), label = "Dock selection")
            Box(Modifier.offset { IntOffset(selectionOffset.value.roundToPx(), 0) }.width(itemWidth).fillMaxHeight().padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = if (dark) .12f else .17f))
                .border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(28.dp)))
            Row(Modifier.fillMaxSize().selectableGroup()) {
                destinations.forEach { (key, label, icon) ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp))
                        .selectable(selected = route == key, role = Role.Tab, onClick = {
                            if (route != key) {
                                if (Repository.state.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                nav(key)
                            }
                        }).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(26.dp), tint = if (route == key) Color.White else Color(0xDDF1F5FC))
                    }
                }
            }
        }
        }
    }
}
@Composable fun Page(bottomPadding: Dp = 28.dp, content: @Composable ColumnScope.() -> Unit) {
    val frost = LocalFrostState.current
    val source = if (frost != null) Modifier.hazeSource(frost, zIndex = 1f, key = "page") else Modifier
    Column(Modifier.fillMaxSize().then(source).padding(bottom = LocalPageBottomInset.current)
        .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
        .padding(top = 16.dp + LocalPageTopInset.current, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}
@Composable fun Caption(text: String) { Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable fun Heading(title: String, subtitle: String? = null) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge); subtitle?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable fun Primary(text: String, enabled: Boolean = true, icon: ImageVector? = Icons.AutoMirrored.Outlined.ArrowForward, action: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Button({ if (Repository.state.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); action() },
        Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xE6F2F7FF), contentColor = Color(0xFF204875),
            disabledContainerColor = Color.White.copy(alpha = .10f), disabledContentColor = Color.White.copy(alpha = .55f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) .72f else .16f)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
        Text(text, Modifier.weight(1f)); icon?.let { Icon(it, null, Modifier.size(24.dp)) }
    }
}
@Composable fun FrostedPrimary(text: String, enabled: Boolean = true, icon: ImageVector = Icons.Outlined.PlayArrow,
    supportingText: String? = null, sceneOnly: Boolean = false, action: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(if (pressed && enabled) .98f else 1f,
        animationSpec = if (Repository.state.reduceMotion) snap() else tween(120), label = "Glass action press")
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(30.dp)
    FloatingGlassChrome(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .alpha(if (enabled) 1f else .45f), shape, highContrast = true, sceneOnly = sceneOnly) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = enabled, role = Role.Button) {
                if (Repository.state.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                action()
            }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
                supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE0EBFA)) }
            }
            Icon(icon, null, Modifier.size(26.dp), tint = Color.White)
        }
    }
}
@Composable fun SoftCard(content: @Composable ColumnScope.() -> Unit) { GlassSurface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
@Composable fun ToggleRow(title: String, description: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange).semantics { contentDescription = title }, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(title, style = MaterialTheme.typography.bodyLarge); if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, null, enabled = enabled) } }
@Composable fun LinkRow(title: String, detail: String, icon: ImageVector = Icons.Outlined.ChevronRight, action: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = action).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable fun Input(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, supporting: String? = null) { val keyboard = LocalSoftwareKeyboardController.current; val focus = LocalFocusManager.current; OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White.copy(alpha = .10f), unfocusedContainerColor = Color.White.copy(alpha = .06f)), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focus.clearFocus() }), supportingText = supporting?.let { { Text(it) } }) }
@Composable fun ChoiceRow(options: List<String>, selected: String, choose: (String) -> Unit) { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { FilterChip(selected == it, { choose(it) }, label = { Text(it) }) } } }
fun clock(millis: Long): String { val sec = (millis.coerceAtLeast(0) + 999) / 1000; return if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60) else "%02d:%02d".format(sec / 60, sec % 60) }
fun duration(millis: Long): String { val m = millis / 60_000; return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }
fun minuteLabel(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
fun profileDuration(minutes: Int) = when (minutes) {
    0 -> "Open-ended"
    10_080 -> "1 week"
    1_440 -> "1 day"
    60 -> "1 hour"
    1 -> "1 minute"
    else -> if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60} hours" else "$minutes minutes"
}
fun profileArt(profile: Profile): CatArt = CatArt.entries.firstOrNull { it.name == profile.art } ?: CatArt.FOCUS

@Composable fun Home(s: FocusState, now: Long, usage: Map<String, Long>, usageReady: Boolean, nav: (String) -> Unit, start: (Profile) -> Unit, stop: () -> Unit) {
    Page {
        val session = s.session
        if (session == null) {
            ActiveBlocks(s, now)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Triple("Block apps", "blockApps", R.drawable.cat_block_apps), Triple("Block websites", "blockWebsites", R.drawable.cat_block_websites)).forEach { (title, target, art) ->
                    GlassSurface(Modifier.weight(1f).height(152.dp).clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button) { nav(target) }.semantics { contentDescription = title },
                        shape = RoundedCornerShape(20.dp), color = if (target == "blockApps") FoculyBrand.lilac else FoculyBrand.peach,
                        contentColor = MaterialTheme.colorScheme.onSurface) {
                        Box(Modifier.fillMaxSize().padding(8.dp)) {
                            Image(painterResource(art), null, Modifier.fillMaxSize().padding(bottom = 24.dp), contentScale = ContentScale.Fit)
                            Text(title, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                                style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            GlassSurface(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable { nav("blockFeeds") }, color = FoculyBrand.lime, contentColor = MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CatDoodle(CatArt.FEEDS, Modifier.size(72.dp))
                    Column(Modifier.weight(1f)) { Text("Shorts & Reels", style = MaterialTheme.typography.titleMedium); Text("Pause the endless scroll", style = MaterialTheme.typography.bodySmall) }
                    Icon(Icons.Outlined.ChevronRight, null)
                }
            }
            if (!Repository.protectionReady()) LinkRow("Set up protection", "Enable blocking when you’re ready.", Icons.Outlined.Shield) { nav("permissions") }
            else Caption("PROTECTION CONNECTED")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Find your rhythm", style = MaterialTheme.typography.titleMedium)
                s.profiles.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { p -> GlassSurface(Modifier.weight(1f).heightIn(min = 220.dp).clip(RoundedCornerShape(20.dp)).clickable { start(p) },
                        shape = RoundedCornerShape(20.dp), color = profileArt(p).tint(), contentColor = MaterialTheme.colorScheme.onSurface) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(painterResource(profileArt(p).drawable), null,
                                Modifier.fillMaxWidth().height(124.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                                Text("${profileDuration(p.minutes)} · ${p.mode.label}", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f))
                            }
                        }
                    } }; if (pair.size == 1) Spacer(Modifier.weight(1f))
                } }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { Caption("ONE THING AT A TIME") }; DoodleBadge(CatArt.FOCUS, Modifier.size(88.dp)) }
            Text(session.intention, style = MaterialTheme.typography.headlineLarge)
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                val accent = MaterialTheme.colorScheme.primary
                val line = MaterialTheme.colorScheme.outlineVariant
                Canvas(Modifier.size(230.dp).clip(CircleShape).background(Color.White.copy(alpha = .06f)).border(1.dp, Color.White.copy(alpha = .20f), CircleShape)) { drawCircle(line, style = Stroke(6.dp.toPx())); val progress = if (session.end == 0L) .85f else ((session.end - now).toFloat() / (session.end - session.start)).coerceIn(0f, 1f); drawArc(accent, -90f, 360 * progress, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (session.end == 0L) clock(now - session.start) else clock(session.remaining(now)), style = MaterialTheme.typography.displayLarge)
                    Caption(if (session.end == 0L) "TIME IN FOCUS" else "REMAINING")
                }
            }
            SoftCard {
                val appCount = RulesEngine.selectedAppCount(s, session.profile)
                val domainCount = RulesEngine.selectedDomainCount(s, session.profile)
                Caption(session.profile.name.uppercase())
                Text(if (s.managedLockdown && ManagedProtection.verifiedSuspendedCount() > 0) "${ManagedProtection.verifiedSuspendedCount()} apps locked by Android" else if (!session.timerOnly && Repository.protectionReady()) "$appCount ${if (appCount == 1) "selected app" else "selected apps"} · $domainCount ${if (domainCount == 1) "website" else "websites"}" else if (session.timerOnly) "Timer only · protection is off" else "Blocking interrupted · check protection health", style = MaterialTheme.typography.bodyLarge)
                Text("${session.profile.mode.label} mode · essential apps stay available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
                s.apps.forEach { app -> GlassSurface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppBadge(app.packageName)
                    val installed = remember(app.packageName, PackageStatus.revision) { ManagedProtection.installed(Repository.context, app.packageName) }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { block(BlockKind.APPS, setOf(app.packageName)) }.padding(horizontal = 12.dp, vertical = 10.dp)) { Text(app.label, style = MaterialTheme.typography.titleSmall); Text(buildList { add(if (installed) "Tap to block" else "Not installed · rule kept"); if (app.dailyMinutes > 0) add("${app.allowance(System.currentTimeMillis())}m/day"); if (app.openingMinutes > 0) add("${app.openingMinutes}m/visit") }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton({ block(BlockKind.APPS, setOf(app.packageName)) }, enabled = app.enabled && app.packageName !in s.allowlist) { Icon(Icons.Outlined.LockClock, "Block ${app.label}", Modifier.size(20.dp)) }
                    IconButton({ editApp(app) }) { Icon(Icons.Outlined.Edit, "Edit ${app.label}", Modifier.size(20.dp)) }
                } } }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LinkRow("Always allowed", "${s.allowlist.size} additional exceptions · phone and settings stay available", Icons.Outlined.LockOpen) { nav("allowlist") }
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
                DoodleCard(CatArt.FEEDS, "Keep the app. Pause the feed.", "Choose Shorts or Reels below.", FoculyBrand.lime)
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
                if (s.schedules.isEmpty()) DoodleCard(CatArt.ROUTINE, "Give focus a regular place.", "A quiet morning, study hours, or time to wind down.", FoculyBrand.lime)
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
    LaunchedEffect(PackageStatus.revision) { apps = withContext(Dispatchers.IO) {
        ctx.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != ctx.packageName && !FocusAccessibilityService.essential(ctx, it.activityInfo.packageName) }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(ctx.packageManager).toString(), it.loadIcon(ctx.packageManager)) }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    } }
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (allow) "These apps stay available even when a rule would block them." else "Choose what you want to pause. Phone, emergency access, your launcher, and settings are always available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Input("Search installed apps", query, { query = it })
        if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        val visible = apps?.filter { it.label.contains(query, true) }.orEmpty()
        if (apps != null && visible.isEmpty()) Text("No matching apps on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 92.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    }
    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).background(Color(0xFFF2F7FF), RoundedCornerShape(24.dp))) {
        Primary("Done · ${if (allow) s.allowlist.size else s.apps.size} selected", icon = Icons.Outlined.Check, action = done)
    }
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
    var intention by rememberSaveable { mutableStateOf(initial.intention) }
    var mode by rememberSaveable { mutableStateOf(initial.mode.name) }
    var customTime by rememberSaveable { mutableStateOf(initial.minutes !in setOf(0, 25, 50, 90, 1_440, 10_080)) }
    var scopedApps by rememberSaveable { mutableStateOf(initial.apps) }
    var scopedDomains by rememberSaveable { mutableStateOf(initial.domains) }
    var allApps by rememberSaveable { mutableStateOf(initial.useAllApps) }
    var allDomains by rememberSaveable { mutableStateOf(initial.useAllDomains) }
    var art by rememberSaveable { mutableStateOf(initial.art) }
    var chooseApps by rememberSaveable { mutableStateOf(false) }
    var chooseWebsites by rememberSaveable { mutableStateOf(false) }
    val installed = selectableApps(s)
    var strictAcknowledged by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(mode) { strictAcknowledged = false }
    val duplicateName = editing && s.profiles.any { it.id != profileId && it.name.equals(name.trim(), true) }
    val valid = name.isNotBlank() && !duplicateName && minutes.toIntOrNull()?.let { it in (if (mode == "STRICT") 1 else 0)..10_080 } == true
    fun configured() = Profile(profileId, name.trim(), minutes.toIntOrNull() ?: 50, intention.trim(), FocusMode.valueOf(mode), scopedApps, scopedDomains, allApps, allDomains, art)
    val draftProfile = configured()
    val strictNeedsAcknowledgement = mode == "STRICT" && (!editing || original?.mode != FocusMode.STRICT)
    Box(Modifier.fillMaxSize().imePadding()) {
    CompositionLocalProvider(LocalPageBottomInset provides (LocalPageBottomInset.current + 140.dp)) {
    Page {
        if (editing) Heading(if (original == null) "New profile." else "Edit profile.")
        else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            DoodleBadge(profileArt(draftProfile), Modifier.size(80.dp))
        }
        if (editing) {
            GlassSurface(shape = RoundedCornerShape(24.dp), color = FoculyBrand.lilac, contentColor = MaterialTheme.colorScheme.onSurface) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CatDoodle(CatArt.entries.firstOrNull { it.name == art } ?: CatArt.FOCUS, Modifier.size(88.dp))
                    Column { Text(name.ifBlank { "Your focus" }, style = MaterialTheme.typography.titleMedium); Text(profileDuration(minutes.toIntOrNull() ?: 50), style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(CatArt.FOCUS, CatArt.STUDY, CatArt.GUARD, CatArt.BREAK, CatArt.PAUSE, CatArt.CHOOSE, CatArt.FEEDS, CatArt.ROUTINE, CatArt.TUNE).forEach { option ->
                    GlassSurface(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).clickable { art = option.name }
                        .semantics { contentDescription = "${option.name.lowercase()} illustration"; selected = art == option.name },
                        shape = RoundedCornerShape(18.dp), color = if (art == option.name) FoculyBrand.lilac else MaterialTheme.colorScheme.surfaceVariant) {
                        CatDoodle(option, Modifier.padding(8.dp))
                    }
                }
            }
        }
        if (editing) Input("Name", name, { name = it.take(40) })
        Input("Intention", intention, { intention = it.take(120) })
        Caption("DURATION")
        val durationChoice = if (customTime) "Custom" else when (minutes) { "0" -> "Open end"; "1440" -> "1 day"; "10080" -> "1 week"; else -> "${minutes}m" }
        ChoiceRow(listOf("25m", "50m", "90m", "1 day", "1 week", "Custom") + if (mode == "STRICT") emptyList() else listOf("Open end"), durationChoice) {
            customTime = it == "Custom"
            if (!customTime) minutes = when (it) { "Open end" -> "0"; "1 day" -> "1440"; "1 week" -> "10080"; else -> it.removeSuffix("m") }
        }
        if (customTime) Input("Minutes", minutes, { minutes = it.filter(Char::isDigit).take(5) }, true)
        Caption("RESISTANCE")
        ModeCards(FocusMode.valueOf(mode), manual = minutes == "0") { mode = it.name }
        if (mode == "STRICT") {
            if (strictNeedsAcknowledgement) ToggleRow("I understand Strict", "No early end in FocuLY.", strictAcknowledged) { strictAcknowledged = it }
            Text("Android can still disable protection on a personal phone.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TargetSummary("Apps", s.apps.filter { it.enabled && it.packageName !in s.allowlist && (allApps || it.packageName in scopedApps) }.map { it.label }, Icons.Outlined.Apps) { chooseApps = true }
        TargetSummary("Websites", (if (allDomains) s.domains else scopedDomains.intersect(s.domains)).sorted(), Icons.Outlined.Public) { chooseWebsites = true }
        if (!valid) Text(if (duplicateName) "Choose a unique profile name." else "Check the name and minute ranges above.", color = MaterialTheme.colorScheme.error)
        if (!editing && s.session != null) Text("End your current session before starting another.", color = MaterialTheme.colorScheme.error)
    }
    }
    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
        FrostedPrimary(if (editing) "Save profile" else if (mode == "STRICT") "Start strict session" else "Start focus",
            valid && (editing || s.session == null) && (!strictNeedsAcknowledgement || strictAcknowledged),
            if (editing) Icons.Outlined.Check else Icons.Outlined.PlayArrow,
            supportingText = "${profileDuration(minutes.toIntOrNull() ?: 0)} · ${FocusMode.valueOf(mode).label}") {
            if (editing) onSave(configured()) else onStart(configured(), minutes.toInt(), intention)
        }
    }
    }
    if (chooseApps) AppSelectionSheet(installed,
        if (allApps) s.apps.filter { it.enabled && it.packageName !in s.allowlist }.map { it.packageName }.toSet() else scopedApps,
        { chosen ->
            val added = installed.orEmpty().filter { it.pkg in chosen }
            if (Repository.update { state -> state.copy(apps = state.apps.map { if (it.packageName in chosen) it.copy(enabled = true) else it } + added.filterNot { app -> state.apps.any { it.packageName == app.pkg } }.map { AppRule(it.pkg, it.label) }) }) { allApps = false; scopedApps = chosen }
        }, { chooseApps = false }, allApps, { allApps = it; if (!it) scopedApps = s.apps.filter { rule -> rule.enabled && rule.packageName !in s.allowlist }.map { rule -> rule.packageName }.toSet() })
    if (chooseWebsites) WebsiteSelectionSheet(s.domains, if (allDomains) s.domains else scopedDomains, allDomains,
        { allDomains = false; scopedDomains = it }, { allDomains = it; if (!it) scopedDomains = s.domains },
        { host -> if (Repository.update { it.copy(domains = it.domains + host) }) { if (!allDomains) scopedDomains = scopedDomains + host; true } else false }, { chooseWebsites = false })
}

@Composable fun ScheduleEditor(s: FocusState, original: Schedule?, createProfile: () -> Unit, save: (Schedule) -> Unit) {
    val scheduleId = rememberSaveable { original?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(original?.name ?: "Quiet hours") }
    var start by rememberSaveable { mutableStateOf(minuteLabel(original?.startMinute ?: 540)) }
    var end by rememberSaveable { mutableStateOf(minuteLabel(original?.endMinute ?: 660)) }
    var days by rememberSaveable { mutableStateOf(original?.days ?: setOf(1, 2, 3, 4, 5)) }
    var profile by rememberSaveable { mutableStateOf(original?.profileId ?: s.profiles.firstOrNull()?.id.orEmpty()) }
    LaunchedEffect(s.profiles) { if (s.profiles.none { it.id == profile }) profile = s.profiles.firstOrNull()?.id.orEmpty() }
    var reminder by rememberSaveable { mutableStateOf(original?.reminder ?: false) }
    var conflict by remember { mutableStateOf<Schedule?>(null) }
    fun time(raw: String): Int? { val pieces = raw.split(':'); if (pieces.size != 2) return null; val h = pieces[0].toIntOrNull() ?: return null; val m = pieces[1].toIntOrNull() ?: return null; return if (h in 0..23 && m in 0..59) h * 60 + m else null }
    val valid = name.isNotBlank() && days.isNotEmpty() && time(start) != null && time(end) != null && time(start) != time(end) && s.profiles.any { it.id == profile }
    Page {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("A place for focus.", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium); DoodleBadge(CatArt.ROUTINE, Modifier.size(80.dp)) }
        Input("Routine name", name, { name = it.take(40) })
        ChoiceRow(listOf("Weekdays", "Weekend", "Every day"), when (days) { setOf(1, 2, 3, 4, 5) -> "Weekdays"; setOf(6, 7) -> "Weekend"; (1..7).toSet() -> "Every day"; else -> "Custom" }) { days = when (it) { "Weekdays" -> setOf(1, 2, 3, 4, 5); "Weekend" -> setOf(6, 7); else -> (1..7).toSet() } }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { (1..7).forEach { d -> FilterChip(d in days, { days = if (d in days) days - d else days + d }, label = { Text(DayOfWeek.of(d).name.take(3)) }) } }
        Input("Starts (24-hour HH:mm)", start, { start = it.take(5) })
        Input("Ends (24-hour HH:mm)", end, { end = it.take(5) }, supporting = "An earlier end time continues into the following day.")
        Text("Session profile", style = MaterialTheme.typography.titleMedium)
        if (s.profiles.isEmpty()) ActionCard("Create a profile", "Routines need a saved focus profile", Icons.Outlined.Add, action = createProfile)
        ChoiceRow(s.profiles.map { it.name }, s.profiles.find { it.id == profile }?.name ?: "") { choice -> profile = s.profiles.first { it.name == choice }.id }
        ToggleRow("A gentle heads-up", "Remind me about five minutes before.", reminder) { reminder = it }
        if (!valid && s.profiles.isNotEmpty()) Text("Choose at least one day and two different valid times.", color = MaterialTheme.colorScheme.error)
        Primary("Save routine", valid, Icons.Outlined.Check) {
            val candidate = Schedule(scheduleId, name.trim(), profile, days, time(start)!!, time(end)!!, original?.enabled ?: true, reminder)
            if (s.schedules.any { it.id != candidate.id && candidate.overlaps(it) }) conflict = candidate else save(candidate)
        }
    }
    conflict?.let { candidate -> AlertDialog(onDismissRequest = { conflict = null }, title = { Text("Routines overlap", fontFamily = Mono) }, text = { Text("Both routines will apply. Their restrictions combine, and the strictest applicable mode wins. Allowlisted apps stay available.") }, confirmButton = { TextButton({ conflict = null; save(candidate) }) { Text("Save overlapping routine") } }, dismissButton = { TextButton({ conflict = null }) { Text("Adjust times") } }) }
}

@Composable fun ProfileScreen(s: FocusState, edit: (Profile) -> Unit, add: () -> Unit, remove: (Profile) -> Unit) {
    var goal by rememberSaveable { mutableStateOf(s.goalMinutes.toString()) }
    var pendingDelete by remember { mutableStateOf<Profile?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var photoStatus by remember { mutableStateOf("") }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            val result = withContext(Dispatchers.IO) { runCatching { BlockingArtwork.importImage(ctx, uri) } }
            result.onSuccess { image ->
                if (Repository.update { it.copy(avatarImage = image) }) {
                    withContext(Dispatchers.IO) { BlockingArtwork.prune(ctx, emptySet()) }
                    photoStatus = "Photo updated."
                } else photoStatus = "Couldn't save your photo. Try again."
            }.onFailure { photoStatus = if (it is IllegalArgumentException) it.message.orEmpty() else "Couldn't open this photo. Try another image." }
            importing = false
        }
    }
    val canEdit = !RulesEngine.strictActive(s, System.currentTimeMillis())
    Page {
        SoftCard {
            Text("About you", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileAvatar(s.name, s.avatarImage, Modifier.size(80.dp))
                Column(Modifier.weight(1f)) {
                    TextButton({ photoPicker.launch("image/*") }, enabled = !importing) { Text(if (importing) "Adding photo…" else "Change photo") }
                    if (s.avatarImage.isNotEmpty()) TextButton({
                        if (Repository.update { it.copy(avatarImage = "") }) {
                            scope.launch(Dispatchers.IO) { BlockingArtwork.prune(ctx, emptySet()) }
                            photoStatus = "Photo removed."
                        }
                    }, enabled = !importing) { Text("Remove photo") }
                }
            }
            if (photoStatus.isNotEmpty()) Text(photoStatus, style = MaterialTheme.typography.bodySmall)
            Input("Name", s.name, { value -> Repository.update { it.copy(name = value.take(30)) } })
            Input("Default intention", s.intention, { value -> Repository.update { it.copy(intention = value.take(120)) } })
            Input("Daily goal · minutes", goal, { goal = it.filter(Char::isDigit); goal.toIntOrNull()?.takeIf { it in 1..1440 }?.let { minutes -> Repository.update { it.copy(goalMinutes = minutes) } } }, true)
        }
        Text("Your profiles", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(add, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = canEdit, shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(10.dp)); Text("Create profile")
        }
        s.profiles.forEach { p -> GlassSurface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DoodleBadge(profileArt(p), Modifier.size(52.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleSmall)
                    Text("${profileDuration(p.minutes)} · ${p.mode.label}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton({ menuFor = p.id }, enabled = canEdit) { Icon(Icons.Outlined.MoreVert, "Profile options for ${p.name}") }
                    DropdownMenu(expanded = menuFor == p.id, onDismissRequest = { menuFor = null }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menuFor = null; edit(p) })
                        if (s.profiles.size > 1) DropdownMenuItem(text = { Text("Delete") }, onClick = { menuFor = null; pendingDelete = p })
                    }
                }
            }
        } }
    }
    pendingDelete?.let { profile ->
        val routineCount = s.schedules.count { it.profileId == profile.id }
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete ${profile.name}?") },
            text = { Text(if (routineCount > 0) "This also removes $routineCount linked ${if (routineCount == 1) "routine" else "routines"}. Active sessions keep their current settings." else "Active sessions keep their current settings. You can create this profile again later.") },
            confirmButton = { TextButton({ remove(profile); pendingDelete = null }) { Text("Delete profile", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("Keep profile") } })
    }
}

@Composable private fun ProfileAvatar(name: String, image: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, image) {
        value = withContext(Dispatchers.IO) { BlockingArtwork.bitmap(ctx, image) }
    }
    Box(modifier.clip(CircleShape).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
        val photo = bitmap
        if (photo != null) Image(photo.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (name.isNotBlank()) Text(name.trim().first().uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        else Icon(Icons.Outlined.Person, null, Modifier.fillMaxSize().padding(5.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable fun SettingsScreen(s: FocusState, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf(Repository.usageGranted()) }
    var accessibility by remember { mutableStateOf(Repository.protectionGranted()) }
    var connected by remember { mutableStateOf(ProtectionRuntime.live()) }
    var batteryExempt by remember { mutableStateOf(false) }
    var admin by remember { mutableStateOf(ManagedProtection.adminEnabled(ctx)) }
    var preciseEnding by remember { mutableStateOf(ManagedProtection.preciseEnding(ctx)) }
    var consent by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }
    var breakNoticesAllowed by remember { mutableStateOf(FocusNotifications.breaksAllowed(ctx)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { breakNoticesAllowed = FocusNotifications.breaksAllowed(ctx); BreakReminders.schedule(ctx) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            exportStatus = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(Repository.export().toByteArray()) } ?: error("Could not open the destination") }.fold({ "Your data was exported." }, { "Export failed. Choose a different destination." }) }
        }
    }
    LaunchedEffect(Unit) { while (true) { usage = Repository.usageGranted(); accessibility = Repository.protectionGranted(); connected = ProtectionRuntime.live(); breakNoticesAllowed = FocusNotifications.breaksAllowed(ctx); batteryExempt = ctx.getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName); admin = ManagedProtection.adminEnabled(ctx); preciseEnding = ManagedProtection.preciseEnding(ctx); delay(1000) } }
    Page {
        DetailsCard("Permissions", if (connected && s.disclosure) "Protection connected" else "Connect blocking when you're ready", Icons.Outlined.Shield) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(if (connected && s.disclosure) Icons.Outlined.VerifiedUser else Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Text(if (connected && s.disclosure) "Blocking is ready" else "Blocking is off", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CatDoodle(CatArt.GUARD, Modifier.size(60.dp))
            }
            if (accessibility && !connected) Text("Service disconnected. Toggle FocuLY off and on in Android Accessibility settings.", style = MaterialTheme.typography.bodyMedium)
            if (ProtectionRuntime.lastError.isNotBlank()) Text(ProtectionRuntime.lastError, color = MaterialTheme.colorScheme.error)
            ActionCard("App & feed protection", if (connected && s.disclosure) "Connected" else "Required for blocking", Icons.Outlined.Shield, connected && s.disclosure) {
                if (s.disclosure) Repository.openAccessibility() else consent = true
            }
            ActionCard("Usage access", if (usage) "Connected" else "Optional · budgets and insights", Icons.Outlined.Timer, usage) { Repository.openUsage() }
            ActionCard("Notifications", if (breakNoticesAllowed) "Allowed" else "Check permission and channels", Icons.Outlined.NotificationsNone, breakNoticesAllowed) {
                if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName))
            }
            ActionCard("Background activity", if (batteryExempt) "Unrestricted" else "Review battery settings", Icons.Outlined.BatteryChargingFull, batteryExempt) { Repository.openBatterySettings() }
            ActionCard("Uninstall resistance", if (admin) "Administrator active" else "Optional Android administration", Icons.Outlined.AdminPanelSettings, admin) {
                if (admin) nav("managed") else ManagedProtection.requestAdmin(ctx)
            }
            if (ManagedProtection.isOwner(ctx)) ActionCard("Precise session endings", if (preciseEnding) "Allowed" else "Required for managed Strict", Icons.Outlined.Alarm, preciseEnding) { ManagedProtection.openPreciseEnding(ctx) }
            LinkRow("App information", "Restricted settings & recovery") { Repository.openAppInfo() }
            if (android.os.Build.MANUFACTURER.equals("OnePlus", true) || android.os.Build.MANUFACTURER.equals("OPPO", true)) Text("OxygenOS: allow background activity in FocuLY's App Info → Battery settings. Menu names vary.", style = MaterialTheme.typography.bodyMedium)
            TextButton({ consent = true }) { Text("Read protection disclosure") }
            if (s.disclosure) TextButton({ if (Repository.update { it.copy(disclosure = false) }) { FocusAccessibilityService.instance?.clearOverlay(); FocusNotifications.session(ctx) } }, enabled = !RulesEngine.strictActive(s, System.currentTimeMillis())) { Text("Withdraw consent") }
        }
        ActionCard("Device protection", "Uninstall rules, app controls & PIN", Icons.Outlined.Lock) { nav("managed") }
        ActionCard("Your blocking screen", "Message, emoji & live preview", Icons.Outlined.EmojiEmotions) { nav("messages") }
        DetailsCard("Reminders", if (s.breakReminders) "Gentle breaks · ${s.breakReminderMinutes}m" else "Gentle breaks are off", Icons.Outlined.NotificationsNone) {
            ToggleRow("Gentle break reminders", "A friendly nudge, even for apps you haven't blocked.", s.breakReminders) { value -> Repository.update { it.copy(breakReminders = value) } }
            if (s.breakReminders) {
                ChoiceRow(listOf("30m", "45m", "60m", "90m"), "${s.breakReminderMinutes}m") { value -> Repository.update { it.copy(breakReminderMinutes = value.removeSuffix("m").toInt()) } }
                Text("Per app, per day · 8am–10pm · at most 3 reminders a day. No new blocks are created.", style = MaterialTheme.typography.bodySmall)
                if (!usage || !breakNoticesAllowed) Text("Enable Usage access and Notifications under Permissions above.", style = MaterialTheme.typography.bodySmall)
            }
            ToggleRow("Completion & routine reminders", "", s.reminders) { value -> Repository.update { it.copy(reminders = value) }; FocusNotifications.plan(ctx) }
        }
        SoftCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Appearance", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); DoodleBadge(CatArt.TUNE, Modifier.size(68.dp)) }
            ToggleRow("Reduce motion", "", s.reduceMotion) { value -> Repository.update { it.copy(reduceMotion = value) } }
            ToggleRow("Haptic feedback", "", s.haptics) { value -> Repository.update { it.copy(haptics = value) } }
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
            LinkRow("Privacy policy", "How FocuLY handles your data", Icons.Outlined.PrivacyTip) { uriHandler.openUri("https://focus.venusapp.in/privacy") }
            LinkRow("Export my data", "Rules, blocks & history", Icons.Outlined.Download) { export.launch("focus-data-${LocalDate.now()}.json") }
            if (exportStatus.isNotEmpty()) Text(exportStatus, style = MaterialTheme.typography.bodyMedium)
            LinkRow("Delete my data", "Available outside Strict", Icons.Outlined.Delete) { delete = true }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Made with 💜 by ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Veil", Modifier.heightIn(min = 48.dp).clickable { uriHandler.openUri("https://venusapp.in") }
                .wrapContentHeight(Alignment.CenterVertically).semantics { contentDescription = "Veil website" },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
        }
    }
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("How protection works", fontFamily = Mono) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("FocuLY uses Android Accessibility access to observe which app is open, check selected feed controls and supported browser address fields, and display a blocking screen for rules you choose.")
        Text("This access can expose on-screen text. FocuLY checks it only in memory on your device. It does not store screen content, send it anywhere, or use it for advertising.")
        Text("Access is optional. You can withdraw consent inside FocuLY when Strict is inactive, or turn accessibility protection off in Android settings at any time. Without access, focus timers still work. Managed whole-app suspension is controlled separately by Android device-owner policies.")
    } }, confirmButton = { TextButton({ Repository.update { it.copy(disclosure = true) }; consent = false; Repository.openAccessibility() }) { Text("Agree & open settings") } }, dismissButton = { TextButton({ consent = false }) { Text("Not now") } })
    if (delete) AlertDialog(onDismissRequest = { delete = false }, title = { Text("Clear your space?", fontFamily = Mono) }, text = { Text("This deletes FocuLY’s rules, profiles, sessions, and override history and ends any active session. It does not delete Android’s system usage history. This cannot be undone.") }, confirmButton = { TextButton({ if (Repository.deleteAll()) { delete = false; nav("home") } }) { Text("Delete local data", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ delete = false }) { Text("Keep data") } })
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
            GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Caption("FOCUS TODAY"); Text(duration(focused), style = MaterialTheme.typography.headlineMedium); Text("Logged sessions", style = MaterialTheme.typography.bodySmall) } }
            GlassSurface(Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Caption("COMPLETED"); Text(records.count { it.completed && it.end in dayStart until dayEnd }.toString(), style = MaterialTheme.typography.headlineMedium); Text("FocuLY sessions", style = MaterialTheme.typography.bodySmall) } }
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
            Text(if (owner) "FocuLY removal is automatically blocked while a ban, focus rule, or app policy is active." else "Your FocuLY PIN protects in-app controls. Android still permits deactivation and removal.", style = MaterialTheme.typography.bodyMedium)
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
