package de.rogallab.mobile.data.local

import androidx.room.*
import de.rogallab.mobile.data.dtos.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface IArticleDao {

   @Query("SELECT * FROM article")
   fun select(): Flow<List<Article>>

   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun upsert(article: Article): Long

   @Delete
   suspend fun remove(article: Article)

}