package de.rogallab.mobile.domain.readmodels

import androidx.compose.runtime.Immutable

@Immutable
data class CarListItem(
   val id: String,
   val displayName: String,
   val primaryImagePath: String?,
   val price: Int?,
   val sellerFullName: String?
)