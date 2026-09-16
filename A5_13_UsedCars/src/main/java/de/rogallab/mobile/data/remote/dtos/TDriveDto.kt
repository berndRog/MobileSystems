package de.rogallab.mobile.data.remote.dtos

import kotlin.time.Instant

data class TDriveDto(
   val id: String,
   val personId: String,
   val carId: String,
   val start: Instant,
   val isCompleted: Boolean,
)
