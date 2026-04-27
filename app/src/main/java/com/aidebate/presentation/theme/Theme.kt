package com.aidebate.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// === Primary Palette — Deep Indigo ===
val Primary = Color(0xFF3F51B5)
val PrimaryLight = Color(0xFF757DE8)
val PrimaryDark = Color(0xFF002984)
val PrimaryContainer = Color(0xFFDBE1FF)
val OnPrimary = Color.White
val OnPrimaryContainer = Color(0xFF001452)

// === Secondary Palette — Warm Amber ===
val Secondary = Color(0xFFFF8F00)
val SecondaryLight = Color(0xFFFFC046)
val SecondaryDark = Color(0xFFC56000)
val SecondaryContainer = Color(0xFFFFECB3)
val OnSecondary = Color(0xFF1C1B1F)
val OnSecondaryContainer = Color(0xFF311300)

// === Tertiary Palette — Teal ===
val Tertiary = Color(0xFF00897B)
val TertiaryLight = Color(0xFF4EBAAA)
val TertiaryDark = Color(0xFF005B4F)
val TertiaryContainer = Color(0xFFB2DFDB)
val OnTertiary = Color.White
val OnTertiaryContainer = Color(0xFF00201C)

// === Semantic ===
val ErrorColor = Color(0xFFD32F2F)
val ErrorContainerColor = Color(0xFFFFDAD6)
val OnErrorColor = Color.White
val OnErrorContainerColor = Color(0xFF410002)

val SuccessGreen = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFE65100)

// === Surfaces ===
val SurfaceLight = Color(0xFFF8F9FF)
val BackgroundLight = Color(0xFFF2F3F9)
val SurfaceVariantLight = Color(0xFFEBEDF7)
val OutlineLight = Color(0xFFC4C6D4)

val SurfaceDark = Color(0xFF111318)
val BackgroundDark = Color(0xFF0D0F13)
val SurfaceVariantDark = Color(0xFF1E2028)
val OutlineDark = Color(0xFF44474F)
val OnSurfaceDark = Color(0xFFE3E2E8)
val OnBackgroundDark = Color(0xFFE3E2E8)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = ErrorColor,
    errorContainer = ErrorContainerColor,
    onError = OnErrorColor,
    onErrorContainer = OnErrorContainerColor,
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF46464F),
    outline = OutlineLight,
    outlineVariant = Color(0xFFD6D8E3)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF001A6E),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryContainer,
    secondary = SecondaryLight,
    onSecondary = Color(0xFF3E2700),
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryContainer,
    tertiary = TertiaryLight,
    onTertiary = Color(0xFF003830),
    tertiaryContainer = TertiaryDark,
    onTertiaryContainer = TertiaryContainer,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = ErrorContainerColor,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC4C6D4),
    outline = OutlineDark,
    outlineVariant = Color(0xFF2E3038)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AiDebateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
