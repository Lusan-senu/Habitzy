package com.htj.habitzy.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

object HabitzyMotion {
    val expressiveSpring: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 380f)
    val celebrationSpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 300f)
    val standardSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 300f)
}

private val TabSlideSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis = 300)
private val SlideSlideSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis = 250)
private val FadeSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 300)

private fun slideDirection(target: Int, initial: Int): Int =
    if (target >= initial) 1 else -1

fun <S> AnimatedContentTransitionScope<S>.sharedAxisXIn(): EnterTransition {
    val direction = slideDirection(targetState.hashCode(), initialState.hashCode())
    return slideInHorizontally(animationSpec = TabSlideSpec, initialOffsetX = { it / 3 * direction }) +
        fadeIn(animationSpec = FadeSpec)
}

fun <S> AnimatedContentTransitionScope<S>.sharedAxisXOut(): ExitTransition {
    val direction = slideDirection(targetState.hashCode(), initialState.hashCode())
    return slideOutHorizontally(animationSpec = TabSlideSpec, targetOffsetX = { it / 3 * -direction }) +
        fadeOut(animationSpec = FadeSpec)
}

fun <S> AnimatedContentTransitionScope<S>.slideFromEndIn(): EnterTransition =
    slideInHorizontally(animationSpec = SlideSlideSpec, initialOffsetX = { it }) +
        fadeIn(animationSpec = FadeSpec)

fun <S> AnimatedContentTransitionScope<S>.slideToEndOut(): ExitTransition =
    slideOutHorizontally(animationSpec = SlideSlideSpec, targetOffsetX = { it }) +
        fadeOut(animationSpec = FadeSpec)
