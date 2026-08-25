package de.rogallab.mobile.ui.people.create_detail.mvi.comp

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
import de.rogallab.mobile.ui.people.create_detail.mvi.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.mvi.PersonViewModelMvi

@Composable
fun PersonAdapterMvi(
   viewModel: PersonViewModelMvi = viewModel(),
   modifier: Modifier
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Stateholder = ViewModel mit beobachtbare Zuständen (StateFlow)
   val personUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   PersonScreen(
      firstName = personUiState.person.firstName,           // State ↓
      onFirstNameChange = { firstName: String ->            // Event ↑
         viewModel.onIntent(PersonIntent.FirstNameChange(firstName))
      },
      lastName = personUiState.person.lastName,             // State ↓
      onLastNameChange = { lastName: String ->              // Event ↑
         viewModel.onIntent(PersonIntent.LastNameChange(lastName)) },
      onSave = {
         Alog.d(tag, "onSave clicked")
      },
      onCancel = {
         Alog.d(tag, "onCancel clicked")
      },
      modifier = modifier
   )

}

/*
 * Didaktik / Lernziele:
 *
 * - Der PersonAdapter bildet die Schnittstelle zwischen ViewModel und UI.
 *   Er kennt sowohl die MVI-Seite mit PersonUiState und PersonIntent als
 *   auch die einfache Parameter-Schnittstelle des PersonScreen.
 *
 * - Der PersonScreen bleibt dadurch unabhängig von ViewModel, StateFlow
 *   und konkreten Intent-Typen. Er erhält nur Daten und Callbacks.
 *
 * - Das unterstützt State Hoisting:
 *   Der Zustand liegt außerhalb des Screens und wird von oben übergeben.
 *
 * - Gleichzeitig bleibt der Datenfluss unidirektional:
 *   State fließt vom ViewModel zum Screen,
 *   Benutzeraktionen fließen als Intents zurück zum ViewModel.
 *
 * - Der Adapter enthält keine fachliche Logik. Er beobachtet State und
 *   übersetzt lediglich zwischen zwei Schnittstellen.
 *
 * - Diese Trennung erleichtert Tests und Previews, weil der PersonScreen
 *   ohne echtes ViewModel mit einfachen Beispielwerten aufgerufen werden kann.
 *
 * - Die zusätzliche Adapter-Schicht erzeugt etwas Boilerplate, macht dafür
 *   aber die Architekturgrenze zwischen zustandsbehafteter MVI-Schicht und
 *   zustandsloser Compose-UI explizit sichtbar.
 */
