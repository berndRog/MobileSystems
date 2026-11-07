package de.rogallab.mobile.ui.navigation.comp

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import de.rogallab.mobile.ui.navigation.PeopleKey
import de.rogallab.mobile.ui.navigation.PersonKey
import de.rogallab.mobile.ui.navigation.PopReason
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Maps each [NavKey] to the screen that is shown for it.
 *
 * This function only wires keys to screens (adapter + ViewModel). It contains
 * no navigation logic: all dependencies are passed in as parameters, and the
 * back stack is never modified here.
 *
 * @param backStack the live back stack, used read-only by the guards below.
 *   Pass the same instance that NavDisplay uses, not a copy, so that the
 *   guards always see the current top entry.
 * @param onMessage shows an informational message (e.g. in a Snackbar).
 * @param onError shows an error message (e.g. in a Snackbar).
 * @param onNavigateBack removes the current destination; the [PopReason]
 *   selects the pop animation (Save or Cancel).
 * @param onNavigateTo adds a [PersonKey] on top of the back stack.
 * @return an entry provider that can be passed to NavDisplay.
 */
fun appNavEntryProvider(
   backStack: List<NavKey>,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (PopReason) -> Unit,
   onNavigateTo: (PersonKey) -> Unit,
): (NavKey) -> NavEntry<NavKey> {

   return entryProvider<NavKey> {

      // Root destination: list of people.
      entry<PeopleKey> { peopleKey ->
         // ViewModel is scoped to this NavEntry by the ViewModelStore decorator.
         val peopleViewModel = koinViewModel<PeopleViewModel>()

         PeopleAdapter(
            viewModel = peopleViewModel,

            // Global messages are shown by the single SnackbarHost above NavDisplay.
            onMessage = onMessage,
            onError = onError,

            // System back on the root is a cancel; remove() keeps the root entry.
            onNavigateBack = { onNavigateBack(PopReason.Cancel) },

            // null -> create, id -> detail/edit.
            onNavigateTo = { personId ->
               // Guard: ignore late or repeated effects (e.g. a double tap)
               // when this entry is no longer the top of the back stack.
               if (backStack.lastOrNull() === peopleKey) {
                  onNavigateTo(PersonKey(personId))
               }
            },
         )
      }

      // Shared destination for create and edit.
      entry<PersonKey> { personKey ->
         // The personId is passed to Koin as a parameter (null -> create).
         val personViewModel = koinViewModel<PersonViewModel> {
            parametersOf(personKey.personId)
         }

         PersonAdapter(
            viewModel = personViewModel,

            onMessage = onMessage,
            onError = onError,

            onNavigateBack = { reason ->
               // Guard: a late navigation effect must only affect its own
               // entry. If another destination is on top, ignore it.
               if (backStack.lastOrNull() == personKey) {
                  onNavigateBack(
                     // Translate the screen-level reason into the
                     // navigation-level reason that selects the animation.
                     when (reason) {
                        BackReason.Save -> PopReason.Save
                        BackReason.Cancel -> PopReason.Cancel
                     }
                  )
               }
            },
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - appNavEntryProvider ist aus AppNavigation ausgelagert, damit AppNavigation
 *   übersichtlich bleibt und trotzdem alles im selben Package liegt:
 *
 *      AppNavigation       -> Scaffold, SnackbarHost, Back-Stack, Animationen
 *      appNavEntryProvider -> Zuordnung NavKey -> Screen (Adapter + ViewModel)
 *
 * - Der Provider enthält keine Navigationslogik. Er verändert den Back-Stack
 *   nicht, sondern ordnet nur Keys Screens zu und reicht Verhalten an die
 *   Adapter durch.
 *
 * - Alle Abhängigkeiten werden explizit als Parameter übergeben:
 *
 *      backStack      -> List<NavKey>, nur lesend, für die Guards
 *      onMessage      -> (String) -> Unit
 *      onError        -> (String) -> Unit
 *      onNavigateBack -> (PopReason) -> Unit
 *      onNavigateTo   -> (PersonKey) -> Unit
 *
 *   Der Provider kennt weder SnackbarController noch SnackbarHostState. Mit
 *   Function-Typen statt konkreter Klassen kennt er nur das Verhalten, das er
 *   tatsächlich braucht, und lässt sich ohne UI-Infrastruktur testen.
 *   Streng "pure" ist die Funktion nicht, weil backStack ein veränderlicher
 *   Zustand bleibt, den die Guards lesen. Entscheidend ist, dass alle
 *   Abhängigkeiten sichtbar in der Signatur stehen und kein globaler Zustand
 *   verwendet wird.
 *
 * - Der Back-Stack muss dieselbe Instanz sein, die NavDisplay verwendet. Eine
 *   Kopie würde die Guards wirkungslos machen, weil sie den aktuellen
 *   obersten Eintrag nicht mehr sehen.
 *
 * - Die Guards verhindern, dass verspätete oder doppelte Navigationseffekte
 *   (z. B. ein zweiter Tap oder ein Effekt nach dem Speichern) einen anderen
 *   Eintrag beeinflussen. Ein Effekt wirkt nur, wenn sein Entry noch oben auf
 *   dem Back-Stack liegt.
 *
 * - BackReason (Screen-Ebene) wird in PopReason (Navigations-Ebene) übersetzt.
 *   So kennt der Screen die Animation nicht, und die Art der Rücknavigation
 *   (Save oder Cancel) bleibt über die Animation sichtbar.
 *
 * - Die ViewModels werden pro NavEntry über Koin erzeugt. Die personId wird
 *   als Parameter übergeben (null -> Create, sonst Detail/Edit), sodass ein
 *   Screen und ein ViewModel beide Fälle abdecken.
 *
 * Lernziele:
 *
 * - Einen entryProvider in eine eigene Funktion auslagern und seine
 *   Abhängigkeiten explizit als Parameter übergeben.
 * - Function-Typen statt konkreter Klassen übergeben und die Grenzen
 *   dieses Ansatzes kennen (wachsende Parameterliste, z. B. bei Undo).
 * - Lesenden und schreibenden Zugriff in der Signatur unterscheiden
 *   (List statt MutableList).
 * - Guards einsetzen, um späte oder doppelte Navigationseffekte zu ignorieren.
 * - Screen-Ebene (BackReason) und Navigations-Ebene (PopReason) trennen.
 * - Die Zuordnung Key -> Screen von der Verwaltung des Back-Stacks trennen.
 */