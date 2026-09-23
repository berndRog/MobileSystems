package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
   // Observes all articles stored in the local Room database.
   fun observeAll(): Flow<Result<List<Article>>>

   // Inserts or replaces one locally stored article.
   suspend fun save(article: Article): Result<Unit>

   // Removes the locally stored article identified by its URL.
   suspend fun remove(url: String): Result<Unit>
}

/*
 * Didaktik und Lernziele
 *
 * - Gespeicherte Artikel liegen lokal in Room und sind deshalb als Flow
 *   beobachtbar. Änderungen werden automatisch an die UI weitergegeben.
 *
 * - Die URL ist zugleich fachliche Identität und Primärschlüssel eines Artikels.
 *   Die Domain-Schnittstelle bleibt dabei unabhängig von DAO und ArticleDto.
 *
 * Lernziele:
 *
 * - Einen lokalen Repository-Port mit lesenden und schreibenden Operationen nutzen.
 * - Room-spezifische Typen von Domain und UI fernhalten.
 */
