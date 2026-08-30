package de.rogallab.mobile.data.local.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto

@Database(
   entities = [PersonDto::class, CarDto::class, TDriveDto::class],
   version = Globals.databaseVersion,
   exportSchema = false,
)
@ColumnTypeConverters(StringListColumnConverter::class)
abstract class AppDatabase : RoomDatabase() {
   abstract fun createPersonDao(): IPersonDao
   abstract fun createCarDao(): ICarDao
   abstract fun createTDriveDao(): ITDriveDao
}
