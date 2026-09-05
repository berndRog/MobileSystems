package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.cars.list.CarsEffect
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.cars.list.CarsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarsAdapter(
   viewModel: CarsViewModel,
   snackbarHostState: SnackbarHostState,
   bottomBar: @Composable () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, String) -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val carsUiState by viewModel.stateFlow.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is CarsEffect.ShowMessage -> onMessage(effect.message)
         is CarsEffect.ShowError -> onError(effect.message)
         is CarsEffect.ConfirmRemove -> onConfirmRemove(
            effect.message,
            effect.actionLabel,
            effect.carId,
         )
         is CarsEffect.NavigateTo -> onNavigateTo(effect.carId)
      }
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(title = { Text(stringResource(R.string.cars_title)) })
      },
      floatingActionButton = {
         ExtendedFloatingActionButton(
            containerColor = colorScheme.secondary,
            onClick = { viewModel.onIntent(CarsIntent.Create) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.action_create)) },
         )
      },
      bottomBar = bottomBar,
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding(),
         )
      },
   ) { innerPadding ->
      if (carsUiState.isLoading && carsUiState.cars.isEmpty()) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      }
      else {
         CarsScreen(
            carsUiState = carsUiState,
            lazyListState = lazyListState,
            onIntent = viewModel::onIntent,
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - CarsAdapter übernimmt analog zu PeopleAdapter den vollständigen Scaffold.
 * - CarsScreen bleibt auf die Darstellung der Fahrzeugliste beschränkt.
 * - TopAppBar, FAB, SnackbarHost und Bottom-Navigation sind Rahmenaufgaben des
 *   Adapters und nicht Bestandteil des zustandslosen Screens.
 */
