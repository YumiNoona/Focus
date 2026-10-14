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
val Violet = Color(0xFF7760BA)
private val Light = lightColorScheme(primary = Violet, onPrimary = Color.White, primaryContainer = Color(0xFFEBE5F7), onPrimaryContainer = Color(0xFF4C3884),
    background = Color(0xFFF9F8F4), onBackground = Color(0xFF25232A), surface = Color(0xFFF9F8F4), onSurface = Color(0xFF25232A),
    surfaceVariant = Color(0xFFF0EEE8), onSurfaceVariant = Color(0xFF6B6572), outline = Color(0xFFD9D6DE), outlineVariant = Color(0xFFE8E5E0))
private val Dark = darkColorScheme(primary = Color(0xFFC7B5F2), onPrimary = Color(0xFF30204F), primaryContainer = Color(0xFF3B2C58), onPrimaryContainer = Color(0xFFE9DFFB),
    background = Color(0xFF19181C), onBackground = Color(0xFFF4F1F6), surface = Color(0xFF19181C), onSurface = Color(0xFFF4F1F6),
    surfaceVariant = Color(0xFF27252C), onSurfaceVariant = Color(0xFFB3ACBA), outline = Color(0xFF625C69), outlineVariant = Color(0xFF36313D))
@Composable fun StillTheme(theme: String = "System", content: @Composable () -> Unit) {
    val dark = theme == "Dark" || (theme == "System" && isSystemInDarkTheme())
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = Typography(
        displayLarge = TextStyle(fontFamily = Mono, fontSize = 44.sp, lineHeight = 52.sp),
        headlineLarge = TextStyle(fontFamily = Mono, fontSize = 28.sp, lineHeight = 38.sp),
        headlineMedium = TextStyle(fontFamily = Mono, fontSize = 24.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontFamily = Mono, fontSize = 20.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = Mono, fontSize = 18.sp, lineHeight = 26.sp),
        titleSmall = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 18.sp)
    ), content = content)
}
