package de.rogallab.mobile.domain.readmodels

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

// Projection for one row of the test drive list.
// Driver and car names are resolved by the query, not by the UI.
@Immutable
data class TDriveListItem(
   val id: String,
   val driverFullName: String,
   val carDisplayName: String,
   val start: Instant,
   val isCompleted: Boolean,
)
