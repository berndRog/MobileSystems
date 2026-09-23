package de.rogallab.mobile.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import de.rogallab.mobile.data.local.dtos.ArticleDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IArticleDao {
   // Room reruns the query whenever the Article table changes.
   @Query("SELECT * FROM Article ORDER BY publishedAt DESC")
   fun observeAll(): Flow<List<ArticleDto>>

   // Replacing on conflict makes saving the same article idempotent.
   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun save(articleDto: ArticleDto)

   // The URL is the primary key and therefore identifies the row to delete.
   @Query("DELETE FROM Article WHERE url = :url")
   suspend fun remove(url: String): Int
}

/*
 * Didaktik und Lernziele
 *
 * - Das DAO enthält ausschließlich Room-Zugriffe auf ArticleDto. Die sortierte
 *   Abfrage ist als Flow dauerhaft beobachtbar.
 *
 * - OnConflictStrategy.REPLACE erlaubt das erneute Speichern desselben Artikels,
 *   ohne einen zweiten Datensatz anzulegen.
 *
 * Lernziele:
 *
 * - SQL-Abfragen und Schreiboperationen in einem DAO kapseln.
 * - Reaktive Room-Abfragen und Primärschlüsselverhalten verstehen.
 */
