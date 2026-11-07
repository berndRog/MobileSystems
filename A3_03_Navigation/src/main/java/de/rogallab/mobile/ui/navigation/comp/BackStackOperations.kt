package de.rogallab.mobile.ui.navigation.comp

import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.shared.domain.utilities.Alog

private const val TAG = "<-BackStack"

/**
 * Adds a destination on top of the back stack.
 *
 * The last element of the list is the current destination, so the added
 * [destination] becomes visible immediately.
 *
 * @param destination the destination to show.
 * @param backStack the back stack that is modified.
 */
internal fun add(
   destination: NavKey,
   backStack: MutableList<NavKey>,
) {
   // Put at the end of the list, so that it becomes the current destination.
   backStack.add(destination)

   logNavigationOperation("add", destination, backStack)
}

/**
 * Removes the current destination from the back stack.
 *
 * The root destination (the first entry) is never removed. If only the root
 * is left, the call is ignored and only logged.
 *
 * @param backStack the back stack that is modified.
 */
internal fun remove(
   backStack: MutableList<NavKey>,
) {
   if (backStack.size > 1) {
      // Remove the last destination, so that the previous one becomes
      // the current destination.
      val removedDestination = backStack.removeLastOrNull()
      logNavigationOperation("pop", removedDestination, backStack)
   } else {
      // Nothing to pop: the root destination must stay on the stack.
      logNavigationOperation(
         "pop ignored - root remains", backStack.lastOrNull(), backStack)
   }
}

/**
 * Logs one navigation operation together with the complete back stack.
 *
 * Example output:
 * `add: PersonKey(personId=null) | stack(2) = [PeopleKey -> PersonKey(personId=null)]`
 *
 * @param operation short name of the operation, e.g. "add" or "pop".
 * @param destination the destination that was added or removed, or null.
 * @param backStack the back stack after the operation was applied.
 */
internal fun logNavigationOperation(
   operation: String,
   destination: NavKey?,
   backStack: List<NavKey>,
) {
   val destinationText = destination?.toString() ?: "none"
   val stackText = backStack.joinToString(
      separator = " -> ",
      prefix = "[",
      postfix = "]",
   )

   Alog.d(TAG, "$operation: $destinationText | stack(${backStack.size}) = $stackText")
}

/*
 * Didaktik und Lernziele
 *
 * - Ein Navigation-3-Back-Stack ist nichts anderes als eine Liste von NavKey.
 *   Das letzte Element ist das aktuelle Ziel:
 *
 *      [PeopleKey]                          -> Liste sichtbar
 *      [PeopleKey, PersonKey(null)]         -> Create sichtbar
 *      [PeopleKey, PersonKey("4711")]       -> Detail/Edit sichtbar
 *
 *   Navigieren bedeutet daher: Element hinzufügen (add) oder das
 *   letzte Element entfernen (remove).
 *
 * - Die Operationen sind aus AppNavigation ausgelagert. AppNavigation kümmert
 *   sich um Scaffold, SnackbarHost und Animationen, diese Datei nur um die
 *   Manipulation des Back-Stacks. Beide Aufgaben sind damit getrennt.
 *
 * - Die Funktionen sind normale Top-Level-Funktionen, der Back-Stack wird
 *   explizit als Parameter übergeben:
 *
 *      add(PersonKey(personId), backStack)
 *      remove(backStack)
 *
 *   Alle Abhängigkeiten stehen sichtbar in der Signatur. Bewusst keine
 *   Extension-Funktionen mit den Namen add/remove: Die Member-Funktionen
 *   der Liste hätten Vorrang und würden Logging und Root-Schutz umgehen.
 *
 * - Die Root-Destination wird nie entfernt. Ein Pop auf einen Stack mit nur
 *   einem Eintrag wird ignoriert und protokolliert. Damit entsteht kein
 *   leerer Back-Stack, den NavDisplay nicht darstellen könnte.
 *
 * - Jede Operation protokolliert den kompletten Back-Stack. So lässt sich in
 *   Logcat nachvollziehen, wie sich der Stack bei Add, Pop, System-Back und
 *   verspäteten Navigationseffekten verändert.
 *
 * - Die Funktionen sind internal, weil sie nur innerhalb des Moduls gebraucht
 *   werden. Sie benötigen weder Compose noch Android-Klassen und lassen sich
 *   mit einem einfachen JVM-Unit-Test prüfen, z. B. "remove auf [PeopleKey]
 *   lässt den Stack unverändert". Alog muss dafür im Test verfügbar oder
 *   ersetzbar sein.
 *
 * Lernziele:
 *
 * - Den Navigation-3-Back-Stack als einfache Liste verstehen und direkt
 *   verändern.
 * - Stack-Operationen von UI-Code (Scaffold, Snackbar, Animationen) trennen.
 * - Abhängigkeiten explizit als Parameter übergeben.
 * - Die Root-Destination vor dem Entfernen schützen.
 * - Navigationsoperationen mit dem vollständigen Stack protokollieren, um
 *   das Verhalten nachvollziehen zu können.
 * - Logik ohne Compose-Abhängigkeit mit einem Unit-Test absichern.
 */