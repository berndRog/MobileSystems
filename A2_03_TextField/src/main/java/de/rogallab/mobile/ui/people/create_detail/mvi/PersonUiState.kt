package de.rogallab.mobile.ui.people.create_detail.mvi

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Person

@Immutable
data class PersonUiState(
   val person: Person,
   val isLoading: Boolean
)