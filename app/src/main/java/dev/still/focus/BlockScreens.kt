package dev.still.focus

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

fun blockIcon(kind: BlockKind): ImageVector = when (kind) {
    BlockKind.APPS -> Icons.Outlined.Apps
    BlockKind.WEBSITES -> Icons.Outlined.Public
    BlockKind.FEEDS -> Icons.Outlined.PlayCircleOutline
}
fun blockDeadline(plan: BlockPlan, now: Long): String {
    if (plan.end == 0L) return "Until I unblock"
    val zone = ZoneId.systemDefault()
    val at = Instant.ofEpochMilli(plan.end).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = when (at.toLocalDate()) { today -> "Today"; today.plusDays(1) -> "Tomorrow"; else -> at.format(DateTimeFormatter.ofPattern("EEE, d MMM")) }
    return "$date · ${at.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

@Composable fun AppBadge(packageName: String) {
    val ctx = LocalContext.current
    val bitmap = remember(packageName, PackageStatus.revision) { runCatching {
        val drawable = ctx.packageManager.getApplicationIcon(packageName)
        Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888).also { drawable.setBounds(0, 0, 72, 72); drawable.draw(android.graphics.Canvas(it)) }.asImageBitmap()
    }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
    else Icon(Icons.Outlined.Apps, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
}

@Composable fun DetailsCard(title: String, summary: String, icon: ImageVector = Icons.Outlined.Info, initiallyOpen: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    SoftCard {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(title, style = MaterialTheme.typography.titleSmall); if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Collapse $title" else "Expand $title")
        }
        if (expanded) content()
    }
}

@Composable fun SelectionCard(title: String, subtitle: String, icon: ImageVector, selected: Boolean, enabled: Boolean = true, tile: Boolean = false, onSelect: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(18.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
        if (tile) Column(Modifier.heightIn(min = 124.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary); if (selected) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) else Spacer(Modifier.size(20.dp)) }
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Row(Modifier.padding(18.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable fun ActionCard(title: String, subtitle: String, icon: ImageVector, confirmed: Boolean = false, action: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(role = Role.Button, onClick = action), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(18.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(if (confirmed) Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable fun ModeCards(mode: FocusMode, manual: Boolean = false, select: (FocusMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FocusMode.entries.forEach { option ->
            GlassSurface(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).selectable(mode == option, enabled = !manual || option != FocusMode.STRICT, role = Role.RadioButton, onClick = { select(option) }),
                color = if (mode == option) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(when (option) { FocusMode.GENTLE -> Icons.Outlined.NotificationsNone; FocusMode.STANDARD -> Icons.Outlined.Shield; FocusMode.STRICT -> Icons.Outlined.Lock }, null, tint = if (manual && option == FocusMode.STRICT) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f) else MaterialTheme.colorScheme.primary)
                    Text(option.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable fun ActiveBlocks(s: FocusState, now: Long) {
    val ctx = LocalContext.current
    val plans = s.blocks.filter { it.alive(now) }
    var unblocking by remember { mutableStateOf<BlockPlan?>(null) }
    if (plans.isNotEmpty()) {
        Text("Active blocks", style = MaterialTheme.typography.titleMedium)
        plans.forEach { plan -> SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (plan.kind == BlockKind.APPS && plan.targets.size == 1) AppBadge(plan.targets.first()) else Icon(blockIcon(plan.kind), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("${plan.targets.size} ${if (plan.targets.size == 1) plan.kind.label.lowercase().removeSuffix("s") else plan.kind.label.lowercase()}", style = MaterialTheme.typography.titleMedium)
                    Text(blockDeadline(plan, now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (plan.mode == FocusMode.STRICT) Icon(Icons.Outlined.Lock, "Strict block", tint = MaterialTheme.colorScheme.primary)
            }
            Text(plan.targets.joinToString(" · ") { target -> if (plan.kind == BlockKind.APPS) s.apps.find { it.packageName == target }?.label ?: runCatching { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(target, 0)).toString() }.getOrDefault(target) else target }, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Caption(plan.mode.label.uppercase())
                Spacer(Modifier.weight(1f))
                if (plan.mode == FocusMode.STRICT) Caption("AUTO UNBLOCK")
                else TextButton({ unblocking = plan }, enabled = !RulesEngine.strictActive(s, now)) { Icon(Icons.Outlined.LockOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Unblock") }
            }
        } }
        if (!Repository.protectionReady() && !(s.managedLockdown && ManagedProtection.verifiedSuspendedCount() > 0)) DoodleCard(CatArt.REMINDER, "Protection is interrupted.", "Reconnect in Settings to resume blocking.", FoculyBrand.peach)
    }
    unblocking?.let { plan ->
        var reason by remember(plan.id) { mutableStateOf("") }
        var remaining by remember(plan.id) { mutableIntStateOf(plan.mode.waitSeconds) }
        LaunchedEffect(plan.id) {
            Repository.beginUnblock(plan.id)
            val end = android.os.SystemClock.elapsedRealtime() + plan.mode.waitSeconds * 1000
            while (remaining > 0) { delay(250); remaining = ((end - android.os.SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceAtLeast(0) }
        }
        AlertDialog(onDismissRequest = { unblocking = null }, title = { Text("Unblock ${plan.kind.label.lowercase()}?", fontFamily = Mono) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DoodleBadge(plan.mode.catArt(), Modifier.fillMaxWidth().height(88.dp))
            Text(if (plan.mode == FocusMode.STANDARD) "Take a moment before removing this block." else "This block will end now.")
            if (plan.mode == FocusMode.STANDARD) Input("Your reason", reason, { reason = it.take(120) })
            if (remaining > 0) Caption("READY IN ${remaining}s")
        } }, confirmButton = { TextButton({ if (Repository.unblock(plan.id, reason)) unblocking = null }, enabled = remaining == 0 && (plan.mode != FocusMode.STANDARD || reason.isNotBlank())) { Text("Unblock") } }, dismissButton = { TextButton({ unblocking = null }) { Text("Keep block") } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun BlockEditor(s: FocusState, kind: BlockKind, initialTargets: Set<String>?, done: () -> Unit, settings: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val apps = selectableApps(s)
    val labels = when (kind) {
        BlockKind.APPS -> s.apps.associate { it.packageName to it.label } + apps.orEmpty().associate { it.pkg to it.label }
        BlockKind.WEBSITES -> s.domains.associateWith { it }
        BlockKind.FEEDS -> supportedFeeds.associateWith { it }
    }
    val defaults = when (kind) {
        BlockKind.APPS -> s.apps.filter { it.enabled && it.packageName !in s.allowlist }.map { it.packageName }.toSet()
        BlockKind.WEBSITES -> s.domains
        BlockKind.FEEDS -> s.feeds
    }
    var targets by rememberSaveable(kind, initialTargets) { mutableStateOf((initialTargets ?: defaults).toSet()) }
    var choosingApps by rememberSaveable { mutableStateOf(kind == BlockKind.APPS && targets.isEmpty()) }
    var domain by rememberSaveable { mutableStateOf("") }
    var domainError by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf(BlockPeriod.DAY) }
    var mode by rememberSaveable { mutableStateOf(FocusMode.STANDARD) }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    LaunchedEffect(period, mode) { acknowledged = false; if (period == BlockPeriod.MANUAL && mode == FocusMode.STRICT) mode = FocusMode.STANDARD }
    val valid = targets.isNotEmpty() && (period != BlockPeriod.TIMER || minutes.toIntOrNull()?.let { it in 1..1440 } == true)
    val preview = BlockPlan(kind = kind, targets = targets, mode = mode, start = now, end = period.deadline(now, minutes.toIntOrNull() ?: 60), period = period)
    val ready = Repository.protectionReady()
    Column(Modifier.fillMaxSize().imePadding()) {
        Box(Modifier.weight(1f)) {
            Page {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Block ${kind.label.lowercase()}.", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    DoodleBadge(if (kind == BlockKind.FEEDS) CatArt.FEEDS else CatArt.CHOOSE, Modifier.size(80.dp))
                }
                when (kind) {
                    BlockKind.APPS -> TargetSummary("Apps", targets.map { labels[it] ?: it }, Icons.Outlined.Apps) { choosingApps = true }
                    BlockKind.WEBSITES -> {
                        Input("Website or domain", domain, { domain = it; domainError = false }, supporting = if (domainError) "Use a domain like reddit.com" else null)
                        OutlinedButton({
                            val host = RulesEngine.normalizeDomain(domain)
                            if (host == null) domainError = true else { targets = targets + host; domain = ""; keyboard?.hide(); focus.clearFocus() }
                        }, Modifier.fillMaxWidth(), enabled = domain.isNotBlank()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add website") }
                        if (targets.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            targets.sorted().forEach { host -> InputChip(true, { targets = targets - host }, label = { Text(host) },
                                trailingIcon = { Icon(Icons.Outlined.Close, "Remove $host", Modifier.size(16.dp)) }) }
                        }
                    }
                    BlockKind.FEEDS -> supportedFeeds.forEach { feed ->
                        ToggleRow(feed, "", feed in targets) { checked -> targets = if (checked) targets + feed else targets - feed }
                    }
                }
                Caption("HOW LONG")
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlockPeriod.entries.forEach { option -> FilterChip(period == option, { period = option }, label = { Text(option.label) }) }
                }
                if (period == BlockPeriod.TIMER) {
                    ChoiceRow(listOf("15", "30", "60", "120"), minutes) { minutes = it }
                    Input("Minutes", minutes, { minutes = it.filter(Char::isDigit).take(4) }, numeric = true, supporting = "1–1440 minutes")
                }
                Caption("RESISTANCE")
                ModeCards(mode, period == BlockPeriod.MANUAL) { mode = it }
                Text(when (mode) { FocusMode.GENTLE -> "Gentle reminder · temporary visits"; FocusMode.STANDARD -> "30-second pause · reason required"; FocusMode.STRICT -> "Locked until expiry · no early unblock" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (period == BlockPeriod.MANUAL) Caption("STRICT NEEDS AN AUTOMATIC ENDING")
                if (mode == FocusMode.STRICT) {
                    ToggleRow("Lock until expiry", "No early unblock or rule changes", acknowledged) { acknowledged = it }
                    Text(if (ManagedProtection.isOwner()) "Android blocks removing FocuLY during Strict." else "Android can still disable or remove FocuLY on this phone.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (kind != BlockKind.APPS) DetailsCard("Detection coverage", "Experimental · whole-app fallback available", Icons.Outlined.Info) {
                    Text("Detection depends on the app's exposed controls. Choose the whole app for a stronger boundary.")
                }
            }
        }
        GlassSurface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${targets.size} selected · ${mode.label}", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(blockDeadline(preview, now), Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val chooseFirst = kind == BlockKind.APPS && targets.isEmpty()
                Primary(if (chooseFirst) "Choose apps" else if (!ready) "Connect protection" else "Block now",
                    (chooseFirst || valid && (mode != FocusMode.STRICT || acknowledged)) && !RulesEngine.strictActive(s, now), Icons.Outlined.Lock) {
                    if (chooseFirst) choosingApps = true
                    else if (!Repository.protectionReady()) settings()
                    else {
                        val saved = Repository.update { state -> when (kind) {
                            BlockKind.APPS -> state.copy(apps = state.apps.map { if (it.packageName in targets) it.copy(enabled = true) else it } +
                                targets.filterNot { pkg -> state.apps.any { it.packageName == pkg } }.map { AppRule(it, labels[it] ?: it) })
                            BlockKind.WEBSITES -> state.copy(domains = state.domains + targets)
                            BlockKind.FEEDS -> state.copy(feeds = state.feeds + targets)
                        } }
                        if (saved && Repository.block(kind, targets, period, mode, minutes.toIntOrNull() ?: 60)) done()
                    }
                }
            }
        }
    }
    if (choosingApps) AppSelectionSheet(apps, targets, { targets = it }, { choosingApps = false })
}
