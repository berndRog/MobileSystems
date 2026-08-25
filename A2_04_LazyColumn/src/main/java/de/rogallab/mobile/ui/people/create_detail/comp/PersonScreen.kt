package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog

// Stateless Composable

@Composable
fun PersonScreen(
   firstName: String,                     // State ↓
   onFirstNameChange: (String) -> Unit,   // Event ↑
   lastName: String,                      // State ↓
   onLastNameChange: (String) -> Unit,    // Event ↑
   onSave: () -> Unit,                    // Event ↑
   onCancel: () -> Unit,                  // Event ↑
   modifier: Modifier
) {
   val tag = "<-PersonScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   var enableSave by remember { mutableStateOf(false) }
   enableSave = firstName.isNotEmpty() && lastName.isNotEmpty()

   Column(
      modifier = modifier.padding(top = 8.dp).fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
   ){
      InputName(
         name = firstName,
         onNameChange = { it ->
            Alog.d(tag, "onLastNameChange: $it")
            onFirstNameChange(it)
          },
         label = "Vorname"
      )
      InputName(
         name = lastName,
         onNameChange = { it ->
            Alog.d(tag, "onLastNameChange: $it")
            onLastNameChange(it)
         },
         label = "Nachname"
      )

      Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(
            40.dp, Alignment.CenterHorizontally),
      ) {
         OutlinedButton(
            onClick = { onCancel() },
         ) {
            Text(text = "Abbrechen")
         }

         Button(
            onClick = { onSave() },
            enabled = enableSave,
         ) {
            Text(text = "Speichern")
         }
      }
   }
}

