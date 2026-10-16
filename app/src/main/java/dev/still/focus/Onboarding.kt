package dev.still.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun FoculyOnboarding(done: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(step > 0) { step-- }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()
        .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 12.dp)) {
        if (step == 3) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SetupChecklist(onboarding = true, done = done) }
        } else {
            val art = listOf(CatArt.PAUSE, CatArt.STUDY, CatArt.GUARD)[step]
            val color = listOf(FoculyBrand.blush, FoculyBrand.lime, FoculyBrand.peach)[step]
            val title = listOf("Less scroll.\nMore life.", "Find your\nrhythm.", "Keep your\nboundary.")[step]
            val description = listOf(
                "Pause what pulls you away.",
                "Keep time for what matters.",
                "Choose what stays out."
            )[step]
            GlassSurface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = color, contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(title, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp)
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CatDoodle(art, Modifier.fillMaxSize().graphicsLayer(scaleX = 1.18f, scaleY = 1.18f))
                    }
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.semantics {
                contentDescription = "Onboarding step ${step + 1} of 4"
                progressBarRangeInfo = ProgressBarRangeInfo((step + 1).toFloat(), 0f..4f, 3)
            }, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(4) { index ->
                    Box(Modifier.width(if (index == step) 28.dp else 18.dp).height(4.dp).clip(CircleShape)
                        .background(if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { if (step == 3) done() else step++ },
                modifier = Modifier.size(64.dp).semantics {
                    contentDescription = if (step == 3) "Finish onboarding" else if (step == 2) "Set up protection" else "Next"
                }, shape = CircleShape, contentPadding = PaddingValues(0.dp)) {
                Icon(if (step == 3) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(26.dp))
            }
        }
    }
    }
}
