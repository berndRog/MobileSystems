package de.rogallab.mobile.data.local.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.shared.data.local.database.InstantConverters
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

// Room commits the schema, seed data and database version in one creation transaction.
class SeedDatabase(
   private val _seed: Seed
) : RoomDatabase.Callback() {
   override suspend fun onCreate(connection: SQLiteConnection) {
      // DAOs would recursively open this database. Use Room's callback connection instead.
      connection.prepare(
         "INSERT INTO Person (id, firstName, lastName, email, phone, imagePath) VALUES (?, ?, ?, ?, ?, ?)"
      ).use { statement ->
         _seed.createPersonDtos().forEach { person ->
            coroutineContext.ensureActive()
            statement.bindText(1, person.id)
            statement.bindText(2, person.firstName)
            statement.bindText(3, person.lastName)
            statement.bindNullableText(4, person.email)
            statement.bindNullableText(5, person.phone)
            statement.bindNullableText(6, person.imagePath)
            statement.step()
            statement.reset()
         }
      }
      connection.prepare(
         "INSERT INTO Car (id, manufacturer, model, price, imagePaths, personId) VALUES (?, ?, ?, ?, ?, ?)"
      ).use { statement ->
         _seed.createCarDtos().forEach { car ->
            coroutineContext.ensureActive()
            statement.bindText(1, car.id)
            statement.bindText(2, car.manufacturer)
            statement.bindText(3, car.model)
            if (car.price == null) statement.bindNull(4)
            else statement.bindLong(4, car.price.toLong())
            statement.bindText(5, StringListColumnConverter.toJson(car.imagePaths))
            statement.bindNullableText(6, car.personId)
            statement.step()
            statement.reset()
         }
      }
      connection.prepare(
         "INSERT INTO TDrive (id, personId, carId, start, isCompleted) VALUES (?, ?, ?, ?, ?)"
      ).use { statement ->
         _seed.createsTestDriveDtos().forEach { tDrive ->
            coroutineContext.ensureActive()
            statement.bindText(1, tDrive.id)
            statement.bindText(2, tDrive.personId)
            statement.bindText(3, tDrive.carId)
            statement.bindText(4, requireNotNull(InstantConverters.instantToIsoString(tDrive.start)))
            statement.bindLong(5, if (tDrive.isCompleted) 1L else 0L)
            statement.step()
            statement.reset()
         }
      }

      // Room enables foreign keys in onOpen, after the creation transaction.
      connection.prepare("PRAGMA foreign_key_check").use { statement ->
         check(!statement.step()) { "Seed data contains invalid foreign key references" }
      }
      coroutineContext.ensureActive()
   }

   private fun SQLiteStatement.bindNullableText(index: Int, value: String?) {
      if (value == null) bindNull(index) else bindText(index, value)
   }
}
