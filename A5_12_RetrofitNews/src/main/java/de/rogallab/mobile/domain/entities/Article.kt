package de.rogallab.mobile.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class Article(
   val url: String,
   val sourceName: String = "",
   val author: String? = null,
   val title: String = "",
   val description: String? = null,
   val content: String? = null,
   val publishedAt: String = "",
   val imageUrl: String? = null,
)

/*
 * Didaktik und Lernziele
 *
 * - Article ist das gemeinsame Domain-Modell für entfernte Suchergebnisse,
 *   lokale Speicherung, Navigation und Darstellung.
 *
 * - Die Entität enthält weder Retrofit- noch Room-Annotationen. Serializable
 *   wird ausschließlich für die typsichere Navigation benötigt.
 *
 * Lernziele:
 *
 * - Eine technologieunabhängige Domain-Entität verwenden.
 * - Dasselbe fachliche Modell über mehrere technische Schichten führen.
 */
