package com.pythonide.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pythonide.app.ui.theme.PythonIDEShould
import org.junit.Rule
import org.junit.Test

class NavigationDrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testNavigationDrawerOpensAndCloses() {
        composeTestRule.setContent {
            PythonIDEShould {
                // Test drawer state
            }
        }

        composeTestRule.onRoot().performClick()
        composeTestRule.waitForIdle()
    }
}
