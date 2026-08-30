package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.composables.SwipeEditDeleteItem

private const val TAG = "<-CarsLazyList"

@Composable
fun CarsLazyList(
   cars: List<Car>,
   people: List<Person>,
   lazyListState: LazyListState,
   onIntent: (CarsIntent) -> Unit,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   LazyColumn(
      state = lazyListState,
      contentPadding = PaddingValues(
         start = 12.dp,
         end = 12.dp,
         bottom = 96.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      itemsIndexed(
         items = cars,
         key = { _, car -> car.id },
      ) { originalIndex, car ->

         SwipeEditDeleteItem(
            itemKey = car.id,
            editContentDescription = R.string.accessibility_edit_car,
            deleteContentDescription = R.string.accessibility_delete_car,
            onEdit = { onIntent(CarsIntent.Open(car.id)) },
            onRemove = { onIntent( CarsIntent.Remove(car, originalIndex, )) }
         ) {
            val sellerName = people.firstOrNull { person ->
               person.id == car.sellerId
            }?.displayName.orEmpty()

            CarCard(
               car = car,
               sellerName = sellerName
            )
         }
      }
   }
}

