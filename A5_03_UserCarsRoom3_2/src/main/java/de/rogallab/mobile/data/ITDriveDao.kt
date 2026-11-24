package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.readmodels.TDriveListRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ITDriveDao {
   // ─────────────────────────────────────────────────────────────
   // Read side: used by TDriveReadModel
   // ─────────────────────────────────────────────────────────────

   // TDrivesScreen: all test drives including driver and car names.
   // INNER JOIN is sufficient: both foreign keys are required.
   // The Flow observes TDrive, Person and Car.
   @Query("""
      SELECT TDrive.*,
             TRIM(Person.firstName || ' ' || Person.lastName) AS driverFullName,
             TRIM(Car.manufacturer || ' ' || Car.model)       AS carDisplayName
      FROM TDrive
      JOIN Person ON Person.id = TDrive.personId
      JOIN Car    ON Car.id    = TDrive.carId
      ORDER BY TDrive.isCompleted, TDrive.start
   """)
   fun observeAllWithDriverAndCar(): Flow<List<TDriveListRow>>

   // ─────────────────────────────────────────────────────────────
   // Write side: used by TDriveRepository
   // ─────────────────────────────────────────────────────────────

   // Loads an aggregate for editing or deleting.
   @Query("""
      SELECT *
      FROM TDrive
      WHERE id = :testDriveId
      LIMIT 1
   """)
   suspend fun findById(testDriveId: String): TDriveDto?

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(tDriveDto: TDriveDto): Long

   @Update
   suspend fun update(tDriveDto: TDriveDto): Int

   @Delete
   suspend fun delete(tDriveDto: TDriveDto): Int

   // ─────────────────────────────────────────────────────────────
   // Infrastructure: used by SeedDatabase and tests
   // ─────────────────────────────────────────────────────────────

   @Query("SELECT COUNT(*) FROM TDrive")
   suspend fun count(): Int

   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(tDriveDtos: List<TDriveDto>)

}
