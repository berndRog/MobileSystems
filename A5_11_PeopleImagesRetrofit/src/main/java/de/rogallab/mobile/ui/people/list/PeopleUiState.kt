package de.rogallab.mobile.ui.people.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.PersonListItem

@Immutable
data class PeopleUiState(
   val isLoading: Boolean = false,
   val listItems: List<PersonListItem> = emptyList(),
   val loadFailure: String? = null,
)

/*
 * The list displays read-model projections, not editable domain entities.
 * Retry restarts observation; deletion still requires a confirmation effect.
 */
