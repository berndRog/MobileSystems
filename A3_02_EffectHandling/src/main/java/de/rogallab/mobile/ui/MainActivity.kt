package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import de.rogallab.mobile.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {

         // One Snackbar host is sufficient because only one screen is selected
         // manually in this learning step.
         val snackbarHostState = remember { SnackbarHostState() }
         val snackbarController = rememberSnackbarController(
            snackbarHostState = snackbarHostState,
         )

         // Detail screen.
         val personViewModel = koinViewModel<PersonViewModel> {
            parametersOf("01000000-0000-0000-0000-000000000000")
         }

         // List screen.
         val peopleViewModel = koinViewModel<PeopleViewModel>()

         AppTheme {

            PersonAdapter(
               viewModel = personViewModel,
               snackbarHostState = snackbarHostState,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError
            )

//            PeopleAdapter(
//               viewModel = peopleViewModel,
//               snackbarHostState = snackbarHostState,
//               onMessage = snackbarController::showMessage,
//               onError = snackbarController::showError,
//            )

         }
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
 * - Navigation und Undo sind bereits in Effects und Callbacks vorbereitet,
 *   ihre Funktionen bleiben in diesem Schritt bewusst leer.
 *
 * Lernziele:
 *
 * - Effect-Handling unabhängig von Navigation kennenlernen.
 * - String-Ressourcen im ViewModel über IStringProvider auflösen.
 * - Snackbar-Ausgabe kapseln, ohne selbst CoroutineScope.launch aufzurufen.
 */
