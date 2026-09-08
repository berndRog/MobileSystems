package de.rogallab.mobile.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto

@Database(
   entities = [PersonDto::class],
   version = Globals.databaseVersion,
   exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
   abstract fun personDao(): IPersonDao
}

/*
 * Didaktik und Lernziele
 *
 * - AppDatabase gehört bewusst zu A5_01 und nicht zu Shared. Das Beispiel zeigt
 *   damit die vollständige Room-Schicht innerhalb eines konkreten Projekts.
 *
 * - Room erzeugt die konkrete Implementierung von AppDatabase und IPersonDao
 *   zur Compile-Zeit über KSP.
 */
