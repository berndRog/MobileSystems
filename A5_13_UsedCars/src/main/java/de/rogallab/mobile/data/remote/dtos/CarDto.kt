package de.rogallab.mobile.data.remote.dtos

import kotlinx.serialization.Serializable

@Serializable
data class CarDto(
   val id: String,
   val manufacturer: String,
   val model: String,
   val price: Int? = null,
   val imageUrls: List<String> = emptyList(),
   val personId: String,
)
