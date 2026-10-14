package dev.still.focus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val Mono = FontFamily(Font(R.font.jetbrains_mono))
private val Light = lightColorScheme(primary = FoculyBrand.ink, onPrimary = FoculyBrand.paper,
    primaryContainer = FoculyBrand.lilac, onPrimaryContainer = Color(0xFF352A4C), secondary = Color(0xFF725582),
    background = FoculyBrand.paper, onBackground = FoculyBrand.ink, surface = FoculyBrand.paper, onSurface = FoculyBrand.ink,
    surfaceVariant = Color(0xFFF0EEE5), onSurfaceVariant = Color(0xFF66665F), outline = Color(0xFF97978E), outlineVariant = Color(0xFFE2E0D7))
private val Dark = darkColorScheme(primary = Color(0xFFF2F0E6), onPrimary = Color(0xFF202520),
    primaryContainer = Color(0xFF41384F), onPrimaryContainer = Color(0xFFEAE3F7), secondary = Color(0xFFD1BDD9),
    background = Color(0xFF191D19), onBackground = Color(0xFFF2F0E6), surface = Color(0xFF191D19), onSurface = Color(0xFFF2F0E6),
    surfaceVariant = Color(0xFF292E28), onSurfaceVariant = Color(0xFFC1C3B8), outline = Color(0xFF92968A), outlineVariant = Color(0xFF3F453C))
@Composable fun StillTheme(theme: String = "System", content: @Composable () -> Unit) {
    val dark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(
        displayLarge = TextStyle(fontFamily = Mono, fontSize = 42.sp, lineHeight = 50.sp),
        displayMedium = TextStyle(fontFamily = Mono, fontSize = 36.sp, lineHeight = 44.sp),
        displaySmall = TextStyle(fontFamily = Mono, fontSize = 32.sp, lineHeight = 40.sp),
        headlineLarge = TextStyle(fontFamily = Mono, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = Mono, fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = Mono, fontSize = 21.sp, lineHeight = 29.sp),
        titleLarge = TextStyle(fontFamily = Mono, fontSize = 20.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = Mono, fontSize = 17.sp, lineHeight = 25.sp),
        titleSmall = TextStyle(fontFamily = Mono, fontSize = 15.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = Mono, fontSize = 15.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 19.sp),
        labelMedium = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, lineHeight = 15.sp)
    )) { ProvideTextStyle(MaterialTheme.typography.bodyLarge) { content() } }
}
