package com.pythonide.app.viewmodel

import com.pythonide.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testThemeModeState() {
        val viewModel = TestSettingsViewModel()

        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun testAmoledBlackState() {
        val viewModel = TestSettingsViewModel()

        assertFalse(viewModel.useAmoledBlack.value)

        viewModel.setAmoledBlack(true)
        assertTrue(viewModel.useAmoledBlack.value)

        viewModel.setAmoledBlack(false)
        assertFalse(viewModel.useAmoledBlack.value)
    }

    @Test
    fun testDynamicColorsState() {
        val viewModel = TestSettingsViewModel()

        assertTrue(viewModel.useDynamicColors.value)

        viewModel.setDynamicColors(false)
        assertFalse(viewModel.useDynamicColors.value)
    }

    @Test
    fun testFontSizeState() {
        val viewModel = TestSettingsViewModel()

        assertEquals(14, viewModel.fontSize.value)

        viewModel.setFontSize(18)
        assertEquals(18, viewModel.fontSize.value)

        viewModel.setFontSize(12)
        assertEquals(12, viewModel.fontSize.value)
    }

    @Test
    fun testTimeoutState() {
        val viewModel = TestSettingsViewModel()

        assertEquals(30_000, viewModel.timeout.value)

        viewModel.setTimeout(60_000)
        assertEquals(60_000, viewModel.timeout.value)
    }
}

class TestSettingsViewModel {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useAmoledBlack = MutableStateFlow(false)
    val useAmoledBlack: StateFlow<Boolean> = _useAmoledBlack.asStateFlow()

    private val _useDynamicColors = MutableStateFlow(true)
    val useDynamicColors: StateFlow<Boolean> = _useDynamicColors.asStateFlow()

    private val _fontSize = MutableStateFlow(14)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _timeout = MutableStateFlow(30_000)
    val timeout: StateFlow<Long> = _timeout.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setAmoledBlack(enabled: Boolean) {
        _useAmoledBlack.value = enabled
    }

    fun setDynamicColors(enabled: Boolean) {
        _useDynamicColors.value = enabled
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size
    }

    fun setTimeout(timeout: Long) {
        _timeout.value = timeout
    }
}
