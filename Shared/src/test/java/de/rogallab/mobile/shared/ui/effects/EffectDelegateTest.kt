package de.rogallab.mobile.shared.ui.effects

import app.cash.turbine.test
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectDelegateTest {

   @Test
   fun emit_deliversEffectToFlow() = runTest {
      val delegate = EffectDelegate<String>()
      val received = async { delegate.effects.first() }

      delegate.emit("saved")

      assertEquals("saved", received.await())
   }

   @Test
   fun emit_preservesOrderOfBufferedEffects() = runTest {
      val delegate = EffectDelegate<String>()

      delegate.emit("first")
      delegate.emit("second")

      delegate.effects.test {
         assertEquals("first", awaitItem())
         assertEquals("second", awaitItem())
         cancelAndIgnoreRemainingEvents()
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen ausschließlich die technische Aufgabe des EffectDelegate.
 * - Ein gesendeter Effect muss über den bereitgestellten Flow ankommen.
 * - Mehrere gepufferte Effects müssen in ihrer Sendereihenfolge ankommen.
 * - Feature-spezifische Effects werden bewusst in den jeweiligen Modulen getestet.
 */
