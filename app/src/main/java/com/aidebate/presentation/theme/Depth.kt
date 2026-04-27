package com.aidebate.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================
// 3-LAYER DEPTH SYSTEM
// ============================================================
// Base  — matte background, no elevation
// Content — cards, surfaces, soft shadow
// Focus  — active / animated elements, glow + stronger shadow
// ============================================================

object Depth {
    // ---- Base Layer ----
    val base = 0.dp

    // ---- Content Layer ----
    val content = 2.dp
    val contentShadow = Color(0x1A000000)

    // ---- Focus Layer ----
    val focus = 6.dp
    val focusShadow = Color(0x33000000)
    val focusGlow = Color(0x1A000000)

    // ---- Utility helpers ----
    fun elevation(isContent: Boolean, isFocus: Boolean): Int {
        return when {
            isFocus -> 6
            isContent -> 2
            else -> 0
        }
    }
}
