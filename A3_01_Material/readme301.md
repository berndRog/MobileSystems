# A3_01_Material

## Purpose and prerequisites

This Android teaching module combines **Material 3 and Jetpack Compose** with a person-management example. Two teaching priorities are explored in depth: developing the reusable **`InputValueString`** component from a Material `OutlinedTextField`, and building a **multilingual English/German app**. A list (`PeopleAdapter`/`PeopleScreen`) and a create/detail form (`PersonAdapter`/`PersonScreen`) connect these topics with validation and ViewModel state.

Recommended prerequisites are Kotlin, basic Compose layouts, text input, and `LazyColumn`, as introduced by the preceding A2 examples.

## Learning objectives

After studying the module, students should be able to:

- Structure a screen using `Scaffold`, `TopAppBar`, `Card`, buttons, an extended floating action button, and loading indicators.
- Develop `InputValueString` in `Shared` from an `OutlinedTextField`, with labels, icons, keyboard options, focus handling, and validation feedback; reuse it for different form fields.
- Provide English and German string resources under the same resource IDs and retrieve localized labels, actions, and validation messages without changing the UI logic.
- Explain how `MaterialTheme` supplies colors and typography; compare light/dark themes and dynamic colors on Android 12 or later.
- Trace the unidirectional flow **UI callback → Intent → ViewModel → UiState → UI**, including lifecycle-aware collection of `StateFlow`.
- Separate ViewModel-backed application state from local presentation state, such as when an input field displays validation feedback.
- Reuse validation rules during input and before saving, and distinguish normalization, sanitizing, and validation.
- Explain the mapping between optional domain values and text-field strings, and the role of the repository in loading, saving, and removing people.

## Didactic approach

The example moves from individual Compose elements towards a structured application. The same person data connects the visual and architectural topics, keeping the domain familiar while introducing new responsibilities.

`PeopleAdapter` and `PersonAdapter` connect ViewModels to screens through values and callbacks. The screens receive no ViewModel; adapters translate their callbacks into intents. This makes state hoisting and the architecture boundary visible.

### From TextField to InputValueString

Study `TextField` in depth through the Material `OutlinedTextField` variant. Develop the example step by step: start with `value` and `onValueChange`, add labels and icons, configure `KeyboardType` and `ImeAction`, and introduce focus handling and validation feedback. Extract the reusable component as `InputValueString` into `Shared` and use it in `PersonScreen` for first name, last name, email, and phone.

The input value comes from outside; local state controls when an error appears. Validation runs on focus loss or Next/Done; editing hides the existing error. `PersonValidator` supplies reusable rules. `PersonViewModel` normalizes and sanitizes values and validates the whole person before persistence. Compare field feedback with this final save check.

### Multilingual app: English and German

Treat multilingual support as part of component design. `InputValueString` receives an already localized label and a validation function; it does not decide which language to display. `PersonScreen` retrieves labels with `stringResource(R.string.firstname)`, while `PersonValidator` obtains localized error texts from Android resources.

In A3_01, `res/values/strings.xml` contains the default English texts and `res/values-de-rDE/strings.xml` their German counterparts for Germany. `Shared` also provides resources in `values` and `values-de`. Matching resource IDs keep UI logic independent of the translation. Compare English and German labels, actions, and errors by changing the device/emulator language, and inspect how text length affects the layout.

## Suggested learning path

1. **Explore the form:** In `ui/MainActivity.kt`, select `PersonAdapter` instead of `PeopleAdapter`. Supply one ViewModel parameter: `null` for a new person or an existing ID for detail mode.
2. **Develop and reuse:** Build up `InputValueString` from `OutlinedTextField`. Compare its four uses in `PersonScreen`, including validators, keyboard types, and Next/Done actions.
3. **Validate:** Compare empty, invalid, and valid inputs, focus loss, and renewed editing. Explain why an enabled Save button does not replace full validation.
4. **Switch language:** Compare the app in English and German (Germany). Trace one label and one validation message from their resource ID to both translations; check longer texts in the layout.
5. **Connect to the list:** Select `PeopleAdapter`, inspect `Card` typography, and trace a delete callback through the adapter, intent, ViewModel, and repository back to the observed list.

The existing tests under `src/test/java` illustrate form normalization, validation, and basic ViewModel behavior. A suitable learning outcome is an annotated data-flow sketch, an explanation of component reuse, and a comparison of the form in both languages.

## Position in the learning sequence

| Module | Added focus |
| --- | --- |
| **A3_01_Material** | Material UI, reusable TextField, English/German resources, validation, mapping, and state |
| A3_02_EffectHandling | One-time message/error effects and Snackbar handling |
| A3_03_Navigation | Navigation 3 back stack connecting list and form |
| A3_04_SwipeGestures | Swipe-to-detail and swipe-to-delete with Snackbar confirmation |
| A3_05_SwipeDeleteUndo (optional) | Immediate visual removal, Undo, delayed repository deletion, and restoration |

In A3_01, `MainActivity` currently displays the list. Create/detail navigation is not connected; the form is selected manually for teaching. Operation feedback is logged. The form's Cancel action clears first and last name. These details help distinguish this module from the later stages.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_01_Material), including `InputValueString` and language resources in `Shared`.
