package de.rogallab.mobile.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import de.rogallab.mobile.data.local.dtos.ArticleDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IArticleDao {
   @Query("SELECT * FROM Article ORDER BY publishedAt DESC")
   fun observeAll(): Flow<List<ArticleDto>>

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun save(articleDto: ArticleDto)

   @Query("DELETE FROM Article WHERE url = :url")
   suspend fun remove(url: String): Int
}
