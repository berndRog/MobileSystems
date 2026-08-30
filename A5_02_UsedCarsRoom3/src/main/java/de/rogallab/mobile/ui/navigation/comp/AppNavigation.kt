package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.input_detail.comp.CarAdapter
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import de.rogallab.mobile.ui.cars.list.comp.CarsAdapter
import de.rogallab.mobile.ui.navigation.AppNavigator
import de.rogallab.mobile.ui.navigation.CarKey
import de.rogallab.mobile.ui.navigation.CarListKey
import de.rogallab.mobile.ui.navigation.ITopLevelNavItem
import de.rogallab.mobile.ui.navigation.NavigationAnimations
import de.rogallab.mobile.ui.navigation.PersonKey
import de.rogallab.mobile.ui.navigation.PersonListKey
import de.rogallab.mobile.ui.navigation.PopReason
import de.rogallab.mobile.ui.navigation.TDriveKey
import de.rogallab.mobile.ui.navigation.TDrivesKey
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import de.rogallab.mobile.ui.tdrives.input_detail.comp.TDriveAdapter
import de.rogallab.mobile.ui.tdrives.list.TDrivesIntent
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import de.rogallab.mobile.ui.tdrives.list.comp.TDrivesAdapter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation() {
   val navigationState = rememberAppNavigationState()
   val navigator = remember(navigationState) { AppNavigator(navigationState) }
   val snackbarHostState = remember { SnackbarHostState() }
   val snackbarController = rememberSnackbarController(snackbarHostState)
   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
         AppBottomNavigationBar(
            navItems = navigationState.navItems,
            currentTopLevelNavItem = navigationState.currentTopLevelNavItem,
            onTopLevelSelected = navigator::switchTopLevel,
         )
      },
   ) { contentPadding ->
      val appEntryProvider = entryProvider {
         entry<PersonListKey> {
            val viewModel = koinViewModel<PeopleViewModel>()
            PeopleAdapter(
               viewModel = viewModel,
               modifier = Modifier.padding(contentPadding).fillMaxSize(),
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onConfirmRemove = { message, actionLabel, personId ->
                  snackbarController.showAction(
                     message = message,
                     actionLabel = actionLabel,
                     onAction = { viewModel.onIntent(PeopleIntent.ConfirmRemove(personId)) },
                  )
               },
               onBack = navigator::pop,
               onNavigateTo = { navigator.push(PersonKey(it)) },
            )
         }
         entry<PersonKey> { key ->
            val viewModel = koinViewModel<PersonViewModel> { parametersOf(key.personId) }
            PersonAdapter(
               viewModel = viewModel,
               modifier = Modifier.padding(contentPadding).fillMaxSize(),
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onNavigateBack = { reason ->
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               },
            )
         }

         entry<CarListKey> {
            val viewModel = koinViewModel<CarsViewModel>()
            CarsAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onConfirmRemove = { message, actionLabel, carId ->
                  snackbarController.showAction(
                     message = message,
                     actionLabel = actionLabel,
                     onAction = { viewModel.onIntent(CarsIntent.ConfirmRemove(carId)) },
                  )
               },
               onNavigateTo = { navigator.push(CarKey(it)) },
            )
         }
         entry<CarKey> { key ->
            val viewModel = koinViewModel<CarViewModel> { parametersOf(key.carId) }
            CarAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onNavigateBack = { reason ->
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               },
            )
         }

         entry<TDrivesKey> {
            val viewModel = koinViewModel<TDrivesViewModel>()
            TDrivesAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onConfirmRemove = { message, actionLabel, tDriveId ->
                  snackbarController.showAction(
                     message = message,
                     actionLabel = actionLabel,
                     onAction = { viewModel.onIntent(TDrivesIntent.ConfirmRemove(tDriveId)) },
                  )
               },
               onNavigateTo = { navigator.push(TDriveKey(it)) },
            )
         }
         entry<TDriveKey> { key ->
            val viewModel = koinViewModel<TDriveViewModel> { parametersOf(key.tDriveId) }
            TDriveAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onNavigateBack = { reason ->
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               },
            )
         }
      }

      NavDisplay(
         entries = navigationState.toDecoratedEntries(appEntryProvider),
         onBack = {
            currentPopReason = PopReason.CANCEL
            navigator.pop()
         },
         transitionSpec = NavigationAnimations.enterTransitionSpec,
         popTransitionSpec = NavigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec = NavigationAnimations.predictivePopTransitionSpec,
      )
   }
}

private fun BackReason.toPopReason(): PopReason = when (this) {
   BackReason.Save -> PopReason.SAVE
   BackReason.Cancel -> PopReason.CANCEL
}

@Composable
private fun AppBottomNavigationBar(
   navItems: List<ITopLevelNavItem>,
   currentTopLevelNavItem: ITopLevelNavItem,
   onTopLevelSelected: (ITopLevelNavItem) -> Unit,
) {
   NavigationBar {
      navItems.forEach { item ->
         val selected = item == currentTopLevelNavItem
         val label = stringResource(item.labelResourceId)
         NavigationBarItem(
            selected = selected,
            onClick = { onTopLevelSelected(item) },
            icon = {
               Icon(
                  imageVector = if (selected) item.iconActive else item.iconOutlined,
                  contentDescription = label,
               )
            },
            label = { Text(label) },
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_02 behält drei unabhängige Navigation-3-Back-Stacks für Personen,
 *   Fahrzeuge und Probefahrten.
 * - SnackbarController ersetzt den früheren Coordinator vollständig.
 * - Jede Liste fordert eine Löschung nur an. Erst die Action der Snackbar
 *   sendet ConfirmRemove an das zuständige ViewModel.
 * - Save/Cancel und Predictive Back verwenden weiterhin die bekannten
 *   unterschiedlichen Navigationstransitionen.
 */
