package de.rogallab.mobile.domain.readmodels

import androidx.compose.runtime.Immutable

@Immutable
data class SellerCarItem(
   val id: String,
   val displayName: String,
   val primaryImagePath: String?,
   val price: Int?,
)