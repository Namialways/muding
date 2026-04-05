# Editor Color Picker Redesign Design

Date: 2026-04-05
Project: androidprintScreen
Scope: editor color quick-access rail, editor color picker dialog, editor color persistence

## 1. Goal

Redesign the editor color selection flow so picking a color feels fast and repeatable for a screenshot annotation tool.

The redesign must solve these current problems:

- the editor rail only exposes three recent colors and a palette button, so common colors are not reliably one tap away
- the color dialog is currently just three RGB sliders, which makes routine color picking feel like manual adjustment work
- the dialog applies changes only at confirmation time, but it does not provide stronger structure for previewing, reusing, and saving common colors
- the app persists only recent colors, so users cannot explicitly keep a small set of preferred annotation colors

## 2. Non-goals

- No full Photoshop-style saturation/value workspace in this phase
- No alpha channel editing in this phase
- No large preset color library in this phase
- No changes to drawing behavior outside color selection
- No separate settings page for editor colors in this phase

## 3. Product Direction

Chosen direction: a compact balanced dialog that keeps visual picking and precise input, but stays optimized for fast selection rather than advanced color theory controls.

This means:

- keep the overall `B` vertical layout direction explored during brainstorming
- opening the dialog should feel like entering a focused picker, not a large control center
- visual picking remains available, but the UI should not introduce extra color-model concepts the user does not need
- `Hex` and `RGB` remain available for correction and precise entry
- favorites and recents exist to reduce repeated manual adjustment

This is intentionally not:

- a professional design-tool color studio
- a panel dominated by large swatch libraries
- a dialog that mutates the canvas on every draft color change

## 4. Existing Implementation Context

Current behavior is centered in these files:

- `app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt`
  - `EditorColorRail` shows up to three colors based on current color plus recents
  - `ColorPaletteDialog` is an `AlertDialog` built around three RGB sliders and a preview card
- `app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt`
  - stores current editor color
  - records recent colors immediately inside `selectColor`
  - limits recent colors to three entries
