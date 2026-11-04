# A2_02_Layout

English | [Deutsch](readme202_ger.md)

## Purpose

This project demonstrates how Jetpack Compose arranges content with `Column`, `Row`, `Box`, and `FlowRow`, and how modifiers affect size, padding, and available space. It also contains image examples using Compose `Image` and Coil `AsyncImage`.

## Examples

| Composable | What to inspect |
| --- | --- |
| `ExampleColumn` | Vertical placement, fixed sizes, `fillMaxWidth`, and `weight` in a column |
| `ExampleRow` | Horizontal placement and how weighted children divide remaining width |
| `ExampleBox` | Stacking content in the same area |
| `ExampleFlowRow` | Wrapping items onto further rows when width runs out |
| `ImagesScreen` | Image loading and the effect of `ContentScale` |

`MainActivity` currently calls `ExampleColumn` inside a Material 3 `Scaffold`. Calls for the other examples are commented out so they can be enabled one at a time. The activity applies the scaffold's inner padding before the example-specific modifiers. The examples are independent layout experiments rather than a navigation flow or a data-entry app.

## Try it

Open this directory as an Android Studio project, or run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Switch the call in `MainActivity` to another example. Compare the same layout on different screen widths and orientations, and adjust `weight`, fixed sizes, and `ContentScale` to see which area each child occupies. The included tests are starter examples, not targeted layout assertions.
