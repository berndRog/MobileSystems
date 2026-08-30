package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveIntent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator

@Composable
fun TDriveContent(
   tDrive: TDrive,
   startInput: String,
   people: List<Person>,
   cars: List<Car>,
   validator: TDriveValidator,
   onIntent: (TDriveIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-TDriveContent"
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(tag, "Composition #${cCount.intValue++}") }


   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      PersonSelectionField(
         people = people,
         selectedPersonId = tDrive.personId,
         label = stringResource(R.string.person_selection_label),
         allowNone = false,
         onPersonSelected = { personId ->
            onIntent(TDriveIntent.PersonChanged(personId))
         },
      )

      CarSelectionField(
         cars = cars,
         selectedCarId = tDrive.carId,
         onCarSelected = { carId ->
            onIntent(TDriveIntent.CarChanged(carId))
         },
      )

      InputValueString(
         value = startInput,
         onValueChange = { start ->
            onIntent(TDriveIntent.StartChanged(start))
         },
         label = stringResource(
            R.string.test_drive_field_start,
            DateTimeText.pattern,
         ),
         leadingIcon = Icons.Default.DateRange,
         validate = validator::validateStart,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = tDrive.notes.orEmpty(),
         onValueChange = { notes ->
            onIntent(TDriveIntent.NotesChanged(notes))
         },
         label = stringResource(R.string.test_drive_field_notes),
         leadingIcon = Icons.Default.Description,
         imeAction = ImeAction.Done,
      )

      Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically,
      ) {
         Checkbox(
            checked = tDrive.isCompleted,
            onCheckedChange = { isCompleted ->
               onIntent(TDriveIntent.CompletedChanged(isCompleted))
            },
         )
         Text(text = stringResource(R.string.test_drive_completed))
      )
   }
}