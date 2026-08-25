package de.rogallab.mobile.ui.people.create_detail.mvvm.comp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.create_detail.PersonScreen
import de.rogallab.mobile.ui.people.create_detail.mvvm.PersonViewModelMvvm

@Composable
fun PersonAdapterMvvm(
   viewModel: PersonViewModelMvvm = viewModel(),
   modifier: Modifier
) {
   val tag = "<-PersonAdapterMvvm"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Stateholder -> ViewModel
   val personUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   PersonScreen(
      firstName = personUiState.person.firstName,      // State ↓
      onFirstNameChange = { firstName ->               // Event ↑
         viewModel.changeFirstName(firstName)
      },

      lastName = personUiState.person.lastName,       // State ↓
      onLastNameChange = { lastName ->                // Event ↑
         viewModel.changeLastName(lastName)
      },

      onSave = {
         Alog.d(tag, "onSave clicked")
         viewModel.save()
      },
      onCancel = {
         Alog.d(tag, "onCancel clicked")
         viewModel.cancel()
      },

      modifier = modifier
   )

}