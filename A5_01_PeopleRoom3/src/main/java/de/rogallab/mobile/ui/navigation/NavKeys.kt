package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PeopleListKey : NavKey

// Shared navigation key for creating and editing a person.
//
// A null personId creates a new person. A non-null personId loads and edits the
// selected person.
@Serializable
data class PersonKey(
   val personId: String? = null,
) : NavKey
