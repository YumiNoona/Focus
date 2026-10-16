package dev.still.focus

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun ProtectionConsent(close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("How protection works", fontFamily = Mono) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("FocuLY uses Android Accessibility to identify the open app, check supported browser address fields and selected feed controls, and show your blocking screen.")
            Text("On-screen text is checked in memory, on this device. FocuLY never records or uploads screen content. Access is optional; timers work without it.")
            Text("You can turn access off in Android settings at any time. Managed app policies are controlled separately by Android device-owner administration.")
        }
    }, confirmButton = { TextButton({ if (Repository.update { it.copy(disclosure = true) }) { close(); Repository.openAccessibility() } }) { Text("Agree & open settings") } }, dismissButton = { TextButton(close) { Text("Not now") } })
}

@Composable fun SetupChecklist(onboarding: Boolean = false, doneLabel: String = "Done", done: () -> Unit) {
    val ctx = LocalContext.current
    var pulse by remember { mutableIntStateOf(0) }
    var consent by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf(false) }
    val notification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { pulse++ }
    LaunchedEffect(Unit) { while (true) { delay(1000); pulse++ } }
    val ready = pulse.let { Repository.protectionReady() }
    val usage = Repository.usageGranted()
    val admin = ManagedProtection.adminEnabled(ctx)
    val notifications = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    if (onboarding) Page {
        Heading("Make space for focus.")
        GlassSurface(shape = RoundedCornerShape(28.dp), color = FoculyBrand.lilac, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ready) "You're ready." else "A quieter screen.", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth())
                CatDoodle(CatArt.GUARD, Modifier.fillMaxWidth().height(210.dp))
                Text(if (ready) "Protection connected." else "Connect to pause apps and sites.",
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            }
        }
        ActionCard("Connect protection", if (ready) "Connected" else "Apps & websites", Icons.Outlined.Shield, ready) {
            if (Repository.state.disclosure) Repository.openAccessibility() else consent = true
        }
        DetailsCard("More options", "", Icons.Outlined.Tune) {
            ActionCard("Usage access", if (usage) "Connected" else "Daily limits", Icons.Outlined.Timer, usage) { Repository.openUsage() }
            ActionCard("Notifications", if (notifications) "Allowed" else "Session updates", Icons.Outlined.NotificationsNone, notifications) {
                if (Build.VERSION.SDK_INT >= 33) notification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            ActionCard("Background activity", "Battery settings", Icons.Outlined.BatteryChargingFull) { Repository.openBatterySettings() }
            ActionCard("Uninstall resistance", if (admin) "Active" else "Optional", Icons.Outlined.AdminPanelSettings, admin) {
                if (!admin) ManagedProtection.requestAdmin(ctx)
            }
            ActionCard("Admin PIN", if (AdminPin.configured()) "Set" else "Optional", Icons.Outlined.Password, AdminPin.configured()) { pin = true }
        }
    } else Page {
        Caption("YOUR BOUNDARY, CONNECTED")
        Heading("Ready when you are.", "Grant each permission in Android, then return here.")
        SoftCard {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (ready) "Blocking is ready" else "Connect protection first", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                CatDoodle(CatArt.GUARD, Modifier.size(80.dp))
            }
            Caption("${listOf(ready, usage, admin, AdminPin.configured(), notifications).count { it }} / 5 CONNECTED · OPTIONAL ITEMS CAN WAIT")
            LinearProgressIndicator(progress = { listOf(ready, usage, admin, AdminPin.configured(), notifications).count { it } / 5f }, modifier = Modifier.fillMaxWidth())
        }
        ActionCard("App & feed protection", if (ready) "Connected" else "Required for blocking", Icons.Outlined.Shield, ready) { if (Repository.state.disclosure) Repository.openAccessibility() else consent = true }
        ActionCard("Usage access", if (usage) "Connected" else "Optional · limits & app detection fallback", Icons.Outlined.Timer, usage) { Repository.openUsage() }
        ActionCard("Uninstall resistance", if (ManagedProtection.uninstallBlocked(ctx)) "Android removal policy verified" else if (admin) "Active · Android can still deactivate it" else "Optional · adds an Android deactivation step", Icons.Outlined.AdminPanelSettings, admin) { if (!admin) ManagedProtection.requestAdmin(ctx) }
        ActionCard("FocuLY admin PIN", if (AdminPin.configured()) "Set · protects FocuLY device controls" else "Optional · 6–12 digits", Icons.Outlined.Password, AdminPin.configured()) { pin = true }
        ActionCard("Notifications", if (notifications) "Allowed" else "Optional · session & completion updates", Icons.Outlined.NotificationsNone, notifications) { if (Build.VERSION.SDK_INT >= 33) notification.launch(Manifest.permission.POST_NOTIFICATIONS) }
        ActionCard("Background activity", "Check your phone's battery restrictions", Icons.Outlined.BatteryChargingFull) { Repository.openBatterySettings() }
        DetailsCard("What Strict can protect", "Regular phone vs managed device", Icons.Outlined.Info) {
            Text("Strict has no early exit inside FocuLY. On a regular phone, Android still allows disabling protection or removing this app after deactivating its administrator. Your FocuLY PIN cannot protect Android settings.")
            Text("Device-owner setup on a dedicated managed device adds OS app suspension and uninstall blocking. FocuLY cannot grant itself this role on your personal phone.")
        }
        Primary(doneLabel, icon = Icons.Outlined.Check) { done() }
    }
    if (consent) ProtectionConsent { consent = false }
    if (pin) PinDialog { pin = false; pulse++ }
}

