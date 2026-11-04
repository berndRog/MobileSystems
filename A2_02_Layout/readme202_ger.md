# A2_02_Layout

[English](readme202.md) | Deutsch

## Zielsetzung

Dieses Projekt zeigt, wie Jetpack Compose Inhalte mit `Column`, `Row`, `Box` und `FlowRow` anordnet und wie Modifier Größe, Abstand und verfügbaren Platz beeinflussen. Bildbeispiele mit Compose `Image` und Coil `AsyncImage` sind ebenfalls enthalten.

## Beispiele

| Composable | Untersuchungsschwerpunkt |
| --- | --- |
| `ExampleColumn` | Vertikale Anordnung, feste Größen, `fillMaxWidth` und `weight` in einer Spalte |
| `ExampleRow` | Horizontale Anordnung und Verteilung der verbleibenden Breite über `weight` |
| `ExampleBox` | Übereinanderliegende Inhalte im selben Bereich |
| `ExampleFlowRow` | Umbruch in weitere Zeilen bei zu geringer Breite |
| `ImagesScreen` | Bildladen und Wirkung von `ContentScale` |

`MainActivity` ruft derzeit `ExampleColumn` innerhalb eines Material-3-`Scaffold` auf. Die anderen Aufrufe sind auskommentiert und lassen sich einzeln aktivieren. Das innere Padding des Scaffolds wird vor den beispielspezifischen Modifiern angewendet. Die Dateien sind voneinander unabhängige Layoutversuche, keine Navigation oder Dateneingabe-App.

## Ausprobieren

Öffne den Ordner als Android-Studio-Projekt oder führe aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Aktiviere in `MainActivity` nacheinander andere Beispiele. Vergleiche das Layout bei verschiedenen Bildschirmbreiten und Ausrichtungen und verändere `weight`, feste Größen und `ContentScale`. Die enthaltenen Tests sind Startbeispiele, keine gezielten Layoutprüfungen.
