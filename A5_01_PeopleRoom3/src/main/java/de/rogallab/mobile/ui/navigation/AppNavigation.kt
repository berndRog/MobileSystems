package de.rogallab.mobile.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorIntent
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorViewModel
import de.rogallab.mobile.ui.people.input_detail.composables.PersonAdapter
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonVmArgs
import de.rogallab.mobile.ui.people.list.composables.PeopleAdapter
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


private const val TAG = "<-PeopleNavigation"

@Composable
fun PeopleNavigation(
   coordinatorViewModel: PeopleCoordinatorViewModel =
      koinActivityViewModel<PeopleCoordinatorViewModel>(),
   personListViewModel: PeopleViewModel =
      koinActivityViewModel<PeopleViewModel>(),
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(TAG, "Composition #${nComp.intValue++}") }

   // Creates the standard saveable Navigation 3 back stack.
   val backStack = rememberNavBackStack(PeopleListKey)

   // Observes the coordinator state in a lifecycle-aware manner.
   val coordinatorState by
      coordinatorViewModel.state.collectAsStateWithLifecycle()

   // One SnackbarHostState is shared by all screens in this navigation graph.
   val snackbarHostState = remember { SnackbarHostState() }

   val context = LocalContext.current
   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   // Side-Effects Events & Snackbar
   PeopleCoordinatorEffectHandler(
      coordinatorViewModel = coordinatorViewModel,
      personListViewModel = personListViewModel,
      snackbarHostState = snackbarHostState,
   )

   // Logs the initial or restored back stack when this navigation enters
   // the composition.
   LaunchedEffect(backStack) {
      logNavigationOperation(operation = "initial",
         destination = backStack.lastOrNull(), backStack = backStack)
   }

   // Provides one shared layout container for navigation and Snackbar output
   // -----------------------------------------------------------------------
   Scaffold(
      //contentWindowInsets = WindowInsets.safeContent,
      //modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 8.dp),
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
         )
      },
   ) { contentPadding ->

      // Displays the destination represented by the last key of the back stack.
      //
      // NavDisplay observes changes to the back stack. Adding or removing a key
      // automatically changes the visible navigation entry.
      NavDisplay(
         modifier = Modifier,
         backStack = backStack,

         // Handles system back navigation.
         onBack = {
            currentPopReason = PopReason.CANCEL
            pop(backStack)
         },

         // Preserves saveable Compose state separately for every navigation
         // entry, for example scroll positions and rememberSaveable values.
         //
         // The ViewModelStore decorator also gives every navigation entry its
         // own ViewModelStoreOwner. A PersonKey therefore receives its own
         // PersonViewModel instance.
         entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(
               rememberSaveableStateHolder()
            ),
            rememberViewModelStoreNavEntryDecorator(),
         ),

         // Standard Android navigation animations:
         transitionSpec = NavigationAnimations.enterTransitionSpec,
         popTransitionSpec = NavigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec = NavigationAnimations.predictivePopTransitionSpec,

         // Maps every supported navigation key to its visible screen content.
         entryProvider = entryProvider {

            // Root entry of the application.
            //
            // The list ViewModel is activity-scoped because the list state is
            // shared with the coordinator and is needed when a person is
            // restored after an Undo action.
            entry<PeopleListKey> {
               PeopleAdapter(
                  viewModel = personListViewModel,
                  contentPadding = contentPadding,

                  // Opens the shared person screen in create mode, i.e Id is null
                  onCreate = { push(PersonKey(), backStack) },

                  // Opens the shared person screen in edit mode.
                  onOpen = { personId -> push(PersonKey(personId), backStack) },

                  // Starts the coordinated remove and undo workflow.
                  onRemove = { person, originalIndex ->
                     coordinatorViewModel.onIntent(
                        PeopleCoordinatorIntent.RemovePerson(person, originalIndex))
                  },

                  // Forwards list-loading errors to the shared Snackbar host.
                  onMessage = { message ->
                     coordinatorViewModel.onIntent(
                        PeopleCoordinatorIntent.ShowMessage(message))
                  },
               )
            }

            // Shared navigation entry for creating and editing a person.
            //
            // The nullable personId of PersonKey determines the screen mode.
            entry<PersonKey> { personKey ->

               // Creates a PersonViewModel scoped to this PersonKey entry.
               val personViewModel = koinViewModel<PersonViewModel> {
                  parametersOf(PersonVmArgs(personKey.personId))
               }

               PersonAdapter(
                  viewModel = personViewModel,
                  contentPadding = contentPadding,

                  // Cancels the current create or edit operation and returns
                  // to the previous navigation entry.
                  onBack = {
                     currentPopReason = PopReason.CANCEL
                     pop(backStack)
                  },

                  // The PersonViewModel has already performed final validation.
                  // The coordinator persists the create or update operation and
                  // publishes the resulting Snackbar message.
                  onSave = { person, isNew ->
                     coordinatorViewModel.onIntent(
                        PeopleCoordinatorIntent.SavePerson(person = person, isNew = isNew, ))
                     pop(backStack)
                  },

                  // Forwards validation and image-storage messages from the
                  // feature screen to the shared Snackbar coordinator.
                  onMessage = { message ->
                     coordinatorViewModel.onIntent(
                        PeopleCoordinatorIntent.ShowMessage(message))
                  },
               )
            }
         },
      )
   } // Scaffold
}

