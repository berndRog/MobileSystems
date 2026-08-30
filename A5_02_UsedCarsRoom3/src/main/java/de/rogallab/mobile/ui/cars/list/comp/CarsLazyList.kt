package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.ui.cars.list.CarsIntent

@Composable
fun CarsLazyList(
   cars: List<Car>,
   people: List<Person>,
   lazyListState: LazyListState,
   onIntent: (CarsIntent) -> Unit,
) {
   val detailContentDescription = stringResource(R.string.accessibility_edit_car)
   val deleteContentDescription = stringResource(R.string.accessibility_delete_car)

   LazyColumn(
      state = lazyListState,
      contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(items = cars, key = Car::id) { car ->
         SwipeCard(
            onDetail = { onIntent(CarsIntent.Detail(car.id)) },
            onDelete = { onIntent(CarsIntent.RequestRemove(car.id)) },
            detailContentDescription = detailContentDescription,
            deleteContentDescription = deleteContentDescription,
            modifier = Modifier.animateItem(),
         ) {
            val sellerName = people.firstOrNull { it.id == car.sellerId }?.displayName.orEmpty()
            CarCard(car = car, sellerName = sellerName)
         }
      }
   }
}
