package de.rogallab.mobile.ui.tdrives.list.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.tdrives.list.TDrivesIntent
import de.rogallab.mobile.ui.tdrives.list.TDrivesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TDrivesScreen(
   tDrivesUiState: TDrivesUiState,
   lazyListState: LazyListState,
   contentPadding: PaddingValues,
   onIntent: (TDrivesIntent) -> Unit,
) {
   Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
      Column(modifier = Modifier.fillMaxSize()) {
         TopAppBar(windowInsets = WindowInsets(0), title = { Text(stringResource(R.string.test_drives_title)) })
         if (tDrivesUiState.isLoading && tDrivesUiState.tDrives.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
         } else {
            TDrivesList(tDrivesUiState.tDrives, tDrivesUiState.people, tDrivesUiState.cars, lazyListState, onIntent)
         }
      }
      ExtendedFloatingActionButton(
         modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
         onClick = { onIntent(TDrivesIntent.Create) },
         icon = { Icon(Icons.Default.Add, contentDescription = null) },
         text = { Text(stringResource(R.string.action_create)) },
      )
   }
}

@Composable
private fun TDrivesList(
   tDrives: List<TDrive>, people: List<Person>, cars: List<Car>,
   lazyListState: LazyListState, onIntent: (TDrivesIntent) -> Unit,
) {
   val detailContentDescription = stringResource(R.string.accessibility_edit_test_drive)
   val deleteContentDescription = stringResource(R.string.accessibility_delete_test_drive)

   LazyColumn(
      state = lazyListState,
      contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(items = tDrives, key = TDrive::id) { tDrive ->
         SwipeCard(
            onDetail = { onIntent(TDrivesIntent.Detail(tDrive.id)) },
            onDelete = { onIntent(TDrivesIntent.RequestRemove(tDrive.id)) },
            detailContentDescription = detailContentDescription,
            deleteContentDescription = deleteContentDescription,
            modifier = Modifier.animateItem(),
         ) {
            val personName = people.firstOrNull { it.id == tDrive.personId }?.displayName
               ?: stringResource(R.string.value_not_available)
            val carName = cars.firstOrNull { it.id == tDrive.carId }?.displayName
               ?: stringResource(R.string.value_not_available)
            TDriveCard(tDrive, personName, carName) { onIntent(TDrivesIntent.Detail(tDrive.id)) }
         }
      }
   }
}

@Composable
private fun TDriveCard(tDrive: TDrive, personName: String, carName: String, onClick: () -> Unit) {
   Card(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(12.dp),
   ) {
      Row(
         modifier = Modifier.padding(16.dp),
         horizontalArrangement = Arrangement.spacedBy(16.dp),
         verticalAlignment = Alignment.CenterVertically,
      ) {
         Icon(Icons.Default.Event, contentDescription = null)
         Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.test_drive_card_title, personName, carName), style = MaterialTheme.typography.titleMedium)
            Text(DateTimeText.format(tDrive.start), style = MaterialTheme.typography.bodyMedium)
            Text(
               stringResource(if (tDrive.isCompleted) R.string.test_drive_status_completed else R.string.test_drive_status_planned),
               style = MaterialTheme.typography.bodySmall,
            )
         }
      }
   }
}
