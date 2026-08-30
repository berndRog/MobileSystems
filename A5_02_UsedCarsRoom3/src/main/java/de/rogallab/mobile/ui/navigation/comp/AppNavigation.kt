package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.tdrives.input_detail.comp.TDriveAdapter
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveVmArgs
import de.rogallab.mobile.ui.tdrives.list.comp.TDrivesAdapter
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import de.rogallab.mobile.ui.people.input_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonVmArgs
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.coordinator.CoordinatorIntent
import de.rogallab.mobile.ui.coordinator.CoordinatorViewModel
import de.rogallab.mobile.ui.cars.input_detail.comp.CarAdapter
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.input_detail.CarVmArgs
import de.rogallab.mobile.ui.cars.list.comp.CarsAdapter
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import de.rogallab.mobile.ui.coordinator.CoordinatorEffectHandler
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
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TAG = "<-AppNavigation"

// Central composition point for top-level navigation, feature navigation,
// Snackbar messages and the global Undo workflow.
@Composable
fun AppNavigation(
   coordinatorViewModel: CoordinatorViewModel =
      koinActivityViewModel<CoordinatorViewModel>(),
) {
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount.intValue++}") }

   // Holds three independent standard Navigation 3 back stacks.
   val navigationState = rememberAppNavigationState()

   // Executes navigation operations without owning additional state.
   val navigator = remember(navigationState) {
      AppNavigator(navigationState)
   }

   // Shared SnackbarHost for People, Cars and TestDrives.
   val snackbarHostState = remember { SnackbarHostState() }

   // Controls the visually distinct reverse transition after Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   CoordinatorEffectHandler(
      coordinatorViewModel = coordinatorViewModel,
      snackbarHostState = snackbarHostState,
   )

   Scaffold(
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState)
      },
      bottomBar = {
         AppBottomNavigationBar(
            navItems = navigationState.navItems,
            currentTopLevelNavItem = navigationState.currentTopLevelNavItem,
            onTopLevelSelected = navigator::switchTopLevel,
         )
      },
   ) { contentPadding ->

      // Maps each serializable key to one stateless screen adapter.
      val appEntryProvider = entryProvider {
         entry<PersonListKey> {
            val personListViewModel = koinViewModel<PeopleViewModel>()

            PeopleAdapter(
               viewModel = personListViewModel,
               coordinatorEvents = coordinatorViewModel.personEvents,
               contentPadding = contentPadding,
               onCreate = {
                  navigator.push(PersonKey())
               },
               onOpen = { personId ->
                  navigator.push(PersonKey(personId))
               },
               onRemove = { person, originalIndex ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.RemovePerson(
                        person = person,
                        originalIndex = originalIndex,
                     )
                  )
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
               },
            )
         }

         entry<PersonKey> { personKey ->
            val personViewModel = koinViewModel<PersonViewModel> {
               parametersOf(PersonVmArgs(personKey.personId))
            }

            PersonAdapter(
               viewModel = personViewModel,
               contentPadding = contentPadding,
               onBack = {
                  currentPopReason = PopReason.CANCEL
                  navigator.pop()
               },
               onSave = { person, isNew ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.SavePerson(person, isNew)
                  )
                  currentPopReason = PopReason.SAVE
                  navigator.pop()
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
               },
            )
         }

         entry<CarListKey> {
            val carsViewModel = koinViewModel<CarsViewModel>()

            CarsAdapter(
               viewModel = carsViewModel,
               coordinatorEvents = coordinatorViewModel.carEvents,
               contentPadding = contentPadding,
               onCreate = {
                  navigator.push(CarKey())
               },
               onOpen = { carId ->
                  navigator.push(CarKey(carId))
               },
               onRemove = { car, originalIndex ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.RemoveCar(car, originalIndex)
                  )
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
               },
            )
         }

         entry<CarKey> { carKey ->
            val carViewModel = koinViewModel<CarViewModel> {
               parametersOf(CarVmArgs(carKey.carId))
            }

            CarAdapter(
               viewModel = carViewModel,
               contentPadding = contentPadding,
               onBack = {
                  currentPopReason = PopReason.CANCEL
                  navigator.pop()
               },
               onSave = { car, isNew ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.SaveCar(car, isNew)
                  )
                  currentPopReason = PopReason.SAVE
                  navigator.pop()
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
               },
            )
         }

         entry<TDrivesKey> {
            val testDriveListViewModel =
               koinViewModel<TDrivesViewModel>()

            TDrivesAdapter(
               viewModel = testDriveListViewModel,
               coordinatorEvents = coordinatorViewModel.testDriveEvents,
               contentPadding = contentPadding,
               onCreate = {
                  navigator.push(TDriveKey())
               },
               onOpen = { testDriveId ->
                  navigator.push(TDriveKey(testDriveId))
               },
               onRemove = { testDrive, originalIndex ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.RemoveTDrive(
                        testDrive,
                        originalIndex,
                     )
                  )
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
               },
            )
         }

         entry<TDriveKey> { testDriveKey ->
            val testDriveViewModel = koinViewModel<TDriveViewModel> {
               parametersOf(
                  TDriveVmArgs(
                     testDriveKey.tDriveId
                  )
               )
            }

            TDriveAdapter(
               viewModel = testDriveViewModel,
               contentPadding = contentPadding,
               onBack = {
                  currentPopReason = PopReason.CANCEL
                  navigator.pop()
               },
               onSave = { testDrive, isNew ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.SaveTDrive(
                        testDrive,
                        isNew,
                     )
                  )
                  currentPopReason = PopReason.SAVE
                  navigator.pop()
               },
               onMessage = { message ->
                  coordinatorViewModel.onIntent(
                     CoordinatorIntent.ShowMessage(message)
                  )
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
         popTransitionSpec =
            NavigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec =
            NavigationAnimations.predictivePopTransitionSpec,
      )
   }
}

