package de.rogallab.mobile.ui.count

import de.rogallab.mobile.shared.domain.utilities.Alog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CountViewModelTest {
   @Before fun setUp() { Alog.set(useAndroidLog = false) }
   @After fun tearDown() { Alog.reset() }

   @Test fun incrementCount_updatesObservableCount() {
      val viewModel = CountViewModel()
      assertEquals(0, viewModel.stateFlow.value)

      viewModel.incrementCount()
      viewModel.incrementCount()

      assertEquals(2, viewModel.stateFlow.value)
   }
}
