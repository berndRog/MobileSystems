package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.CarDto
import kotlinx.coroutines.flow.Flow

@Dao
interface ICarDao {
   @Query("SELECT * FROM Car ORDER BY manufacturer, model")
   fun selectAll(): Flow<List<CarDto>>

   @Query("SELECT * FROM Car WHERE id = :carId LIMIT 1")
   suspend fun findById(carId: String): CarDto?

   @Query("SELECT COUNT(*) FROM Car")
   suspend fun count(): Int

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(carDto: CarDto)

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(carDtos: List<CarDto>)

   @Update
   suspend fun update(carDto: CarDto): Int

   @Delete
   suspend fun delete(carDto: CarDto): Int
}
