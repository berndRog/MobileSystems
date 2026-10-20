# A3_01_Material

## Zielsetzung und Voraussetzungen

Dieses Android-Lehrmodul verbindet **Material 3 und Jetpack Compose** mit einer Personenverwaltung. Zwei Schwerpunkte werden exemplarisch vertieft: die Entwicklung der wiederverwendbaren Komponente **`InputValueString`** aus einem Material-`OutlinedTextField` und eine **multilinguale App mit Englisch und Deutsch**. 

Liste (`PeopleAdapter`/`PeopleScreen`) und Formular (`PersonAdapter`/`PersonScreen`) zum Anlegen beziehungsweise Bearbeiten verbinden diese Themen mit Validierung und ViewModel-State.

Empfohlen werden Kenntnisse in Kotlin, einfachen Compose-Layouts, Texteingaben und `LazyColumn`, wie sie die vorherigen A2-Beispiele behandeln.

## Lernziele

Nach der Bearbeitung können Studierende:

- Eine Oberfläche mit `Scaffold`, `TopAppBar`, `Card`, Buttons, einem erweiterten Floating Action Button und Ladeindikatoren strukturieren.
- `InputValueString` in `Shared` aus einem `OutlinedTextField` entwickeln: mit Labels, Icons, Tastaturoptionen, Fokussteuerung und Validierungsrückmeldungen; die Komponente für unterschiedliche Formularfelder wiederverwenden.
- Englische und deutsche String-Ressourcen unter denselben Ressourcen-IDs bereitstellen und lokalisierte Labels, Aktionen und Validierungsmeldungen ohne Änderung der UI-Logik abrufen.
- Erklären, wie `MaterialTheme` Farben und Typografie bereitstellt, und helle/dunkle Themes sowie dynamische Farben ab Android 12 vergleichen.
- Den unidirektionalen Datenfluss **UI-Callback → Intent → ViewModel → UiState → UI** nachvollziehen und `StateFlow` lifecycle-aware beobachten.
- Anwendungszustand im ViewModel von lokalem Darstellungszustand unterscheiden, etwa der Sichtbarkeit einer Validierungsmeldung.
- Validierungsregeln bei der Eingabe und vor dem Speichern wiederverwenden sowie Normalisierung, Bereinigung und Validierung unterscheiden.
- Die Abbildung optionaler Domain-Werte auf Textfeld-Strings und die Rolle des Repository beim Laden, Speichern und Löschen erklären.

## Didaktischer Ansatz

Das Beispiel führt von einzelnen Compose-Elementen zu einer strukturierten Anwendung. Dieselben Personendaten verbinden Gestaltung und Architektur. Dadurch bleibt der fachliche Kontext vertraut, während neue Verantwortlichkeiten hinzukommen.

`PeopleAdapter` und `PersonAdapter` verbinden ViewModels über Werte und Callbacks mit den Screens. Die Screens erhalten kein ViewModel; die Adapter übersetzen ihre Callbacks in Intents. So werden State Hoisting und die Architekturgrenze sichtbar.

### Vom TextField zu InputValueString

Das `TextField` wird anhand der Material-Variante `OutlinedTextField` intensiv untersucht. Die Entwicklung erfolgt schrittweise: zunächst `value` und `onValueChange`, dann Labels und Icons, anschließend `KeyboardType`, `ImeAction`, Fokussteuerung und Validierungsrückmeldungen. Daraus entsteht `InputValueString` als wiederverwendbare Komponente in `Shared`. `PersonScreen` nutzt sie für Vorname, Nachname, E-Mail und Telefonnummer.

Der Eingabewert kommt von außen; lokaler Zustand steuert, wann ein Fehler erscheint. Validiert wird bei Fokusverlust oder Next/Done; erneutes Bearbeiten blendet den vorhandenen Fehler aus. `PersonValidator` liefert wiederverwendbare Regeln. `PersonViewModel` normalisiert und bereinigt die Werte und prüft die gesamte Person vor dem Speichern. Rückmeldung am Feld und abschließende Speicherprüfung werden miteinander verglichen.

