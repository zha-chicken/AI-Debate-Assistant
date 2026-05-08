package com.aidebate.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val Primary = Color(0xFFA9B9CA)
val PrimaryLight = Color(0xFFD7E2EE)
val PrimaryDark = Color(0xFF708495)
val PrimaryContainer = Color(0xFF314953)
val OnPrimary = Color(0xFF122128)
val OnPrimaryContainer = Color(0xFFE7EEF4)

val Secondary = Color(0xFFE4A184)
val SecondaryLight = Color(0xFFFFC8B1)
val SecondaryDark = Color(0xFF9E604A)
val SecondaryContainer = Color(0xFF573D36)
val OnSecondary = Color(0xFF2B1711)
val OnSecondaryContainer = Color(0xFFFFE1D4)

val Tertiary = Color(0xFF9FC5BC)
val TertiaryLight = Color(0xFFCDEEE6)
val TertiaryDark = Color(0xFF638980)
val TertiaryContainer = Color(0xFF284842)
val OnTertiary = Color(0xFF0A211D)
val OnTertiaryContainer = Color(0xFFE1F5F0)

val ErrorColor = Color(0xFFFFB4A9)
val ErrorContainerColor = Color(0xFF573532)
val OnErrorColor = Color(0xFF3A0B07)
val OnErrorContainerColor = Color(0xFFFFDAD4)

val SuccessGreen = Color(0xFF9FC5BC)
val WarningAmber = Color(0xFFE0B977)

val BackgroundDark = Color(0xFF13242B)
val SurfaceDark = Color(0xFF22363F)
val SurfaceVariantDark = Color(0xFF2B424B)
val OutlineDark = Color(0x668AA0AA)
val OnSurfaceDark = Color(0xFFEAF1F4)
val OnBackgroundDark = Color(0xFFEAF1F4)

val GlassSurface = Color(0x66354A53)
val GlassSurfaceStrong = Color(0x99445861)
val GlassStroke = Color(0x5598AAB2)
val WarmGlow = Color(0xFFE4A184)

private val AppColorScheme = darkColorScheme(
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
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC7D2D7),
    outline = OutlineDark,
    outlineVariant = Color(0x335D737D),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun AiDebateTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            SideEffect {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalRoleTokenMap provides RoleTokenDefaults.all,
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark)
            ) {
                content()
            }
        }
    }
}
