package de.rogallab.mobile.ui.people.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.composables.EditableScreenLayout
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import de.rogallab.mobile.ui.people.input_detail.PersonUiState
import de.rogallab.mobile.ui.people.input_detail.PersonValidator

// Stateless editor used for both Create and Edit in this aspect.
//
// The nullable entity in the state separates initial loading from the actual
// form content. User-visible failures are emitted as UiText and displayed by
// the shared SnackbarHost instead of rendering a separate error page.
private const val TAG = "<-PersonScreen"

@Composable
fun PersonScreen(
   personUiState: PersonUiState,
   validator: PersonValidator,
   contentPadding: PaddingValues,
   onIntent: (PersonIntent) -> Unit,
) {
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(TAG, "Composition #${compositionCount++}")
   }

   val person = personUiState.person
   EditableScreenLayout(
      title = if (personUiState.isNew) {
         R.string.person_create_title
      }
      else {
         R.string.person_edit_title
      },
      isLoading = personUiState.isLoading,
      hasContent = person != null,
      contentPadding = contentPadding,
      actionSpacing = 16.dp,
      onCancel = { onIntent(PersonIntent.Cancel) },
      onSave = { onIntent(PersonIntent.Save) },
   ) {
      if (person != null) {
         PersonContent(
            person = person,
            validator = validator,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}
