package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
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
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, String) -> Unit,
   onNavigateBack: () -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val tag = "<-PeopleAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val peopleUiState: PeopleUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { peopleEffect ->
      when (peopleEffect) {
         is PeopleEffect.ShowMessage -> onMessage(peopleEffect.message)
         is PeopleEffect.ShowError -> onError(peopleEffect.message)
         is PeopleEffect.ConfirmRemove -> onConfirmRemove(
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
      floatingActionButton = {
         ExtendedFloatingActionButton(
            containerColor = colorScheme.secondary,
            onClick = { viewModel.onIntent(PeopleIntent.Create) },
            icon = {
               Icon(imageVector = Icons.Default.Add,
                  contentDescription = null)
            },
            text = { Text(text = stringResource(R.string.action_create)) },
         )
      },
   ) { innerPadding ->
      if (peopleUiState.isLoading && peopleUiState.people.isEmpty()) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      }
      else {
         PeopleScreen(
            people = peopleUiState.people,
            onDetail = { personId ->
               viewModel.onIntent(PeopleIntent.Detail(personId))
            },
            onDelete = { personId ->
               viewModel.onIntent(PeopleIntent.RequestRemove(personId))
            },
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding)
               .padding(horizontal = 16.dp),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der PeopleAdapter verbindet PeopleViewModel und PeopleScreen und enthält
 *   wie in A5_01 den Scaffold für diesen Bildschirm.
 * - TopAppBar und FAB gehören zum Adapter; PeopleScreen enthält nur die Liste.
 * - Der einzige SnackbarHost und die Bottom-Navigation liegen im äußeren
 *   Scaffold von AppNavigation und gelten für alle Ziele.
 */
