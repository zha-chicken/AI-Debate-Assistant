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

object RhetorixColors {
    val BackgroundBase = Color(0xFF13242B)
    val BackgroundDeep = Color(0xFF0E1A20)
    val BackgroundGlowCyan = Color(0x3386C7C8)
    val BackgroundGlowAmber = Color(0x22C9A06A)
}

object RhetorixSurfaces {
    val GlassBase = Color(0xB322363F)
    val GlassRaised = Color(0x99445861)
    val GlassMuted = Color(0x6622363F)
    val GlassStrong = Color(0xCC445861)
}

object RhetorixBorders {
    val Subtle = Color(0x24D2E1E6)
    val Standard = Color(0x33D2E1E6)
    val FocusCyan = Color(0x7386C7C8)
    val FocusAmber = Color(0x73C9A06A)
    val Error = Color(0x73C77972)
}

object RhetorixAccents {
    val Cyan = Color(0xFF86C7C8)
    val Amber = Color(0xFFC9A06A)
    val Peach = Color(0xFFC68F7B)
    val Green = Color(0xFF8FAE9B)
    val Salmon = Color(0xFFC77972)
    val Lavender = Color(0xFFA99ABF)
}

object RhetorixTextColors {
    val Primary = Color(0xFFF3F7F8)
    val Secondary = Color(0xB3F3F7F8)
    val Tertiary = Color(0x80F3F7F8)
    val Disabled = Color(0x59F3F7F8)
}

val Primary = RhetorixAccents.Cyan
val PrimaryLight = Color(0xFFD7E2EE)
val PrimaryDark = Color(0xFF708495)
val PrimaryContainer = Color(0xFF314953)
val OnPrimary = RhetorixTextColors.Primary
val OnPrimaryContainer = RhetorixTextColors.Primary

val Secondary = RhetorixAccents.Peach
val SecondaryLight = Color(0xFFFFC8B1)
val SecondaryDark = Color(0xFF9E604A)
val SecondaryContainer = Color(0xFF573D36)
val OnSecondary = RhetorixTextColors.Primary
val OnSecondaryContainer = Color(0xFFFFE1D4)

val Tertiary = RhetorixAccents.Green
val TertiaryLight = Color(0xFFCDEEE6)
val TertiaryDark = Color(0xFF638980)
val TertiaryContainer = Color(0xFF284842)
val OnTertiary = RhetorixTextColors.Primary
val OnTertiaryContainer = Color(0xFFE1F5F0)

val ErrorColor = RhetorixAccents.Salmon
val ErrorContainerColor = Color(0xFF573532)
val OnErrorColor = RhetorixTextColors.Primary
val OnErrorContainerColor = Color(0xFFFFDAD4)

val SuccessGreen = RhetorixAccents.Green
val WarningAmber = RhetorixAccents.Amber

val BackgroundDark = RhetorixColors.BackgroundBase
val SurfaceDark = Color(0xFF22363F)
val SurfaceVariantDark = Color(0xFF2B424B)
val OutlineDark = RhetorixBorders.Standard
val OnSurfaceDark = RhetorixTextColors.Primary
val OnBackgroundDark = RhetorixTextColors.Primary

val GlassSurface = RhetorixSurfaces.GlassBase
val GlassSurfaceStrong = RhetorixSurfaces.GlassRaised
val GlassStroke = RhetorixBorders.Standard
val WarmGlow = RhetorixAccents.Amber

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
