package de.rogallab.mobile.data.remote.dtos

// JSON representation returned by PeopleApi.
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
 * - ImageUrl ist eine absolute Server-URL und kein lokaler Android-Dateipfad.
 */
