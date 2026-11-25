package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

interface IPersonReadModel {
   // PeopleScreen: all items: List<personListItem> for the list.
   fun observeListItems(): Flow<Result<List<PersonListItem>>>
}
