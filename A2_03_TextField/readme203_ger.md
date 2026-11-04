# A2_03_TextField

[English](readme203.md) | Deutsch

## Zielsetzung

An einem Personenformular zeigt dieses Projekt kontrollierte `TextField`-Eingaben und einen gerichteten Datenfluss in Jetpack Compose. `PersonScreen` erhält Vor- und Nachname sowie Callbacks für Änderungen, Speichern und Abbrechen; der Screen entscheidet nicht, wo die Werte gespeichert werden.

## Varianten der Zustandsverwaltung

| Variante | Zuständigkeit für den Zustand | Verarbeitung von Ereignissen |
| --- | --- | --- |
| `PersonAdapterSth` | Composable-Zustand mit `remember` | Aktualisiert einen an `PersonScreen` übergebenen `Person`-Wert |
| `PersonAdapterMvvm` | `PersonViewModelMvvm` mit `StateFlow` | Ruft ViewModel-Methoden für Änderungen und Aktionen auf |
| `PersonAdapterMvi` | `PersonViewModelMvi` mit `StateFlow` | Übergibt typisierte `PersonIntent`-Werte an `onIntent()` |

`MainActivity` zeigt derzeit `PersonAdapterSth`; die Aufrufe für MVVM und MVI sind zum Vergleich auskommentiert. `InputName` ist das wiederverwendbare Textfeld-Composable. Der Speichern-Button ist nur aktiv, wenn beide Namen nicht leer sind. Speichern und Abbrechen zeigen hier die Ereignisverarbeitung; eine Person wird nicht in einer Datenbank abgelegt.

## Ausprobieren

Öffne den Ordner als Android-Studio-Projekt oder führe aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Gib in beide Felder Text ein und leere eines wieder. Aktiviere danach in `MainActivity` einen anderen Adapter und verfolge eine Eingabe von `onValueChange` zum Zustandsinhaber und zurück zu `PersonScreen`. Vergleiche direkte Callbacks bei MVVM mit Intents bei MVI. Die enthaltenen Tests sind Startbeispiele und prüfen das Formular nicht gezielt.
