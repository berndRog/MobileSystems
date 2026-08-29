package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import de.rogallab.mobile.ui.people.input_detail.PersonUiState
import de.rogallab.mobile.ui.people.input_detail.PersonValidator

/**
 * Stateless UI for creating and editing a person.
 *
 * The screen is always an editor. The only workflow-specific differences are
 * the title and the loading state required for an existing person.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(
   state: PersonUiState,
   validator: PersonValidator,
   contentPadding: PaddingValues,
   onIntent: (PersonIntent) -> Unit,
) {

   val tag = "<-PersonScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      TopAppBar(
         windowInsets = WindowInsets(0), // no insets for the top bar, because the screen is already padded
         title = {
            Text(
               text = if (state.isNew) stringResource(R.string.person_create_title)
               else stringResource(R.string.person_edit_title))
         }
      )

      val person = state.person

      when {
         state.isLoading -> {
            Column(
               modifier = Modifier.fillMaxSize(),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally,
            ) {
               CircularProgressIndicator()
            }
         }

         person != null -> {
            Column(
               modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 16.dp),
               verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
               PersonContent(
                  person = person,
                  validator = validator,
                  onIntent = onIntent,
                  modifier = Modifier.fillMaxWidth(),
               )

               Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(
                     40.dp, Alignment.CenterHorizontally),
               ) {
                  OutlinedButton(onClick = { onIntent(PersonIntent.Cancel) }) {
                     Text(text = stringResource(R.string.action_cancel))
                  }

                  Button(onClick = { onIntent(PersonIntent.Save) }) {
                     Text(text = stringResource(R.string.action_save))
                  }
               }
            }
         }
      }
   }
}