### Multilinguale App: Englisch und Deutsch

Mehrsprachigkeit wird bereits beim Komponentenentwurf berücksichtigt. `InputValueString` erhält ein fertig lokalisiertes Label und eine Validierungsfunktion; die Komponente entscheidet nicht selbst über die Sprache. `PersonScreen` liest Labels mit `stringResource(R.string.firstname)`, während `PersonValidator` lokalisierte Fehlertexte aus Android-Ressourcen bezieht.

In A3_01 enthält `res/values/strings.xml` die englischen Standardtexte; `res/values-de-rDE/strings.xml` enthält die deutschen Entsprechungen für Deutschland. Auch `Shared` stellt Ressourcen in `values` und `values-de` bereit. Übereinstimmende Ressourcen-IDs halten die UI-Logik unabhängig von der Übersetzung. Durch Umstellen der Geräte-/Emulatorsprache werden englische und deutsche Labels, Aktionen und Fehler verglichen und die Auswirkungen unterschiedlicher Textlängen auf das Layout untersucht.

## Vorgeschlagener Lernpfad

1. **Formular erkunden:** In `ui/MainActivity.kt` den `PersonAdapter` statt des `PeopleAdapter` auswählen. Genau einen ViewModel-Parameter übergeben: `null` für eine neue Person oder eine vorhandene ID für den Detailmodus.
2. **Entwickeln und wiederverwenden:** `InputValueString` schrittweise aus `OutlinedTextField` entwickeln. Die vier Verwendungen in `PersonScreen` hinsichtlich Validatoren, Tastaturtypen und Next-/Done-Aktionen vergleichen.
3. **Validieren:** Leere, ungültige und gültige Eingaben, Fokusverlust und erneutes Bearbeiten vergleichen. Begründen, warum ein aktivierter Speichern-Button die vollständige Validierung nicht ersetzt.
4. **Sprache wechseln:** Die App auf Englisch und Deutsch (Deutschland) vergleichen. Ein Label und eine Validierungsmeldung von der Ressourcen-ID bis zu beiden Übersetzungen verfolgen; längere Texte im Layout prüfen.
5. **Verbindung zur Liste herstellen:** `PeopleAdapter` auswählen, die Typografie der `Card` untersuchen und einen Lösch-Callback durch Adapter, Intent, ViewModel und Repository bis zur aktualisierten Liste verfolgen.

Die vorhandenen Tests unter `src/test/java` veranschaulichen Formularnormalisierung, Validierung und grundlegendes ViewModel-Verhalten. Als Lernnachweis eignen sich eine kommentierte Datenflussskizze, eine Erklärung der Komponentenwiederverwendung und ein Vergleich des Formulars in beiden Sprachen.

## Einordnung in die Lernprogression

| Modul | Zusätzlicher Schwerpunkt |
| --- | --- |
| **A3_01_Material** | Material-Oberfläche, wiederverwendbares TextField, en/de-Ressourcen, Validierung, Mapping und State |
| A3_02_EffectHandling | Einmalige Meldungs-/Fehler-Effects und Snackbar-Verarbeitung |
| A3_03_Navigation | Navigation-3-Backstack zur Verbindung von Liste und Formular |
| A3_04_SwipeGestures | Swipe-to-Detail und Swipe-to-Delete mit Snackbar-Bestätigung |
| A3_05_SwipeDeleteUndo (optional) | Sofortiges visuelles Entfernen, Undo, verzögertes Löschen im Repository und Wiederherstellung |

In A3_01 zeigt `MainActivity` aktuell die Liste. Die Navigation zum Anlegen beziehungsweise Bearbeiten ist noch nicht verbunden; zur Demonstration wird das Formular manuell ausgewählt. Operationsmeldungen erscheinen im Log. Die Abbrechen-Aktion des Formulars leert Vor- und Nachnamen. Diese Details verdeutlichen die Abgrenzung zu den späteren Lernschritten.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_01_Material), einschließlich `InputValueString` und Sprachressourcen in `Shared`.
