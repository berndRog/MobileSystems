package de.rogallab.mobile.ui.tdrives.list.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.tdrives.list.TDrivesIntent
import de.rogallab.mobile.ui.tdrives.list.TDrivesUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TDrivesScreen(
   tDrivesUiState: TDrivesUiState,
   lazyListState: LazyListState,
   onIntent: (TDrivesIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   TDrivesList(
      tDrives = tDrivesUiState.tDrives,
      lazyListState = lazyListState,
      onIntent = onIntent,
      modifier = modifier,
   )
}

@Composable
private fun TDrivesList(
   tDrives: List<TDriveListItem>,
   lazyListState: LazyListState,
   onIntent: (TDrivesIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val detailContentDescription = stringResource(R.string.accessibility_edit_test_drive)
   val deleteContentDescription = stringResource(R.string.accessibility_delete_test_drive)

   LazyColumn(
      modifier = modifier,
      state = lazyListState,
      contentPadding = PaddingValues(
         start = 12.dp,
         end = 12.dp,
         bottom = 96.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(items = tDrives, key = TDriveListItem::id) { tDrive ->
         SwipeCard(
            onDetail = { onIntent(TDrivesIntent.Detail(tDrive.id)) },
            onDelete = { onIntent(TDrivesIntent.RequestRemove(tDrive.id)) },
            detailContentDescription = detailContentDescription,
            deleteContentDescription = deleteContentDescription,
            modifier = Modifier.animateItem(),
         ) {
            // The projection already contains driver and car names.
            TDriveCard(tDrive = tDrive)
         }
      }
   }
}

@Composable
private fun TDriveCard(
   tDrive: TDriveListItem,
) {
   Card(
      modifier = Modifier
         .height(90.dp)
         .fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
   ) {

      //Icon(Icons.Default.Event, contentDescription = null)
      Column(modifier = Modifier.padding(horizontal = 8.dp)) {
         Text(
            stringResource(R.string.test_drive_card_title, tDrive.driverFullName, tDrive.carDisplayName),
            style = MaterialTheme.typography.bodyMedium,
         )
         Text(
            DateTimeText.format(
               tDrive.start.toLocalDateTime(TimeZone.currentSystemDefault())
            ),
            style = MaterialTheme.typography.bodyMedium,
         )
         Text(
            stringResource(
               if (tDrive.isCompleted) R.string.test_drive_status_completed
               else R.string.test_drive_status_planned,
            ),
            style = MaterialTheme.typography.bodySmall,
         )
      }
   }
}
