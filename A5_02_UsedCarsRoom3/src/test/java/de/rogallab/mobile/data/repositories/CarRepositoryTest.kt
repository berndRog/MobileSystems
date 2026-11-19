package de.rogallab.mobile.data.repositories

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertTrue
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.domain.entities.Car
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

class CarRepositoryTest {
   private val entity = Car(id = "c1", manufacturer = "Ford", model = "Focus")

   @Before
   fun setUp() {
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

   private class FakeDao : ICarDao {
      var insertFailure: Throwable? = null
      var updateFailure: Throwable? = null
      var deleteFailure: Throwable? = null
      var updatedRows = 1
      var deletedRows = 1
      var written: CarDto? = null
      override fun observeAll(): Flow<List<CarDto>> = flowOf(emptyList())
      override suspend fun findById(carId: String): CarDto? = null
      override suspend fun findByPersonId(personId: String): List<CarDto> = emptyList()
      override suspend fun count(): Int = 0
      override suspend fun insert(carDto: CarDto): Long {
         insertFailure?.let { throw it }
         written = carDto
         return 42L
      }
      override suspend fun insert(carDtos: List<CarDto>) {
         carDtos.forEach { insert(it) }
      }
      override suspend fun update(carDto: CarDto): Int {
         updateFailure?.let { throw it }
         written = carDto
         return updatedRows
      }
      override suspend fun delete(carDto: CarDto): Int {
         deleteFailure?.let { throw it }
         written = carDto
         return deletedRows
      }
   }

   @Test
   fun create_convertsRowIdToUnitAndMapsEntity() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao).create(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun update_convertsOneAffectedRowToUnit() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao).update(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun remove_convertsOneAffectedRowToUnit() = runTest {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao).remove(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest {
      for (rows in listOf(-1, 0, 2)) {
         val dao = FakeDao().apply {
            updatedRows = rows
            deletedRows = rows
         }
         val repository = CarRepository(dao)

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
      val repository = CarRepository(dao)

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
      val repository = CarRepository(dao)
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
      val dao = object : ICarDao by FakeDao() {
         override suspend fun insert(carDto: CarDto): Long = -1L
      }
      assertTrue(CarRepository(dao).create(entity).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest {
      assertEquals(null, CarRepository(FakeDao()).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ICarDao by FakeDao() {
            override suspend fun findById(carId: String): CarDto? = throw throwable
         }
         val repository = CarRepository(dao)
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
         val dao = object : ICarDao by FakeDao() {
            override fun observeAll() = flow<List<CarDto>> { throw throwable }
         }
         val repository = CarRepository(dao)
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
