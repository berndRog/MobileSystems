# A3_04_SwipeGestures

## Zielsetzung und Voraussetzungen

Dieses Projekt erweitert **A3_03_Navigation** um horizontale Wischgesten in der Personenliste. `PeopleScreen` bettet jede `PersonCard` in die wiederverwendbare `SwipeCard` aus `Shared` ein. Eine Richtung öffnet eine vorhandene Person im Detailformular, die andere fordert über eine Snackbar eine Löschbestätigung an.

Vorausgesetzt werden Material 3, `LazyColumn`, State Hoisting und der Ablauf von Intent über ViewModel und Effect bis zum Adapter, einschließlich Navigation 3 und Snackbar-Ausgabe.

## Lernziele

Nach der Bearbeitung können Studierende:

- Material 3 `SwipeToDismissBox` über `SwipeCard` nutzen und den konkreten Karteninhalt als Composable-Lambda übergeben.
- `StartToEnd` dem Öffnen der Details und `EndToStart` einer Löschanfrage zuordnen.
- Gestenerkennung, UI-Callbacks, Intents, ViewModel-Effects und Repository-Zugriffe trennen.
- Erklären, warum Löschanfrage und Löschbestätigung zwei verschiedene Schritte sind.
- Richtungsabhängige Farben und Symbole als Rückmeldung während der Geste einsetzen.
- Das Zurücksetzen des Swipe-Zustands, aktuelle Callbacks nach Rekomposition sowie stabile Listen-Keys und `Modifier.animateItem()` erklären.

## Konkreter Gesten- und Effect-Ablauf

| Geste | Intent aus `PeopleAdapter` | Effect aus `PeopleViewModel` | Ergebnis in der UI |
| --- | --- | --- | --- |
| `StartToEnd` | `Detail(personId)` | `NavigateTo(personId)` | `AppNavigation` öffnet `PersonKey(personId)` |
| `EndToStart` | `RequestRemove(personId)` | `ConfirmRemove(message, actionLabel, personId)` | Eine Action-Snackbar fragt nach Bestätigung |

Beim Löschen lautet die vollständige Abfolge:

**Swipe → `RequestRemove`-Intent → `ConfirmRemove`-Effect → Snackbar-Aktion → `ConfirmRemove`-Intent → Repository-Löschung → aktualisierte Liste**

`PeopleAdapter` leitet den Bestätigungs-Effect an `onConfirmRemove` weiter. `AppNavigation` zeigt mit `SnackbarController.showAction()` die Anfrage. Erst die Aktion der Snackbar sendet `PeopleIntent.ConfirmRemove(personId)` an das ViewModel zurück. Wird die Snackbar geschlossen oder läuft sie ab, bleibt die Person erhalten.

Nach erfolgreichem Löschen gibt das ViewModel `ShowMessage` aus, bei einem Fehler `ShowError`. Die beobachtete Repository-Liste liefert den neuen UI-Zustand. Der Swipe allein löscht die Person nicht und entfernt sie auch nicht vorläufig aus der Liste.

## Didaktischer Ansatz

Die Geste ist ein weiterer Eingabeweg in die vorhandene Architektur. Navigation und Snackbar-Verarbeitung aus A3_03 bleiben erhalten. `PeopleScreen` bekommt Werte und Callbacks und kennt weder Repository noch Navigation. Neue Personen werden weiterhin über den FAB angelegt.

`SwipeCard` kapselt die Gestenmechanik, ohne `Person` zu kennen. Ihr `content`-Lambda liefert die `PersonCard`; lokalisierte Inhaltsbeschreibungen kommen vom Aufrufer. So bleiben wiederverwendbare Interaktion und anwendungsspezifischer Inhalt getrennt.

Der Hintergrund richtet sich nach `dismissDirection`: Die Detailrichtung nutzt `primaryContainer` mit Edit-Symbol, die Löschrichtung `errorContainer` mit Delete-Symbol. Die visuelle Rückmeldung während des Wischens ist von der Aktion nach Abschluss der Geste zu unterscheiden.

Vor dem Callback setzt `SwipeCard` ihren Zustand mit `snapTo(Settled)` zurück. `remember` hält den Dismiss-Handler stabil, während `rememberUpdatedState` aktuelle Callbacks bereitstellt. Personen-IDs identifizieren Einträge in der `LazyColumn`; `animateItem()` animiert Änderungen nach dem Repository-Update.

## Vorgeschlagener Lernpfad

1. Beide Wischrichtungen ausprobieren und Farben, Symbole, Detailansicht und Bestätigungs-Snackbar vergleichen.
2. Den Detail-Callback von `SwipeCard` über `PeopleScreen`, `PeopleAdapter` und `PeopleViewModel` bis zum Navigation-Effect verfolgen.
3. Die Löschanfrage einmal verwerfen und einmal bestätigen. Den Zeitpunkt der Repository-Löschung bestimmen.
4. `SwipeCardTest` für Gesten und Zustandsreset sowie `PeopleViewModelSwipeTest` für Bestätigung, erfolgreiche und fehlgeschlagene Löschung lesen.

Als Lernnachweis eignet sich eine Ablaufskizze, die Löschanfrage, Bestätigungs-Effect, Bestätigungs-Intent, Repository-Ergebnis und Listenaktualisierung trennt.

## Position in der Lernfolge

**A3_01:** Material-Oberfläche und Eingaben → **A3_02:** Nachrichten- und Fehler-Effects → **A3_03:** Navigation und Backstack → **A3_04:** Gesten und bestätigtes Löschen.

A3_04 bestätigt vor dem Löschen. A3_05 ergänzt sofortiges visuelles Entfernen mit Undo, verzögertem Repository-Commit und Wiederherstellung bei Fehlern.
