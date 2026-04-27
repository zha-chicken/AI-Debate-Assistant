package com.aidebate.presentation.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring

// ============================================================
// MOTION PRIMITIVES
// ============================================================
// Provides reusable animation specs so all components share
// a consistent motion language.
// ============================================================

// ---- Timing ----
object MotionDuration {
    val instant = 150
    val fast = 250
    val normal = 400
    val slow = 600
    val deliberate = 800
}

// ---- Easing Curves ----
object MotionEasing {
    val standard: Easing = FastOutSlowInEasing
    val linear: Easing = LinearEasing
    val decelerate: Easing = Easing { t -> 1f - (1f - t) * (1f - t) }
    val accelerate: Easing = Easing { t -> t * t }
}

// ---- Tween presets ----
object MotionTween {
    val fadeIn = tween<Float>(durationMillis = MotionDuration.normal, easing = MotionEasing.standard)
    val fadeOut = tween<Float>(durationMillis = MotionDuration.fast, easing = MotionEasing.linear)
    val slideIn = tween<Int>(durationMillis = MotionDuration.normal, easing = MotionEasing.decelerate)
    val scaleIn = tween<Float>(durationMillis = MotionDuration.fast, easing = MotionEasing.standard)
}

// ---- Spring presets ----
object MotionSpring {
    val lowStiffness = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
    val mediumStiffness = spring<Float>(dampingRatio = 0.7f, stiffness = 500f)
    val highStiffness = spring<Float>(dampingRatio = 0.8f, stiffness = 800f)
}

// ---- Hierarchy-driven delay ----
fun importanceDelay(index: Int, baseMs: Int = 70, maxMs: Int = 500): Int =
    (index * baseMs).coerceAtMost(maxMs)

// ---- Semantic animation builders ----
enum class AnimationIntent {
    ENTRY,       // first appearance
    EXIT,        // removal
    EMPHASIS,    // highlighting an important element
    TRANSITION,  // between states
}

fun durationFor(intent: AnimationIntent): Int = when (intent) {
    AnimationIntent.ENTRY -> MotionDuration.normal
    AnimationIntent.EXIT -> MotionDuration.fast
    AnimationIntent.EMPHASIS -> MotionDuration.slow
    AnimationIntent.TRANSITION -> MotionDuration.normal
}

fun easingFor(intent: AnimationIntent): Easing = when (intent) {
    AnimationIntent.ENTRY -> MotionEasing.decelerate
    AnimationIntent.EXIT -> MotionEasing.accelerate
    AnimationIntent.EMPHASIS -> MotionEasing.standard
    AnimationIntent.TRANSITION -> MotionEasing.standard
}
