package com.spendtrack.app.ui.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}

data class Appearance(val mode: ThemeMode = ThemeMode.SYSTEM, val dynamicColor: Boolean = true)

/** Per-device look-and-feel settings. Not financial data, so plain SharedPreferences is fine. */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(read())
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    fun setMode(mode: ThemeMode) = update(_appearance.value.copy(mode = mode))

    fun setDynamicColor(enabled: Boolean) = update(_appearance.value.copy(dynamicColor = enabled))

    private fun update(value: Appearance) {
        prefs.edit {
            putString(KEY_MODE, value.mode.name)
            putBoolean(KEY_DYNAMIC, value.dynamicColor)
        }
        _appearance.value = value
    }

    private fun read() = Appearance(
        mode = prefs.getString(KEY_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
    )

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_DYNAMIC = "dynamic_color"
    }
}
