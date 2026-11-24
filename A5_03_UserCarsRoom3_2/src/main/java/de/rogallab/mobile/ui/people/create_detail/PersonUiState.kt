package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.SellerCarItem

@Immutable
data class PersonUiState(
   val form: PersonForm = PersonForm(),
   val cars: List<SellerCarItem> = emptyList(),   // loaded on demand via ICarReadModel
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
   val loadFailure: PersonLoadFailure? = null,
   val isCarsLoading: Boolean = false,
)
