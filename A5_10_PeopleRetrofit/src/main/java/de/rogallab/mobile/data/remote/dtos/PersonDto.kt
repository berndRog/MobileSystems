package de.rogallab.mobile.data.remote.dtos

import kotlinx.serialization.Serializable

// JSON representation returned by PeopleApi.
@Serializable
data class PersonDto(
   val id: String,
   val firstName: String,
   val lastName: String,
   val email: String? = null,
   val phone: String? = null,
   val imageUrl: String? = null,
)

/*
 * Didaktik und Lernziele
 *
 * - PersonDto bildet den JSON-Vertrag der WebAPI ab und bleibt in der Data-Schicht.
 * - @Serializable erzeugt die für kotlinx.serialization benötigte Serialisierung.
 * - Die Domain verwendet weiterhin Person und kennt weder Retrofit noch JSON.
 * - imageUrl ist ein optionaler String wie jedes andere DTO-Attribut. Die API
 *   speichert seinen Wert, verarbeitet aber keine Bilddatei.
 */
