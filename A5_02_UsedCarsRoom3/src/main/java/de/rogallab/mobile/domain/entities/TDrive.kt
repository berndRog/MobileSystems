package de.rogallab.mobile.domain.entities

import java.time.LocalDateTime

data class TDrive(
   val personId: String? = null,
   val carId: String? = null,
   val start: LocalDateTime,
   val notes: String? = null,
   val isCompleted: Boolean = false,
   val id: String,
)
