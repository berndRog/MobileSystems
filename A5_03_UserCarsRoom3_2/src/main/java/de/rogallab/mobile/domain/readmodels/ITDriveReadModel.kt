package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

// Read side for the TDrive table.
// Returns projections tailored to the screens; never used for writing.
interface ITDriveReadModel {

   // TDrivesScreen: all test drives including driver and car names.
   fun observeListItems(): Flow<Result<List<TDriveListItem>>>
}
