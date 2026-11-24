package de.rogallab.mobile.ui.cars.input_detail

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm

@Immutable
data class CarUiState(
   val form: CarForm = CarForm(),
   val people: List<PersonSelectItem> = emptyList(),   // seller selection
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
   val loadFailure: CarLoadFailure? = null,
   val isPeopleLoading: Boolean = false,
   val peopleLoadFailure: String? = null,
)
