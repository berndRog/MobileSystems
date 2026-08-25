package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun InputName(   // with local state
   name: String,                     // State ↓
   onNameChange: (String) -> Unit,   // Event ↑
   label: String = "Name"
) {

   TextField(
      modifier = Modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            if (!focusState.isFocused) {
               Alog.d("<-InputName", "focus changed name:$name")
               onNameChange(name) // Event ↑
            }
         },
      value = name,
      onValueChange = { it: String ->
         Alog.d("<-InputName", "onValueChange: $it")
         onNameChange(it) // Event ↑
      },
      label = { Text(text = label) },
      singleLine = true,
      isError = name.length > 20,
      supportingText = {
         if (name.length > 20) {
            Text("Zu lang (maximal 20 Zeichen)")
         }
      },

      keyboardOptions = KeyboardOptions.Default.copy(
         autoCorrectEnabled = false,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Done
      ),

      keyboardActions = KeyboardActions(
         onAny = {
            Alog.d("<-InputName", "keyboardActions, onNameChange:$name")
            onNameChange(name) // Event ↑
         }
      ),
   )
}