/** Passwords never enter saveable state, exports, screenshots, or logs. */
@Composable fun PinDialog(close: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val verify = AdminPin.configured() && !AdminPin.authorized()
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text(if (verify) "Unlock device controls" else "Set your FocuLY PIN", fontFamily = Mono) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (verify) "Changes unlock for 90 seconds. Strict still cannot end early." else "6–12 digits. This protects controls inside FocuLY, not Android's administrator or uninstall screens.")
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12); error = "" }, label = { Text("FocuLY PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), enabled = !busy)
            if (!verify) OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(12) }, label = { Text("Confirm PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), enabled = !busy)
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton({
        if (!verify && (pin.length !in 6..12 || pin != confirm)) error = "Enter 6–12 matching digits."
        else { busy = true; scope.launch { val ok = withContext(Dispatchers.Default) { if (verify) AdminPin.verify(pin) else AdminPin.set(pin) }; busy = false; pin = ""; confirm = ""; if (ok) close() else error = if (AdminPin.remainingSeconds() > 0) "Try again in ${AdminPin.remainingSeconds()} seconds." else if (RulesEngine.strictActive(Repository.state, System.currentTimeMillis())) "PIN changes wait until Strict ends." else "PIN was not accepted." } }
    }, enabled = !busy && pin.length >= 6) { Text(if (verify) "Unlock" else "Save PIN") } }, dismissButton = { TextButton({ if (!busy) close() }) { Text("Cancel") } })
}

@Composable fun DeviceControlsGate(content: @Composable () -> Unit) {
    var pulse by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1000); pulse++ } }
    if (pulse.let { AdminPin.authorized() }) content() else {
        Page {
            Heading("Keep your boundary.")
            SoftCard { Icon(Icons.Outlined.Password, null, tint = MaterialTheme.colorScheme.primary); Text("Device controls are locked", style = MaterialTheme.typography.titleMedium); Text("Enter your FocuLY PIN to manage Android administration and app policies.") }
            Primary("Enter FocuLY PIN", icon = Icons.Outlined.LockOpen) { dialog = true }
        }
    }
    if (dialog) PinDialog { dialog = false; pulse++ }
}

@Composable fun PinScreen(s: FocusState) {
    val ctx = LocalContext.current
    var pulse by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val recovery = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { status = if (AdminPin.recoverAfterDeviceCredential()) "PIN reset. Choose a new PIN." else "Recovery waits until Strict ends."; pulse++ }
    }
    LaunchedEffect(Unit) { while (true) { delay(1000); pulse++ } }
    val configured = pulse.let { AdminPin.configured() }
    val strict = RulesEngine.strictActive(s, System.currentTimeMillis())
    Page {
        Heading("A little more resistance.")
        SoftCard { Icon(Icons.Outlined.Password, null, tint = MaterialTheme.colorScheme.primary); Text(if (configured) "FocuLY PIN is set" else "Your controls, protected", style = MaterialTheme.typography.titleMedium); Text("Guards device protection, per-app policies, consent withdrawal, and local data deletion. Rules in Strict stay locked even with your PIN.") }
        Primary(if (configured && !AdminPin.authorized()) "Unlock with PIN" else if (configured) "Change PIN" else "Set FocuLY PIN", enabled = !strict, icon = Icons.Outlined.Password) { dialog = true }
        if (configured && AdminPin.authorized()) TextButton({ if (AdminPin.remove()) pulse++ }, enabled = !strict) { Text("Remove FocuLY PIN") }
        if (configured) DetailsCard("Forgot your PIN?", "Verify Android's screen lock", Icons.Outlined.LockReset) {
            Text("Reset requires your phone's screen-lock credential and is unavailable during Strict. If no screen lock is set, configure one in Android first.")
            TextButton({
                @Suppress("DEPRECATION") val intent = ctx.getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("Recover FocuLY PIN", "Confirm your phone's screen lock to reset the FocuLY PIN.")
                if (intent != null) recovery.launch(intent) else status = "Set an Android screen lock before using PIN recovery."
            }, enabled = !strict) { Text("Verify phone screen lock") }
        }
        if (status.isNotBlank()) Text(status)
        Caption("THIS PIN DOES NOT LOCK ANDROID SETTINGS OR PREVENT UNINSTALLING FOCUS ON A REGULAR PHONE.")
    }
    if (dialog) PinDialog { dialog = false; pulse++ }
}

