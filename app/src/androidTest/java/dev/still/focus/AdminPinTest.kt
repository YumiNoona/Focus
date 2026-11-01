package dev.still.focus

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*

class AdminPinTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun setup() {
        Assume.assumeTrue(android.os.Build.HARDWARE in setOf("ranchu", "goldfish") ||
            android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk"))
        Repository.init(ctx)
        assertFalse(RulesEngine.strictActive(Repository.state, System.currentTimeMillis()))
        ctx.getSharedPreferences("focus-credential", 0).edit().clear().commit(); AdminPin.lock()
    }
    @After fun cleanup() { ctx.getSharedPreferences("focus-credential", 0).edit().clear().commit(); AdminPin.lock() }
    @Test fun pinProtectsControlsAndCannotBeReplacedWithoutAuthentication() {
        assertFalse(AdminPin.set("1234"))
        assertTrue(AdminPin.set("682491")); AdminPin.lock()
        assertFalse(AdminPin.set("987654"))
        assertFalse(Repository.update { it.copy(uninstallProtection = !it.uninstallProtection) })
        assertFalse(Repository.deleteAll())
        assertFalse(AdminPin.remove())
        assertFalse(AdminPin.verify("111111"))
        assertTrue(AdminPin.verify("682491"))
        assertTrue(AdminPin.authorized())
        assertFalse(Repository.export().contains("682491"))
        assertFalse(ctx.getSharedPreferences("focus-credential", 0).all.values.any { it.toString().contains("682491") })
        assertTrue(AdminPin.remove())
    }
    @Test fun failedAttemptsPersistAndEnforceCooldown() {
        assertTrue(AdminPin.set("682491")); AdminPin.lock()
        repeat(5) { assertFalse(AdminPin.verify("111111")) }
        assertTrue(AdminPin.remainingSeconds() > 0)
        AdminPin.lock()
        assertFalse(AdminPin.verify("682491"))
    }
    @Test fun deletingLocalDataAlsoRemovesPinCredential() {
        assertTrue(AdminPin.set("682491"))
        assertTrue(Repository.deleteAll())
        assertFalse(AdminPin.configured())
        assertTrue(ctx.getSharedPreferences("focus-credential", 0).all.isEmpty())
    }
}
