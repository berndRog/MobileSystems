package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

interface IPersonReadModel {
   // PeopleScreen: all people for the list.
   fun observeListItems(): Flow<Result<List<PersonListItem>>>

   // CarScreen (seller), TDriveScreen (driver): people for a selection field.
   fun observeSelectItems(): Flow<Result<List<PersonSelectItem>>>
}