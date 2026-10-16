package dev.still.focus

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.*
import org.junit.Assert.*

class BlockingArtworkTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    @Before fun reset() { ui.runOnIdle { Repository.update { FocusState(onboarding = true) } } }
    @After fun clean() { ui.runOnIdle { Repository.update { FocusState(onboarding = true) }; BlockingArtwork.prune(ui.activity, emptySet()) } }
    private fun openMessages() {
        ui.onNodeWithContentDescription("Settings").performClick()
        ui.onNodeWithText("Your blocking screen").performScrollTo().performClick()
    }
    @Test fun importedStickerSurvivesRecreationAndPersistsUntilRemoved() {
        openMessages()
        val source = Uri.parse("android.resource://${ui.activity.packageName}/${R.drawable.cat_patience}")
        val monitor = instrumentation.addMonitor(IntentFilter(Intent.ACTION_GET_CONTENT).apply {
            addDataType("image/*"); addCategory(Intent.CATEGORY_OPENABLE)
        },
            ActivityResult(Activity.RESULT_OK, Intent().setData(source)), true)
        try {
            ui.onNodeWithText("Add image or sticker").performScrollTo().performClick()
            ui.waitUntil(10_000) { ui.onAllNodesWithText("Replace your image").fetchSemanticsNodes().isNotEmpty() }
        } finally { instrumentation.removeMonitor(monitor) }
        ui.onNodeWithContentDescription("Use 🐾").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Use 🐾").assertIsSelected()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithContentDescription("Use 🐾").assertIsSelected()
        ui.onNodeWithText("Replace your image").assertExists()
        ui.onNodeWithText("Save blocking screen").assertIsDisplayed().performClick()
        ui.runOnIdle {
            val message = Repository.state.blockMessage
            assertEquals("🐾", message.emoji)
            assertTrue(message.image.matches(Regex("[a-f0-9]{64}\\.png")))
            val bitmap = BlockingArtwork.bitmap(ui.activity, message.image)!!
            assertTrue(bitmap.width <= 1024 && bitmap.height <= 1024)
            assertTrue(bitmap.hasAlpha()); bitmap.recycle()
            val decode = Repository::class.java.getDeclaredMethod("decode", String::class.java).apply { isAccessible = true }
            assertEquals(message, (decode.invoke(Repository, Repository.export()) as FocusState).blockMessage)
        }
        ui.onNodeWithText("Remove your image").performScrollTo().performClick()
        ui.onNodeWithText("No illustration").performScrollTo().performClick()
        ui.onNodeWithText("Save blocking screen").performClick()
        ui.runOnIdle { assertEquals("NONE", Repository.state.blockMessage.art); assertEquals("", Repository.state.blockMessage.image) }
    }
    @Test fun rejectsUnsafeNamesAndNonImagesAndKeepsStrictCustomizationLocked() {
        assertNull(BlockingArtwork.bitmap(ui.activity, "../../state.xml"))
        assertFalse(BlockingArtwork.validName("content://any/provider"))
        val source = Uri.parse("android.resource://${ui.activity.packageName}/${R.font.jetbrains_mono}")
        assertTrue(runCatching { BlockingArtwork.importImage(ui.activity, source) }.isFailure)
        ui.runOnIdle {
            val now = System.currentTimeMillis()
            Repository.update { it.copy(blocks = listOf(BlockPlan(kind = BlockKind.APPS, targets = setOf("dev.still.fixture"), mode = FocusMode.STRICT, start = now, end = now + 2_000))) }
            assertFalse(Repository.update { it.copy(blockMessage = BlockMessage(art = "TEA")) })
        }
        Thread.sleep(2100)
        ui.runOnIdle { Repository.reconcile() }
        openMessages()
        ui.onNodeWithText("ILLUSTRATION").performScrollTo()
        ui.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).onFirst()
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 220f) }
        // Select a built-in thumbnail, save, and verify the shared native/preview resolver.
        ui.onNodeWithText("Patience").performClick()
        ui.onNodeWithText("Save blocking screen").performClick()
        ui.runOnIdle {
            assertEquals("PATIENCE", Repository.state.blockMessage.art)
            assertEquals(CatArt.PATIENCE, BlockingArtwork.art(Repository.state.blockMessage, FocusMode.STRICT))
        }
    }
}