// Navigation backstack operations
// --------------------------------------------------------------------------
// Adds a destination to the standard Navigation 3 back stack.
fun push(destination: NavKey, backStack: MutableList<NavKey>) {
   backStack.add(destination)

   logNavigationOperation(operation = "push",
      destination = destination, backStack = backStack)
}

// Removes the current destination while preserving the root destination.
// When only PeopleListKey remains, the operation is ignored. This prevents
// the application from creating an empty navigation back stack.
fun pop(backStack: MutableList<NavKey>) {
   if (backStack.size > 1) {
      val removedDestination = backStack.removeLastOrNull()
      logNavigationOperation(operation = "pop",
         destination = removedDestination, backStack = backStack)
   } else {
      logNavigationOperation(
         operation = "pop ignored - root remains",
         destination = backStack.lastOrNull(), backStack = backStack)
   }
}


// Logs one navigation operation without modifying navigation state.
//
// The output contains the operation, the affected destination and the
// complete current back stack.
private fun logNavigationOperation(
   operation: String,
   destination: NavKey?,
   backStack: List<NavKey>,
) {
   val destinationText =
      destination?.toString() ?: "none"

   val stackText = backStack.joinToString(
      separator = " -> ",
      prefix = "[",
      postfix = "]",
   )

   AppLogger.debug(
      tag = TAG,
      message = "$operation: $destinationText | " +
         "stack(${backStack.size}) = $stackText",
   )
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - PeopleRoom3 verwendet den standardmäßigen Navigation-3-Back-Stack aus
//   rememberNavBackStack() und kein eigenes Navigations-ViewModel.
// - Navigation wird durch die Listenoperationen add() und removeLastOrNull()
//   sichtbar und unmittelbar verändert.
// - Die Log-Ausgabe zeigt jede Navigationoperation, das betroffene Ziel und
//   anschließend den vollständigen aktuellen Back Stack.
// - PeopleListKey bleibt als Wurzelziel im Stack erhalten.
// - Die Entry-Decorators zeigen zwei unterschiedliche Zuständigkeiten:
//   speicherbaren Compose-Zustand und ViewModel-Scoping.
// - Jeder PersonKey erhält über den ViewModelStore-Decorator eine eigene
//   PersonViewModel-Instanz für Create oder Edit.
// - Personen werden ausschließlich per Swipe-to-Delete in der Liste entfernt.
// - Snackbar und Undo bleiben als UI-Koordination im
//   PeopleCoordinatorViewModel gebündelt.