- `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
- `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
- `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
  - persists recent colors in shared preferences

The redesign should extend these existing boundaries instead of introducing a separate color subsystem.

## 5. Information Architecture

The editor keeps two color access layers:

1. the inline editor color rail for fastest reuse
2. the dialog for browsing, previewing, and precision entry

### 5.1 Editor Color Rail

Keep the current rail footprint and button count.

It should continue to render:

- three quick-access color slots
- one palette button

The rail data source changes from "current color plus recents" to:

- favorites first
- recents second
- current color highlighted if it matches one of those slots

The rail must not grow wider or denser than it is now.

### 5.2 Color Picker Dialog

The dialog remains modal and self-contained.

Its vertical sections are:

1. title and dismiss affordance
2. compact visual picker area
3. preview and precise input area
4. favorites row
5. recents row
6. footer actions

The lower two rows must stay visually compact: three favorite swatches and three recent swatches only.

## 6. Screen Design

### 6.1 Overall Layout

Use the brainstormed `B` structure:

- upper half prioritizes visual selection
- middle area handles preview and exact values
- lower area is reserved for a small number of reusable colors

The dialog should remain clearly smaller and calmer than a full-screen editor sheet. Avoid large boxed subsections or repeated explanatory labels.

### 6.2 Visual Picker Area

The top section should stay visual, but it must not become a terminology-heavy professional control surface.

Requirements:

- support drag interaction for quick color selection
- default to the current editor color when opened
- update dialog draft color immediately during dragging
- not write back to editor state until confirmation

Implementation detail is flexible at planning time. The UI may use a simplified visual picker surface rather than a full saturation/value square plus multiple auxiliary controls, as long as the behavior stays lightweight and direct.

### 6.3 Preview and Precise Input

The middle section should provide:

- a prominent preview swatch
- `Hex` input
- `R`, `G`, and `B` numeric inputs
- a favorite toggle beside or within the preview area

Layout goals:

- preview and favorite affordance should be visible without dominating the dialog
- `Hex` should be the first precise input shown
- `RGB` should stay compact and aligned
- the area should read as one editing group, not multiple stacked cards

## 7. Interaction Rules

These rules were explicitly approved during brainstorming and must be preserved:

- opening the dialog positions it at the current annotation color rather than resetting to a default green or white
- dragging or typing updates only dialog-local preview state
- `Apply` commits the color back to the editor
- `Cancel` discards all draft changes
- tapping a favorite or recent swatch updates the draft color but does not close the dialog

Additional interaction rules:

- closing the dialog by outside dismissal is equivalent to `Cancel`
- selecting a quick color on the inline rail still applies immediately, matching current behavior
- favorite toggling does not auto-apply the draft color to the editor

## 8. Favorites and Recents

The redesign introduces a separate favorite list and keeps the recent list intentionally small.

### 8.1 Favorites

- user-managed through the star toggle in the dialog
- maximum of three colors
- deduplicated by ARGB value
- if the list is full and the user favorites a new color, the oldest favorite should be evicted

### 8.2 Recents

- maximum of three colors
- deduplicated by ARGB value
- ordered most-recent first
- only updated when the user taps `Apply` in the dialog or chooses an inline quick color
- draft preview changes must not enter recents

### 8.3 Rail Population

The three inline quick slots should be populated from:

1. favorites in stored order
2. recents in stored order, skipping duplicates already present

If fewer than three colors exist, empty placeholders continue to render.

## 9. State and Data Flow

Recommended state ownership:

- `AnnotationViewModel`
  - source of truth for committed editor color
  - source of truth for favorites and recents
  - helper methods for committing color and updating stored swatches
- `ColorPickerDialog`
  - owns temporary draft state for the currently edited color
  - owns temporary input text state for partially valid `Hex` and numeric fields

Desired data flow:

1. editor opens dialog with committed current color plus persisted favorites and recents
2. dialog creates local draft state initialized from the committed current color
3. visual dragging, precise input, and swatch taps mutate only draft state
4. favorite toggle updates favorites state without auto-applying the draft color to the canvas
5. `Apply` sends the final chosen color to the view model
6. view model commits current color, records recent usage, persists storage, and updates selected path style if needed
7. `Cancel` closes without touching committed color or recents

The view model should stop recording recent colors on every `selectColor` call made by dialog draft changes. The commit path needs to distinguish between draft selection and confirmed selection.

## 10. Persistence

Extend `AppSettingsRepository` and `CaptureFlowSettings` to persist favorite editor colors in addition to recents.

Storage rules:

- favorites and recents both serialize as compact ARGB string lists, matching existing recent color persistence style
- both lists deduplicate before saving
- both lists cap at three items
- existing recent-color data should continue to load without migration failure

Preferred repository additions:

- `getFavoriteEditorColors(): List<Int>`
- `setFavoriteEditorColors(colors: List<Int>)`

## 11. Validation and Error Handling

### 11.1 Hex Input

- accept `#RRGGBB` and `RRGGBB`
- preserve temporary invalid text while the user is editing
- only update preview when the text forms a valid color
- on apply, use the last valid draft color rather than forcing invalid text into state

### 11.2 RGB Input

- accept integer values
- clamp committed component values to `0..255`
- preserve smooth interaction when values are edited incrementally

### 11.3 Empty Swatch States

- if there are no favorites or recents yet, show compact empty slots rather than hiding the entire row
- empty states should not inflate the dialog height or introduce extra instructional copy

## 12. Testing Strategy

Focus tests on state and persistence behavior rather than snapshot-heavy UI testing.

Required coverage:

- favorite list deduplication and three-item cap
- recent list deduplication and three-item cap
- `Apply` writes recent colors
- `Cancel` does not mutate committed color or recent colors
- rail population prefers favorites and backfills from recents
- invalid `Hex` text does not overwrite the last valid preview color

Reasonable implementation split:

- pure Kotlin tests for color history and favorites management logic
- targeted view model tests for apply versus cancel semantics where practical
- optional Compose tests only if the dialog input state becomes hard to verify through lower-level logic

## 13. Implementation Notes

Keep this work focused on the existing editor stack.

Target files are expected to include:

- `app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt`
- `app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt`
- `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
- `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
- `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`

Introducing one small helper for swatch history management is acceptable if it keeps `AnnotationViewModel` simpler and makes business rules testable. A large new feature module is not justified for this scope.
