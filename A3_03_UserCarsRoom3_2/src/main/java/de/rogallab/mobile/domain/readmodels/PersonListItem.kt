package de.rogallab.mobile.domain.readmodels

// domain/readmodels/PeopleListItem.kt   (Projektion)
data class PersonListItem(
   val id: String,
   val fullName: String,
   val email: String?,
   val phone: String?,
   val imagePath: String?,
)