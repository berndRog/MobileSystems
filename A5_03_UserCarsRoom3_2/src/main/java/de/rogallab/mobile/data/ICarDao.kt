package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.readmodels.CarListRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ICarDao {

   // ─────────────────────────────────────────────────────────────
   // Read side: used by CarReadModel
   // ─────────────────────────────────────────────────────────────
   // CarsScreen: all cars including the seller's full name.
   // LEFT JOIN keeps cars without a seller in the result.
   // The Flow observes both tables, Car and Person.
   @Query("""
      SELECT Car.*,
             TRIM(Person.firstName || ' ' || Person.lastName) AS sellerFullName
      FROM Car
      LEFT JOIN Person ON Person.id = Car.personId
      ORDER BY Car.manufacturer, Car.model
   """)
   fun observeAllWithSeller(): Flow<List<CarListRow>>

   // TDriveScreen: all cars for the car selection field.
   @Query("""SELECT * FROM Car ORDER BY manufacturer, model""")
   fun observeAll(): Flow<List<CarDto>>

   // PersonScreen (bottom sheet): cars offered by one seller.
   @Query("""
      SELECT *
      FROM Car
      WHERE personId = :personId
      ORDER BY manufacturer, model
   """)
   suspend fun findByPersonId(personId: String): List<CarDto>

   // ─────────────────────────────────────────────────────────────
   // Write side: used by CarRepository
   // ─────────────────────────────────────────────────────────────
   // Loads an aggregate for editing or deleting.
   @Query("""SELECT * FROM Car WHERE id = :carId LIMIT 1""")
   suspend fun findById(carId: String): CarDto?

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(carDto: CarDto): Long

   @Update
   suspend fun update(carDto: CarDto): Int

   @Delete
   suspend fun delete(carDto: CarDto): Int


   // ─────────────────────────────────────────────────────────────
   // Infrastructure: used by SeedDatabase and tests
   // ─────────────────────────────────────────────────────────────
   @Query("SELECT COUNT(*) FROM Car")
   suspend fun count(): Int

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(carDtos: List<CarDto>)
}
