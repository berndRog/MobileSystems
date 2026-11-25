package de.rogallab.mobile.data.local

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
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
      coroutineContext.ensureActive()
   }

   private fun SQLiteStatement.bindNullableText(index: Int, value: String?) {
      if (value == null) bindNull(index) else bindText(index, value)
   }
}
