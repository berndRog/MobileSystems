package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleUiState
import de.rogallab.mobile.ui.people.list.PeopleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleAdapter(
   viewModel: PeopleViewModel,
   snackbarHostState: SnackbarHostState,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onUndo: (String, String, String) -> Unit,
   onNavigateBack: () -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val tag = "<-PeopleAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect the PeopleUiState from the ViewModel.
   val peopleUiState: PeopleUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   // Collect one-time effects and forward them to simple callbacks.
   EffectHandler(viewModel.effects) { peopleEffect ->
      Alog.d(tag, "Effect: $peopleEffect")
      when (peopleEffect) {
         is PeopleEffect.ShowMessage -> onMessage(peopleEffect.message)
         is PeopleEffect.ShowError -> onError(peopleEffect.message)
         is PeopleEffect.ShowUndo -> onUndo(
            peopleEffect.message,
            peopleEffect.actionLabel,
            peopleEffect.personId,
         )
         PeopleEffect.NavigateBack -> onNavigateBack()
         is PeopleEffect.NavigateTo -> onNavigateTo(peopleEffect.personId)
      }
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(title = { Text(text = stringResource(R.string.people_list)) })
      },
      floatingActionButtonPosition = FabPosition.End,
      floatingActionButton = {
         ExtendedFloatingActionButton(
            containerColor = colorScheme.secondary,
            onClick = {
               Alog.d(tag, "Create new person")
               viewModel.onIntent(PeopleIntent.Create)
            },
            icon = {
               Icon(imageVector = Icons.Default.Add,
                  contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.action_create)) },
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState,
            modifier = Modifier.imePadding())
      },
   ) { innerPadding ->

      // Show either a loading indicator or the stateless PeopleScreen.
      if (peopleUiState.isLoading && peopleUiState.people.isEmpty()) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(
               modifier = Modifier.size(64.dp)
            )
         }

         // Show the stateless PeopleScreen with the current list of people.
      } else {
         val people = peopleUiState.people

         PeopleScreen(
            people = people,
            onDetail = { personId ->
               viewModel.onIntent(PeopleIntent.Detail(personId))
            },
            onDelete = { personId ->
               Alog.d(tag, "Delete: $personId")
               val person = people.find { it.id == personId }
               if (person != null)
                  viewModel.onIntent(PeopleIntent.Remove(person))
            },
            restoredPersonId = peopleUiState.restoredPersonId,
            onRestoreHandled = {
               viewModel.onIntent(PeopleIntent.RestoreHandled)
            },
            modifier = Modifier.padding(horizontal = 16.dp),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der PeopleAdapter verbindet PeopleViewModel und PeopleScreen.
 *
 * - Dauerhafter State wird mit collectAsStateWithLifecycle() beobachtet.
 *   Einmalige Effects werden getrennt mit dem generischen EffectHandler
 *   gesammelt.
 *
 * - Der Adapter übersetzt feature-spezifische Effects in einfache Callbacks:
 *
 *      ShowMessage  -> onMessage()
 *      ShowError    -> onError()
 *      ShowUndo     -> onUndo()
 *      NavigateBack -> onBack()
 *      NavigateTo   -> onNavigateTo()
 *
 * - Ein Tap auf eine Person und Swipe StartToEnd werden beide als Detail-
 *   Navigation behandelt. Create bleibt eine separate FAB-Aktion.
 *
 * - restoredPersonId wird als State an PeopleScreen weitergegeben. Nachdem der
 *   Screen geprüft hat, ob das wiederhergestellte Element sichtbar ist, sendet
 *   der Adapter RestoreHandled zurück an das ViewModel.
 *
 * - ShowUndo wird als Action-Snackbar weitergereicht. Die eigentliche
 *   Entscheidung Undo oder Commit erfolgt oberhalb des Adapters, nachdem die
 *   Snackbar beendet wurde.
 *
 * Lernziele:
 *
 * - State und Effects getrennt verarbeiten.
 * - Funktionen als Parameter zur Entkopplung von UI-Schichten einsetzen.
 * - Einmalige UI-Aufträge über State plus Intent bestätigen.
 * - Detail, Delete, Undo und Navigation über klar benannte Callbacks verbinden.
 */