@Composable fun MessageScreen(s: FocusState) {
    var previewMode by rememberSaveable { mutableStateOf(FocusMode.STANDARD) }
    var title by rememberSaveable { mutableStateOf(s.blockMessage.title) }
    var body by rememberSaveable { mutableStateOf(s.blockMessage.body) }
    var emoji by rememberSaveable { mutableStateOf(s.blockMessage.emoji) }
    var status by remember { mutableStateOf("") }
    val locked = RulesEngine.strictActive(s, System.currentTimeMillis())
    Page {
        Heading("Your pause, your words.")
        ChoiceRow(FocusMode.entries.map { it.label }, previewMode.label) { label -> previewMode = FocusMode.entries.first { it.label == label } }
        SoftCard {
            Caption("BLOCKING SCREEN PREVIEW")
            DoodleBadge(previewMode.catArt(), Modifier.fillMaxWidth().height(112.dp))
            Text(emoji, style = MaterialTheme.typography.displaySmall)
            Text(title.ifBlank { "See you later." }, style = MaterialTheme.typography.headlineMedium)
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyLarge)
            Caption("APP NAME · ${previewMode.label.uppercase()} · UNLOCK TIME")
            Button({}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Return to focus") }
        }
        Caption("QUICK PRESETS")
        ChoiceRow(listOf("See you later", "Don't open", "One thing at a time"), when (title) { "Don't open this yet." -> "Don't open"; "One thing at a time." -> "One thing at a time"; else -> "See you later" }) { preset ->
            if (!locked) { title = when (preset) { "Don't open" -> "Don't open this yet."; "One thing at a time" -> "One thing at a time."; else -> "See you later." }; body = when (preset) { "Don't open" -> "Your future self will thank you."; "One thing at a time" -> "Stay with what you chose to do."; else -> "Make room for what matters." } }
        }
        ChoiceRow(listOf("🌿", "✨", "🧘", "🔒", "👋", "No emoji"), emoji.ifBlank { "No emoji" }) { if (!locked) emoji = if (it == "No emoji") "" else it }
        Input("Headline", title, { title = it.take(80) }, supporting = "${title.length}/80")
        Input("Supporting message", body, { body = it.take(180) }, supporting = "${body.length}/180 · optional")
        Input("Emoji or symbol", emoji, { emoji = it.take(16) })
        Primary("Save blocking screen", enabled = !locked && title.isNotBlank(), icon = Icons.Outlined.Check) { if (Repository.update { it.copy(blockMessage = BlockMessage(title.trim(), body.trim(), emoji.trim())) }) { ManagedProtection.sync(); status = "Saved. Your next blocking screen uses these words." } }
        if (status.isNotBlank()) Text(status)
        if (locked) Caption("MESSAGE CHANGES WAIT UNTIL STRICT ENDS.")
    }
}

