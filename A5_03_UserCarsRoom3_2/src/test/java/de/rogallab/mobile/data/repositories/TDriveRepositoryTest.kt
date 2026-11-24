package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TDriveRepositoryTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val entity = TDrive(id = "t1", personId = "p1", carId = "c1", start = Instant.parse("2026-01-01T12:00:00Z"), isCompleted = false)

   @Before
   fun setUp() {
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun create_convertsRowIdToUnitAndMapsEntity() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeTDriveDao()

      assertEquals(Unit, TDriveRepository(dao).create(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun update_convertsOneAffectedRowToUnit() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeTDriveDao()

      assertEquals(Unit, TDriveRepository(dao).update(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun remove_convertsOneAffectedRowToUnit() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeTDriveDao()

      assertEquals(Unit, TDriveRepository(dao).remove(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest(mainDispatcherRule.testDispatcher) {
      for (rows in listOf(-1, 0, 2)) {
         val dao = FakeTDriveDao().apply {
            updatedRows = rows
            deletedRows = rows
         }
         val repository = TDriveRepository(dao)

         assertTrue(repository.update(entity).exceptionOrNull() is IllegalStateException)
         assertTrue(repository.remove(entity).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun writeOperations_returnDaoExceptionsAsFailures() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeTDriveDao().apply {
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
   fun writeOperations_rethrowCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val cancellation = CancellationException("write cancelled")
      val dao = FakeTDriveDao().apply {
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
   fun create_invalidRowId_returnsFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = object : ITDriveDao by FakeTDriveDao() {
         override suspend fun insert(tDriveDto: TDriveDto): Long = -1L
      }
      assertTrue(TDriveRepository(dao).create(entity).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest(mainDispatcherRule.testDispatcher) {
      assertEquals(null, TDriveRepository(FakeTDriveDao()).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ITDriveDao by FakeTDriveDao() {
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

}
