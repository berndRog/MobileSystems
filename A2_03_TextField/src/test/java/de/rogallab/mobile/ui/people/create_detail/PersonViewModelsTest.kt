package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.create_detail.mvi.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.mvi.PersonViewModelMvi
import de.rogallab.mobile.ui.people.create_detail.mvvm.PersonViewModelMvvm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PersonViewModelsTest {
   @Before fun setUp() { Alog.set(useAndroidLog = false) }
   @After fun tearDown() { Alog.reset() }

   @Test fun mvvm_trimsInputAndCancelClearsNames() {
      val viewModel = PersonViewModelMvvm()
      viewModel.changeFirstName("  Ada  ")
      viewModel.changeLastName("  Lovelace  ")

      assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
      assertEquals("Lovelace", viewModel.stateFlow.value.person.lastName)

      viewModel.cancel()
      assertEquals("", viewModel.stateFlow.value.person.firstName)
      assertEquals("", viewModel.stateFlow.value.person.lastName)
   }

   @Test fun mvi_intentsUpdateStateAndCancelClearsNames() {
      val viewModel = PersonViewModelMvi()
      viewModel.onIntent(PersonIntent.FirstNameChange("  Ada  "))
      viewModel.onIntent(PersonIntent.LastNameChange("  Lovelace  "))

      assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
      assertEquals("Lovelace", viewModel.stateFlow.value.person.lastName)

      viewModel.onIntent(PersonIntent.Cancel)
      assertEquals("", viewModel.stateFlow.value.person.firstName)
      assertEquals("", viewModel.stateFlow.value.person.lastName)
   }
}
