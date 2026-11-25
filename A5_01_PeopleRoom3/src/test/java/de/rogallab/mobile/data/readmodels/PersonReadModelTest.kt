package de.rogallab.mobile.data.readmodels

import app.cash.turbine.test
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.repositories.FakePersonDao
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class PersonReadModelTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val ada = PersonDto(
      id = "p1",
      firstName = "Ada",
      lastName = "Lovelace",
      email = "ada@example.org",
      phone = "+44 1234",
      imagePath = "/images/ada.jpg",
   )
   private val grace = PersonDto("p2", "Grace", "Hopper", null, null, null)
   // Independent expectations: do not use the production mapper to construct them.
   private val adaItem = PersonListItem("p1", "Ada Lovelace", "ada@example.org", "+44 1234", "/images/ada.jpg")
   private val graceItem = PersonListItem("p2", "Grace Hopper", null, null, null)

   private fun observing(observation: Flow<List<PersonDto>>): IPersonDao =
      object : IPersonDao by FakePersonDao() {
         override fun observeAll(): Flow<List<PersonDto>> = observation
      }

   @Test
   fun observeListItems_mapsAllFieldsAndPreservesNulls() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = observing(flow { emit(listOf(ada, grace)) })
      val readModel = PersonReadModel(dao, mainDispatcherRule.testDispatcher)

      assertEquals(listOf(adaItem, graceItem), readModel.observeListItems().first().getOrThrow())
   }

   @Test
   fun observeListItems_emitsEmptyAndUpdatedListsInDaoOrder() = runTest(mainDispatcherRule.testDispatcher) {
      val dao = FakePersonDao()
      val readModel = PersonReadModel(dao, mainDispatcherRule.testDispatcher)

      readModel.observeListItems().test {
         assertEquals(emptyList<PersonListItem>(), awaitItem().getOrThrow())
         dao.insert(grace)
         assertEquals(listOf(graceItem), awaitItem().getOrThrow())
         dao.insert(ada)
         assertEquals(listOf(graceItem, adaItem), awaitItem().getOrThrow())
         dao.update(ada.copy(lastName = "Byron"))
         assertEquals(listOf(graceItem, adaItem.copy(fullName = "Ada Byron")), awaitItem().getOrThrow())
         dao.delete(grace)
         assertEquals(listOf(adaItem.copy(fullName = "Ada Byron")), awaitItem().getOrThrow())
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun observeListItems_returnsUpstreamFailureAndCompletes() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("observe failed")
      val dao = observing(flow {
         emit(listOf(ada))
         throw failure
      })

      PersonReadModel(dao, mainDispatcherRule.testDispatcher).observeListItems().test {
         assertEquals(listOf(adaItem), awaitItem().getOrThrow())
         val actual = awaitItem().exceptionOrNull()
         assertTrue(actual is IllegalStateException)
         assertEquals(failure.message, actual?.message)
         // Coroutine stack-trace recovery may copy the exception and retain its cause.
         assertTrue(actual === failure || actual?.cause === failure)
         awaitComplete()
      }
   }

   @Test
   fun observeListItems_propagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val cancellation = CancellationException("observe cancelled")
      val dao = observing(flow { throw cancellation })

      try {
         PersonReadModel(dao, mainDispatcherRule.testDispatcher).observeListItems().first()
         fail("CancellationException must propagate")
      } catch (actual: CancellationException) {
         assertSame(cancellation, actual)
      }
   }

   @Test
   fun observeListItems_canCollectAgainAfterFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("first collection failed")
      var collections = 0
      val dao = observing(flow {
         collections++
         if (collections == 1) throw failure
         emit(listOf(ada))
      })
      val observation = PersonReadModel(dao, mainDispatcherRule.testDispatcher).observeListItems()

      assertSame(failure, observation.first().exceptionOrNull())
      assertEquals(listOf(adaItem), observation.first().getOrThrow())
      assertEquals(2, collections)
   }

   @Test
   fun observeListItems_collectorCancellation_cancelsUpstream() = runTest(mainDispatcherRule.testDispatcher) {
      var upstreamCancelled = false
      val dao = observing(flow {
         try {
            emit(listOf(ada))
            awaitCancellation()
         } finally {
            upstreamCancelled = true
         }
      })
      val results = mutableListOf<Result<List<PersonListItem>>>()
      val readModel = PersonReadModel(dao, mainDispatcherRule.testDispatcher)
      val collector = backgroundScope.launch { readModel.observeListItems().collect { results += it } }
      runCurrent()

      collector.cancelAndJoin()

      assertTrue(upstreamCancelled)
      assertEquals(listOf(listOf(adaItem)), results.map { it.getOrThrow() })
   }

   @Test
   fun observeListItems_usesInjectedDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "read model IO")
      var upstreamDispatcher: ContinuationInterceptor? = null
      val dao = observing(flow {
         upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
         emit(listOf(ada))
      })

      assertEquals(listOf(adaItem), PersonReadModel(dao, dispatcherIo).observeListItems().first().getOrThrow())
      assertSame(dispatcherIo, upstreamDispatcher)
   }

   @Test
   fun appModule_resolvesReadModelWithDaoAndDispatcherFromPlainAndCombinedContexts() =
      runTest(mainDispatcherRule.testDispatcher) {
         val dispatcherIo = StandardTestDispatcher(testScheduler, name = "read model IO")
         val contexts = listOf<CoroutineContext>(dispatcherIo, dispatcherIo + CoroutineName("Room queries"))
         for (context in contexts) {
            var upstreamDispatcher: ContinuationInterceptor? = null
            val dao = observing(flow {
               upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
               emit(listOf(ada))
            })
            val application = koinApplication {
               modules(appModule(dispatcherIo = context), module { single<IPersonDao> { dao } })
            }
            try {
               assertSame(context, application.koin.get<CoroutineContext>(named("dispatcherIo")))
               assertSame(dispatcherIo, application.koin.get<CoroutineDispatcher>(named("dispatcherIo")))
               val readModel = application.koin.get<IPersonReadModel>()
               assertSame(readModel, application.koin.get<IPersonReadModel>())
               assertEquals(listOf(adaItem), readModel.observeListItems().first().getOrThrow())
               assertSame(dispatcherIo, upstreamDispatcher)
            } finally {
               application.close()
            }
         }
      }
}
