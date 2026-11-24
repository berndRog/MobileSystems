package de.rogallab.mobile.ui.tdrives.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.readmodels.TDriveListItem

// Driver and car names are part of TDriveListItem;
// separate people and car lists are no longer needed.
@Immutable
data class TDrivesUiState(
   val tDrives: List<TDriveListItem> = emptyList(),
   val isLoading: Boolean = false,
)
