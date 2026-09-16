package de.rogallab.mobile.domain.entities

import kotlin.time.Instant

data class TDrive(
   val personId: String? = null,
   val carId: String? = null,
   val start: Instant,
   val isCompleted: Boolean = false,
   val id: String,
)
