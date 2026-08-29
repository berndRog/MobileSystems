package de.rogallab.mobile.ui.components

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
import de.rogallab.mobile.domain.utilities.AppLogger.compose

/**
 * Displays a reusable single-line input field with optional input
 * transformation and validation.
 *
 * The actual input value is owned by the calling composable or its ViewModel.
 * This composable only manages presentation-related validation state.
 *
 * Validation is triggered when:
 * - the input field loses focus,
 * - the user selects the Next IME action,
 * - the user selects the Done IME action.
 *
 * While the user edits the value again, an existing validation message is
 * hidden. The field is validated again when editing is finished.
 *
 * @param value Current text value supplied by the state owner.
 *
 * @param onValueChange Called whenever the transformed input value changes.
 * The callback should usually dispatch an intent to the ViewModel.
 *
 * @param label Label displayed inside the outlined text field.
 *
 * @param leadingIcon Optional icon displayed at the beginning of the field.
 *
 * @param validate Reine Validierungsfunktion. Sie liefert bei gültiger Eingabe
 * null, andernfalls eine Fehlermeldung.
 *
 * @param transformInput Optional transformation or filtering function applied
 * before the new value is propagated. It can be used, for example, to filter
 * phone-number characters or convert umlauts in an email address.
 *
 * @param keyboardType Type of software keyboard requested for this input.
 *
 * @param imeAction Action displayed in the software keyboard, such as Next
 * or Done.
 *
 * @param modifier Modifier applied to the complete text field.
 */
@Composable
fun InputValueString(
   modifier: Modifier = Modifier,
   value: String,
   onValueChange: (String) -> Unit,
   label: String,
   leadingIcon: ImageVector? = null,
   validate: (String) -> String? = { null },
   transformInput: (String) -> String = { inputValue -> inputValue },
   keyboardType: KeyboardType = KeyboardType.Text,
   imeAction: ImeAction = ImeAction.Done,
) {
   val tag = "<-InputValueString"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }


   val focusManager = LocalFocusManager.current

   // Prevents validation before the user has interacted with the field.
   // onFocusChanged may also be called during initial composition.
   var hasBeenFocused by rememberSaveable { mutableStateOf(false) }

   // Controls whether validation feedback is currently visible.
   // The input value itself remains outside this composable.
   var showValidation by rememberSaveable { mutableStateOf(false) }

   // Validator returns either:
   // - null: the current value is valid,
   // - String: the current value is invalid.
   // Validation function should be pure and should not update state.
   val errorMessage: String? = if (showValidation) validate(value) else null

   val isError = errorMessage != null

   // Marks editing as finished and enables validation feedback.
   //
   // This function deliberately does not call onValueChange because finishing
   // an edit does not change the input value.
   fun finishEditing() {
      AppLogger.debug(tag, "finishEditing:<$value>")
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
         AppLogger.debug(tag, "onValueChange:<$transformedValue>")

         // Hide an existing error while the user edits the value.
         // Validation is performed again when editing is finished.
         showValidation = false

         // Propagate only actual value changes.
         // Validation itself never invokes this callback.
         if (transformedValue != value) {
            onValueChange(transformedValue)
         }
      },

      label = {
         Text(text = label)
      },

      textStyle = MaterialTheme.typography.bodyLarge,

      // The icon is decorative because the field already has a descriptive label.
      // A null content description prevents duplicate screen-reader announcements.

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

            // FocusDirection.Next follows the logical focus order instead of
            // assuming that the next field is physically below this field.
            focusManager.moveFocus(
               focusDirection = FocusDirection.Next,
            )
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

