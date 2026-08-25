package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PeopleKey : NavKey

@Serializable
data class PersonKey(
   val personId: String? = null,
) : NavKey
