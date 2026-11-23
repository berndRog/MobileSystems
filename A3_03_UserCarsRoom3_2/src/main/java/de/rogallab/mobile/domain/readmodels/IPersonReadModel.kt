package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

// domain/IPersonReadModel.kt
interface IPersonReadModel {
   fun observeListItems(): Flow<Result<List<PersonListItem>>>
}