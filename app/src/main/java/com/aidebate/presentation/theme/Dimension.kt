package com.aidebate.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ============================================================
// SPACING SYSTEM — normalized to 8 / 12 / 16 / 24 only
// ============================================================

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

// ============================================================
// RADIUS SYSTEM — standardized shape tokens
// ============================================================

object Radii {
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp

    val smallShape = RoundedCornerShape(small)
    val mediumShape = RoundedCornerShape(medium)
    val largeShape = RoundedCornerShape(large)
}
