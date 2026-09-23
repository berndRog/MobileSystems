package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Article

interface INewsRepository {
   // Loads one result page for the given search term from the news source.
   suspend fun search(
      searchText: String,
      page: Int = 1,
   ): Result<List<Article>>
}

/*
 * Didaktik und Lernziele
 *
 * - Das Interface bildet den Port zwischen ViewModel und entfernter Datenquelle.
 *   Weder Retrofit noch NewsAPI-Datentypen werden in die UI-Schicht weitergegeben.
 *
 * - search(...) ist bewusst ein suspendierender Einzelaufruf und kein Flow:
 *   Eine HTTP-Anfrage liefert genau einen Snapshot der Suchergebnisse.
 *
 * Lernziele:
 *
 * - Eine entfernte Datenquelle hinter einem Repository-Port abstrahieren.
 * - Einmalige Netzwerkzugriffe von beobachtbaren Datenströmen unterscheiden.
 */
