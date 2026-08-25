package de.rogallab.mobile.ui.colorpicker

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ColorPickerViewModel(): ViewModel() {

    // StateFlow for UI state
    private val _colorUiStateFlow = MutableStateFlow<Color>(Color.Black)
    val colorUiStateFlow: StateFlow<Color> = _colorUiStateFlow.asStateFlow()

    fun onColorChange(color: Color) {
        // Update the color state with the new color
        _colorUiStateFlow.value = color
    }

}