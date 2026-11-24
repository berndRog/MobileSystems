package de.rogallab.mobile.domain.readmodels

import androidx.compose.runtime.Immutable

// Projection for selecting a car (test drive form).
@Immutable
data class CarSelectItem(
   val id: String,
   val displayName: String,
)
