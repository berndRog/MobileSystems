package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import java.util.Locale

// Selects a person by searching for last name or first name.
//
// The result list intentionally contains text only. This is sufficient for
// seller and interested-person selection and keeps the relation UI compact.
@Composable
fun PersonSelectionField(
   people: List<Person>,
   selectedPersonId: String?,
   label: String,
   allowNone: Boolean,
   onPersonSelected: (String?) -> Unit,
   modifier: Modifier = Modifier,
) {
   val noneText = stringResource(R.string.person_selection_none)
   SelectionField(
      items = people,
      selectedId = selectedPersonId,
      label = label,
      openContentDescription = stringResource(R.string.person_selection_open),
      searchLabel = stringResource(R.string.person_selection_search),
      emptyResultText = stringResource(R.string.person_selection_no_results),
      noneText = if (allowNone) noneText else null,
      itemId = { person -> person.id },
      itemText = { person -> person.selectionName },
      matchesQuery = { person, normalizedQuery ->
         person.lastName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            person.firstName.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
            person.selectionName.lowercase(Locale.getDefault()).contains(normalizedQuery)
      },
      onSelected = onPersonSelected,
      modifier = modifier,
   )
}

private val Person.selectionName: String
   get() = listOf(lastName.trim(), firstName.trim())
      .filter { namePart -> namePart.isNotBlank() }
      .joinToString(", ")
