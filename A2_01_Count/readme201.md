# A2_01_Count

English | [Deutsch](readme201_ger.md)

## Purpose

This introductory Jetpack Compose project shows how observable state drives a counter UI. Pressing the button increments the displayed number and causes the relevant composables to recompose.

## Compare the implementations

| Example | State location | Main idea |
| --- | --- | --- |
| `CountScreen1` | Inside the composable, using `remember { mutableIntStateOf(...) }` | Read and change a `MutableState<Int>` through `.value` |
| `CountScreen2` | Inside the composable, using `remember` | Use Kotlin property delegation (`by`) for the same state pattern |
| `Stateholder` + `CountScreen3` | In the parent, using `rememberSaveable` | Pass the count down and a button callback up; keep the screen stateless |
| `CountAdapter` + `CountViewModel` | In a ViewModel's `StateFlow` | Move state ownership outside the composable |

`MainActivity` currently displays `Stateholder`; the alternative calls are commented out. `CountScreen3` receives `count` and `onIncrementCount` and can therefore be previewed or reused without owning the counter. `rememberSaveable` lets the stateholder restore its count across supported activity recreation. The `SideEffect` logging in the examples helps observe recomposition; it is demonstration code, not the counter state itself.

## Try it

Open this directory as an Android Studio project, or build and run its unit tests here:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Run the app, tap the button, and compare what changes when you enable `CountScreen1`, `CountScreen2`, or `CountAdapter` in `MainActivity`. Ask where the count is stored, which component can update it, and what happens on activity recreation. The included unit and instrumented tests are starter examples rather than focused counter-behavior tests.
