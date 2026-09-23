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
   // Room provides the generated DAO implementation through the database.
   abstract fun createArticleDao(): IArticleDao
}

/*
 * Didaktik und Lernziele
 *
 * - AppDatabase beschreibt die lokale Room-Datenbank dieses Moduls mit ihrer
 *   Entity und stellt den Einstieg zum generierten Article-DAO bereit.
 *
 * Lernziele:
 *
 * - Entity, Datenbankversion und DAO in einer RoomDatabase zusammenführen.
 * - Generierte Room-Implementierungen über abstrakte Verträge beziehen.
 */
