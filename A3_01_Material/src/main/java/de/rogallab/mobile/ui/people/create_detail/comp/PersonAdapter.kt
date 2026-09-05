package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

/**
 * Adapts the ViewModel interface to the simple state and callbacks
 * expected by the stateless PersonScreen.
 *
 * The adapter observes the ViewModel state and translates UI callbacks
 * into PersonIntent instances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel
) {
   val tag = "<-PersonAdapter"

   // Counts successful compositions for diagnostic logging.
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect PersonUiState with lifecycle awareness.
   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()
   // Person data
   val person = personUiState.person
   var enableSave by remember { mutableStateOf(false) }
   enableSave = person.firstName.isNotEmpty() && person.lastName.isNotEmpty()

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(onClick = {
                  if (enableSave) viewModel.onIntent(PersonIntent.Save)
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_back))
               }
            },
            title = {
               Text(text = stringResource( if (personUiState.isNew) R.string.person_create
               else R.string.person_detail ))
            }
         )
      }
   ) { innerPadding ->
      // Show a loading indicator if the person data is still being loaded.
      if (personUiState.isLoading) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      } else {
         // Map ViewModel state to simple screen parameters and
         // map screen callbacks back to MVI intents.
         PersonScreen(
            firstName = person.firstName,
            onFirstNameChange = { viewModel.onIntent(PersonIntent.FirstNameChange(it)) },

            lastName = person.lastName,
            onLastNameChange = { viewModel.onIntent(PersonIntent.LastNameChange(it)) },

            email = person.email,
            onEmailChange = { viewModel.onIntent(PersonIntent.EmailChange(it)) },

            phone = person.phone,
            onPhoneChange = { viewModel.onIntent(PersonIntent.PhoneChange(it)) },

            imagePath = person.imagePath,

            onSave = { viewModel.onIntent(PersonIntent.Save) },
            onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding)
               .padding(horizontal = 16.dp)
               .verticalScroll(rememberScrollState())
               .imePadding(),
         )

      }
   }
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
