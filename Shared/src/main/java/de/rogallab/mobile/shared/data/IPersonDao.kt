package de.rogallab.mobile.shared.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.shared.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IPersonDao {
   // QUERIES ---------------------------------------------
   @Query("SELECT * FROM Person ORDER BY firstName")
   fun observeAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :personId")
   suspend fun findById(personId: String): PersonDto?

   @Query("SELECT COUNT(*) FROM Person")
   suspend fun count(): Int

   // COMMANDS --------------------------------------------
   @Insert(onConflict = OnConflictStrategy.Companion.ABORT)
   suspend fun insert(personDto: PersonDto)

   @Update
   suspend fun update(personDto: PersonDto)

   @Delete
   suspend fun delete(personDto: PersonDto)
}