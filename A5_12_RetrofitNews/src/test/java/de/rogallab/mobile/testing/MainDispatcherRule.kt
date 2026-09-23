package de.rogallab.mobile.testing

import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
   val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

   override fun starting(description: Description) {
      // Replace Android Main with a deterministic test dispatcher.
      Alog.set(useAndroidLog = false)
      Dispatchers.setMain(testDispatcher)
   }

   override fun finished(description: Description) {
      Dispatchers.resetMain()
      Alog.reset()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Regel ersetzt Dispatchers.Main für jeden Test und stellt ihn danach
 *   zuverlässig wieder her. Coroutines lassen sich dadurch deterministisch steuern.
 *
 * Lernziele:
 *
 * - ViewModel-Coroutines ohne Android-Laufzeit testen.
 * - Gemeinsames Test-Setup in einer JUnit-Regel kapseln.
 */
