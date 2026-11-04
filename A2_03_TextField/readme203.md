# A2_03_TextField

English | [Deutsch](readme203_ger.md)

## Purpose

This project uses a person form to demonstrate controlled `TextField` input and one-way data flow in Jetpack Compose. `PersonScreen` receives first and last names plus change, save, and cancel callbacks; it does not choose where those values are stored.

## State variants

| Variant | State owner | Event handling |
| --- | --- | --- |
| `PersonAdapterSth` | Composable state with `remember` | Updates a `Person` value passed to `PersonScreen` |
| `PersonAdapterMvvm` | `PersonViewModelMvvm` with `StateFlow` | Calls ViewModel methods for form changes and actions |
| `PersonAdapterMvi` | `PersonViewModelMvi` with `StateFlow` | Dispatches typed `PersonIntent` values to `onIntent()` |

`MainActivity` currently shows `PersonAdapterSth`; the MVVM and MVI calls are commented out for comparison. `InputName` is the reusable text-field composable. The save button is enabled only when both names are nonempty. Save and cancel in these examples illustrate event handling; they do not persist a person to a database.

## Try it

Open this directory as an Android Studio project, or run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Type in both fields, clear one field, and watch the save button. Then switch the adapter in `MainActivity` and trace a keystroke from `onValueChange` back to the state owner and down to `PersonScreen`. Compare direct callbacks in MVVM with intents in MVI. The included tests are starter examples rather than focused form tests.
