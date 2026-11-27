package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.runtime.Immutable

@Immutable
data class PersonUiState(
   val form: PersonForm = PersonForm(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
   val isEditingImages: Boolean = false,
   val loadFailure: PersonLoadFailure? = null,
)
