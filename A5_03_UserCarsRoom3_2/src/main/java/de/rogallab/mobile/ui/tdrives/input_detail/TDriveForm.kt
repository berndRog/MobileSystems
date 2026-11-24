package de.rogallab.mobile.ui.tdrives.input_detail

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.shared.domain.utilities.newUuid

// UI model for creating and editing a test drive.
// Holds raw input values; driver and car may still be missing,
// and the start time is the text of the input field.
@Immutable
data class TDriveForm(
   val id: String = newUuid(),
   val personId: String? = null,      // driver
   val carId: String? = null,
   val startInput: String = "",
   val isCompleted: Boolean = false,
)
