package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Before
import org.junit.Test

import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.repositories.IPersonRepository
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
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalCoroutinesApi::class)
class PersonRepositoryTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()


   @Before
   fun setup() {
      // Local JVM tests must not call the android.util.Log stub.
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun create_emitsPersistedPerson() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = PersonRepository(FakePersonDao(), mainDispatcherRule.testDispatcher)
      val person = Person(
         id = "p-1",
         firstName = "Ada",
         lastName = "Lovelace",
         email = "ada.lovelace@example.org",
         phone = "+44 20 7946 0101",
         imagePath = "/images/ada.jpg",
      )

      repository.observeAll().test {
         assertTrue(awaitItem().getOrThrow().isEmpty())

         repository.create(person).getOrThrow()

         assertEquals(
            listOf(person),
            awaitItem().getOrThrow()
         )

         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun updateAndRemove_returnDaoFailuresAsResults() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakePersonDao()
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
      val person = Person(
         id = "p-1",
         firstName = "Ada",
         lastName = "Lovelace"
      )

      assertEquals(Unit, repository.create(person).getOrThrow())
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertTrue(repository.update(person).isFailure)
      assertTrue(repository.remove(person).isFailure)
      assertEquals(person, repository.findById(person.id).getOrThrow())

      dao.updateFailure = null
      dao.deleteFailure = null
      assertEquals(Unit, repository.update(person).getOrThrow())
      assertEquals(Unit, repository.remove(person).getOrThrow())
      assertEquals(null, repository.findById(person.id).getOrThrow())
   }

   @Test
   fun create_existingId_returnsFailureAndKeepsOriginal() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = PersonRepository(FakePersonDao(), mainDispatcherRule.testDispatcher)
      val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      assertEquals(Unit, repository.create(person).getOrThrow())

      assertTrue(repository.create(person.copy(firstName = "Changed")).isFailure)
      assertEquals(person, repository.findById(person.id).getOrThrow())
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(id = "missing")
      for (rows in listOf(-1, 0, 2)) {
         val dao = object : IPersonDao by FakePersonDao() {
            override suspend fun update(personDto: PersonDto): Int = rows
            override suspend fun delete(personDto: PersonDto): Int = rows
         }
         val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
         assertTrue(repository.update(person).exceptionOrNull() is IllegalStateException)
         assertTrue(repository.remove(person).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun writeOperations_rethrowCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val cancellation = CancellationException("write cancelled")
      val dao = FakePersonDao().apply {
         insertFailure = cancellation
         updateFailure = cancellation
         deleteFailure = cancellation
      }
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
      val person = Person(id = "p1")
      val operations: List<suspend () -> Result<Unit>> = listOf(
         { repository.create(person) },
         { repository.update(person) },
         { repository.remove(person) },
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
      val person = Person(id = "p1")
      val dao = object : IPersonDao by FakePersonDao() {
         override suspend fun insert(personDto: PersonDto): Long = -1L
      }
      assertTrue(PersonRepository(dao, mainDispatcherRule.testDispatcher).create(person).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest(mainDispatcherRule.testDispatcher) {
      assertEquals(null, PersonRepository(FakePersonDao(), mainDispatcherRule.testDispatcher).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : IPersonDao by FakePersonDao() {
            override suspend fun findById(personId: String): PersonDto? = throw throwable
         }
         val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
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
         val dao = object : IPersonDao by FakePersonDao() {
            override fun observeAll() = flow<List<PersonDto>> { throw throwable }
         }
         val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
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
   private fun observing(observation: Flow<List<PersonDto>>): IPersonDao =
      object : IPersonDao by FakePersonDao() {
         override fun observeAll(): Flow<List<PersonDto>> = observation
      }

   @Test
   fun observeAll_mapsEntitiesOnInjectedDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
      val value = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      var upstreamDispatcher: ContinuationInterceptor? = null
      val dao = observing(flow {
         upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
         emit(listOf(value.toPersonDto()))
      })

      val result = PersonRepository(dao, dispatcherIo).observeAll().first()

      assertEquals(listOf(value), result.getOrThrow())
      assertSame(dispatcherIo, upstreamDispatcher)
   }

   @Test
   fun observeAll_canCollectAgainAfterFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val value = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      val failure = IllegalStateException("first collection failed")
      var collections = 0
      val dao = observing(flow {
         collections++
         if (collections == 1) throw failure
         emit(listOf(value.toPersonDto()))
      })
      val observation = PersonRepository(dao, mainDispatcherRule.testDispatcher).observeAll()

      assertSame(failure, observation.first().exceptionOrNull())
      assertEquals(listOf(value), observation.first().getOrThrow())
      assertEquals(2, collections)
   }

   @Test
   fun observeAll_collectorCancellation_cancelsUpstream() = runTest(mainDispatcherRule.testDispatcher) {
      val value = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      var upstreamCancelled = false
      val dao = observing(flow {
         try {
            emit(listOf(value.toPersonDto()))
            awaitCancellation()
         } finally {
            upstreamCancelled = true
         }
      })
      val results = mutableListOf<Result<List<Person>>>()
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
      val collector = backgroundScope.launch { repository.observeAll().collect { results += it } }
      runCurrent()

      collector.cancelAndJoin()

      assertTrue(upstreamCancelled)
      assertEquals(listOf(listOf(value)), results.map { it.getOrThrow() })
   }

   @Test
   fun appModule_reusesDispatcherFromPlainAndCombinedContexts() = runTest(mainDispatcherRule.testDispatcher) {
      val value = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      val contexts = listOf<CoroutineContext>(
         dispatcherIo,
         dispatcherIo + CoroutineName("Room queries"),
      )
      for (context in contexts) {
         var upstreamDispatcher: ContinuationInterceptor? = null
         val dao = observing(flow {
            upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            emit(listOf(value.toPersonDto()))
         })
         val application = koinApplication {
            modules(
               appModule(dispatcherIo = context),
               module { single<IPersonDao> { dao } },
            )
         }
         try {
            assertSame(context, application.koin.get<CoroutineContext>(named("dispatcherIo")))
            assertSame(dispatcherIo, application.koin.get<CoroutineDispatcher>(named("dispatcherIo")))
            val repository = application.koin.get<IPersonRepository>()
            assertEquals(listOf(value), repository.observeAll().first().getOrThrow())
            assertSame(dispatcherIo, upstreamDispatcher)
         } finally {
            application.close()
         }
      }
   }

}