/*
 * ============================================================================
 * LERNZIELE UND DIDAKTISCHE HINWEISE
 * ============================================================================
 *
 * Lernziele
 * ----------
 *
 * Die Studierenden können nach der Bearbeitung dieses Beispiels:
 *
 * 1. State Hoisting erklären und anwenden.
 *
 *    Der fachlich relevante Eingabewert wird nicht innerhalb der Composable
 *    gespeichert. Er wird über `value` übergeben und über `onValueChange`
 *    verändert.
 *
 *    Datenfluss:
 *
 *       UiState
 *          |
 *          v
 *       InputValueString
 *          |
 *          v
 *       UI Event
 *          |
 *          v
 *       Intent
 *          |
 *          v
 *       ViewModel
 *
 *
 * 2. fachlichen Zustand und lokalen UI-Zustand unterscheiden.
 *
 *    Fachlicher beziehungsweise formularbezogener Zustand:
 *
 *       value
 *
 *    Lokaler Darstellungszustand:
 *
 *       hasBeenFocused
 *       showValidation
 *
 *    Der lokale Zustand steuert ausschließlich, wann eine Fehlermeldung
 *    angezeigt wird. Er verändert nicht den fachlichen Eingabewert.
 *
 *
 * 3. Wertänderung und Validierung als getrennte Vorgänge behandeln.
 *
 *    `onValueChange` wird nur aufgerufen, wenn sich die Eingabe tatsächlich
 *    geändert hat.
 *
 *    Die Validierung bei Fokusverlust löst keine künstliche Wertänderung und
 *    damit auch kein unnötiges Intent an das ViewModel aus.
 *
 *
 * 4. eine reine Validierungsfunktion formulieren.
 *
 *    Die Funktion
 *
 *       (String) -> String?
 *
 *    liefert:
 *
 *       null                 -> Eingabe ist gültig
 *       "Error message"      -> Eingabe ist ungültig
 *
 *    Die Validierungsfunktion verändert selbst keinen Zustand und verursacht
 *    keine Seiteneffekte.
 *
 *
 * 5. Eingabetransformation und Validierung unterscheiden.
 *
 *    `transformInput` verändert oder filtert die aktuelle Benutzereingabe.
 *    Beispiele:
 *
 *       - nicht erlaubte Zeichen entfernen,
 *       - Telefonnummern auf erlaubte Zeichen begrenzen,
 *       - Leerzeichen am Anfang entfernen.
 *
 *    `validate` beurteilt dagegen den bereits transformierten Wert.
 *
 *
 * 6. Tastaturaktionen und Fokussteuerung einsetzen.
 *
 *    `ImeAction.Next` verschiebt den Fokus zum logisch nächsten Eingabefeld.
 *    `ImeAction.Done` beendet die Eingabe und entfernt den Fokus.
 *
 *
 * Didaktische Reduktion
 * ---------------------
 *
 * Die Komponente verwendet weiterhin die value-basierte TextField-API:
 *
 *       value: String
 *       onValueChange: (String) -> Unit
 *
 * Diese Variante zeigt den unidirektionalen Datenfluss besonders deutlich und
 * lässt sich direkt mit UiState, Intent und ViewModel verbinden.
 *
 * Neuere state-based TextField-APIs bieten zusätzliche Möglichkeiten für
 * komplexe Eingabetransformationen. Für die Einführung in State Hoisting,
 * UDF/UDI und formularbezogene Validierung würde diese zusätzliche Abstraktion
 * jedoch zunächst mehr Konzepte gleichzeitig einführen.
 *
 *
 * Didaktische Reihenfolge
 * -----------------------
 *
 * Empfohlene Einführung in mehreren Schritten:
 *
 *    1. einfaches OutlinedTextField,
 *    2. State Hoisting mit value und onValueChange,
 *    3. KeyboardType und ImeAction,
 *    4. Validierung und Fehlermeldung,
 *    5. Validierung bei Fokusverlust,
 *    6. allgemeine Eingabetransformation,
 *    7. Wiederverwendung für Name, E-Mail und Telefonnummer.
 *
 *
 * Verantwortlichkeiten
 * ---------------------
 *
 * Die Composable übernimmt:
 *
 *    - Darstellung,
 *    - lokale Sichtbarkeit der Fehlermeldung,
 *    - Tastaturkonfiguration,
 *    - Fokussteuerung,
 *    - einfache Eingabetransformation.
 *
 * Das ViewModel übernimmt:
 *
 *    - den dauerhaften Formularzustand,
 *    - die Verarbeitung von Intents,
 *    - formularübergreifende Regeln,
 *    - die Entscheidung, ob gespeichert werden darf.
 *
 * Der Use Case beziehungsweise die Domain übernimmt:
 *
 *    - fachliche Invarianten,
 *    - endgültige Prüfung vor dem Speichern,
 *    - Normalisierung fachlicher Werte, sofern erforderlich.
 *
 *
 * Wichtige Erkenntnis
 * -------------------
 *
 * Eine Validierung in der UI verbessert die Bedienbarkeit, ersetzt aber keine
 * fachliche Validierung im Use Case oder in der Domain.
 * ============================================================================
 */

/*
@Composable
fun InputValueString(
   value: String,
   onValueChange: (String) -> Unit,
   label: String,
   leadingIcon: ImageVector? = null,
   validate: (String) -> Pair<Boolean, String> = { false to "" },
   ascii: Boolean = false,
   keyboardType: KeyboardType = KeyboardType.Text,
   imeAction: ImeAction = ImeAction.Done,
   modifier: Modifier = Modifier,
) {
   val tag = "<-InputStringValue"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.value++}") }

   var isError by rememberSaveable { mutableStateOf(false) }
   var errorMessage by rememberSaveable { mutableStateOf("") }
   val focusManager = LocalFocusManager.current

   LaunchedEffect(value) {
      isError = false
      errorMessage = ""
   }

   fun validateAndPropagate(newValue: String) {
      AppLogger.debug(tag, "validateAndPropagate:<$newValue>")
      val (error, text) = validate(newValue)
      isError = error
      errorMessage = text
      onValueChange(newValue)
   }

   OutlinedTextField(
      modifier = modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            if (!focusState.isFocused) { validateAndPropagate(value) }
         },
      value = value,
      onValueChange = { inputValue ->
         var input = inputValue
         if(ascii) input = sanitizeDigit(input) // äöü -> aeoeue for emails
         AppLogger.debug(tag, "onValueChange:<$input>")
         onValueChange(input)
      },
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      leadingIcon = leadingIcon?.let { imageVector ->
         { Icon(imageVector, contentDescription = label) }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
         imeAction = imeAction),
      keyboardActions = KeyboardActions(
         onNext = {
            validateAndPropagate(value)
            focusManager.moveFocus(FocusDirection.Down)
         },
         onDone = {
            validateAndPropagate(value)
            focusManager.clearFocus()
         }
      ),
      isError = isError,
      supportingText = {
         if (isError) Text(text = errorMessage,
            color = MaterialTheme.colorScheme.error)
      },
      trailingIcon = {
         if (isError) Icon(imageVector = Icons.Filled.Error,
            contentDescription = errorMessage,
            tint = MaterialTheme.colorScheme.error)
      }
   )
}

 */