package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import java.util.Locale

@Composable
fun CarSelectionField(
   cars: List<Car>,
   selectedCarId: String?,
   onCarSelected: (String?) -> Unit,
   modifier: Modifier = Modifier,
) {
   SelectionField(
      items = cars,
      selectedId = selectedCarId,
      label = stringResource(R.string.car_selection_label),
      openContentDescription = stringResource(R.string.car_selection_open),
      searchLabel = stringResource(R.string.car_selection_search),
      emptyResultText = stringResource(R.string.car_selection_no_results),
      noneText = stringResource(R.string.car_selection_none),
      itemId = { car -> car.id },
      itemText = { car -> car.displayName },
      matchesQuery = { car, normalizedQuery ->
         car.displayName.lowercase(Locale.getDefault()).contains(normalizedQuery)
      },
      onSelected = onCarSelected,
      modifier = modifier,
   )
}