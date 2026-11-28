package de.rogallab.mobile.shared.ui.effects

import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EffectDispatcherTest {
   private sealed interface Event {
      data class Error(val text: String) : Event
      data class Message(val text: String) : Event
      data class Navigate(val id: Int) : Event
   }

   private data class Request(val resourceId: Int, val args: List<Any>)

   private class Strings : IStringProvider {
      val requests = mutableListOf<Request>()
      var failure: Throwable? = null

      override fun getString(resId: Int, vararg args: Any): String {
         requests += Request(resId, args.toList())
         failure?.let { throw it }
         return "res-$resId:${args.joinToString("|")}"
      }
   }

   private val delegate = EffectDelegate<Event>()
   private val strings = Strings()
   private val output = ByteArrayOutputStream()
   private val errors = ByteArrayOutputStream()
   private lateinit var originalOut: PrintStream
   private lateinit var originalErr: PrintStream

   @Before fun setUp() {
      Alog.set(useAndroidLog = false)
      originalOut = System.out
      originalErr = System.err
      System.setOut(PrintStream(output))
      System.setErr(PrintStream(errors))
   }

   @After fun tearDown() {
      System.setOut(originalOut)
      System.setErr(originalErr)
      Alog.reset()
   }

   private fun dispatcher(
      scope: CoroutineScope,
      messageFactory: ((String) -> Event)? = { Event.Message(it) },
      errorFactory: (String) -> Event = { Event.Error(it) },
   ) = EffectDispatcher(
      _scope = scope,
      _effectDelegate = delegate,
      _stringProvider = strings,
      _errorEffect = errorFactory,
      _messageEffect = messageFactory,
      _tag = "DispatcherTest",
   )

   @Test fun emit_buffersTypedEffectUntilACollectorArrives() = runTest {
      val effects = dispatcher(this)
      effects.emit(Event.Navigate(7))
      assertEquals(Event.Navigate(7), delegate.effects.first())
      assertTrue(strings.requests.isEmpty())
      assertEquals("", output.toString())
   }

   @Test fun resourceError_forwardsArgumentsAndLogsItsCauseExactlyOnce() = runTest {
      val failure = IllegalStateException("write failed")
      dispatcher(this).showError(10, failure, "Ada", 3)

      assertEquals(listOf(Request(10, listOf("Ada", 3))), strings.requests)
      assertEquals(Event.Error("res-10:Ada|3"), delegate.effects.first())
      assertEquals(1, output.toString().lineSequence().count { it.startsWith("E/DispatcherTest:") })
      assertTrue(output.toString().contains("res-10:Ada|3"))
      assertTrue(errors.toString().contains("write failed"))
   }

   @Test fun stringError_preservesTextAndLogsItsCauseExactlyOnce() = runTest {
      dispatcher(this).showError("  Already resolved %s  ", IllegalArgumentException("invalid"))
      assertEquals(Event.Error("  Already resolved %s  "), delegate.effects.first())
      assertTrue(strings.requests.isEmpty())
      assertEquals(1, output.toString().lineSequence().count { it.startsWith("E/DispatcherTest:") })
      assertTrue(errors.toString().contains("invalid"))
   }

   @Test fun errorsWithoutCause_doNotAddErrorLogs() = runTest {
      val effects = dispatcher(this)
      effects.showError(10)
      effects.showError("Validation failed")
      assertEquals(Event.Error("res-10:"), delegate.effects.first())
      assertEquals(Event.Error("Validation failed"), delegate.effects.first())
      assertEquals("", output.toString())
      assertEquals("", errors.toString())
   }

   @Test fun messages_supportResourcesArgumentsAndUnchangedStrings() = runTest {
      val effects = dispatcher(this)
      effects.showMessage(20, "Ada", 3)
      effects.showMessage("  Already resolved %s  ")
      assertEquals(listOf(Request(20, listOf("Ada", 3))), strings.requests)
      assertEquals(Event.Message("res-20:Ada|3"), delegate.effects.first())
      assertEquals(Event.Message("  Already resolved %s  "), delegate.effects.first())
      assertEquals("", output.toString())
   }

   @Test fun errorOnlyConfiguration_needsNoMessageFactory() = runTest {
      val effects = dispatcher(this, messageFactory = null)
      effects.showError("Search required")
      effects.emit(Event.Navigate(1))
      assertEquals(Event.Error("Search required"), delegate.effects.first())
      assertEquals(Event.Navigate(1), delegate.effects.first())
   }

   @Test fun missingMessageFactory_failsExplicitlyForBothOverloads() = runTest {
      val effects = dispatcher(this, messageFactory = null)
      for (send in listOf<suspend () -> Unit>(
         { effects.showMessage("saved") },
         { effects.showMessage(20) },
      )) {
         try {
            send()
            fail("Expected missing message factory")
         } catch (actual: IllegalStateException) {
            assertEquals("No message effect factory configured", actual.message)
         }
      }
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun launchMethods_deferToTheSuppliedScopeAndReturnCompletingJobs() = runTest {
      val effects = dispatcher(this)
      val cause = IllegalStateException("failed")
      // Join each send explicitly; independent launch calls need not be ordered.
      val sends = listOf<Pair<() -> Job, Event>>(
         { effects.launchEmit(Event.Navigate(1)) } to Event.Navigate(1),
         { effects.launchShowError(10, cause, "Ada", 3) } to Event.Error("res-10:Ada|3"),
         { effects.launchShowError("resolved error", cause) } to Event.Error("resolved error"),
         { effects.launchShowMessage(20, "Grace", 2) } to Event.Message("res-20:Grace|2"),
         { effects.launchShowMessage("resolved message") } to Event.Message("resolved message"),
      )
      for ((send, expected) in sends) {
         val job = send()
         assertFalse(job.isCompleted)
         job.join()
         assertTrue(job.isCompleted)
         assertFalse(job.isCancelled)
         assertEquals(expected, delegate.effects.first())
      }
      assertEquals(
         listOf(Request(10, listOf("Ada", 3)), Request(20, listOf("Grace", 2))),
         strings.requests,
      )
      assertEquals(2, output.toString().lineSequence().count { it.startsWith("E/DispatcherTest:") })
   }

   @Test fun cancellationCause_isRethrownBeforeResourcesFactoriesOrLogging() = runTest {
      var factoryCalls = 0
      val effects = dispatcher(this, errorFactory = { factoryCalls++; Event.Error(it) })
      val cancellation = CancellationException("cancelled")
      for (send in listOf<suspend () -> Unit>(
         { effects.showError(10, cancellation, "unused") },
         { effects.showError("unused", cancellation) },
      )) {
         try {
            send()
            fail("Expected cancellation")
         } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
         }
      }
      assertEquals(0, factoryCalls)
      assertTrue(strings.requests.isEmpty())
      assertEquals("", output.toString())
      assertEquals("", errors.toString())
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun cancelledCaller_cannotSendEvenWhenTheChannelHasSpace() = runTest {
      var factoryCalls = 0
      val effects = dispatcher(
         this,
         messageFactory = { factoryCalls++; Event.Message(it) },
         errorFactory = { factoryCalls++; Event.Error(it) },
      )
      val sends = listOf<suspend () -> Unit>(
         { effects.emit(Event.Navigate(1)) },
         { effects.showError(10, IllegalStateException("unused")) },
         { effects.showError("unused", IllegalStateException("unused")) },
         { effects.showMessage(20) },
         { effects.showMessage("unused") },
      )
      for (send in sends) {
         val caller = launch {
            currentCoroutineContext().cancel()
            send()
         }
         caller.join()
         assertTrue(caller.isCancelled)
      }
      assertEquals(0, factoryCalls)
      assertTrue(strings.requests.isEmpty())
      assertEquals("", output.toString())
      assertEquals("", errors.toString())
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun suspendMethods_useTheCallerEvenWhenTheStoredScopeIsCancelled() = runTest {
      val scopeJob = SupervisorJob()
      val scope = CoroutineScope(scopeJob + StandardTestDispatcher(testScheduler))
      scopeJob.cancel()
      val effects = dispatcher(scope)

      effects.showError(10)
      effects.showError("validation")
      effects.showMessage(20)
      effects.showMessage("saved")
      effects.emit(Event.Navigate(1))

      assertEquals(Event.Error("res-10:"), delegate.effects.first())
      assertEquals(Event.Error("validation"), delegate.effects.first())
      assertEquals(Event.Message("res-20:"), delegate.effects.first())
      assertEquals(Event.Message("saved"), delegate.effects.first())
      assertEquals(Event.Navigate(1), delegate.effects.first())
      assertFalse(scopeJob.children.any())
   }

   @Test fun cancelledStoredScope_preventsEveryLaunchBody() = runTest {
      val scopeJob = SupervisorJob()
      val scope = CoroutineScope(scopeJob + StandardTestDispatcher(testScheduler))
      scopeJob.cancel()
      val effects = dispatcher(scope)
      val jobs = listOf(
         effects.launchEmit(Event.Navigate(1)),
         effects.launchShowError(10, IllegalStateException("unused")),
         effects.launchShowError("unused", IllegalStateException("unused")),
         effects.launchShowMessage(20),
         effects.launchShowMessage("unused"),
      )
      runCurrent()
      assertTrue(jobs.all { it.isCancelled })
      assertTrue(strings.requests.isEmpty())
      assertEquals("", output.toString())
      assertEquals("", errors.toString())
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun launchCancellationCause_cancelsTheReturnedJobWithoutAnEffect() = runTest {
      val effects = dispatcher(this)
      val cancellation = CancellationException("cancelled")
      val jobs = listOf(
         effects.launchShowError(10, cancellation),
         effects.launchShowError("unused", cancellation),
      )
      jobs.forEach { it.join() }
      assertTrue(jobs.all { it.isCancelled })
      assertTrue(strings.requests.isEmpty())
      assertEquals("", output.toString())
      assertEquals("", errors.toString())
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun fullBuffer_suspendsTheCallerAndCancellationRemovesItsPendingSend() = runTest {
      val effects = dispatcher(this)
      var completedSends = 0
      val sender = launch {
         // A burst fills the normal buffer without relying on its exact capacity.
         repeat(10_000) {
            effects.emit(Event.Navigate(it))
            completedSends++
         }
      }
      runCurrent()
      assertTrue(sender.isActive)
      assertTrue(completedSends in 1 until 10_000)
      val buffered = completedSends
      sender.cancelAndJoin()

      delegate.effects.test {
         repeat(buffered) { assertEquals(Event.Navigate(it), awaitItem()) }
         expectNoEvents()
      }
   }

   @Test fun sequentialSuspendCalls_preserveMessageNavigationAndErrorOrder() = runTest {
      val effects = dispatcher(this)
      effects.showMessage(20, "Ada")
      effects.emit(Event.Navigate(1))
      effects.showError("validation")
      delegate.effects.test {
         assertEquals(Event.Message("res-20:Ada"), awaitItem())
         assertEquals(Event.Navigate(1), awaitItem())
         assertEquals(Event.Error("validation"), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun providerFailures_propagateWithoutFallbackEffects() = runTest {
      val effects = dispatcher(this)
      val failure = IllegalArgumentException("resource failed")
      strings.failure = failure
      for (send in listOf<suspend () -> Unit>(
         { effects.showError(10) },
         { effects.showMessage(20) },
      )) {
         try {
            send()
            fail("Expected provider failure")
         } catch (actual: IllegalArgumentException) {
            assertSame(failure, actual)
         }
      }
      assertEquals("", output.toString())
      delegate.effects.test { expectNoEvents() }
   }

   @Test fun factoryFailures_propagateWithoutRecursiveErrorHandling() = runTest {
      val failure = IllegalArgumentException("factory failed")
      val effects = dispatcher(
         this,
         messageFactory = { throw failure },
         errorFactory = { throw failure },
      )
      for (send in listOf<suspend () -> Unit>(
         { effects.showError("error") },
         { effects.showMessage("message") },
      )) {
         try {
            send()
            fail("Expected factory failure")
         } catch (actual: IllegalArgumentException) {
            assertSame(failure, actual)
         }
      }
      delegate.effects.test { expectNoEvents() }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen Aufbereitung und Versand unabhängig von einem Feature.
 * - Der vorhandene Delegate bleibt der einzige Transport für typisierte Effects.
 * - Tests unterscheiden neue Jobs von suspendierenden Aufrufen im aktuellen Scope.
 * - Cancellation erzeugt weder einen Ersatz-Effect noch einen zusätzlichen Log.
 * - Backpressure und Reihenfolge bleiben auch ohne aktiven UI-Collector erhalten.
 */
