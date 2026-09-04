@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.htj.habitzy.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens for Habitzy.
 *
 * Phase 5 of the M3 Expressive migration: every spec below (other than
 * [celebrationSpring]) is derived from `MaterialTheme.motionScheme` — wired
 * into MaterialExpressiveTheme in Theme.kt (Phase 1) — instead of hand-picked
 * damping/stiffness numbers. Hand-built components (nav cluster, streak
 * badge, nav transitions) now animate on the same system as stock M3
 * components (buttons, switches, nav items).
 *
 * These are @Composable reads of a CompositionLocal, so call them from
 * composable scope. If you need one inside a non-composable callback (e.g.
 * NavHost's enterTransition lambda), resolve it to a local val first — see
 * HabitzyNavHost.kt.
 */
object HabitzyMotion {

    /** Everyday spatial motion — size/position changes (buttons, cards, nav items). */
    @Composable
    @ReadOnlyComposable
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultSpatialSpec()

    /** Quicker spatial motion for small, frequent movements. */
    @Composable
    @ReadOnlyComposable
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.fastSpatialSpec()

    /** Slower, more deliberate spatial motion for large/important movements. */
    @Composable
    @ReadOnlyComposable
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.slowSpatialSpec()

    /** Non-spatial motion — fades, color/opacity changes. */
    @Composable
    @ReadOnlyComposable
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = MaterialTheme.motionScheme.defaultEffectsSpec()

    /**
     * Deliberate exception: bouncier than the shared scheme. Reserved for the
     * "all habits done today" celebration (StreakBadge's bounce-on-new-record,
     * CelebrationConfetti). Everything else should prefer the specs above.
     */
    val celebrationSpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 300f)
}

private fun slideDirection(target: Int, initial: Int): Int =
    if (target >= initial) 1 else -1

/**
 * Shared-axis-X transition (Habits ↔ Insights ↔ Settings tabs). [spatialSpec] /
 * [effectsSpec] must be resolved from `MaterialTheme.motionScheme` in the calling
 * composable and passed in — NavHost's transition lambdas aren't themselves @Composable.
 */
fun <S> AnimatedContentTransitionScope<S>.materialSharedAxisXIn(
    spatialSpec: FiniteAnimationSpec<IntOffset>,
    effectsSpec: FiniteAnimationSpec<Float>,
): EnterTransition {
    val direction = slideDirection(targetState.hashCode(), initialState.hashCode())
    return slideInHorizontally(animationSpec = spatialSpec, initialOffsetX = { it / 3 * direction }) +
            fadeIn(animationSpec = effectsSpec)
}

fun <S> AnimatedContentTransitionScope<S>.materialSharedAxisXOut(
    spatialSpec: FiniteAnimationSpec<IntOffset>,
    effectsSpec: FiniteAnimationSpec<Float>,
): ExitTransition {
    val direction = slideDirection(targetState.hashCode(), initialState.hashCode())
    return slideOutHorizontally(animationSpec = spatialSpec, targetOffsetX = { it / 3 * -direction }) +
            fadeOut(animationSpec = effectsSpec)
}

/** Push-in-from-end transition (drilling into a detail/edit/settings screen). */
fun <S> AnimatedContentTransitionScope<S>.materialSlideFromEndIn(
    spatialSpec: FiniteAnimationSpec<IntOffset>,
    effectsSpec: FiniteAnimationSpec<Float>,
): EnterTransition =
    slideInHorizontally(animationSpec = spatialSpec, initialOffsetX = { it }) +
            fadeIn(animationSpec = effectsSpec)

fun <S> AnimatedContentTransitionScope<S>.materialSlideToEndOut(
    spatialSpec: FiniteAnimationSpec<IntOffset>,
    effectsSpec: FiniteAnimationSpec<Float>,
): ExitTransition =
    slideOutHorizontally(animationSpec = spatialSpec, targetOffsetX = { it }) +
            fadeOut(animationSpec = effectsSpec)