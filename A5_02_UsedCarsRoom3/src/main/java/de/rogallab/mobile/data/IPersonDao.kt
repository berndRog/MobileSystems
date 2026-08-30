package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.relations.PersonWithCarsDto
import de.rogallab.mobile.data.local.relations.PersonWithTestDriveCarsDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IPersonDao {
   @Query("SELECT * FROM Person ORDER BY lastName, firstName")
   fun observeAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findById(personId: String): PersonDto?

   @Transaction
   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findWithCars(personId: String): PersonWithCarsDto?

   @Transaction
   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findWithTestDriveCars(personId: String): PersonWithTestDriveCarsDto?

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
