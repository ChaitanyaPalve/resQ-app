package com.phoenix.phoenixnet.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * State holder for the app's global theming and operational mode.
 */
class PhoenixThemeState(
    initialDarkMode: Boolean = true,
    initialFireMode: Boolean = true
) {
    var isDarkMode by mutableStateOf(initialDarkMode)
    var isFireMode by mutableStateOf(initialFireMode)
}

/**
 * CompositionLocal to provide theme state throughout the hierarchy.
 */
val LocalPhoenixThemeState = compositionLocalOf<PhoenixThemeState> {
    error("No PhoenixThemeState provided")
}

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    val themeState = PhoenixThemeState()
}
