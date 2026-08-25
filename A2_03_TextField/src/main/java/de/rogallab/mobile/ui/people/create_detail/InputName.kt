package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun InputName(
   name: String,                     // State ↓
   onNameChange: (String) -> Unit,   // Event ↑
   label: String = "Name"
) {
   val tag = "<-InputName"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   TextField(
      modifier = Modifier
         .fillMaxWidth(),
      value = name,                                        // State ↓
      onValueChange = { it: String ->
         Alog.d("<-InputName", "onValueChange: $it")
         onNameChange(it)                    // Event ↑

      },
      label = { Text(text = label) },
      singleLine = true,
      isError =  name.length > 20,
      supportingText = {
         if (name.length > 20) {
            Text("Zu lang (maximal 20 Zeichen)")
         }
      }
   )
}