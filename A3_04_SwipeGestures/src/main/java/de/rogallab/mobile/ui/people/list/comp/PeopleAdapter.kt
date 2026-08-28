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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
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
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onBack: () -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val tag = "<-PeopleAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect the persistent UI state from the ViewModel.
   val peopleUiState: PeopleUiState by
   viewModel.stateFlow.collectAsStateWithLifecycle()

   // Collect one-time effects and forward them to simple callbacks.
   EffectHandler(viewModel.effects) { peopleEffect ->
      when (peopleEffect) {
         is PeopleEffect.ShowMessage -> onMessage(peopleEffect.message)
         is PeopleEffect.ShowError -> onError(peopleEffect.message)
         PeopleEffect.NavigateBack -> onBack()
         is PeopleEffect.NavigateTo -> onNavigateTo(peopleEffect.personId)
      }
   }

   Box(
      modifier = modifier.fillMaxSize()
   ) {
      Column {
         TopAppBar(
            windowInsets = WindowInsets(0),
            title = {
               Text(text = stringResource(R.string.people_list))
            },
         )

         // Show either a loading indicator or the stateless PeopleScreen.
         if (peopleUiState.isLoading && peopleUiState.people.isEmpty()) {
            Column(
               modifier = Modifier.fillMaxWidth(),
               verticalArrangement = Arrangement.Top,
               horizontalAlignment = Alignment.CenterHorizontally,
            ) {
               Alog.d(tag, "Loading People...")
               CircularProgressIndicator(modifier = Modifier.size(64.dp))
            }
         }
         else {
            val people = peopleUiState.people

            PeopleScreen(
               people = people,
               onDetail = { personId ->
                  Alog.d(tag, "Navigate to Detail: $personId")
                  viewModel.onIntent(PeopleIntent.Detail(personId))
               },
               onEdit = { personId ->
                  Alog.d(tag, "Swipe edit: $personId")
                  viewModel.onIntent(PeopleIntent.Detail(personId))
               },
               onDelete = { personId ->
                  Alog.d(tag, "Swipe delete: $personId")
                  val person = people.find { it.id == personId }
                  if (person != null) {
                     viewModel.onIntent(PeopleIntent.Remove(person))
                  }
               },
               modifier = Modifier.padding(horizontal = 16.dp),
            )
         }
      }

      PeopleCreateButton(
         onCreate = {
            Alog.d(tag, "Create new person")
            viewModel.onIntent(PeopleIntent.Create)
         }
      )
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
 *   in A3_03. Neu sind die beiden Swipe-Callbacks onEdit und onDelete.
 *
 * - Swipe-to-Edit wird auf denselben PeopleIntent.Detail abgebildet wie ein
 *   normaler Detail-Aufruf. Die Navigation muss deshalb nicht wissen, ob das
 *   Bearbeiten durch Tap oder Swipe ausgelöst wurde.
 *
 * - Swipe-to-Delete sucht die Person anhand ihrer stabilen ID und sendet
 *   PeopleIntent.Remove an das ViewModel. In A3_04 wird damit unmittelbar aus
 *   dem Repository gelöscht.
 *
 * - Effects bleiben auf Meldungen und Navigation beschränkt. Einen ShowUndo-
 *   Effect gibt es in diesem Schritt bewusst noch nicht.
 *
 * Lernziele:
 *
 * - Neue UI-Interaktionen auf bestehende Intents abbilden.
 * - Swipe-Callbacks von Navigation und Persistenz entkoppeln.
 * - Einen kleinen Gesten-Schritt ohne zusätzliche Undo-Infrastruktur aufbauen.
 */
