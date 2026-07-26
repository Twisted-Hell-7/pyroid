package com.pythonide.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.pythonide.domain.model.ThemeMode

@Immutable
data class PythonIDEColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
)

private val LightColorScheme = PythonIDEColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAED),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF188038),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCEADAD),
    onTertiaryContainer = Color(0xFF1D192B),
    error = Color(0xFFD93025),
    onError = Color.White,
    errorContainer = Color(0xFFFCEAE9),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FAFB),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFF8FAFB),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5),
    scrim = Color.Black,
    inverseSurface = Color(0xFF313333),
    inverseOnSurface = Color(0xFFF1F3F4),
    inversePrimary = Color(0xFFA8C7FA),
    surfaceDim = Color(0xFFDDE3EA),
    surfaceBright = Color(0xFFF8FAFB),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F4F7),
    surfaceContainer = Color(0xFFECF0F3),
    surfaceContainerHigh = Color(0xFFE6EAED),
    surfaceContainerHighest = Color(0xFFE0E3E7),
)

private val DarkColorScheme = PythonIDEColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF303134),
    secondaryContainer = Color(0xFF48494A),
    onSecondaryContainer = Color(0xFFE8EAED),
    tertiary = Color(0xFF81C995),
    onTertiary = Color(0xFF003920),
    tertiaryContainer = Color(0xFF005233),
    onTertiaryContainer = Color(0xFFCEADAD),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFCEAE9),
    background = Color(0xFF131314),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF131314),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF3C4043),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF313333),
    inversePrimary = Color(0xFF1A73E8),
    surfaceDim = Color(0xFF131314),
    surfaceBright = Color(0xFF3A3C3E),
    surfaceContainerLowest = Color(0xFF0E0E0F),
    surfaceContainerLow = Color(0xFF1B1B1C),
    surfaceContainer = Color(0xFF1F2021),
    surfaceContainerHigh = Color(0xFF2A2A2B),
    surfaceContainerHighest = Color(0xFF353536),
)

private val AmoledColorScheme = PythonIDEColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF9AA0A6),
    onSecondary = Color(0xFF303134),
    secondaryContainer = Color(0xFF2A2A2B),
    onSecondaryContainer = Color(0xFFE8EAED),
    tertiary = Color(0xFF81C995),
    onTertiary = Color(0xFF003920),
    tertiaryContainer = Color(0xFF005233),
    onTertiaryContainer = Color(0xFFCEADAD),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFCEAE9),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF5F6368),
    outlineVariant = Color(0xFF2A2A2A),
    scrim = Color.Black,
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF1A73E8),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF0F0F0F),
    surfaceContainerHigh = Color(0xFF151515),
    surfaceContainerHighest = Color(0xFF1A1A1A),
)

