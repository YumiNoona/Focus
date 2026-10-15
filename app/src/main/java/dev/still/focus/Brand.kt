package dev.still.focus

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object FoculyBrand {
    const val name = "FocuLY"
    val ink = Color(0xFF202520)
    val blush = Color(0xFFF8DED9)
    val lime = Color(0xFFF0F7D6)
    val peach = Color(0xFFFCE6D1)
    val lilac = Color(0xFFEAE3F7)
    val paper = Color(0xFFFBFAF5)
}

enum class CatArt(@param:DrawableRes val drawable: Int) {
    PAUSE(R.drawable.cat_pause), STUDY(R.drawable.cat_study), GUARD(R.drawable.cat_guard),
    REMINDER(R.drawable.cat_reminder), PATIENCE(R.drawable.cat_patience), BOUNDARY(R.drawable.cat_boundary),
    FOCUS(R.drawable.cat_focus), BREAK(R.drawable.cat_break), COMPLETE(R.drawable.cat_complete),
    CHOOSE(R.drawable.cat_choose), FEEDS(R.drawable.cat_feeds), ROUTINE(R.drawable.cat_routine), TUNE(R.drawable.cat_tune)
}

/** Shared by the native blocking overlay and its in-app preview. */
fun FocusMode.catArt(): CatArt = when (this) {
    FocusMode.GENTLE -> CatArt.REMINDER
    FocusMode.STANDARD -> CatArt.PATIENCE
    FocusMode.STRICT -> CatArt.BOUNDARY
}

fun CatArt.tint(): Color = when (this) {
    CatArt.REMINDER -> FoculyBrand.peach
    CatArt.PATIENCE, CatArt.BREAK -> FoculyBrand.lilac
    CatArt.BOUNDARY -> FoculyBrand.blush
    else -> FoculyBrand.lime
}

/** A light backing keeps black doodle strokes readable with either app theme. */
@Composable fun DoodleBadge(art: CatArt, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = art.tint()) {
        CatDoodle(art, Modifier.padding(8.dp))
    }
}

@Composable fun BrandWordmark(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.foculy_logo), null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
        Text(FoculyBrand.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable fun CatDoodle(art: CatArt, modifier: Modifier = Modifier) {
    Image(painterResource(art.drawable), null, modifier, contentScale = ContentScale.Fit)
}

/** Pastel surfaces keep the original black ink readable in light and dark themes. */
@Composable fun DoodleCard(art: CatArt, title: String, detail: String, tint: Color = FoculyBrand.lime) {
    Surface(shape = RoundedCornerShape(28.dp), color = tint, contentColor = FoculyBrand.ink) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            CatDoodle(art, Modifier.size(104.dp))
        }
    }
}
