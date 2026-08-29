package de.rogallab.mobile.ui.people.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.people.list.composables.PersonCard
import de.rogallab.mobile.ui.people.list.composables.SwipePeopleItem

private const val TAG = "<-PeopleScreen"

// Stateless list screen.
//
// The screen arranges the top app bar, the current list content and the
// create action. It knows neither Koin, Navigation 3, a ViewModel nor the
// coordinator. User-visible errors are deliberately handled by the shared
// Snackbar workflow outside this screen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
   peopleUiState: PeopleUiState,
   listState: LazyListState,
   contentPadding: PaddingValues,
   onIntent: (PeopleIntent) -> Unit,
) {
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${compositionCount++}") }

   Box(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      Column(
         modifier = Modifier.fillMaxSize(),
      ) {
         TopAppBar(
            windowInsets = WindowInsets(0),
            title = {
               Text(text = stringResource(R.string.people_title))
            },
         )

         PeopleContent(
            peopleUiState = peopleUiState,
            listState = listState,
            onIntent = onIntent,
         )
      }

      PeopleCreateButton(
         onCreate = {
            onIntent(PeopleIntent.Create)
         },
      )
   }
}

// Selects the content that represents the current UI state.
//
// The screen shows a progress indicator only while the initial list is being
// loaded. Afterwards it always shows the current list. Load failures are sent
// as one-shot events and displayed by the central SnackbarHost.
@Composable
private fun PeopleContent(
   peopleUiState: PeopleUiState,
   listState: LazyListState,
   onIntent: (PeopleIntent) -> Unit,
) {
   if (peopleUiState.isLoading && peopleUiState.people.isEmpty()) {
      Box(
         modifier = Modifier.fillMaxSize(),
         contentAlignment = Alignment.Center,
      ) {
         CircularProgressIndicator()
      }
   }
   else {
      PeopleList(
         people = peopleUiState.people,
         listState = listState,
         onIntent = onIntent,
      )
   }
}

// Displays all people in a vertically scrolling list.
//
// Every person ID is used as a stable key so Compose can preserve the state
// of unchanged list items when the list is updated.
@Composable
private fun PeopleList(
   people: List<Person>,
   listState: LazyListState,
   onIntent: (PeopleIntent) -> Unit,
) {
   LazyColumn(
      state = listState,
      contentPadding = PaddingValues(
         start = 12.dp,
         end = 12.dp,
         bottom = 96.dp,
      ),
   ) {
      itemsIndexed(
         items = people,
         key = { _, person ->
            person.id
         },
      ) { originalIndex, person ->
         SwipePeopleItem(
            person = person,
            originalIndex = originalIndex,
            onIntent = onIntent,
         ) {
            PersonCard(
               firstName = person.firstName,
               lastName = person.lastName,
               email = person.email,
               phone = person.phone,
               imagePath = person.imagePath,
            )
         }
      }
   }
}

// Displays the primary action for creating a new person.
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
