# A2_01_Count

[English](readme201.md) | Deutsch

## Zielsetzung

Dieses Einstiegsprojekt zeigt, wie beobachtbarer Zustand eine Jetpack-Compose-Oberfläche steuert. Ein Tastendruck erhöht den angezeigten Zähler und führt zur Rekomposition der betroffenen Composables.

## Implementierungen vergleichen

| Beispiel | Ort des Zustands | Grundidee |
| --- | --- | --- |
| `CountScreen1` | Im Composable mit `remember { mutableIntStateOf(...) }` | `MutableState<Int>` über `.value` lesen und ändern |
| `CountScreen2` | Im Composable mit `remember` | Denselben Zustand über Kotlin-Property-Delegation (`by`) verwenden |
| `Stateholder` + `CountScreen3` | Im Eltern-Composable mit `rememberSaveable` | Zähler nach unten und Button-Ereignis als Callback nach oben geben; Screen zustandslos halten |
| `CountAdapter` + `CountViewModel` | Im `StateFlow` eines ViewModels | Zuständigkeit für den Zustand aus dem Composable herauslösen |

`MainActivity` zeigt derzeit `Stateholder`; die anderen Aufrufe sind auskommentiert. `CountScreen3` bekommt `count` und `onIncrementCount` als Parameter und kann daher ohne eigenen Zählerzustand verwendet oder als Preview dargestellt werden. Mit `rememberSaveable` kann der Stateholder seinen Wert bei unterstützter Neuerstellung der Activity wiederherstellen. Das `SideEffect`-Logging macht Rekompositionen sichtbar; es ist Anschauungscode und nicht der Zählerzustand.

## Ausprobieren

Öffne den Ordner als Android-Studio-Projekt oder baue und teste ihn hier:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Starte die App, tippe auf den Button und aktiviere zum Vergleich `CountScreen1`, `CountScreen2` oder `CountAdapter` in `MainActivity`. Prüfe, wo der Zähler liegt, wer ihn verändern darf und was bei einer Neuerstellung der Activity geschieht. Die enthaltenen Unit- und instrumentierten Tests sind einfache Startbeispiele und prüfen das Zählerverhalten nicht gezielt.
