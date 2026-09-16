package de.rogallab.mobile.data.remote.dtos

// JSON representation returned by PeopleImagesApi.
data class PersonDto(
   val id: String,
   val firstName: String,
   val lastName: String,
   val email: String?,
   val phone: String?,
   val imageUrl: String?,
)

/*
 * Didaktik und Lernziele
 *
 * - PersonDto bildet den JSON-Vertrag der WebAPI ab und bleibt in der Data-Schicht.
 * - Die Domain verwendet weiterhin Person und kennt weder Retrofit noch JSON.
 * - imageUrl ist ein optionaler String in der Antwort. PeopleImagesApi verwaltet
 *   diesen Wert; Create und Update ändern ihn nicht direkt.
 */
