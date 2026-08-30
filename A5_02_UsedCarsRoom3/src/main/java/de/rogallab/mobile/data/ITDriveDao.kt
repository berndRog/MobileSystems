package de.rogallab.mobile.data

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import de.rogallab.mobile.data.local.dtos.TDriveDto
import kotlinx.coroutines.flow.Flow

@Dao
interface ITDriveDao {
   @Query("SELECT * FROM TDrive ORDER BY isCompleted, start")
   fun observeAll(): Flow<List<TDriveDto>>
   @Query("SELECT * FROM TDrive WHERE id = :testDriveId LIMIT 1")
   suspend fun findById(testDriveId: String): TDriveDto?
   @Query("SELECT COUNT(*) FROM TDrive")
   suspend fun count(): Int
   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(tDriveDto: TDriveDto)
   @Insert(onConflict = OnConflictStrategy.ABORT)
   suspend fun insert(tDriveDtos: List<TDriveDto>)
   @Update
   suspend fun update(tDriveDto: TDriveDto): Int
   @Delete
   suspend fun delete(tDriveDto: TDriveDto): Int
}
