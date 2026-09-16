package de.rogallab.mobile.data.remote.dtos

data class CarDto(
   val id: String,
   val manufacturer: String,
   val model: String,
   val price: Int?,
   val imageUrls: List<String> = emptyList(),
   val personId: String,
)
