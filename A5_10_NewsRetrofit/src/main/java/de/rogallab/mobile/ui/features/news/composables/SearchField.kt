package de.rogallab.mobile.ui.features.news.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import kotlinx.coroutines.delay

@Composable
fun SearchField(
   searchText: String,
   onSearchTextChange: (String) -> Unit,
   onTriggerSearch: () -> Unit
) {
   var localSearchText by rememberSaveable { mutableStateOf(searchText) }
   var keyboardController = LocalSoftwareKeyboardController.current

   // Update localSearchText when name changes
   LaunchedEffect(searchText) {
      localSearchText = searchText
   }

   // Debounce mechanism to delay onNameChange call
   LaunchedEffect(localSearchText) {
      delay(300) // Adjust delay as needed
      if (localSearchText != searchText) {
         onSearchTextChange(localSearchText)
      }
   }

   OutlinedTextField(
      modifier = Modifier
         .padding(bottom = 8.dp)
         .fillMaxWidth(),
      value = localSearchText,
      onValueChange = {
         localSearchText = it
         // onSearchTextChange(it) see debouncing
      },
      label = {
         Text(text = stringResource(R.string.searchtext))
      },
      leadingIcon = {
         Icon(imageVector = Icons.Outlined.Search,
            contentDescription = "Search News",
            modifier = Modifier.clickable { onTriggerSearch() })
      },
      trailingIcon = {
         Icon(imageVector = Icons.Outlined.Clear,
            contentDescription = "Delete  search text",
            modifier = Modifier.clickable {
               localSearchText = ""
               onSearchTextChange("")
               onTriggerSearch()
            })
      },
      keyboardOptions = KeyboardOptions(
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Search
      ),
      keyboardActions = KeyboardActions(
         onSearch = {
            onTriggerSearch()
            keyboardController?.hide()
         }
      ),
      textStyle = MaterialTheme.typography.titleMedium,
      singleLine = true,
   )
}