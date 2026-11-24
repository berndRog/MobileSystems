package de.rogallab.mobile.domain.entities

import kotlin.time.Instant

// Domain entity for a test drive.
// Only valid test drives exist here: driver and car are required.
// Incomplete input lives in TDriveForm (UI model).
data class TDrive(
   val id: String,
   val personId: String,      // driver
   val carId: String,
   val start: Instant,
   val isCompleted: Boolean,
)