// Renders the single Material 3 NavigationBar of the application.
@Composable
private fun AppBottomNavigationBar(
   navItems: List<ITopLevelNavItem>,
   currentTopLevelNavItem: ITopLevelNavItem,
   onTopLevelSelected: (ITopLevelNavItem) -> Unit,
) {
   NavigationBar {
      navItems.forEach { topLevelNavItem ->
         val isSelected = topLevelNavItem == currentTopLevelNavItem
         val label = stringResource(topLevelNavItem.labelResourceId)

         NavigationBarItem(
            selected = isSelected,
            onClick = { onTopLevelSelected(topLevelNavItem) },
            icon = {
               Icon(
                  imageVector =
                     if (isSelected) topLevelNavItem.iconActive
                     else topLevelNavItem.iconOutlined,
                  contentDescription = label,
               )
            },
            label = {
               Text(text = label)
            },
         )
      }
   }
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - UsedCarsRoom3 verwendet drei standardmäßige Navigation-3-Back-Stacks aus
//   rememberNavBackStack() und kein eigenes Navigation-ViewModel.
// - Die Material-3-NavigationBar bildet die obere Navigationsebene.
// - Jeder Hauptbereich behält seinen eigenen Navigationsverlauf.
// - AppNavigator protokolliert alle Operationen und Back Stacks für Logcat.
// - NavigationAnimations macht Push, Save, Cancel und Predictive Back durch
//   die bewusst lange Animationsdauer visuell unterscheidbar.
// - Alle sichtbaren Meldungen werden als UiText über den gemeinsamen
//   SnackbarHost ausgegeben; Listen und Formulare besitzen keine Fehlerseiten.
// - Create und Edit verwenden innerhalb eines Aspekts denselben Screen und
//   dieselbe entry-spezifische ViewModel-Klasse.
// - People, Cars und TestDrives werden nur per Swipe in den Listen
//   gelöscht. Im Person-Formular kann ausschließlich das Foto entfernt werden.
