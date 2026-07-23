package com.pythonide.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object NavTransitions {
    private const val DURATION = 300

    val slideIn: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(DURATION / 2, delayMillis = DURATION / 4)
    )

    val slideOut: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(DURATION / 2)
    )

    val popEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(DURATION / 2, delayMillis = DURATION / 4)
    )

    val popExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(DURATION / 2)
    )

    val verticalSlideIn: EnterTransition = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(DURATION / 2, delayMillis = DURATION / 4)
    )

    val verticalSlideOut: ExitTransition = slideOutVertically(
        targetOffsetY = { fullHeight -> -fullHeight / 3 },
        animationSpec = tween(DURATION, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(DURATION / 2)
    )
}

fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<androidx.navigation.NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { NavTransitions.slideIn },
        exitTransition = { NavTransitions.slideOut },
        popEnterTransition = { NavTransitions.popEnter },
        popExitTransition = { NavTransitions.popExit },
        content = content
    )
}

fun NavGraphBuilder.animatedComposable(
    route: String,
    vararg arguments: Pair<String, NavType<*>>,
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments.map { (name, type) ->
            navArgument(name) { this.type = type }
        },
        enterTransition = { NavTransitions.slideIn },
        exitTransition = { NavTransitions.slideOut },
        popEnterTransition = { NavTransitions.popEnter },
        popExitTransition = { NavTransitions.popExit },
        content = content
    )
}
