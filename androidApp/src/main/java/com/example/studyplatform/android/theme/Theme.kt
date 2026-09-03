package com.example.studyplatform.android.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Primary,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = Secondary,
    background = Background,
    surface = Surface,
    surfaceVariant = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Border,
    error = Accent,
    onError = Color.White,
    errorContainer = AccentLight,
    onErrorContainer = Accent,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessLight,
    onTertiaryContainer = Success,
)

/** One radius vocabulary, so cards, fields and buttons agree with each other. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun StudyPlatformTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        // The scale was defined but never handed to MaterialTheme, so every `Text`
        // without an explicit style fell back to the platform default.
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
