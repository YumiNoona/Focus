package dev.still.focus

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TargetApp(val pkg: String, val label: String)

/** One picker shared by instant blocks and profile/session scopes. No nested app menus. */
@Composable fun selectableApps(s: FocusState): List<TargetApp>? {
    var installed by remember { mutableStateOf<List<TargetApp>?>(null) }
    LaunchedEffect(PackageStatus.revision) {
        installed = withContext(Dispatchers.IO) {
            val ctx = Repository.context
            ctx.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { TargetApp(it.activityInfo.packageName, it.loadLabel(ctx.packageManager).toString()) }
                .distinctBy { it.pkg }
                .filterNot { FocusAccessibilityService.essential(ctx, it.pkg) }
        }
    }
    return installed?.let { apps ->
        (apps + s.apps.map { TargetApp(it.packageName, it.label) }).distinctBy { it.pkg }
            .filterNot { it.pkg in s.allowlist || FocusAccessibilityService.essential(Repository.context, it.pkg) }
            .sortedBy { it.label.lowercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppSelectionSheet(
    apps: List<TargetApp>?, selected: Set<String>, change: (Set<String>) -> Unit,
    close: () -> Unit, allSelected: Boolean? = null, changeAll: ((Boolean) -> Unit)? = null
) {
    var query by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = close, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).imePadding().padding(horizontal = 22.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose apps", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                DoodleBadge(CatArt.CHOOSE, Modifier.size(68.dp))
            }
            if (allSelected != null && changeAll != null) ToggleRow("All selected apps", "Use your saved app list", allSelected, onChange = changeAll)
            Input("Search apps", query, { query = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ change(apps.orEmpty().map { it.pkg }.toSet()) }, enabled = apps != null) { Text("Select all") }
                TextButton({ change(emptySet()) }) { Text("Clear") }
            }
            if (apps == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            val visible = apps.orEmpty().filter { it.label.contains(query, true) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (apps != null && visible.isEmpty()) item { Text("No matching apps.", style = MaterialTheme.typography.bodyMedium) }
                items(visible, key = { it.pkg }) { app ->
                    val checked = app.pkg in selected
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).toggleable(checked, role = Role.Checkbox) {
                        change(if (checked) selected - app.pkg else selected + app.pkg)
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppBadge(app.pkg)
                        Text(app.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked, null)
                    }
                }
            }
            Primary("Done · ${selected.size} selected", icon = null, action = close)
        }
    }
}

val supportedFeeds = listOf("YouTube Shorts", "Instagram Reels", "Facebook Reels")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun WebsiteSelectionSheet(domains: Set<String>, selected: Set<String>, allSelected: Boolean,
    change: (Set<String>) -> Unit, changeAll: (Boolean) -> Unit, add: (String) -> Boolean, close: () -> Unit) {
    var domain by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    ModalBottomSheet(onDismissRequest = close, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .94f), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 580.dp).imePadding().padding(horizontal = 22.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose websites", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                DoodleBadge(CatArt.CHOOSE, Modifier.size(68.dp))
            }
            ToggleRow("All saved websites", "Use your saved domain list", allSelected, onChange = changeAll)
            Input("Add website", domain, { domain = it; error = false }, supporting = if (error) "Use a domain like reddit.com" else null)
            OutlinedButton({
                val host = RulesEngine.normalizeDomain(domain)
                if (host == null) error = true else if (add(host)) { domain = ""; keyboard?.hide(); focus.clearFocus() }
            }, Modifier.fillMaxWidth(), enabled = domain.isNotBlank()) { Text("Add") }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (domains.isEmpty()) item { Text("Add a domain above.", style = MaterialTheme.typography.bodyMedium) }
                items(domains.sorted(), key = { it }) { host ->
                    val checked = host in selected
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(checked, role = Role.Checkbox) { change(if (checked) selected - host else selected + host) },
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(host, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Checkbox(checked, null)
                    }
                }
            }
            Primary("Done · ${selected.size} selected", icon = null, action = close)
        }
    }
}

@Composable fun TargetSummary(title: String, labels: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) {
    ActionCard(title, if (labels.isEmpty()) "None selected" else labels.take(2).joinToString(" · ") + if (labels.size > 2) " +${labels.size - 2}" else "", icon, action = action)
}
