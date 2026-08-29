package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.domain.entities.Person

// Shared state for creating and editing a person.
//
// The optional personId of the navigation key determines the workflow. The
// ViewModel exposes that decision as isNew, so the shared PersonScreen can
// adapt its title and loading UI without knowing navigation arguments.
//
// Error messages are deliberately not stored in this state. Field errors are
// presented locally by InputValueString. Errors detected when saving or
// loading are emitted as one-shot PersonEvent.ShowSnackbar events.
data class PersonUiState(
   val person: Person? = null,
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
)
