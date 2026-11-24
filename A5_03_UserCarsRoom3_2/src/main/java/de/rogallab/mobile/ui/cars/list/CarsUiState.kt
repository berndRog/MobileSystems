package de.rogallab.mobile.ui.cars.list

import de.rogallab.mobile.domain.readmodels.CarListItem

data class CarsUiState(
   val cars: List<CarListItem> = emptyList(),
   val isLoading: Boolean = false,
)
