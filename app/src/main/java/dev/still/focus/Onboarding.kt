package dev.still.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun FoculyOnboarding(done: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(step > 0) { step-- }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandWordmark(Modifier.weight(1f).wrapContentWidth(Alignment.Start))
            if (step < 3) TextButton({ step = 3 }) { Text("Skip intro") }
            else Text("04 / 04", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp).semantics {
            contentDescription = "Onboarding step ${step + 1} of 4"
            progressBarRangeInfo = ProgressBarRangeInfo((step + 1).toFloat(), 0f..4f, 3)
        }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index -> Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) }
        }
        if (step == 3) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SetupChecklist(done) }
            TextButton({ step-- }, Modifier.fillMaxWidth()) { Text("Back to the intro") }
        } else {
            val art = listOf(CatArt.PAUSE, CatArt.STUDY, CatArt.GUARD)[step]
            val color = listOf(FoculyBrand.blush, FoculyBrand.lime, FoculyBrand.peach)[step]
            val title = listOf("Less scroll.\nMore life.", "Find your\nrhythm.", "Keep your\nboundary.")[step]
            val description = listOf(
                "Pause distracting apps, websites, Shorts and Reels. Make space for the things you love.",
                "A few minutes, a day, a week, or until you unblock. Build a rhythm that feels like you.",
                "Choose a gentle pause, a little resistance, or a Strict timer. Connect protection on the next screen."
            )[step]
            Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = color, contentColor = FoculyBrand.ink) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(listOf("A LITTLE MORE ROOM", "ONE THING AT A TIME", "YOUR RULES, YOUR PACE")[step], style = MaterialTheme.typography.labelMedium)
                    Text(title, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 43.sp)
                    CatDoodle(art, Modifier.fillMaxWidth().height(230.dp))
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                    Text(listOf("Less noise. More you.", "Small steps count, too.", "Phone and emergency access stay available.")[step], style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(18.dp))
            Primary(if (step == 2) "Set up protection" else "Next", icon = if (step == 2) Icons.Outlined.Shield else Icons.AutoMirrored.Outlined.ArrowForward) { step++ }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton({ step-- }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Back") }
                else Text("No account. No ads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${step + 1} / 4", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
