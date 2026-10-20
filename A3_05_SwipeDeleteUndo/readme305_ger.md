# A3_05_SwipeDeleteUndo

## Zielsetzung und Voraussetzungen

Diese optionale Vertiefung baut auf **A3_04_SwipeGestures** auf. A3_04 fordert vor dem Löschen eine Bestätigung an. A3_05 verändert die Interaktion: **Ein Swipe blendet die Person sofort aus; eine Snackbar bietet Undo an. Das Löschen im Repository wird verschoben, bis die Snackbar ohne Undo endet.**

Liste (`PeopleAdapter`/`PeopleScreen`), Formular (`PersonAdapter`/`PersonScreen`), Navigation 3 und die wiederverwendbare `SwipeCard` werden weiterverwendet. Vorausgesetzt werden die Gesten-, Intent-, ViewModel-, Effect- und Snackbar-Abläufe der vorherigen Module.

## Lernziele

Nach der Bearbeitung können Studierende:

- Sofortiges visuelles Entfernen vom späteren Löschen im Repository unterscheiden.
- `Remove`, `ShowUndo`, `UndoRemove` und `CommitRemove` ihren jeweiligen Intent- und Effect-Pfaden zuordnen.
- Erklären, warum Undo in dieser Implementierung keinen ausgleichenden Schreibzugriff auf das Repository benötigt.
- Mit `IVisualRemoval<Person>` und `VisualRemovalDelegate<T>` temporären Entfernungszustand von Persistenz und Snackbar-Ausgabe trennen.
- Nach fehlgeschlagenem Löschen den sichtbaren Eintrag wiederherstellen und den Fehler über `ShowError` ausgeben.
- Erklären, wie stabile Keys, Item-Animation und Scrollen einen wiederhergestellten Eintrag sichtbar machen.

## Konkreter Swipe-to-Delete/Undo-Ablauf

`EndToStart` ruft den Delete-Callback auf. `PeopleAdapter` ermittelt die Person und sendet `PeopleIntent.Remove(person)`. Das ViewModel blendet sie über `IVisualRemoval` aus, veröffentlicht die sichtbare Liste und gibt `PeopleEffect.ShowUndo(message, actionLabel, personId)` aus.

**Swipe → `Remove`-Intent → visuelles Entfernen + `ShowUndo`-Effect → Adapter → Action-Snackbar**

Der Effect geht vom ViewModel aus und fordert eine UI-Aktion an. `PeopleAdapter` leitet ihn über `onUndo` weiter; `AppNavigation` ruft `SnackbarController.showAction()` auf. Das Snackbar-Ergebnis bestimmt den nächsten Intent:

| Ergebnis | Intent zurück an das ViewModel | Folge |
| --- | --- | --- |
| Benutzer wählt Undo | `UndoRemove(personId)` | Sichtbarkeit wiederherstellen; kein Löschen im Repository |
| Snackbar wird ohne Undo geschlossen oder läuft ab | `CommitRemove(personId)` | `repository.remove(person)` aufrufen |
| Löschen im Repository schlägt fehl | — | Sichtbarkeit wiederherstellen und `ShowError` ausgeben |

`StartToEnd` öffnet weiterhin das Detailformular einer vorhandenen Person. Neue Personen werden weiterhin über den FAB angelegt.

## Didaktischer Ansatz

Der Vergleich mit A3_04 führt zwei getrennte Zustände ein: die zuletzt empfangenen Repository-Daten und die aktuell sichtbare Liste. `VisualRemovalDelegate<T>` in `Shared` verwaltet Ausgangsdaten, ausgeblendete IDs und ausstehende Löschungen. `visibleItems()` filtert ausgeblendete Einträge aus der Ausgangsliste. Repository-Emissionen aktualisieren die Ausgangsdaten, ohne ausstehende Löschungen vorzeitig wieder sichtbar zu machen.

Undo entfernt den Pending-/Hidden-Zustand; eine persistente Löschung hat noch nicht stattgefunden. Nach erfolgreichem Commit bleibt die ID ausgeblendet, bis die beobachtete Ausgangsliste das Entfernen bestätigt. Dadurch werden beide Reihenfolgen von Repository-Emission und Operationsabschluss berücksichtigt. Der Delegate kennt weder Repository noch Compose, Coroutines oder Effects; das ViewModel koordiniert diese Verantwortlichkeiten.

`PeopleViewModel` und Snackbar-Infrastruktur werden in `AppNavigation` oberhalb von `NavDisplay` bezogen. Ausstehende Snackbar-Callbacks erreichen dadurch bei Zielwechseln dasselbe ViewModel. Der Adapter beobachtet State lifecycle-aware.

Nach Undo oder fehlgeschlagenem Commit kennzeichnet `restoredPersonId` den wiederhergestellten Eintrag. `PeopleScreen` macht ihn bei Bedarf mit `ScrollToItemIfNotVisible` sichtbar. Anschließend löscht `RestoreHandled` das Ziel. Stabile Personen-IDs und `animateItem()` unterstützen Entfernen, Wiedereinfügen und Verschieben.

## Vorgeschlagener Lernpfad und optionale Vertiefung

**Kern, wenn die Lehrzeit knapp ist:**

1. Bestätigung in A3_04 mit sofortigem visuellem Entfernen in A3_05 vergleichen.
2. Einen Swipe über `Remove` und `ShowUndo` verfolgen; Undo mit dem Ablauf der Snackbar vergleichen.
3. Erklären, wann das Repository verändert wird und warum Undo kein erneutes Einfügen in die Datenbank benötigt.

**Vertiefung, wenn Zeit bleibt:**

- `VisualRemovalDelegate` und die Reihenfolge von Ausgangsdaten-Updates und Commit-Abschluss untersuchen.
- In `PeopleViewModelSwipeTest` visuelles Entfernen ohne Repository-Zugriff, Undo, verzögerten Commit, Wiederherstellung nach Fehler und doppelte Löschanforderungen nachvollziehen.
- In `PeopleScreenRestoreTest` untersuchen, wie wiederhergestellte Einträge am Listenanfang/-ende sichtbar gemacht und bestätigt werden.

Als Lernnachweis eignet sich eine Ablaufskizze mit sichtbarer Liste und Repository-Inhalt für jeden Schritt. Das Kapitel kann zunächst demonstriert werden; Delegate-Interna und Scrollbehandlung eignen sich als spätere Vertiefung.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_05_SwipeDeleteUndo), einschließlich Entfernungs-, Scroll- und Snackbar-Infrastruktur in `Shared`.
