package dev.still.focus

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import org.junit.Assert.assertTrue

/** ScrollTo uses the whole viewport; a floating overlay also needs physical tap clearance. */
fun SemanticsNodeInteraction.performScrollClearOfDock(ui: ComposeContentTestRule): SemanticsNodeInteraction {
    performScrollTo()
    val dock = ui.onAllNodesWithContentDescription("Home").fetchSemanticsNodes().firstOrNull() ?: return this
    val clearance = 16f
    val delta = fetchSemanticsNode().boundsInRoot.bottom - dock.boundsInRoot.top + clearance
    if (delta > 0f) {
        ui.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).onFirst()
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
        ui.waitForIdle()
    }
    assertTrue("Action must be reachable above the floating dock", fetchSemanticsNode().boundsInRoot.bottom <= dock.boundsInRoot.top - clearance + 1f)
    return this
}
