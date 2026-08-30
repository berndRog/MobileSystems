package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionField(
   items: List<T>,
   selectedId: String?,
   label: String,
   openContentDescription: String,
   searchLabel: String,
   emptyResultText: String,
   noneText: String? = null, // null = kein "None"-Eintrag
   itemId: (T) -> String,
   itemText: (T) -> String,
   matchesQuery: (T, String) -> Boolean,
   onSelected: (String?) -> Unit,
   modifier: Modifier = Modifier,
) {
   val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
   var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
   var searchQuery by rememberSaveable { mutableStateOf("") }

   val selectedItem = items.firstOrNull { item -> itemId(item) == selectedId }
   val selectedText = selectedItem?.let(itemText) ?: noneText.orEmpty()

   val filteredItems = remember(items, searchQuery) {
      val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
      items
         .sortedBy { item -> itemText(item).lowercase(Locale.getDefault()) }
         .filter { item ->
            normalizedQuery.isBlank() || matchesQuery(item, normalizedQuery)
         }
   }

   Column(
      modifier = modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
   ) {
      Text(text = label)

      OutlinedButton(
         modifier = Modifier.fillMaxWidth(),
         onClick = {
            searchQuery = ""
            showSelectionSheet = true
         },
      ) {
         Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = selectedText)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
               imageVector = Icons.Default.ArrowDropDown,
               contentDescription = openContentDescription,
            )
         }
      }
   }

   if (showSelectionSheet) {
      ModalBottomSheet(
         onDismissRequest = { showSelectionSheet = false },
         sheetState = sheetState,
      ) {
         Column(
            modifier = Modifier
               .fillMaxWidth()
               .imePadding()
               .padding(
                  start = 24.dp,
                  end = 24.dp,
                  bottom = 32.dp,
               ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
         ) {
            Text(
               text = label,
               style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
               modifier = Modifier.fillMaxWidth(),
               value = searchQuery,
               onValueChange = { value -> searchQuery = value },
               singleLine = true,
               label = { Text(searchLabel) },
               leadingIcon = {
                  Icon(
                     imageVector = Icons.Default.Search,
                     contentDescription = null,
                  )
               },
            )

            LazyColumn(
               modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 420.dp),
            ) {
               if (noneText != null) {
                  item(key = "__none__") {
                     SelectionTextRow(
                        text = noneText,
                        onClick = {
                           onSelected(null)
                           showSelectionSheet = false
                        },
                     )
                     HorizontalDivider()
                  }
               }

               items(
                  items = filteredItems,
                  key = itemId,
               ) { item ->
                  SelectionTextRow(
                     text = itemText(item),
                     onClick = {
                        onSelected(itemId(item))
                        showSelectionSheet = false
                     },
                  )
                  HorizontalDivider()
               }

               if (filteredItems.isEmpty()) {
                  item(key = "__empty__") {
                     Text(
                        modifier = Modifier.padding(vertical = 20.dp),
                        text = emptyResultText,
                     )
                  }
               }
            }
         }
      }
   }
}

@Composable
private fun SelectionTextRow(
   text: String,
   onClick: () -> Unit,
) {
   Text(
      modifier = Modifier
         .fillMaxWidth()
         .clickable(onClick = onClick)
         .padding(vertical = 16.dp),
      text = text,
      style = MaterialTheme.typography.bodyLarge,
   )
}
