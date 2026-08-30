package de.rogallab.mobile.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.domain.utilities.AppLogger

// Reusable single-line input with optional transformation and validation.
//
// The caller owns the actual value. This composable keeps only presentation
// state that controls when a validation message becomes visible.

private const val TAG = "<-InputValueString"

@Composable
fun InputValueString(
   modifier: Modifier = Modifier,
   value: String,
   onValueChange: (String) -> Unit,
   label: String,
   leadingIcon: ImageVector? = null,
   validate: (String) -> String? = { inputValue -> null },
   transformInput: (String) -> String = { inputValue -> inputValue },
   keyboardType: KeyboardType = KeyboardType.Text,
   imeAction: ImeAction = ImeAction.Done,
) {

   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   val focusManager = LocalFocusManager.current

   // Prevents validation before the user has interacted with the field.
   var hasBeenFocused by rememberSaveable { mutableStateOf(false) }

   // Hides an old error while the user edits and shows it after editing.
   var showValidation by rememberSaveable { mutableStateOf(false) }

   val errorMessage = if (showValidation) validate(value) else null
   val isError = errorMessage != null

   fun finishEditing() {
      AppLogger.verbose(TAG, "finishEditing:<$value>")
      showValidation = true
   }

   OutlinedTextField(
      modifier = modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            when {
               focusState.isFocused -> hasBeenFocused = true
               hasBeenFocused -> finishEditing()
            }
         },
      value = value,
      onValueChange = { newValue ->
         val transformedValue = transformInput(newValue)
         AppLogger.debug(TAG, "onValueChange:<$transformedValue>")

         showValidation = false
         if (transformedValue != value) {
            onValueChange(transformedValue)
         }
      },
      label = {
         Text(text = label)
      },
      textStyle = MaterialTheme.typography.bodyLarge,
      leadingIcon = leadingIcon?.let { icon ->
         {
            Icon(
               imageVector = icon,
               contentDescription = null,
            )
         }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
         keyboardType = keyboardType,
         imeAction = imeAction,
      ),
      keyboardActions = KeyboardActions(
         onNext = {
            finishEditing()
            focusManager.moveFocus(FocusDirection.Next)
         },
         onDone = {
            finishEditing()
            focusManager.clearFocus()
         },
      ),
      isError = isError,
      supportingText = errorMessage?.let { message ->
         {
            Text(
               text = message,
               color = MaterialTheme.colorScheme.error,
            )
         }
      },
      trailingIcon = errorMessage?.let { message ->
         {
            Icon(
               imageVector = Icons.Filled.Error,
               contentDescription = message,
               tint = MaterialTheme.colorScheme.error,
            )
         }
      },
   )
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - Der fachliche Eingabewert wird per State Hoisting vom Aufrufer verwaltet.
// - hasBeenFocused und showValidation sind ausschließlich lokaler UI-Zustand.
// - Eingabetransformation und Validierung bleiben getrennte Operationen.
// - Die Validierungsfunktion ist rein und liefert String? zurück.
// - Next und Done zeigen die Verbindung von Tastaturaktion und Fokussteuerung.
// - Der Zähler macht erfolgreiche Compositions und Recompositions sichtbar.
