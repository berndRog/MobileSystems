package de.rogallab.mobile.util

import de.rogallab.mobile.domain.utilities.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
   val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

   override fun starting(description: Description) {
      // Local JVM tests must not call the unimplemented android.util.Log stub.
      AppLogger.set(useAndroidLog = false)
      AppLogger.disable()
      Dispatchers.setMain(dispatcher)
   }

   override fun finished(description: Description) {
      Dispatchers.resetMain()
      AppLogger.reset()
   }
}
