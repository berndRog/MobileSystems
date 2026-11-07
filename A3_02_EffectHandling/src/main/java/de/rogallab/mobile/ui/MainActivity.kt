package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {

         // One SnackbarHostState and one visible host are shared by all screens.
         val snackbarHostState = remember { SnackbarHostState() }
         val snackbarController = rememberSnackbarController(
            snackbarHostState = snackbarHostState,
         )

         // Create/Detail screen.
         val personViewModel = koinViewModel<PersonViewModel> {
            parametersOf("01000000-0000-0000-0000-000000000000")
            // parametersOf("12345000-0000-0000-0000-000000000000")
         }

         // List screen.
         val list = false
         // val peopleViewModel = koinViewModel<PeopleViewModel>()


         AppTheme {

            Scaffold(
               modifier = Modifier.fillMaxSize(),
               snackbarHost = {
                  SnackbarHost(
                     hostState = snackbarHostState,
                     modifier = Modifier
                        .imePadding()
                        // 56 dp FAB + 16 dp edge + 16 dp gap = 88 dp
                        .padding(bottom = if (list) 88.dp else 0.dp))
               },
            ) { innerPadding ->

               PersonAdapter(
                  viewModel = personViewModel,
                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,
                  modifier = Modifier
                     .padding(innerPadding)
                     .consumeWindowInsets(innerPadding),
               )

//            PeopleAdapter(
//               viewModel = peopleViewModel,
//               onMessage = snackbarController::showMessage,
//               onError = snackbarController::showError,
//               modifier = Modifier.padding(innerPadding)
//            )

            }
         } //
      }
   }
   companion object {
      private const val TAG = "<-MainActivity"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - In A3_02 wird noch keine Navigation verwendet. Für die Demonstration wird
 *   jeweils genau ein Screen ausgewählt: Create, Detail oder List.
 *
 * - Deshalb ist noch kein Coordinator erforderlich. Ein SnackbarController
 *   reicht aus, weil kein Screenwechsel eine Meldung überleben muss.
 *
 * - Effects transportieren bereits aufgelöste Strings. String-Ressourcen
 *   werden im jeweiligen ViewModel über IStringProvider aufgelöst.
 *
 * - Create und Detail sind noch nicht mit einem Backstack verbunden.
 *   Navigation und Undo folgen in späteren Modulen.
 *
 * Lernziele:
 *
 * - Effect-Handling unabhängig von Navigation kennenlernen.
 * - String-Ressourcen im ViewModel über IStringProvider auflösen.
 * - Snackbar-Ausgabe kapseln, ohne selbst CoroutineScope.launch aufzurufen.
 */
