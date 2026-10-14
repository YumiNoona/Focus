package dev.still.focus

import android.os.SystemClock
import androidx.compose.runtime.*

object ProtectionRuntime {
    var connected by mutableStateOf(false); private set
    var heartbeat by mutableLongStateOf(0); private set
    var lastError by mutableStateOf(""); private set
    var observedPackage by mutableStateOf(""); private set
    var blockedPackage by mutableStateOf(""); private set
    fun connect() { connected = true; lastError = ""; tick() }
    fun disconnect() { connected = false; blockedPackage = "" }
    fun tick() { heartbeat = SystemClock.elapsedRealtime() }
    fun observed(pkg: String) { observedPackage = pkg }
    fun blocked(pkg: String) { blockedPackage = pkg; lastError = "" }
    fun cleared() { blockedPackage = "" }
    fun error(message: String) { lastError = message; blockedPackage = "" }
    fun live() = connected && SystemClock.elapsedRealtime() - heartbeat < 5000 && lastError.isEmpty()
}
