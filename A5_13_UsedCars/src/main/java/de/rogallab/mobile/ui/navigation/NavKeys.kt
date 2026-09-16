package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PersonListKey : NavKey

@Serializable
data class PersonKey(
   val personId: String? = null,
) : NavKey

@Serializable
data object CarListKey : NavKey

@Serializable
data class CarKey(
   val carId: String? = null,
) : NavKey

@Serializable
data object TDrivesKey : NavKey

@Serializable
data class TDriveKey(
   val tDriveId: String? = null,
) : NavKey
