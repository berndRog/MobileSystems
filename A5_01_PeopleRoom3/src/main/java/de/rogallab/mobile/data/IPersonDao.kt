package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.Flow

@Dao
interface IPersonDao {

   @Query("SELECT * FROM Person ORDER BY lastName, firstName")
   fun observeAll(): Flow<List<PersonDto>>

   @Query("SELECT * FROM Person WHERE id = :id LIMIT 1")
   suspend fun findById(id: String): PersonDto?

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

/*
 * Didaktik und Lernziele
 *
 * - IPersonDao ist die lokale Room-3-Schnittstelle von A5_01. Observable Daten
 *   werden als Flow geliefert; einzelne Datenbankoperationen sind suspend.
 *
 * - Das DAO arbeitet ausschließlich mit PersonDto. Das Domain-Modell Person
 *   bleibt dadurch frei von Room-Annotationen.
 */
