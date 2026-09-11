package com.pythonide.app.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
enum class WindowSize { COMPACT, MEDIUM, EXPANDED }

@ReadOnlyComposable
@Composable
fun currentWindowSize(): WindowSize {
    val config = LocalConfiguration.current
    return when {
        config.screenWidthDp < 600 -> WindowSize.COMPACT
        config.screenWidthDp < 840 -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

@ReadOnlyComposable
@Composable
fun isCompact(): Boolean = currentWindowSize() == WindowSize.COMPACT

@ReadOnlyComposable
@Composable
fun isMedium(): Boolean = currentWindowSize() == WindowSize.MEDIUM

@ReadOnlyComposable
@Composable
fun isExpanded(): Boolean = currentWindowSize() == WindowSize.EXPANDED

@ReadOnlyComposable
@Composable
fun isLandscape(): Boolean = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

@ReadOnlyComposable
@Composable
fun isTablet(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 600
}

@ReadOnlyComposable
@Composable
fun navigationBarHeight(): Dp = when {
    isCompact() -> 80.dp
    isLandscape() -> 0.dp
    else -> 80.dp
}

@ReadOnlyComposable
@Composable
fun navigationRailWidth(): Dp = when {
    isExpanded() && !isLandscape() -> 80.dp
    else -> 0.dp
}
