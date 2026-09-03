package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
   onConfirmRemove: (String, String, String) -> Unit,
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
      when (peopleEffect) {
         is PeopleEffect.ShowMessage -> onMessage(peopleEffect.message)
         is PeopleEffect.ShowError -> onError(peopleEffect.message)
         is PeopleEffect.ConfirmRemove ->
            onConfirmRemove(peopleEffect.message, peopleEffect.actionLabel,
               peopleEffect.personId)
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
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
               viewModel.onIntent(PeopleIntent.RequestRemove(personId))
            },
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding)
               .padding(horizontal = 16.dp)
         )
      }
   }
}

@Composable
private fun PeopleCreateButton(
   onCreate: () -> Unit,
) {
   Box(
      modifier = Modifier
         .fillMaxSize()
         .padding(16.dp),
      contentAlignment = Alignment.BottomEnd,
   ) {
      ExtendedFloatingActionButton(
         containerColor = colorScheme.secondary,
         onClick = onCreate,
         icon = {
            Icon(
               imageVector = Icons.Default.Add,
               contentDescription = null,
            )
         },
         text = {
            Text(text = stringResource(R.string.action_create))
         },
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der PeopleAdapter verbindet PeopleViewModel und PeopleScreen wie bereits
 *   in A3_03. Neu ist die Swipe-Unterstützung für Detail und Delete.
 *
 * - Ein Tap auf die PersonCard und Swipe StartToEnd werden auf denselben
 *   PeopleIntent.Detail abgebildet. Die Navigation muss deshalb nicht wissen,
 *   wodurch der Detail-Screen geöffnet wurde.
 *
 * - Swipe EndToStart sendet zunächst PeopleIntent.RequestRemove. Das ViewModel
 *   erzeugt daraufhin PeopleEffect.ConfirmRemove. Der Adapter reicht diesen
 *   einmaligen Effect über onConfirmRemove an die Navigationsebene weiter.
 *
 * - Erst die Action der Bestätigungs-Snackbar führt zu ConfirmRemove und damit
 *   zur Repository-Operation. Ein Dismiss der Snackbar verändert keine Daten.
 *
 * - Das Anlegen einer neuen Person bleibt eine eigene Aktion und wird über den
 *   FAB als PeopleIntent.Create ausgelöst.
 *
 * Lernziele:
 *
 * - Neue UI-Interaktionen auf Intents abbilden.
 * - Destruktive Aktionen über einen Effect bestätigen lassen.
 * - Swipe-Callbacks von Navigation und Persistenz entkoppeln.
 * - Create und Detail als unterschiedliche fachliche Vorgänge benennen.
 */
