package de.rogallab.mobile.domain.readmodels

import androidx.compose.runtime.Immutable

// Projection for selecting a person (seller, test driver).
// selectionName is "lastName, firstName" and is also used for searching.
@Immutable
data class PersonSelectItem(
   val id: String,
   val selectionName: String,
)