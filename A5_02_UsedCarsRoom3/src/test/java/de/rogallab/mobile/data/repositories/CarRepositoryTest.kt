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

import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Rule
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class CarRepositoryTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

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
   fun create_convertsRowIdToUnitAndMapsEntity() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao, mainDispatcherRule.testDispatcher).create(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun update_convertsOneAffectedRowToUnit() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao, mainDispatcherRule.testDispatcher).update(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun remove_convertsOneAffectedRowToUnit() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()

      assertEquals(Unit, CarRepository(dao, mainDispatcherRule.testDispatcher).remove(entity).getOrThrow())
      assertEquals(entity.id, dao.written?.id)
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest(mainDispatcherRule.testDispatcher) {
      for (rows in listOf(-1, 0, 2)) {
         val dao = FakeDao().apply {
            updatedRows = rows
            deletedRows = rows
         }
         val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)

         assertTrue(repository.update(entity).exceptionOrNull() is IllegalStateException)
         assertTrue(repository.remove(entity).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun writeOperations_returnDaoExceptionsAsFailures() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao().apply {
         insertFailure = IllegalStateException("insert failed")
         updateFailure = IllegalStateException("update failed")
         deleteFailure = IllegalStateException("delete failed")
      }
      val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)

      assertSame(dao.insertFailure, repository.create(entity).exceptionOrNull())
      assertSame(dao.updateFailure, repository.update(entity).exceptionOrNull())
      assertSame(dao.deleteFailure, repository.remove(entity).exceptionOrNull())
   }

   @Test
   fun writeOperations_rethrowCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val cancellation = CancellationException("write cancelled")
      val dao = FakeDao().apply {
         insertFailure = cancellation
         updateFailure = cancellation
         deleteFailure = cancellation
      }
      val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)
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
      val dao = object : ICarDao by FakeDao() {
         override suspend fun insert(carDto: CarDto): Long = -1L
      }
      assertTrue(CarRepository(dao, mainDispatcherRule.testDispatcher).create(entity).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest(mainDispatcherRule.testDispatcher) {
      assertEquals(null, CarRepository(FakeDao(), mainDispatcherRule.testDispatcher).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ICarDao by FakeDao() {
            override suspend fun findById(carId: String): CarDto? = throw throwable
         }
         val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)
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
   fun observeAll_handlesExceptionsAndPropagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("observe failed")
      val cancellation = CancellationException("observe cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ICarDao by FakeDao() {
            override fun observeAll() = flow<List<CarDto>> { throw throwable }
         }
         val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)
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
   private fun observing(observation: Flow<List<CarDto>>): ICarDao =
      object : ICarDao by FakeDao() {
         override fun observeAll(): Flow<List<CarDto>> = observation
      }

   @Test
   fun observeAll_mapsEntitiesOnInjectedDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
      val value = entity
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      var upstreamDispatcher: ContinuationInterceptor? = null
      val dao = observing(flow {
         upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
         emit(listOf(value.toCarDto()))
      })

      val result = CarRepository(dao, dispatcherIo).observeAll().first()

      assertEquals(listOf(value), result.getOrThrow())
      assertSame(dispatcherIo, upstreamDispatcher)
   }

   @Test
   fun observeAll_canCollectAgainAfterFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val value = entity
      val failure = IllegalStateException("first collection failed")
      var collections = 0
      val dao = observing(flow {
         collections++
         if (collections == 1) throw failure
         emit(listOf(value.toCarDto()))
      })
      val observation = CarRepository(dao, mainDispatcherRule.testDispatcher).observeAll()

      assertSame(failure, observation.first().exceptionOrNull())
      assertEquals(listOf(value), observation.first().getOrThrow())
      assertEquals(2, collections)
   }

   @Test
   fun observeAll_collectorCancellation_cancelsUpstream() = runTest(mainDispatcherRule.testDispatcher) {
      val value = entity
      var upstreamCancelled = false
      val dao = observing(flow {
         try {
            emit(listOf(value.toCarDto()))
            awaitCancellation()
         } finally {
            upstreamCancelled = true
         }
      })
      val results = mutableListOf<Result<List<Car>>>()
      val repository = CarRepository(dao, mainDispatcherRule.testDispatcher)
      val collector = backgroundScope.launch { repository.observeAll().collect { results += it } }
      runCurrent()

      collector.cancelAndJoin()

      assertTrue(upstreamCancelled)
      assertEquals(listOf(listOf(value)), results.map { it.getOrThrow() })
   }

   @Test
   fun appModule_reusesDispatcherFromPlainAndCombinedContexts() = runTest(mainDispatcherRule.testDispatcher) {
      val value = entity
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      val contexts = listOf<CoroutineContext>(
         dispatcherIo,
         dispatcherIo + CoroutineName("Room queries"),
      )
      for (context in contexts) {
         var upstreamDispatcher: ContinuationInterceptor? = null
         val dao = observing(flow {
            upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            emit(listOf(value.toCarDto()))
         })
         val application = koinApplication {
            modules(
               appModule(dispatcherIo = context),
               module { single<ICarDao> { dao } },
            )
         }
         try {
            assertSame(context, application.koin.get<CoroutineContext>(named("dispatcherIo")))
            assertSame(dispatcherIo, application.koin.get<CoroutineDispatcher>(named("dispatcherIo")))
            val repository = application.koin.get<ICarRepository>()
            assertEquals(listOf(value), repository.observeAll().first().getOrThrow())
            assertSame(dispatcherIo, upstreamDispatcher)
         } finally {
            application.close()
         }
      }
   }

}