package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.repositories.FakeCarDao
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
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
class CarReadModelTest {
   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val row = CarListRow(
      CarDto("c1", "Ford", "Focus", 10000, listOf("/images/focus.jpg"), "p1"),
      "Ada Lovelace",
   )
   private val item = CarListItem("c1", "Ford Focus", "/images/focus.jpg", 10000, "Ada Lovelace")

   @Test
   fun observeListItems_handlesExceptionsAndPropagatesCancellation() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("observe failed")
      val cancellation = CancellationException("observe cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : ICarDao by FakeCarDao() {
            override fun observeAllWithSeller() = flow<List<CarListRow>> { throw throwable }
         }
         val readModel = CarReadModel(dao, mainDispatcherRule.testDispatcher)
         if (throwable is CancellationException) {
            try {
               readModel.observeListItems().first()
               fail("CancellationException must propagate")
            } catch (actual: CancellationException) {
               assertSame(cancellation, actual)
            }
         } else {
            assertSame(failure, readModel.observeListItems().first().exceptionOrNull())
         }
      }
   }
   private fun observing(observation: Flow<List<CarListRow>>): ICarDao =
      object : ICarDao by FakeCarDao() {
         override fun observeAllWithSeller(): Flow<List<CarListRow>> = observation
      }

   @Test
   fun observeListItems_mapsEntitiesOnInjectedDispatcher() = runTest(mainDispatcherRule.testDispatcher) {
      val dispatcherIo = StandardTestDispatcher(testScheduler, name = "repository IO")
      var upstreamDispatcher: ContinuationInterceptor? = null
      val dao = observing(flow {
         upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
         emit(listOf(row))
      })

      val result = CarReadModel(dao, dispatcherIo).observeListItems().first()

      assertEquals(listOf(item), result.getOrThrow())
      assertSame(dispatcherIo, upstreamDispatcher)
   }

   @Test
   fun observeListItems_canCollectAgainAfterFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val failure = IllegalStateException("first collection failed")
      var collections = 0
      val dao = observing(flow {
         collections++
         if (collections == 1) throw failure
         emit(listOf(row))
      })
      val observation = CarReadModel(dao, mainDispatcherRule.testDispatcher).observeListItems()

      assertSame(failure, observation.first().exceptionOrNull())
      assertEquals(listOf(item), observation.first().getOrThrow())
      assertEquals(2, collections)
   }

   @Test
   fun observeListItems_collectorCancellation_cancelsUpstream() = runTest(mainDispatcherRule.testDispatcher) {
      var upstreamCancelled = false
      val dao = observing(flow {
         try {
            emit(listOf(row))
            awaitCancellation()
         } finally {
            upstreamCancelled = true
         }
      })
      val results = mutableListOf<Result<List<CarListItem>>>()
      val readModel = CarReadModel(dao, mainDispatcherRule.testDispatcher)
      val collector = backgroundScope.launch { readModel.observeListItems().collect { results += it } }
      runCurrent()

      collector.cancelAndJoin()

      assertTrue(upstreamCancelled)
      assertEquals(listOf(listOf(item)), results.map { it.getOrThrow() })
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
         val dao = observing(flow {
            upstreamDispatcher = currentCoroutineContext()[ContinuationInterceptor]
            emit(listOf(row))
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
            val readModel = application.koin.get<ICarReadModel>()
            assertEquals(listOf(item), readModel.observeListItems().first().getOrThrow())
            assertSame(dispatcherIo, upstreamDispatcher)
         } finally {
            application.close()
         }
      }
   }

}
