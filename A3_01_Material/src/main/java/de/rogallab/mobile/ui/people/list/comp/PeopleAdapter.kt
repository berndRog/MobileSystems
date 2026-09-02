package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleUiState
import de.rogallab.mobile.ui.people.list.PeopleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable  // MVI pattern
fun PeopleAdapter(
   viewModel: PeopleViewModel
) {
   val tag = "<-PeopleAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   val state: PeopleUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            title = { Text(text = stringResource(R.string.people_list)) }
         )
      }
   ) { innerPadding ->
      // The Box is used to place the FloatingActionButton on top of the content.
      Box(
         modifier = Modifier.fillMaxSize()
      ) {
         Column(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding)
         ) {

            // Show either a loading indicator or the stateless PersonScreen.
            if (state.isLoading && state.people.isEmpty()) {
               Column(
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(16.dp),
                  verticalArrangement = Arrangement.Top,
                  horizontalAlignment = Alignment.CenterHorizontally,
               ) {
                  Alog.d(tag, "Loading People...")
                  CircularProgressIndicator(modifier = Modifier.size(64.dp))
               }

            } else {
               val people = state.people
               PeopleScreen(
                  people = people,
                  onDetail = { personId ->
                     Alog.d(tag, "Navigate to Detail: $personId")
                  },
                  onDelete = { personId ->
                     Alog.d(tag, "Delete: $personId")
                     val person = people.find { it.id == personId }
                     if (person != null)
                        viewModel.onIntent(PeopleIntent.Remove(person))
                  },
                  modifier = Modifier.padding(horizontal = 16.dp)
               )
            }
         } // Column
      }

      PeopleCreateButton(
         onCreate = {
            Alog.d(tag, "Create new person")
            viewModel.onIntent(PeopleIntent.Create)
         }
      )
   } // Box
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