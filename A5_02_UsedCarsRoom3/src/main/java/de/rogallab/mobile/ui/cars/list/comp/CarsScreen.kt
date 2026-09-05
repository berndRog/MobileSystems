package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.cars.list.CarsUiState

private const val TAG = "<-CarsScreen"

@Composable
fun CarsScreen(
   carsUiState: CarsUiState,
   lazyListState: LazyListState,
   onIntent: (CarsIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   val detailContentDescription = stringResource(R.string.accessibility_edit_car)
   val deleteContentDescription = stringResource(R.string.accessibility_delete_car)

   LazyColumn(
      modifier = modifier,
      state = lazyListState,
      contentPadding = PaddingValues(
         start = 12.dp,
         end = 12.dp,
         bottom = 96.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(items = carsUiState.cars, key = Car::id) { car ->
         SwipeCard(
            onDetail = { onIntent(CarsIntent.Detail(car.id)) },
            onDelete = { onIntent(CarsIntent.RequestRemove(car.id)) },
            detailContentDescription = detailContentDescription,
            deleteContentDescription = deleteContentDescription,
            modifier = Modifier.animateItem(),
         ) {
            val sellerName = carsUiState.people
               .firstOrNull { it.id == car.sellerId }
               ?.displayName
               .orEmpty()
            CarCard(car = car, sellerName = sellerName)
         }
      }
   }
}
