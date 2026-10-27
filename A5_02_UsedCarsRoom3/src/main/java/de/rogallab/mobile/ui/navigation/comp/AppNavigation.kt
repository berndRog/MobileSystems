package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.rogallab.mobile.ui.navigation.CarsKey
import de.rogallab.mobile.ui.navigation.ITopLevelNavItem
import de.rogallab.mobile.ui.navigation.NavigationAnimations
import de.rogallab.mobile.ui.navigation.PersonKey
import de.rogallab.mobile.ui.navigation.PeopleKey
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

   // One SnackbarHostState and one controller are shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   // A5_02 keeps its three top-level back stacks and passes the NavigationBar
   // into each destination's Scaffold.
   val bottomBar: @Composable () -> Unit = {
      AppBottomNavigationBar(
         navItems = navigationState.navItems,
         currentTopLevelNavItem = navigationState.currentTopLevelNavItem,
         onTopLevelSelected = navigator::switchTopLevel,
      )
   }

   val appEntryProvider = entryProvider {
      entry<PeopleKey> {
         val viewModel = koinViewModel<PeopleViewModel>()
         PeopleAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
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
            onNavigateBack = {
               if (navigationState.currentBackStack().lastOrNull() == PeopleKey) navigator.pop()
            },
            onNavigateTo = { personId ->
               if (navigationState.currentBackStack().lastOrNull() == PeopleKey) {
                  navigator.push(PersonKey(personId))
               }
            },
         )
      }

      entry<PersonKey> { key ->
         val viewModel = koinViewModel<PersonViewModel> {
            parametersOf(key.personId)
         }
         PersonAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onNavigateBack = { reason ->
               if (navigationState.currentBackStack().lastOrNull() == key) {
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               }
            },
            onNavigateToCar = { carId ->
               if (navigationState.currentBackStack().lastOrNull() == key) {
                  navigator.push(CarKey(carId))
               }
            },
         )
      }

      entry<CarsKey> {
         val viewModel = koinViewModel<CarsViewModel>()
         CarsAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
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
            onNavigateTo = { carId ->
               if (navigationState.currentBackStack().lastOrNull() == CarsKey) {
                  navigator.push(CarKey(carId))
               }
            },
         )
      }

      entry<CarKey> { key ->
         val viewModel = koinViewModel<CarViewModel> {
            parametersOf(key.carId)
         }
         CarAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onNavigateBack = { reason ->
               if (navigationState.currentBackStack().lastOrNull() == key) {
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               }
            },
         )
      }

      entry<TDrivesKey> {
         val viewModel = koinViewModel<TDrivesViewModel>()
         TDrivesAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
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
            onNavigateTo = { tDriveId ->
               if (navigationState.currentBackStack().lastOrNull() == TDrivesKey) {
                  navigator.push(TDriveKey(tDriveId))
               }
            },
         )
      }

      entry<TDriveKey> { key ->
         val viewModel = koinViewModel<TDriveViewModel> {
            parametersOf(key.tDriveId)
         }
         TDriveAdapter(
            viewModel = viewModel,
            bottomBar = bottomBar,
            onMessage = snackbarController::showMessage,
            onError = snackbarController::showError,
            onNavigateBack = { reason ->
               if (navigationState.currentBackStack().lastOrNull() == key) {
                  currentPopReason = reason.toPopReason()
                  navigator.pop()
               }
            },
         )
      }
   }

   Scaffold(
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding(),
         )
      },
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
 * - AppNavigation besitzt den einzigen SnackbarHost oberhalb von NavDisplay.
 *   SnackbarHostState und SnackbarController bleiben bei Zielwechseln erhalten.
 * - Jeder Adapter stellt weiterhin seinen Scaffold mit TopAppBar und
 *   Bottom-Navigation bereit. Die standardmäßigen Insets bleiben erhalten.
 * - Die gemeinsame Bottom-Navigation wird als Composable an die Adapter
 *   delegiert. So bleibt sie in allen drei Top-Level-Bereichen identisch.
 * - Aus dem Person-Detail kann über das Fahrzeug-Bottom-Sheet direkt zu
 *   CarKey(carId) navigiert werden. PersonKey bleibt dabei auf dem Backstack.
 * - Listenadapter ergänzen TopAppBar und FAB; Detailadapter ergänzen eine
 *   navigierbare TopAppBar. Die Screens selbst bleiben zustandslos.
 * - Save/Cancel und Predictive Back verwenden weiterhin die bekannten
 *   unterschiedlichen Navigationstransitionen. Verspätete Effects ändern
 *   nur den aktuell sichtbaren Back-Stack-Eintrag.
 */
