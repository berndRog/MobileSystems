package de.rogallab.mobile.shared.data.local.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.rogallab.mobile.shared.data.local.IPersonDao
import de.rogallab.mobile.shared.data.local.dtos.PersonDto

@Database(
   entities = [
      PersonDto::class
   ],
   version = 1,
   exportSchema = false
)

@ColumnTypeConverters(
   InstantConverters::class
)
abstract class AppDatabasePerson : RoomDatabase() {
   abstract fun createPersonDao(): IPersonDao
}