@Composable fun LifecycleScreen(s: FocusState) {
    val ctx = LocalContext.current
    var pkg by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var quarantine by rememberSaveable { mutableStateOf(true) }
    var uninstall by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf(BlockPeriod.MANUAL) }
    var status by remember { mutableStateOf("") }
    val owner = ManagedProtection.isOwner(ctx)
    val locked = RulesEngine.strictActive(s, System.currentTimeMillis())
    fun choose(packageName: String, name: String) { pkg = packageName; label = name }
    Page {
        Heading("App boundaries.", "Keep a distracting app paused, or keep a useful app installed.")
        SoftCard { Icon(Icons.Outlined.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary); Text(if (owner) "Managed device connected" else "Managed setup required", style = MaterialTheme.typography.titleMedium); Text(if (owner) "Android enforces these controls. Installed app status is verified below." else "You can prepare rules here. They remain inactive on a regular phone until FocuLY is a device-owner administrator.", style = MaterialTheme.typography.bodyMedium) }
        s.lifecycleRules.forEach { rule ->
            SoftCard {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { AppBadge(rule.packageName); Column(Modifier.weight(1f)) { Text(rule.label, style = MaterialTheme.typography.titleSmall); Caption(if (!owner) "PREPARED · NOT ENFORCED" else if (!ManagedProtection.installed(ctx, rule.packageName)) "WAITING FOR INSTALLATION" else "INSTALLED") } }
                if (rule.quarantineInstall) Caption(if (ManagedProtection.appSuspended(rule.packageName)) "✓ LAUNCH PAUSED BY ANDROID" else if (owner && ManagedProtection.installed(ctx, rule.packageName)) "LAUNCH POLICY NOT VERIFIED" else "PAUSE LAUNCH AFTER INSTALLATION")
                if (rule.lockUninstall) Caption(if (ManagedProtection.appUninstallBlocked(rule.packageName)) "✓ UNINSTALL BLOCKED BY ANDROID" else "KEEP INSTALLED · PENDING")
                Caption(if (rule.end == 0L) "UNTIL I REMOVE THIS RULE" else "ENDS ${java.time.Instant.ofEpochMilli(rule.end).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm"))}")
                Row { TextButton({ choose(rule.packageName, rule.label); quarantine = rule.quarantineInstall; uninstall = rule.lockUninstall }, enabled = !locked) { Text("Edit") }; Spacer(Modifier.weight(1f)); IconButton({ if (Repository.update { it.copy(lifecycleRules = it.lifecycleRules.filterNot { item -> item.packageName == rule.packageName }) }) { ManagedProtection.sync(); FocusNotifications.plan(ctx) } }, enabled = !locked) { Icon(Icons.Outlined.Delete, "Remove ${rule.label} policy") } }
            }
        }
        Caption("CHOOSE AN APP · INSTALLED OR NOT")
        ChoiceRow(listOf("Instagram", "YouTube", "TikTok", "Reddit"), label) { name -> choose(mapOf("Instagram" to "com.instagram.android", "YouTube" to "com.google.android.youtube", "TikTok" to "com.zhiliaoapp.musically", "Reddit" to "com.reddit.frontpage").getValue(name), name) }
        s.apps.forEach { app -> ActionCard(app.label, "Use installed app", Icons.Outlined.Apps, pkg == app.packageName) { choose(app.packageName, app.label) } }
        Input("App name", label, { label = it.take(60) })
        Input("Package ID", pkg, { pkg = it.trim().take(160) }, supporting = "Exact Android ID, e.g. com.instagram.android")
        SoftCard {
            ToggleRow("Pause after installation", "Android suspends this app after it installs. It can still be downloaded; this is not a pre-install store filter.", quarantine, !locked) { quarantine = it }
            ToggleRow("Keep installed", "Android blocks removing this individual app.", uninstall, !locked) { uninstall = it }
        }
        Caption("HOW LONG?")
        ChoiceRow(listOf("1 day", "1 week", "Until I change it"), when (period) { BlockPeriod.DAY -> "1 day"; BlockPeriod.WEEK -> "1 week"; else -> "Until I change it" }) { period = when (it) { "1 day" -> BlockPeriod.DAY; "1 week" -> BlockPeriod.WEEK; else -> BlockPeriod.MANUAL } }
        Primary(if (owner) "Apply app controls" else "Save prepared rule", enabled = !locked && pkg.isNotBlank() && label.isNotBlank() && (quarantine || uninstall), icon = Icons.Outlined.Lock) {
            if (!pkg.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+"))) status = "Enter a valid Android package ID."
            else if (FocusAccessibilityService.essential(ctx, pkg) || pkg in s.allowlist) status = "Essential and always-allowed apps cannot be managed here."
            else if (s.lifecycleRules.size >= 100 && s.lifecycleRules.none { it.packageName == pkg }) status = "Remove an existing rule before adding another."
            else {
                val rule = AppLifecycleRule(pkg, label.trim(), uninstall, quarantine, period.deadline(System.currentTimeMillis()))
                if (Repository.update { it.copy(lifecycleRules = it.lifecycleRules.filterNot { item -> item.packageName == pkg } + rule) }) { val applied = ManagedProtection.sync(); FocusNotifications.plan(ctx); status = if (!owner) "Prepared. Not enforced until managed setup is active." else if (applied) "App controls applied." else "Android could not apply this policy. Check protection health." }
            }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)
        DetailsCard("Installation coverage", "Play Store & APK installers", Icons.Outlined.Info) { Text("The package-added receiver applies the launch pause after installation from any installer. Android may show its own administrator screen; FocuLY cannot replace every store or uninstall dialog. Selective prevention before download requires an enterprise managed Play policy.") }
    }
}
