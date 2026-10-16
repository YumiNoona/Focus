package dev.still.focus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Mono = FontFamily(Font(R.font.jetbrains_mono))
val LocalGlassDark = staticCompositionLocalOf { false }
private fun glassColors(dark: Boolean) = darkColorScheme(
    primary = Color(0xFFD2E6FF), onPrimary = Color(0xFF173B65),
    primaryContainer = Color(0x995478A6), onPrimaryContainer = Color.White,
    secondary = Color(0xFFD2E6FF), onSecondary = Color(0xFF173B65),
    secondaryContainer = Color(0x995478A6), onSecondaryContainer = Color.White,
    background = if (dark) Color(0xFF101F35) else Color(0xFF304B6B), onBackground = Color.White,
    surface = if (dark) Color(0xFF1A2D47) else Color(0xFF3B5576), onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = if (dark) .07f else .09f), onSurfaceVariant = Color(0xFFF1F5FC),
    surfaceContainer = if (dark) Color(0xFF1A2D47) else Color(0xFF3B5576),
    surfaceContainerHigh = if (dark) Color(0xFF20344F) else Color(0xFF3B5576),
    surfaceContainerHighest = if (dark) Color(0xFF263C58) else Color(0xFF425E7F),
    outline = Color.White.copy(alpha = .45f), outlineVariant = Color.White.copy(alpha = .20f),
    error = Color(0xFFFFCFD3), onError = Color(0xFF591E2B),
    errorContainer = Color(0xFF793445), onErrorContainer = Color.White
)

@Composable fun StillTheme(theme: String = "System", content: @Composable () -> Unit) {
    val dark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
    CompositionLocalProvider(LocalGlassDark provides dark) {
        MaterialTheme(colorScheme = glassColors(dark), typography = Typography(
            displayLarge = TextStyle(fontFamily = Mono, fontSize = 42.sp, lineHeight = 50.sp),
            displayMedium = TextStyle(fontFamily = Mono, fontSize = 36.sp, lineHeight = 44.sp),
            displaySmall = TextStyle(fontFamily = Mono, fontSize = 32.sp, lineHeight = 40.sp),
            headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 29.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 25.sp),
            titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
            bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.sp),
            labelMedium = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 16.sp),
            labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, lineHeight = 15.sp)
        )) {
            Box(Modifier.fillMaxSize()) {
                GlassBackdrop()
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    ProvideTextStyle(MaterialTheme.typography.bodyLarge, content = content)
                }
            }
        }
    }
}
