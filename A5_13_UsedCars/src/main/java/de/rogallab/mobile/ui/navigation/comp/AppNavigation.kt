package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.unit.dp
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

   // One SnackbarHostState, controller, and visible host serve all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   // The bottom bar is hosted once above NavDisplay for all top-level areas.
   val bottomBar: @Composable () -> Unit = {
      AppBottomNavigationBar(
         navItems = navigationState.navItems,
         currentTopLevelNavItem = navigationState.currentTopLevelNavItem,
         onTopLevelSelected = navigator::switchTopLevel,
      )
   }

   val appEntryProvider = entryProvider {
      entry<PersonListKey> {
         val viewModel = koinViewModel<PeopleViewModel>()
         PeopleAdapter(
            viewModel = viewModel,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onConfirmRemove = { message, actionLabel, personId ->
               snackbarController.showAction(
                  message = message,
                  actionLabel = actionLabel,
                  onAction = {
                     viewModel.onIntent(PeopleIntent.ConfirmRemove(personId))
                  },
               )
            },
            onNavigateBack = navigator::pop,
            onNavigateTo = { navigator.push(PersonKey(it)) },
         )
      }

      entry<PersonKey> { key ->
         val viewModel = koinViewModel<PersonViewModel> {
            parametersOf(key.personId)
         }
         PersonAdapter(
            viewModel = viewModel,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onNavigateBack = { reason ->
               currentPopReason = reason.toPopReason()
               navigator.pop()
            },
            onNavigateToCar = { carId ->
               navigator.push(CarKey(carId))
            },
         )
      }

      entry<CarListKey> {
         val viewModel = koinViewModel<CarsViewModel>()
         CarsAdapter(
            viewModel = viewModel,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onConfirmRemove = { message, actionLabel, carId ->
               snackbarController.showAction(
                  message = message,
                  actionLabel = actionLabel,
                  onAction = {
                     viewModel.onIntent(CarsIntent.ConfirmRemove(carId))
                  },
               )
            },
            onNavigateTo = { navigator.push(CarKey(it)) },
         )
      }

      entry<CarKey> { key ->
         val viewModel = koinViewModel<CarViewModel> {
            parametersOf(key.carId)
         }
         CarAdapter(
            viewModel = viewModel,
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
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onConfirmRemove = { message, actionLabel, tDriveId ->
               snackbarController.showAction(
                  message = message,
                  actionLabel = actionLabel,
                  onAction = {
                     viewModel.onIntent(TDrivesIntent.ConfirmRemove(tDriveId))
                  },
               )
            },
            onNavigateTo = { navigator.push(TDriveKey(it)) },
         )
      }

      entry<TDriveKey> { key ->
         val viewModel = koinViewModel<TDriveViewModel> {
            parametersOf(key.tDriveId)
         }
         TDriveAdapter(
            viewModel = viewModel,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onNavigateBack = { reason ->
               currentPopReason = reason.toPopReason()
               navigator.pop()
            },
         )
      }
   }

   Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
               .imePadding()
               .padding(
                  bottom = when (navigationState.currentBackStack().lastOrNull()) {
                     PersonListKey, CarListKey, TDrivesKey -> 80.dp
                     else -> 0.dp
                  },
               ),
         )
      },
      bottomBar = bottomBar,
   ) { innerPadding ->
      NavDisplay(
         modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
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
 * - AppNavigation enthält den äußeren Scaffold mit genau einem SnackbarHost
 *   und der gemeinsamen Bottom-Navigation. Beides bleibt über NavDisplay
 *   beim Wechsel der Ziele erhalten.
 * - Die Adapter behalten ihre eigenen Scaffolds für TopAppBar, FAB und Inhalt;
 *   sie erzeugen keinen weiteren SnackbarHost und keine Bottom-Navigation.
 * - Aus dem Person-Detail kann über das Fahrzeug-Bottom-Sheet direkt zu
 *   CarKey(carId) navigiert werden. PersonKey bleibt dabei auf dem Backstack.
 * - Listenadapter ergänzen TopAppBar und FAB; Detailadapter ergänzen eine
 *   navigierbare TopAppBar. Die Screens selbst bleiben zustandslos.
 * - Save/Cancel und Predictive Back verwenden weiterhin die bekannten
 *   unterschiedlichen Navigationstransitionen.
 */
