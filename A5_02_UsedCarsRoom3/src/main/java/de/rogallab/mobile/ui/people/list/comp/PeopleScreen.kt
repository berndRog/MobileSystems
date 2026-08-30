package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleUiState

private const val TAG = "<-PeopleScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
   peopleUiState: PeopleUiState,
   lazyListState: LazyListState,
   contentPadding: PaddingValues,
   onIntent: (PeopleIntent) -> Unit,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   Box(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      Column(modifier = Modifier.fillMaxSize()) {
         TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text(stringResource(R.string.people_title)) },
         )
         if (peopleUiState.isLoading && peopleUiState.people.isEmpty()) {
            Box(
               modifier = Modifier.fillMaxSize(),
               contentAlignment = Alignment.Center,
            ) {
               CircularProgressIndicator()
            }
         }
         else {
            PeopleLazyList(
               people = peopleUiState.people,
               lazyListState = lazyListState,
               onIntent = onIntent,
            )
         }
      }

      ExtendedFloatingActionButton(
         modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
         onClick = { onIntent(PeopleIntent.Create) },
         icon = { Icon(Icons.Default.Add, contentDescription = null) },
         text = { Text(stringResource(R.string.action_create)) },
      )
   }
}
