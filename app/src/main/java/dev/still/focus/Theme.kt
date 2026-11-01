package dev.still.focus

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

val Mono = FontFamily(Font(R.font.jetbrains_mono))
val LocalGlassDark = staticCompositionLocalOf { false }
val LocalGlassDarkProgress = compositionLocalOf { 0f }
val LocalFrostState = staticCompositionLocalOf<HazeState?> { null }

private fun glassColors(dark: Float) = darkColorScheme(
    primary = Color(0xFFD2E6FF), onPrimary = Color(0xFF173B65),
    primaryContainer = Color(0x995478A6), onPrimaryContainer = Color.White,
    secondary = Color(0xFFD2E6FF), onSecondary = Color(0xFF173B65),
    secondaryContainer = Color(0x995478A6), onSecondaryContainer = Color.White,
    background = lerp(Color(0xFF304B6B), Color(0xFF101F35), dark), onBackground = Color.White,
    surface = lerp(Color(0xFF3B5576), Color(0xFF1A2D47), dark), onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = .09f - .02f * dark), onSurfaceVariant = Color(0xFFF1F5FC),
    surfaceContainer = lerp(Color(0xFF3B5576), Color(0xFF1A2D47), dark),
    surfaceContainerHigh = lerp(Color(0xFF3B5576), Color(0xFF20344F), dark),
    surfaceContainerHighest = lerp(Color(0xFF425E7F), Color(0xFF263C58), dark),
    outline = Color.White.copy(alpha = .45f), outlineVariant = Color.White.copy(alpha = .20f),
    error = Color(0xFFFFCFD3), onError = Color(0xFF591E2B),
    errorContainer = Color(0xFF793445), onErrorContainer = Color.White
)

private val FoculyTypography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontSize = 42.sp, lineHeight = 50.sp),
    displayMedium = TextStyle(fontFamily = Mono, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Mono, fontSize = 32.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 25.sp),
    titleSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Mono, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, lineHeight = 15.sp)
)

/** Animate one screen's palette, keeping forms, focus, and scroll state mounted. */
@Composable fun StillTheme(theme: String = "Light", reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    val dark = theme == "Dark"
    val frost = rememberHazeState()
    val progress by animateFloatAsState(if (dark) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(440, easing = FastOutSlowInEasing),
        label = "Screen appearance")
    CompositionLocalProvider(LocalGlassDark provides dark, LocalGlassDarkProgress provides progress, LocalFrostState provides frost) {
        MaterialTheme(colorScheme = glassColors(progress), typography = FoculyTypography) {
            Box(Modifier.fillMaxSize()) {
                GlassBackdrop(Modifier.hazeSource(frost, zIndex = 0f, key = "scene"))
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge, content = content)
                }
            }
        }
    }
}
