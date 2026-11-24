package de.rogallab.mobile.ui.cars.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.CarListItem

@Immutable
data class CarsUiState(
   val cars: List<CarListItem> = emptyList(),
   val isLoading: Boolean = false,
   val loadFailure: String? = null,
)
