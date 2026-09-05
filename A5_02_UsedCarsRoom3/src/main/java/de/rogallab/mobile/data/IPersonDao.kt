package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IPersonDao {
   @Query("SELECT * FROM Person ORDER BY lastName, firstName")
   fun observeAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findById(personId: String): PersonDto?

   // One-to-many relationship Person -> Cars.
   // The relationship is expressed directly in SQL and Room returns a multimap.
   @Query(
      """
      SELECT Person.*, Car.*
      FROM Person
      JOIN Car ON Car.sellerId = Person.id
      WHERE Person.id = :personId
      """
   )
   suspend fun findWithCars(personId: String): Map<PersonDto, List<CarDto>>

   // Many-to-many relationship Person <-> Car via the TDrive junction table.
   @Query(
      """
      SELECT Person.*, Car.*
      FROM Person
      JOIN TDrive ON TDrive.personId = Person.id
      JOIN Car ON Car.id = TDrive.carId
      WHERE Person.id = :personId
      """
   )
   suspend fun findWithTestDriveCars(personId: String): Map<PersonDto, List<CarDto>>

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