@Composable
fun PythonIDETheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColors: Boolean = true,
    useAmoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkTheme) {
                val scheme = dynamicDarkColorScheme(context)
                PythonIDEColorScheme(
                    primary = scheme.primary,
                    onPrimary = scheme.onPrimary,
                    primaryContainer = scheme.primaryContainer,
                    onPrimaryContainer = scheme.onPrimaryContainer,
                    secondary = scheme.secondary,
                    onSecondary = scheme.onSecondary,
                    secondaryContainer = scheme.secondaryContainer,
                    onSecondaryContainer = scheme.onSecondaryContainer,
                    tertiary = scheme.tertiary,
                    onTertiary = scheme.onTertiary,
                    tertiaryContainer = scheme.tertiaryContainer,
                    onTertiaryContainer = scheme.onTertiaryContainer,
                    error = scheme.error,
                    onError = scheme.onError,
                    errorContainer = scheme.errorContainer,
                    onErrorContainer = scheme.onErrorContainer,
                    background = scheme.background,
                    onBackground = scheme.onBackground,
                    surface = scheme.surface,
                    onSurface = scheme.onSurface,
                    surfaceVariant = scheme.surfaceVariant,
                    onSurfaceVariant = scheme.onSurfaceVariant,
                    outline = scheme.outline,
                    outlineVariant = scheme.outlineVariant,
                    scrim = scheme.scrim,
                    inverseSurface = scheme.inverseSurface,
                    inverseOnSurface = scheme.inverseOnSurface,
                    inversePrimary = scheme.inversePrimary,
                    surfaceDim = scheme.surfaceDim,
                    surfaceBright = scheme.surfaceBright,
                    surfaceContainerLowest = scheme.surfaceContainerLowest,
                    surfaceContainerLow = scheme.surfaceContainerLow,
                    surfaceContainer = scheme.surfaceContainer,
                    surfaceContainerHigh = scheme.surfaceContainerHigh,
                    surfaceContainerHighest = scheme.surfaceContainerHighest,
                )
            } else {
                val scheme = dynamicLightColorScheme(context)
                PythonIDEColorScheme(
                    primary = scheme.primary,
                    onPrimary = scheme.onPrimary,
                    primaryContainer = scheme.primaryContainer,
                    onPrimaryContainer = scheme.onPrimaryContainer,
                    secondary = scheme.secondary,
                    onSecondary = scheme.onSecondary,
                    secondaryContainer = scheme.secondaryContainer,
                    onSecondaryContainer = scheme.onSecondaryContainer,
                    tertiary = scheme.tertiary,
                    onTertiary = scheme.onTertiary,
                    tertiaryContainer = scheme.tertiaryContainer,
                    onTertiaryContainer = scheme.onTertiaryContainer,
                    error = scheme.error,
                    onError = scheme.onError,
                    errorContainer = scheme.errorContainer,
                    onErrorContainer = scheme.onErrorContainer,
                    background = scheme.background,
                    onBackground = scheme.onBackground,
                    surface = scheme.surface,
                    onSurface = scheme.onSurface,
                    surfaceVariant = scheme.surfaceVariant,
                    onSurfaceVariant = scheme.onSurfaceVariant,
                    outline = scheme.outline,
                    outlineVariant = scheme.outlineVariant,
                    scrim = scheme.scrim,
                    inverseSurface = scheme.inverseSurface,
                    inverseOnSurface = scheme.inverseOnSurface,
                    inversePrimary = scheme.inversePrimary,
                    surfaceDim = scheme.surfaceDim,
                    surfaceBright = scheme.surfaceBright,
                    surfaceContainerLowest = scheme.surfaceContainerLowest,
                    surfaceContainerLow = scheme.surfaceContainerLow,
                    surfaceContainer = scheme.surfaceContainer,
                    surfaceContainerHigh = scheme.surfaceContainerHigh,
                    surfaceContainerHighest = scheme.surfaceContainerHighest,
                )
            }
        }
        isDarkTheme && useAmoledBlack -> AmoledColorScheme
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    val materialColorScheme = androidx.compose.material3.ColorScheme(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        primaryContainer = colorScheme.primaryContainer,
        onPrimaryContainer = colorScheme.onPrimaryContainer,
        secondary = colorScheme.secondary,
        onSecondary = colorScheme.onSecondary,
        secondaryContainer = colorScheme.secondaryContainer,
        onSecondaryContainer = colorScheme.onSecondaryContainer,
        tertiary = colorScheme.tertiary,
        onTertiary = colorScheme.onTertiary,
        tertiaryContainer = colorScheme.tertiaryContainer,
        onTertiaryContainer = colorScheme.onTertiaryContainer,
        error = colorScheme.error,
        onError = colorScheme.onError,
        errorContainer = colorScheme.errorContainer,
        onErrorContainer = colorScheme.onErrorContainer,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        surfaceVariant = colorScheme.surfaceVariant,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        outline = colorScheme.outline,
        outlineVariant = colorScheme.outlineVariant,
        scrim = colorScheme.scrim,
        inverseSurface = colorScheme.inverseSurface,
        inverseOnSurface = colorScheme.inverseOnSurface,
        inversePrimary = colorScheme.inversePrimary,
        surfaceDim = colorScheme.surfaceDim,
        surfaceBright = colorScheme.surfaceBright,
        surfaceContainerLowest = colorScheme.surfaceContainerLowest,
        surfaceContainerLow = colorScheme.surfaceContainerLow,
        surfaceContainer = colorScheme.surfaceContainer,
        surfaceContainerHigh = colorScheme.surfaceContainerHigh,
        surfaceContainerHighest = colorScheme.surfaceContainerHighest,
        surfaceTint = colorScheme.primary,
    )

    MaterialTheme(
        colorScheme = materialColorScheme,
        typography = PythonIDETypography,
        content = content
    )
}
