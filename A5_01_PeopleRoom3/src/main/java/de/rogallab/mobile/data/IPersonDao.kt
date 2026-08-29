package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.Flow

/**
 * Coroutine-based Room-3 DAO.
 *
 * Queries that represent observable UI data return Flow. One-shot operations
 * are suspend functions, as required by the Room-3 coroutine API.
 */
@Dao
interface IPersonDao {
   @Query("SELECT * FROM Person ORDER BY lastName, firstName")
   fun selectAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :personId LIMIT 1")
   suspend fun findById(personId: String): PersonDto?

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
