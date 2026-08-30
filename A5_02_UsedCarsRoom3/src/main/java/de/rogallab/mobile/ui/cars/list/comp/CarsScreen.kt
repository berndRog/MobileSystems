package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsScreen(
   carsUiState: CarsUiState,
   lazyListState: LazyListState,
   contentPadding: PaddingValues,
   onIntent: (CarsIntent) -> Unit,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   Box(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      Column(modifier = Modifier.fillMaxSize()) {
         TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text(stringResource(R.string.cars_title)) },
         )

         if (carsUiState.isLoading && carsUiState.cars.isEmpty()) {
            Box(
               modifier = Modifier.fillMaxSize(),
               contentAlignment = Alignment.Center,
            ) {
               CircularProgressIndicator()
            }
         }
         else {
            val detailContentDescription = stringResource(R.string.accessibility_edit_car)
            val deleteContentDescription = stringResource(R.string.accessibility_delete_car)

            LazyColumn(
               state = lazyListState,
               contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
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
                     val sellerName = carsUiState.people.firstOrNull { it.id == car.sellerId }?.displayName.orEmpty()
                     CarCard(car = car, sellerName = sellerName)
                  }
               }
            }

         }
      }

      ExtendedFloatingActionButton(
         modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
         onClick = { onIntent(CarsIntent.Create) },
         icon = {
            Icon(Icons.Default.Add, contentDescription = null)
         },
         text = { Text(stringResource(R.string.action_create)) },
      )
   }
}
