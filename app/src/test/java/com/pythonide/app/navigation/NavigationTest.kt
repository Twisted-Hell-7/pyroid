package com.pythonide.app.navigation

import org.junit.Assert.*
import org.junit.Test

class NavigationTest {

    @Test
    fun testScreenRoutes() {
        assertEquals("home", Screen.Home.route)
        assertEquals("editor/{filePath}", Screen.Editor.route)
        assertEquals("settings", Screen.Settings.route)
        assertEquals("debugger", Screen.Debugger.route)
        assertEquals("packages", Screen.Packages.route)
        assertEquals("terminal", Screen.Terminal.route)
    }

    @Test
    fun testBottomNavItems() {
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.Editor,
            BottomNavItem.Packages,
            BottomNavItem.Settings
        )

        assertEquals(4, items.size)
        assertTrue(items.all { it.route.isNotEmpty() })
        assertTrue(items.all { it.label.isNotEmpty() })
    }

    @Test
    fun testDrawerItems() {
        val items = listOf(
            DrawerItem.Home,
            DrawerItem.Editor,
            DrawerItem.Packages,
            DrawerItem.Settings,
            DrawerItem.About
        )

        assertEquals(5, items.size)
        assertTrue(items.all { it.route.isNotEmpty() })
    }

    @Test
    fun testNavigationCommands() {
        val command = NavigationCommand.Navigate("home")

        assertEquals("home", command.destination)
        assertTrue(command is NavigationCommand.Navigate)
    }

    @Test
    fun testNavigationCommandPopBack() {
        val command = NavigationCommand.PopBack

        assertTrue(command is NavigationCommand.PopBack)
    }

    @Test
    fun testNavigationCommandNavigateUp() {
        val command = NavigationCommand.NavigateUp

        assertTrue(command is NavigationCommand.NavigateUp)
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor/{filePath}")
    object Settings : Screen("settings")
    object Debugger : Screen("debugger")
    object Packages : Screen("packages")
    object Terminal : Screen("terminal")
}

sealed class BottomNavItem(val route: String, val label: String) {
    object Home : BottomNavItem("home", "Home")
    object Editor : BottomNavItem("editor", "Editor")
    object Packages : BottomNavItem("packages", "Packages")
    object Settings : BottomNavItem("settings", "Settings")
}

sealed class DrawerItem(val route: String, val label: String) {
    object Home : DrawerItem("home", "Home")
    object Editor : DrawerItem("editor", "Editor")
    object Packages : DrawerItem("packages", "Packages")
    object Settings : DrawerItem("settings", "Settings")
    object About : DrawerItem("about", "About")
}

sealed class NavigationCommand {
    data class Navigate(val destination: String) : NavigationCommand()
    object PopBack : NavigationCommand()
    object NavigateUp : NavigationCommand()
}
