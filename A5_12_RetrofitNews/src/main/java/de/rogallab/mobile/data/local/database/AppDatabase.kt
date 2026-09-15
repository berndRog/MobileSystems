package de.rogallab.mobile.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.local.dtos.ArticleDto

@Database(
   entities = [ArticleDto::class],
   version = Globals.databaseVersion,
   exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
   abstract fun createArticleDao(): IArticleDao
}
