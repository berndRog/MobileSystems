package de.rogallab.mobile.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto

@Database(
   entities = [PersonDto::class],
   version = Globals.databaseVersion,
   exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
   abstract fun createPersonDao(): IPersonDao
}
