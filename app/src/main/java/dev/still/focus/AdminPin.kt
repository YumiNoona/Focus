package dev.still.focus

import android.os.SystemClock
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Protects FocuLY controls only. Android administrator deactivation remains a system action. */
object AdminPin {
    private fun prefs() = Repository.context.getSharedPreferences("focus-credential", 0)
    private var unlockedUntil = 0L
    fun configured() = prefs().contains("hash")
    fun authorized() = !configured() || SystemClock.elapsedRealtime() < unlockedUntil
    fun lock() { unlockedUntil = 0L }
    fun remainingSeconds(): Long = ((prefs().getLong("locked-until", 0) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    @Synchronized fun set(pin: String): Boolean {
        if (RulesEngine.strictActive(Repository.state, System.currentTimeMillis()) || !authorized() || !pin.matches(Regex("[0-9]{6,12}"))) return false
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val ok = prefs().edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
            .remove("attempts").remove("locked-until").commit()
        if (ok) unlockedUntil = SystemClock.elapsedRealtime() + 90_000
        return ok
    }
    @Synchronized fun verify(pin: String): Boolean {
        if (!configured() || remainingSeconds() > 0 || !pin.matches(Regex("[0-9]{6,12}"))) return false
        val p = prefs()
        val valid = runCatching { MessageDigest.isEqual(Base64.decode(p.getString("hash", ""), Base64.NO_WRAP), derive(pin, Base64.decode(p.getString("salt", ""), Base64.NO_WRAP))) }.getOrDefault(false)
        if (valid) {
            p.edit().remove("attempts").remove("locked-until").commit()
            unlockedUntil = SystemClock.elapsedRealtime() + 90_000
        } else {
            val attempts = p.getInt("attempts", 0) + 1
            p.edit().putInt("attempts", attempts).putLong("locked-until", if (attempts >= 5) System.currentTimeMillis() + 60_000 else 0).commit()
        }
        return valid
    }
    @Synchronized fun remove(): Boolean {
        if (!authorized() || RulesEngine.strictActive(Repository.state, System.currentTimeMillis())) return false
        lock(); return prefs().edit().clear().commit()
    }
    /** Only call after Android confirms the device screen-lock credential. */
    fun recoverAfterDeviceCredential(): Boolean {
        if (RulesEngine.strictActive(Repository.state, System.currentTimeMillis())) return false
        lock(); return prefs().edit().clear().commit()
    }
}
