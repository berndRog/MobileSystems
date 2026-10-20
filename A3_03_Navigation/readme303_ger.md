# A3_03_Navigation

## Zielsetzung und Voraussetzungen

Dieses Modul verbindet Personenliste und Anlege-/Detailformular über **Navigation 3**. In A3_02 lösen Effects aus dem ViewModel die Ausgabe von Meldungen oder Fehlern in einer Snackbar aus. A3_03 erweitert diesen Mechanismus: **Das ViewModel gibt einen Navigationseffect aus; die Navigationsschicht verändert daraufhin den Backstack.** Die Screens müssen nicht mehr manuell in `MainActivity` ausgewählt werden.

Vorausgesetzt werden die Material-3-Oberfläche aus A3_01 sowie der State–Intent–Effect-Datenfluss, Adapter und Snackbar-Verarbeitung aus A3_02.

## Lernziele

Nach der Bearbeitung können Studierende:

- Eine Benutzeraktion über Intent und ViewModel-Effect bis zum Navigations-Callback und zur Backstack-Änderung verfolgen.
- Navigationsziele mit serialisierbaren `NavKey`-Typen beschreiben und eine Personen-ID an ein gemeinsames Anlege-/Detailziel übergeben.
- Die Aufgaben von `rememberNavBackStack`, `NavDisplay` und `entryProvider` erklären.
- Navigationseffects von Meldungs-/Fehler-Effects unterscheiden und an die zuständige UI-Komponente weiterleiten.
- Erklären, wie Entry Decorators saveable Compose-State und ViewModels pro Navigationseintrag unterstützen.
- Snackbar-Ausgaben über Screenwechsel hinweg erhalten und Save sowie Cancel anhand ihrer Rückkehrgründe und Animationen unterscheiden.

## Konkreter Navigationsablauf

**UI-Aktion → Intent → ViewModel → Navigationseffect → Adapter-Callback → `AppNavigation` → Backstack**

| Auslöser | Vom ViewModel ausgegebener Effect | Ergebnis in `AppNavigation` |
| --- | --- | --- |
| Create-FAB | `PeopleEffect.NavigateTo(null)` | `PersonKey(null)` hinzufügen; leeres Formular öffnen |
| Tap auf die `Card` einer Person | `PeopleEffect.NavigateTo(personId)` | `PersonKey(personId)` hinzufügen; vorhandene Person laden |
| Erfolgreiches Save | `ShowMessage`, danach `NavigateBack(BackReason.Save)` | Snackbar-Ausgabe starten und Formulareintrag entfernen |
| Cancel | `NavigateBack(BackReason.Cancel)` | Formulareintrag ohne Speichern entfernen |

Ein fehlgeschlagenes Speichern erzeugt `ShowError`; das Formular bleibt geöffnet. Die Navigation folgt auf erfolgreiches Speichern. Der Toolbar-Pfeil des Formulars löst Save aus, wenn das Formular verfügbar ist; bei einem Ladefehler löst er Cancel aus. System Back wird direkt in `NavDisplay.onBack` behandelt und entfernt den aktuellen Eintrag mit der Cancel-Animation.

## Didaktischer Ansatz

Personenbeispiel und Effect-Infrastruktur bleiben vertraut; der Backstack kommt als neues Konzept hinzu. `PeopleKey` bezeichnet die Liste und bleibt das Root-Ziel. `PersonKey` enthält eine optionale ID: `null` wählt den Anlegemodus, eine vorhandene ID den Detailmodus. Koin stellt das zugehörige ViewModel mit dieser ID bereit.

`AppNavigation` beginnt mit `rememberNavBackStack(PeopleKey)`. Ein hinzugefügter Key öffnet ein Ziel; das Entfernen des letzten Keys führt zum vorherigen Ziel zurück. Die Hilfsfunktion zum Entfernen erhält das Root-Ziel. Adapter übersetzen konkrete Effects in Callbacks; `AppNavigation` führt die Backstack-Operationen aus. ViewModels bleiben unabhängig von den Navigation-3-APIs.

Meldungs-Effects nehmen einen eigenen Weg zum `SnackbarController`. Ein gemeinsamer `SnackbarHostState`, Host und Controller liegen oberhalb von `NavDisplay`. Ihr Scope bleibt bei einem Zielwechsel bestehen. Dadurch kann die Speicherbestätigung nach der Rückkehr zur Liste sichtbar bleiben. State wird weiterhin lifecycle-aware beobachtet; Ladefehler bleiben im State. Bei einer nicht gefundenen Person wird Back angeboten, bei einem Repository-Ladefehler Retry.

Save verwendet eine horizontale Rückkehranimation. Bei Cancel wird der vorherige Screen eingeblendet, während das Formular nach unten verschwindet. Eine eigene Scale-/Fade-Transition unterstützt Predictive Back.

## Vorgeschlagener Lernpfad

1. **Create und Detail verfolgen:** Beide Aktionen von `PeopleViewModel` über `PeopleAdapter` bis `AppNavigation` nachvollziehen und ihre `PersonKey`-Werte vergleichen.
2. **Backstack untersuchen:** Eine Person öffnen, zurückkehren und die protokollierten Add-/Pop-Operationen vergleichen. Erklären, warum `PeopleKey` erhalten bleibt.
3. **Ergebnisse vergleichen:** Erfolgreich speichern, abbrechen und den Fehlerpfad in `PersonBackEffectTest` untersuchen. Effects, Snackbar-Ausgabe und Navigation zuordnen.
4. **Verantwortlichkeiten erklären:** Skizzieren, welche Komponente Anwendungszustand, Effect-Verarbeitung, Backstack und Snackbar-Ausgabe verwaltet. Die ausgegebenen Ziel-IDs anhand von `NavigationEffectTest` nachvollziehen.

A3_04 erweitert diesen funktionierenden Navigationsablauf um Swipe-to-Detail und Swipe-to-Delete mit Snackbar-Bestätigung. Undo folgt in A3_05.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_03_Navigation).
