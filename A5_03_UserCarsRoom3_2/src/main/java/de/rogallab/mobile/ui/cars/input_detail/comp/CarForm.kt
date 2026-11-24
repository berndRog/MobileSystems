package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.shared.domain.utilities.newUuid

// UI model for creating and editing a car.
// Holds raw input values; may be incomplete or invalid until Save.
@Immutable
data class CarForm(
   val id: String = newUuid(),
   val manufacturer: String = "",
   val model: String = "",
   val priceInput: String = "",
   val imagePaths: List<String> = emptyList(),
   val personId: String? = null,          // seller
)
