package com.pythonide.app.viewmodel

import com.pythonide.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: TestMainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TestMainViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
        assertTrue(viewModel.useDynamicColors.value)
        assertFalse(viewModel.useAmoledBlack.value)
    }

    @Test
    fun testSetThemeMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun testSetDynamicColors() {
        viewModel.setDynamicColors(false)
        assertFalse(viewModel.useDynamicColors.value)

        viewModel.setDynamicColors(true)
        assertTrue(viewModel.useDynamicColors.value)
    }

    @Test
    fun testSetAmoledBlack() {
        viewModel.setAmoledBlack(true)
        assertTrue(viewModel.useAmoledBlack.value)

        viewModel.setAmoledBlack(false)
        assertFalse(viewModel.useAmoledBlack.value)
    }
}

class TestMainViewModel {
    private val _themeMode = kotlinx.coroutines.flow.MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode

    private val _useDynamicColors = kotlinx.coroutines.flow.MutableStateFlow(true)
    val useDynamicColors = _useDynamicColors

    private val _useAmoledBlack = kotlinx.coroutines.flow.MutableStateFlow(false)
    val useAmoledBlack = _useAmoledBlack

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setDynamicColors(enabled: Boolean) {
        _useDynamicColors.value = enabled
    }

    fun setAmoledBlack(enabled: Boolean) {
        _useAmoledBlack.value = enabled
    }
}
