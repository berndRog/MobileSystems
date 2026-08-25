package de.rogallab.mobile.domain.entities

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class DogImage(
   val name: String,          // dog name as String
   val resourcePicture: Int,  // dogs picture as drawable resource
   val id: String = UUID.randomUUID().toString(),
)