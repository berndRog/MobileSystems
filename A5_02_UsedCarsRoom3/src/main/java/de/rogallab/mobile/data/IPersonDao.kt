package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Transaction
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.relations.PersonWithCarsDto
import de.rogallab.mobile.data.local.relations.PersonWithTestDriveCarsDto
import kotlinx.coroutines.flow.Flow

// Coroutine-based Room-3 DAO.
// Observable queries return Flow; one-shot operations are suspend functions.
@Dao
interface IPersonDao {
   @Query("SELECT * FROM Person ORDER BY lastName, firstName")
   fun selectAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findById(personId: String): PersonDto?

   // Reads one person together with all cars offered by that person.
   @Transaction
   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findWithCars(personId: String): PersonWithCarsDto?

   // Reads the cars connected to a person through TDrive.
   @Transaction
   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findWithTestDriveCars(
      personId: String,
   ): PersonWithTestDriveCarsDto?

   @Query("SELECT COUNT(*) FROM Person")
   suspend fun count(): Int

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(personDto: PersonDto)

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(personDtos: List<PersonDto>)

   @Update
   suspend fun update(personDto: PersonDto): Int

   @Delete
   suspend fun delete(personDto: PersonDto): Int
}
