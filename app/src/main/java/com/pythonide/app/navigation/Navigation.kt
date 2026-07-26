package com.pythonide.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.pythonide.app.screens.editor.CodeEditorScreen
import com.pythonide.app.screens.filemanager.FileManagerScreen
import com.pythonide.app.screens.home.HomeScreen
import com.pythonide.app.screens.packages.PackageManagementScreen
import com.pythonide.app.screens.project.ProjectManagementScreen
import com.pythonide.app.screens.terminal.TerminalScreen
import com.pythonide.app.screens.settings.SettingsScreen
import com.pythonide.app.ui.components.AppBottomNavigation
import com.pythonide.app.ui.components.AppNavigationDrawer
import com.pythonide.app.ui.components.AppNavigationRail
import com.pythonide.app.ui.layout.currentWindowSize
import com.pythonide.app.ui.layout.isCompact
import com.pythonide.app.ui.layout.isExpanded
import com.pythonide.app.ui.layout.isLandscape
import com.pythonide.app.ui.components.NavTransitions

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor/{fileId}") {
        fun createRoute(fileId: String? = null): String {
            return if (fileId != null) "editor/${Uri.encode(fileId)}" else "editor/new"
        }
    }
    data object Terminal : Screen("terminal")
    data object FileManager : Screen("filemanager")
    data object Projects : Screen("projects")
    data object Packages : Screen("packages")
    data object Settings : Screen("settings")
}

private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.FileManager.route,
    Screen.Terminal.route,
    Screen.Packages.route,
    Screen.Settings.route
)

@Composable
fun PythonIDENavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes
    val showRail = isExpanded() && !isLandscape() && currentRoute in bottomNavRoutes
    val showDrawer = !isCompact() && !isLandscape()

    AppNavigationDrawer(
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                AppNavigationRail(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        AppBottomNavigation(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = modifier.padding(innerPadding),
                    enterTransition = { NavTransitions.slideIn },
                    exitTransition = { NavTransitions.slideOut },
                    popEnterTransition = { NavTransitions.popEnter },
                    popExitTransition = { NavTransitions.popExit }
                ) {
                    composable(route = Screen.Home.route) {
                        HomeScreen(
                            onNavigateToEditor = { fileId ->
                                navController.navigate(Screen.Editor.createRoute(fileId))
                            },
                            onNavigateToREPL = {
                                navController.navigate(Screen.Terminal.route)
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onNavigateToFileManager = {
                                navController.navigate(Screen.FileManager.route)
                            },
                            onNavigateToProjects = {
                                navController.navigate(Screen.Projects.route)
                            },
                            onNavigateToPackages = {
                                navController.navigate(Screen.Packages.route)
                            }
                        )
                    }

                    composable(
                        route = Screen.Editor.route,
                        arguments = listOf(
                            navArgument("fileId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) { backStackEntry ->
                        val fileId = backStackEntry.arguments?.getString("fileId")?.let { Uri.decode(it) }
                        CodeEditorScreen(
                            fileId = fileId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = Screen.Terminal.route,
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) {
                        TerminalScreen()
                    }

                    composable(
                        route = Screen.FileManager.route,
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) {
                        FileManagerScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onOpenFile = { path ->
                                navController.navigate(Screen.Editor.createRoute(path))
                            }
                        )
                    }

                    composable(
                        route = Screen.Projects.route,
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) {
                        ProjectManagementScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onOpenProject = { projectPath ->
                                navController.navigate(Screen.Editor.createRoute(projectPath))
                            }
                        )
                    }

                    composable(
                        route = Screen.Packages.route,
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) {
                        PackageManagementScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = Screen.Settings.route,
                        enterTransition = { NavTransitions.slideIn },
                        exitTransition = { NavTransitions.slideOut },
                        popEnterTransition = { NavTransitions.popEnter },
                        popExitTransition = { NavTransitions.popExit }
                    ) {
                        SettingsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
