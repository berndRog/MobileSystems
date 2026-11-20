package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Rule
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonRepositoryTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val person = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")

   private class FakeDao : IPersonDao {
      var insertFailure: Throwable? = null
      var insertRowId = 42L
      var updatedRows = 1
      var deletedRows = 1
      var insertedPerson: PersonDto? = null
      var updateFailure: Throwable? = null
      var deleteFailure: Throwable? = null
      var updateCount = 0
      var deleteCount = 0
      var observation: Flow<List<PersonDto>> = flowOf(emptyList())
      override fun observeAll(): Flow<List<PersonDto>> = observation
      override suspend fun findById(id: String): PersonDto? = null
      override suspend fun count(): Int = 0
      override suspend fun insert(personDto: PersonDto): Long {
         insertFailure?.let { throw it }
         insertedPerson = personDto
         return insertRowId
      }
      override suspend fun insert(personDtos: List<PersonDto>) = Unit
      override suspend fun update(personDto: PersonDto): Int {
         updateFailure?.let { throw it }
         updateCount++
         return updatedRows
      }
      override suspend fun delete(personDto: PersonDto): Int {
         deleteFailure?.let { throw it }
         deleteCount++
         return deletedRows
      }
   }

   @Test
   fun updateAndRemove_propagateDaoFailuresAsResults() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertSame(dao.updateFailure, repository.update(person).exceptionOrNull())
      assertSame(dao.deleteFailure, repository.remove(person).exceptionOrNull())

      dao.updateFailure = null
      dao.deleteFailure = null
      assertEquals(Unit, repository.update(person).getOrThrow())
      assertEquals(Unit, repository.remove(person).getOrThrow())
      assertTrue(dao.updateCount == 1)
      assertTrue(dao.deleteCount == 1)
   }

   @Test
   fun create_convertsRowIdToUnitAndMapsPerson() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val result: Result<Unit> = PersonRepository(dao, mainDispatcherRule.testDispatcher).create(person)

      assertEquals(Unit, result.getOrThrow())
      assertEquals(person.id, dao.insertedPerson?.id)
      assertEquals(person.firstName, dao.insertedPerson?.firstName)
      assertEquals(person.lastName, dao.insertedPerson?.lastName)
   }

   @Test
   fun create_propagatesInsertFailureAsResult() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val failure = IllegalStateException("duplicate primary key")
      dao.insertFailure = failure

      assertSame(failure, PersonRepository(dao, mainDispatcherRule.testDispatcher).create(person).exceptionOrNull())
   }

   @Test
   fun updateAndRemove_withNoMatchingRows_returnFailures() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      dao.updatedRows = 0
      dao.deletedRows = 0
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)

      val updateResult = repository.update(person)
      val removeResult = repository.remove(person)

      assertTrue(updateResult.isFailure)
      assertTrue(removeResult.isFailure)
      assertTrue(updateResult.exceptionOrNull() is IllegalStateException)
      assertTrue(removeResult.exceptionOrNull() is IllegalStateException)
      assertEquals("Update failed: Expected 1 row, got 0", updateResult.exceptionOrNull()?.message)
      assertEquals("Delete failed: Expected 1 row, got 0", removeResult.exceptionOrNull()?.message)
      assertEquals(1, dao.updateCount)
      assertEquals(1, dao.deleteCount)
   }

   @Test
   fun create_rethrowsCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val cancellation = CancellationException("insert cancelled")
      dao.insertFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao, mainDispatcherRule.testDispatcher).create(person) }
   }

   @Test
   fun update_rethrowsCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val cancellation = CancellationException("update cancelled")
      dao.updateFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao, mainDispatcherRule.testDispatcher).update(person) }
   }

   @Test
   fun remove_rethrowsCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakeDao()
      val cancellation = CancellationException("delete cancelled")
      dao.deleteFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao, mainDispatcherRule.testDispatcher).remove(person) }
   }

   @Test
   fun observeAll_mapsPeopleOnInjectedDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      var upstreamDispatcher: ContinuationInterceptor? = null
      val dao = FakeDao().apply {
         observation = flow {
            upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            emit(listOf(person.toPersonDto()))
         }
      }

      val result = PersonRepository(dao, dispatcherIo).observeAll().first()

      assertEquals(listOf(person), result.getOrThrow())
      assertSame(dispatcherIo, upstreamDispatcher)
   }

   @Test
   fun observeAll_convertsDaoExceptionToFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("observation failed")
      val dao = FakeDao().apply { observation = flow { throw failure } }

      val result = PersonRepository(dao, mainDispatcherRule.testDispatcher).observeAll().first()

      assertSame(failure, result.exceptionOrNull())
   }

   @Test
   fun observeAll_rethrowsCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val cancellation = CancellationException("observation cancelled")
      val dao = FakeDao().apply { observation = flow { throw cancellation } }

      assertCancellation(cancellation) {
         PersonRepository(dao, mainDispatcherRule.testDispatcher).observeAll().first()
      }
   }

   @Test
   fun observeAll_canCollectAgainAfterFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("first collection failed")
      var collections = 0
      val dao = FakeDao().apply {
         observation = flow {
            collections++
            if (collections == 1) throw failure
            emit(listOf(person.toPersonDto()))
         }
      }
      val observation = PersonRepository(dao, mainDispatcherRule.testDispatcher).observeAll()

      assertSame(failure, observation.first().exceptionOrNull())
      assertEquals(listOf(person), observation.first().getOrThrow())
      assertEquals(2, collections)
   }

   @Test
   fun observeAll_suppressesUnchangedLists() = runTest(mainDispatcherRule.testDispatcher) {
      val changed = person.copy(firstName = "Augusta")
      val dao = FakeDao().apply {
         observation = flowOf(
            listOf(person.toPersonDto()),
            listOf(person.toPersonDto()),
            listOf(changed.toPersonDto()),
         )
      }

      val results = PersonRepository(dao, mainDispatcherRule.testDispatcher).observeAll().toList()

      assertEquals(listOf(listOf(person), listOf(changed)), results.map { it.getOrThrow() })
   }

   @Test
   fun observeAll_collectorCancellation_cancelsUpstream() = runTest(mainDispatcherRule.testDispatcher) {
      var upstreamCancelled = false
      val dao = FakeDao().apply {
         observation = flow {
            try {
               emit(listOf(person.toPersonDto()))
               awaitCancellation()
            } finally {
               upstreamCancelled = true
            }
         }
      }
      val results = mutableListOf<Result<List<Person>>>()
      val repository = PersonRepository(dao, mainDispatcherRule.testDispatcher)
      val collector = backgroundScope.launch { repository.observeAll().collect { results += it } }
      runCurrent()

      collector.cancelAndJoin()

      assertTrue(upstreamCancelled)
      assertEquals(listOf(listOf(person)), results.map { it.getOrThrow() })
   }

   @Test
   fun appModule_reusesDispatcherFromPlainAndCombinedContexts() = runTest(mainDispatcherRule.testDispatcher) {
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      val contexts = listOf<CoroutineContext>(
         dispatcherIo,
         dispatcherIo + CoroutineName("Room queries"),
      )
      for (context in contexts) {
         var upstreamDispatcher: ContinuationInterceptor? = null
         val dao = FakeDao().apply {
            observation = flow {
               upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
               emit(listOf(person.toPersonDto()))
            }
         }
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
            assertEquals(listOf(person), repository.observeAll().first().getOrThrow())
            assertSame(dispatcherIo, upstreamDispatcher)
         } finally {
            application.close()
         }
      }
   }

   private suspend fun <T> assertCancellation(
      expected: CancellationException,
      block: suspend () -> T,
   ) {
      try {
         block()
         fail("CancellationException must propagate")
      } catch (actual: CancellationException) {
         assertSame(expected, actual)
      }
   }
}
