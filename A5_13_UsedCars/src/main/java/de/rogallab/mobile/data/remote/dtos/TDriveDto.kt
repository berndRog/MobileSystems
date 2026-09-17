package de.rogallab.mobile.data.remote.dtos

import de.rogallab.mobile.data.remote.InstantAsStringSerializer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TDriveDto(
   val id: String,
   val personId: String,
   val carId: String,
   @Serializable(with = InstantAsStringSerializer::class)
   val start: Instant,
   val isCompleted: Boolean,
)
