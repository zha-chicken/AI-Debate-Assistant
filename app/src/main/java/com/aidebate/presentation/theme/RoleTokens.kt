package com.aidebate.presentation.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// ============================================================
// ROLE IDENTITIES
// ============================================================

enum class DebateRole { PRO, CON, USER, MODERATOR }

// ============================================================
// COLOR TOKENS — per-role identity palette
// ============================================================

data class RoleColorTokens(
    val primary: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val glow: Color,
    val surface: Color,
    val onSurface: Color,
    val container: Color,
    val onContainer: Color,
    val dim: Color,
    val accent: Color,
)

// ============================================================
// DEPTH BEHAVIOR — elevation + glow rules
// ============================================================

data class RoleDepthTokens(
    val contentElevation: Int,
    val focusElevation: Int,
    val shadowColor: Color,
    val glowRadius: Int,
    val glowAlpha: Float,
)

// ============================================================
// MOTION PROFILE — per-role timing and feel
// ============================================================

data class RoleMotionTokens(
    val entryDurationMs: Int,
    val entryEasing: Easing,
    val delayMs: Int,
    val springStiffness: Float,
    val springDamping: Float,
)

// ============================================================
// INTERACTION FEEDBACK — press, focus, active
// ============================================================

data class RoleInteractionTokens(
    val pressScale: Float,
    val focusGlowAlpha: Float,
    val activeElevationOffset: Int,
    val rippleAlpha: Float,
)

// ============================================================
// FULL ROLE TOKEN AGGREGATE
// ============================================================

data class RoleTokens(
    val role: DebateRole,
    val color: RoleColorTokens,
    val depth: RoleDepthTokens,
    val motion: RoleMotionTokens,
    val interaction: RoleInteractionTokens,
    val label: String,
)

// ============================================================
// DEFAULT ROLE TOKENS — the "energy clash" identities
// ============================================================

object RoleTokenDefaults {

    val Pro = RoleTokens(
        role = DebateRole.PRO,
        label = "PRO",
        color = RoleColorTokens(
            primary = Color(0xFFA9B9CA),
            gradientStart = Color(0xFF4D6570),
            gradientEnd = Color(0xFFA9B9CA),
            glow = Color(0xFFA9B9CA),
            surface = Color(0xFF253B44),
            onSurface = Color(0xFFEAF1F4),
            container = Color(0xFF314953),
            onContainer = Color(0xFFEAF1F4),
            dim = Color(0xFF172A31),
            accent = Color(0xFFD7E2EE),
        ),
        depth = RoleDepthTokens(
            contentElevation = 2,
            focusElevation = 6,
            shadowColor = Color(0xFF3F51B5),
            glowRadius = 12,
            glowAlpha = 0.25f,
        ),
        motion = RoleMotionTokens(
            entryDurationMs = 500,
            entryEasing = FastOutSlowInEasing,
            delayMs = 200,
            springStiffness = 300f,
            springDamping = 0.7f,
        ),
        interaction = RoleInteractionTokens(
            pressScale = 0.97f,
            focusGlowAlpha = 0.35f,
            activeElevationOffset = 4,
            rippleAlpha = 0.12f,
        )
    )

    val Con = RoleTokens(
        role = DebateRole.CON,
        label = "CON",
        color = RoleColorTokens(
            primary = Color(0xFFE4A184),
            gradientStart = Color(0xFF8B5A4A),
            gradientEnd = Color(0xFFE4A184),
            glow = Color(0xFFE4A184),
            surface = Color(0xFF4A332F),
            onSurface = Color(0xFFFFE1D4),
            container = Color(0xFF573D36),
            onContainer = Color(0xFFFFE1D4),
            dim = Color(0xFF2F211F),
            accent = Color(0xFFFFC8B1),
        ),
        depth = RoleDepthTokens(
            contentElevation = 2,
            focusElevation = 6,
            shadowColor = Color(0xFFFF8F00),
            glowRadius = 12,
            glowAlpha = 0.25f,
        ),
        motion = RoleMotionTokens(
            entryDurationMs = 450,
            entryEasing = FastOutSlowInEasing,
            delayMs = 150,
            springStiffness = 350f,
            springDamping = 0.65f,
        ),
        interaction = RoleInteractionTokens(
            pressScale = 0.97f,
            focusGlowAlpha = 0.35f,
            activeElevationOffset = 4,
            rippleAlpha = 0.12f,
        )
    )

    val User = RoleTokens(
        role = DebateRole.USER,
        label = "YOU",
        color = RoleColorTokens(
            primary = Color(0xFF9FC5BC),
            gradientStart = Color(0xFF476A63),
            gradientEnd = Color(0xFF9FC5BC),
            glow = Color(0xFF9FC5BC),
            surface = Color(0xFF284842),
            onSurface = Color(0xFFE1F5F0),
            container = Color(0xFF284842),
            onContainer = Color(0xFFE1F5F0),
            dim = Color(0xFF162C28),
            accent = Color(0xFFCDEEE6),
        ),
        depth = RoleDepthTokens(
            contentElevation = 3,
            focusElevation = 8,
            shadowColor = Color(0xFF00897B),
            glowRadius = 14,
            glowAlpha = 0.3f,
        ),
        motion = RoleMotionTokens(
            entryDurationMs = 350,
            entryEasing = FastOutSlowInEasing,
            delayMs = 100,
            springStiffness = 500f,
            springDamping = 0.8f,
        ),
        interaction = RoleInteractionTokens(
            pressScale = 0.96f,
            focusGlowAlpha = 0.4f,
            activeElevationOffset = 5,
            rippleAlpha = 0.15f,
        )
    )

    val Moderator = RoleTokens(
        role = DebateRole.MODERATOR,
        label = "JUDGE",
        color = RoleColorTokens(
            primary = Color(0xFFB8C5CA),
            gradientStart = Color(0xFF596B72),
            gradientEnd = Color(0xFFB8C5CA),
            glow = Color(0xFFB8C5CA),
            surface = Color(0xFF2C4048),
            onSurface = Color(0xFFEAF1F4),
            container = Color(0xFF2C4048),
            onContainer = Color(0xFFEAF1F4),
            dim = Color(0xFF1C2D34),
            accent = Color(0xFFE2EBEF),
        ),
        depth = RoleDepthTokens(
            contentElevation = 1,
            focusElevation = 3,
            shadowColor = Color(0xFF9E9E9E),
            glowRadius = 8,
            glowAlpha = 0.15f,
        ),
        motion = RoleMotionTokens(
            entryDurationMs = 600,
            entryEasing = LinearEasing,
            delayMs = 300,
            springStiffness = 200f,
            springDamping = 0.9f,
        ),
        interaction = RoleInteractionTokens(
            pressScale = 0.98f,
            focusGlowAlpha = 0.2f,
            activeElevationOffset = 2,
            rippleAlpha = 0.08f,
        )
    )

    fun forRole(role: DebateRole): RoleTokens = when (role) {
        DebateRole.PRO -> Pro
        DebateRole.CON -> Con
        DebateRole.USER -> User
        DebateRole.MODERATOR -> Moderator
    }

    val all: Map<DebateRole, RoleTokens> = DebateRole.entries.associateWith { forRole(it) }
}

// ============================================================
// COMPOSITION LOCAL — accessible from any composable
// ============================================================

val LocalRoleTokenMap = staticCompositionLocalOf<Map<DebateRole, RoleTokens>> { emptyMap() }
