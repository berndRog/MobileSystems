package de.rogallab.mobile.ui.composables

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R

/**
 * Common visual frame for create and edit screens.
 *
 * The feature screen supplies its title, loading/content state, actions and
 * form content. The form remains vertically scrollable and reacts to the IME.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableScreenLayout(
   @StringRes title: Int,
   isLoading: Boolean,
   hasContent: Boolean,
   contentPadding: PaddingValues,
   onCancel: () -> Unit,
   onSave: () -> Unit,
   modifier: Modifier = Modifier,
   actionSpacing: Dp = 40.dp,
   content: @Composable ColumnScope.() -> Unit,
) {
   Column(
      modifier = modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      TopAppBar(
         // The outer Scaffold has already applied the system-bar padding.
         windowInsets = WindowInsets(0),
         title = { Text(stringResource(title)) },
         // auto save on back navigation, because the user might have changed something
         navigationIcon = {
            IconButton(onClick = onSave) {
               Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(R.string.action_save),
               )
            }
         },
      )

      when {
         isLoading -> {
            Column(
               modifier = Modifier.fillMaxSize(),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally,
            ) {
               CircularProgressIndicator()
            }
         }

         hasContent -> {
            Column(
               modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .verticalScroll(rememberScrollState())
                  .imePadding()
                  .padding(
                     start = 16.dp,
                     top = 8.dp,
                     end = 16.dp,
                     bottom = 24.dp,
                  ),
               verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
               content()

               Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(
                     actionSpacing,
                     Alignment.CenterHorizontally,
                  ),
               ) {
                  OutlinedButton(onClick = onCancel) {
                     Text(stringResource(R.string.action_cancel))
                  }
                  Button(onClick = onSave) {
                     Text(stringResource(R.string.action_save))
                  }
               }
            }
         }
      }
   }
}
