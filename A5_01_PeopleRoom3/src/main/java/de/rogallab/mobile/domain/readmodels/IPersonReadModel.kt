package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

interface IPersonReadModel {
   // PeopleScreen: all people for the list.
   fun observeListItems(): Flow<Result<List<PersonListItem>>>

}
