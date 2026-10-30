package dev.still.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** A bundled soft-focus scene keeps the material consistent on Android 9+. */
@Composable fun GlassBackdrop(modifier: Modifier = Modifier) {
    val dark = LocalGlassDarkProgress.current
    Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.glass_lake), null, Modifier.fillMaxSize().blur(12.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0xFF243E5E).copy(alpha = .57f + .21f * dark),
            Color(0xFF233F60).copy(alpha = .54f + .26f * dark),
            Color(0xFF0B2037).copy(alpha = .65f + .24f * dark)
        ))))
    }
}

/** Shared see-through chrome for the floating header and dock. */
@Composable fun FloatingGlassChrome(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(36.dp), highContrast: Boolean = false,
    content: @Composable BoxScope.() -> Unit) {
    val dark = LocalGlassDarkProgress.current
    Box(modifier.shadow(12.dp, shape, ambientColor = Color(0x24324B70), spotColor = Color(0x24324B70))
        .clip(shape)
        .background(Brush.verticalGradient(if (highContrast) listOf(lerp(Color(0xBF203C5E), Color(0xBF182D48), dark),
            lerp(Color(0xBF18334F), Color(0xBF13273F), dark)) else listOf(lerp(Color(0x38FFFFFF), Color(0x245F7BA2), dark),
            lerp(Color(0x162F527C), Color(0x30263D5C), dark))))
        .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .55f), Color(0x227EBACB), Color(0x66B9BAE8)))), shape),
        content = content)
}

/** Translucent tint and a specular edge over the shared scene form the material. */
@Composable fun GlassSurface(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(24.dp),
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fill = when (color) {
        Color.Transparent -> Color.Transparent
        scheme.background -> scheme.surface.copy(alpha = .86f)
        scheme.primaryContainer, scheme.secondaryContainer -> color
        scheme.surfaceVariant -> color
        else -> if (color.alpha < 1f) color else Color(0xFF284669).copy(alpha = .35f)
    }
    Surface(modifier.clip(shape).background(Brush.verticalGradient(listOf(Color(0xFF18385E).copy(alpha = .16f), Color(0xFF18385E).copy(alpha = .24f)))),
        shape = shape, color = fill, contentColor = contentColor, tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .38f), Color.White.copy(alpha = .10f))))) {
        content()
    }
}
