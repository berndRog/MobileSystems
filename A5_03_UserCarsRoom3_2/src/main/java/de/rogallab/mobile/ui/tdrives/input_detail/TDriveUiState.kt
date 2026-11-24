package de.rogallab.mobile.ui.tdrives.input_detail

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.PersonSelectItem

@Immutable
data class TDriveUiState(
   val form: TDriveForm = TDriveForm(),
   val people: List<PersonSelectItem> = emptyList(),   // driver selection
   val cars: List<CarSelectItem> = emptyList(),        // car selection
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
)
