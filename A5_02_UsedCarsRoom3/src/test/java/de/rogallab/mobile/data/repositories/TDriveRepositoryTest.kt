package de.rogallab.mobile.data.repositories

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

class TDriveRepositoryTest {
   private val entity = TDrive(id = "t1", personId = "p1", carId = "c1", start = Instant.parse("2026-01-01T12:00:00Z"))

   @Before
   fun setUp() {
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

   private class FakeDao : ITDriveDao {
      var insertFailure: Throwable? = null
      var updateFailure: Throwable? = null
      var deleteFailure: Throwable? = null
      var updatedRows = 1
      var deletedRows = 1
      var written: TDriveDto? = null
      override fun observeAll(): Flow<List<TDriveDto>> = flowOf(emptyList())
      override suspend fun findById(testDriveId: String): TDriveDto? = null
      override suspend fun count(): Int = 0
      override suspend fun insert(tDriveDto: TDriveDto): Long {
         insertFailure?.let { throw it }
         written = tDriveDto
         return 42L
      }
      override suspend fun insert(tDriveDtos: List<TDriveDto>) {
         tDriveDtos.forEach { insert(it) }
      }
      override suspend fun update(tDriveDto: TDriveDto): Int {
         updateFailure?.let { throw it }
         written = tDriveDto
         return updatedRows
      }
      override suspend fun delete(tDriveDto: TDriveDto): Int {
         deleteFailure?.let { throw it }
         written = tDriveDto
         return deletedRows
      }
   }

   @Test
   fun create_convertsRowIdToUnitAndMapsEntity() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, TDriveRepository(dao).create(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun update_convertsOneAffectedRowToUnit() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, TDriveRepository(dao).update(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun remove_convertsOneAffectedRowToUnit() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, TDriveRepository(dao).remove(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest {
      for (rows in listOf(-1, 0, 2)) {
         val dao = FakeDao().apply {
            updatedRows = rows
            deletedRows = rows
         }
         val repository = TDriveRepository(dao)

         assertTrue(repository.update(entity).exceptionOrNull() is IllegalStateException)
         assertTrue(repository.remove(entity).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun writeOperations_returnDaoExceptionsAsFailures() = runTest {
      val dao = FakeDao().apply {
         insertFailure = IllegalStateException("insert failed")
         updateFailure = IllegalStateException("update failed")
         deleteFailure = IllegalStateException("delete failed")
      }
      val repository = TDriveRepository(dao)

      assertSame(dao.insertFailure, repository.create(entity).exceptionOrNull())
      assertSame(dao.updateFailure, repository.update(entity).exceptionOrNull())
      assertSame(dao.deleteFailure, repository.remove(entity).exceptionOrNull())
   }

   @Test
   fun writeOperations_rethrowCancellation() = runTest {
      val cancellation = CancellationException("write cancelled")
      val dao = FakeDao().apply {
         insertFailure = cancellation
         updateFailure = cancellation
         deleteFailure = cancellation
      }
      val repository = TDriveRepository(dao)
      val operations: List<suspend () -> Result<Unit>> = listOf(
         { repository.create(entity) },
         { repository.update(entity) },
         { repository.remove(entity) },
      )
      for (operation in operations) {
         try {
            operation()
            fail("CancellationException must propagate")
         } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
         }
      }
   }

   @Test
   fun create_invalidRowId_returnsFailure() = runTest {
      val dao = object : ITDriveDao by FakeDao() {
         override suspend fun insert(tDriveDto: TDriveDto): Long = -1L
      }
      assertTrue(TDriveRepository(dao).create(entity).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest {
      assertEquals(null, TDriveRepository(FakeDao()).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ITDriveDao by FakeDao() {
            override suspend fun findById(testDriveId: String): TDriveDto? = throw throwable
         }
         val repository = TDriveRepository(dao)
         if (throwable is CancellationException) {
            try {
               repository.findById("id")
               fail("CancellationException must propagate")
            } catch (actual: CancellationException) {
               assertSame(cancellation, actual)
            }
         } else {
            assertSame(failure, repository.findById("id").exceptionOrNull())
         }
      }
   }

   @Test
   fun observeAll_handlesExceptionsAndPropagatesCancellation() = runTest {
      val failure = IllegalStateException("observe failed")
      val cancellation = CancellationException("observe cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ITDriveDao by FakeDao() {
            override fun observeAll() = flow<List<TDriveDto>> { throw throwable }
         }
         val repository = TDriveRepository(dao)
         if (throwable is CancellationException) {
            try {
               repository.observeAll().first()
               fail("CancellationException must propagate")
            } catch (actual: CancellationException) {
               assertSame(cancellation, actual)
            }
         } else {
            assertSame(failure, repository.observeAll().first().exceptionOrNull())
         }
      }
   }
}
