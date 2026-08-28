package de.rogallab.mobile.shared.ui.effects

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Encapsulates the Snackbar variants used by the application UI.
 */
class SnackbarController(
   private val snackbarHostState: SnackbarHostState,
   private val coroutineScope: CoroutineScope,
) {

   // Shows an informational message that disappears automatically.
   fun showMessage(message: String) {
      coroutineScope.launch {
         snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short,
         )
      }
   }

   // Shows an error until it is dismissed by the user.
   fun showError(error: String) {
      coroutineScope.launch {
         snackbarHostState.showSnackbar(
            message = error,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite,
         )
      }
   }

   // Shows a message with an action, for example Delete or Undo.
   fun showAction(
      message: String,
      actionLabel: String,
      onAction: () -> Unit,
      onDismiss: () -> Unit = {},
   ) {
      coroutineScope.launch {
         val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
         )

         when (result) {
            SnackbarResult.ActionPerformed -> onAction()
            SnackbarResult.Dismissed -> onDismiss()
         }
      }
   }
}

/**
 * Creates a SnackbarController whose CoroutineScope belongs to this composition.
 */
@Composable
fun rememberSnackbarController(
   snackbarHostState: SnackbarHostState,
): SnackbarController {
   val coroutineScope = rememberCoroutineScope()

   return remember(snackbarHostState, coroutineScope) {
      SnackbarController(
         snackbarHostState = snackbarHostState,
         coroutineScope = coroutineScope,
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - SnackbarHostState.showSnackbar() ist suspendierend. Der Controller kapselt
 *   den benötigten CoroutineScope, sodass Aufrufer normale Funktionen verwenden.
 *
 * - Die drei öffentlichen Methoden beschreiben bereits die Art der Snackbar:
 *
 *      showMessage() -> kurze Informationsmeldung
 *      showError()   -> Fehlermeldung mit Dismiss-Schaltfläche
 *      showAction()  -> Meldung mit Aktion, z. B. Delete oder Undo
 *
 *   Ein zusätzliches Nachrichtenobjekt mit eigenem Typ ist deshalb nicht
 *   erforderlich. Die Information wäre sonst doppelt modelliert.
 *
 * - Der Controller wird ab A3_03 oberhalb von NavDisplay erzeugt. Sein Scope
 *   bleibt deshalb beim Wechsel zwischen PeopleScreen und PersonScreen erhalten.
 *
 * - SnackbarHostState übernimmt die Verwaltung gleichzeitig angeforderter
 *   Snackbars. Eine eigene Message-Queue, IDs und MessageConsumed-Intents sind
 *   nicht erforderlich.
 *
 * - Der Controller arbeitet ausschließlich mit fertigen Strings. Ressourcen
 *   werden bereits im ViewModel über IStringProvider aufgelöst.
 *
 * - showAction unterscheidet die beiden Ergebnisse einer Action-Snackbar:
 *
 *      ActionPerformed -> onAction()
 *      Dismissed       -> onDismiss()
 *
 *   In A3_04 bestätigt ActionPerformed eine angeforderte Löschung; Dismissed
 *   lässt den Datensatz unverändert. In A3_05 wird dieselbe Infrastruktur für
 *   Undo bzw. das endgültige Repository-Commit verwendet.
 *
 * Lernziele:
 *
 * - Suspendierende UI-Aufrufe hinter normalen Funktionen kapseln.
 * - SnackbarHostState als vorhandene Material-3-Infrastruktur wiederverwenden.
 * - Screenübergreifende Meldungen ohne Coordinator-ViewModel anzeigen.
 * - Unterschiedliche Snackbar-Varianten durch klar benannte Funktionen abbilden.
 * - Bestätigung und Undo über dieselbe Action-Snackbar-Infrastruktur abbilden.
 */
