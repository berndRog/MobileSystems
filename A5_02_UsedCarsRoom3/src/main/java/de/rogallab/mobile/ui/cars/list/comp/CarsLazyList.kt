package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.composables.SwipeEditDeleteItem

@Composable
fun CarsLazyList(
   cars: List<Car>,
   people: List<Person>,
   lazyListState: LazyListState,
   onIntent: (CarsIntent) -> Unit,
) {
   LazyColumn(
      state = lazyListState,
      contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(items = cars, key = Car::id) { car ->
         SwipeEditDeleteItem(
            itemKey = car.id,
            editContentDescription = R.string.accessibility_edit_car,
            deleteContentDescription = R.string.accessibility_delete_car,
            onEdit = { onIntent(CarsIntent.Detail(car.id)) },
            onRemove = { onIntent(CarsIntent.RequestRemove(car.id)) },
         ) {
            val sellerName = people.firstOrNull { it.id == car.sellerId }?.displayName.orEmpty()
            CarCard(car = car, sellerName = sellerName)
         }
      }
   }
}
