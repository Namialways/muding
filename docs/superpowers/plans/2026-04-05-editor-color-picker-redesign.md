# Editor Color Picker Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the editor's RGB-slider-only color dialog with a compact balanced picker that supports draft preview, favorites, recents, and precise `Hex`/`RGB` entry without making the bottom bar denser.

**Architecture:** Add two small pure-Kotlin helpers: one for favorite/recent swatch rules and one for draft `Hex`/`RGB` editing rules. Extend `AnnotationViewModel` and settings persistence to own committed color, favorites, and recents, then rebuild the `EditorScreen` dialog around local draft state so `Apply` and `Cancel` semantics stay explicit.

**Tech Stack:** Kotlin, Jetpack Compose Material3, SharedPreferences, JUnit4, Gradle

---

## File Map

### Create

- `app/src/main/java/com/muding/android/presentation/editor/EditorColorCollections.kt`
  - Pure editor-specific helper for favorite colors, recent colors, and quick-access rail ordering
- `app/src/main/java/com/muding/android/presentation/editor/ColorPickerDraftState.kt`
  - Pure helper for draft color preview plus `Hex` and `RGB` field parsing
- `app/src/test/java/com/muding/android/presentation/editor/EditorColorCollectionsTest.kt`
  - Unit tests for swatch deduplication, capacity, and rail composition
- `app/src/test/java/com/muding/android/presentation/editor/ColorPickerDraftStateTest.kt`
  - Unit tests for valid and invalid `Hex`/`RGB` editing behavior
- `app/src/test/java/com/muding/android/presentation/editor/AnnotationViewModelColorSelectionTest.kt`
  - ViewModel tests for committed color, favorites persistence, and recent-color recording
- `docs/superpowers/plans/2026-04-05-editor-color-picker-redesign.md`
  - This implementation plan

### Modify

- `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
  - Add favorite editor color persistence API
- `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
  - Wire favorite editor color persistence through `CaptureFlowSettings`
- `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
  - Persist favorite colors and keep recent-color serialization capped at three items
- `app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt`
  - Load favorites and recents, separate draft selection from confirmed selection, and expose favorite-toggle actions
- `app/src/main/java/com/muding/android/presentation/editor/EditorDocumentState.kt`
  - Carry favorite colors and quick-access colors into the screen layer
- `app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt`
  - Rebuild the color rail and dialog UI around compact sections and local draft state
- `app/src/main/res/values/strings.xml`
  - Add labels for `Apply`, favorites, recents, and favorite toggle content descriptions

### Verify Only

- `docs/superpowers/specs/2026-04-05-editor-color-picker-design.md`
  - Approved design source of truth for plan alignment

## Task 1: Add Swatch Collection Rules With TDD

**Files:**
- Create: `app/src/main/java/com/muding/android/presentation/editor/EditorColorCollections.kt`
- Test: `app/src/test/java/com/muding/android/presentation/editor/EditorColorCollectionsTest.kt`

- [ ] **Step 1: Write the failing swatch-rule tests**

Cover:

- favorite deduplication
- favorite capacity capped at three colors
- recent deduplication
- recent capacity capped at three colors
- quick-access rail ordering prefers favorites and backfills from recents

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.EditorColorCollectionsTest`

Expected: FAIL because `EditorColorCollections` does not exist yet.

- [ ] **Step 3: Implement the minimal collection helper**

Add a small immutable helper that:

- stores favorites and recents as ordered ARGB lists
- toggles favorites with oldest-item eviction at three entries
- records recents with deduplication and three-item cap
- exposes `quickAccessColors(limit = 3)`

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.EditorColorCollectionsTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/editor/EditorColorCollections.kt app/src/test/java/com/muding/android/presentation/editor/EditorColorCollectionsTest.kt
git commit -m "test: cover editor color swatch rules"
```

## Task 2: Add Draft Color Editing Rules With TDD

**Files:**
- Create: `app/src/main/java/com/muding/android/presentation/editor/ColorPickerDraftState.kt`
- Test: `app/src/test/java/com/muding/android/presentation/editor/ColorPickerDraftStateTest.kt`

- [ ] **Step 1: Write the failing draft-state tests**

Cover:

- draft initializes from the committed current color
- valid `Hex` input updates preview color
- invalid `Hex` input preserves the last valid preview color
- partial numeric edits preserve field text until a valid value can be applied
- committed `RGB` values clamp to `0..255`

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.ColorPickerDraftStateTest`

Expected: FAIL because `ColorPickerDraftState` does not exist yet.

- [ ] **Step 3: Implement the minimal draft-state helper**

Implement a pure helper that:

- keeps the last valid preview color
- stores editable field text for `Hex`, `R`, `G`, and `B`
- updates preview only when parsed input becomes valid
- exposes a stable committed color for `Apply`

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.ColorPickerDraftStateTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/editor/ColorPickerDraftState.kt app/src/test/java/com/muding/android/presentation/editor/ColorPickerDraftStateTest.kt
git commit -m "test: cover color picker draft parsing"
```

## Task 3: Extend Persistence And ViewModel Commit Semantics

**Files:**
- Modify: `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
- Modify: `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
- Modify: `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/editor/EditorDocumentState.kt`
- Test: `app/src/test/java/com/muding/android/presentation/editor/AnnotationViewModelColorSelectionTest.kt`

- [ ] **Step 1: Write the failing ViewModel tests**

Cover:

- initial state loads favorite and recent colors from `AppSettingsRepository`
- confirming a dialog color records it in recents and updates current color
- toggling favorite color persists the updated favorite list
- quick-access colors prefer favorites before recents
- cancel semantics leave committed color and recents unchanged

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.AnnotationViewModelColorSelectionTest`

Expected: FAIL because the new favorite and commit APIs do not exist yet.

- [ ] **Step 3: Add favorite-color persistence APIs**

Extend `AppSettingsRepository`, `SharedPreferencesAppSettingsRepository`, and `CaptureFlowSettings` with:

- `getFavoriteEditorColors()`
- `setFavoriteEditorColors(colors)`

Keep both favorite and recent lists capped at three serialized entries.

- [ ] **Step 4: Update `AnnotationViewModel` to use committed-color actions**

Add explicit methods for:

- applying a confirmed color choice
- toggling whether a color is a favorite
- exposing favorite colors and quick-access colors

Keep inline rail taps on the current immediate-apply path, but stop using dialog draft changes to record recents.

- [ ] **Step 5: Update `EditorDocumentState` to carry favorite and quick-access colors**

Expose only the data the screen needs, without moving persistence rules into Compose.

- [ ] **Step 6: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.AnnotationViewModelColorSelectionTest`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt app/src/main/java/com/muding/android/presentation/editor/EditorDocumentState.kt app/src/test/java/com/muding/android/presentation/editor/AnnotationViewModelColorSelectionTest.kt
git commit -m "feat: persist editor favorite colors"
```

## Task 4: Rebuild The Editor Rail And Dialog UI

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/java/com/muding/android/presentation/editor/EditorDocumentState.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/editor/AnnotationViewModel.kt`

- [ ] **Step 1: Rewire the inline color rail**

Keep the same footprint and slot count, but populate the three swatches from the view-model-provided quick-access colors instead of `current color + recents`.

- [ ] **Step 2: Replace the RGB-slider-only dialog layout**

Rebuild `ColorPaletteDialog` so it contains:

- a compact visual picker area initialized from the current committed color
- preview swatch plus favorite toggle
- `Hex` field above compact `RGB` fields
- one favorites row with three slots
- one recents row with three slots
- `Cancel` and `Apply` actions

- [ ] **Step 3: Keep dialog state local until confirmation**

Use `ColorPickerDraftState` inside the dialog so:

- drag and input changes affect only preview state
- tapping favorite or recent swatches updates the draft without closing
- `Apply` commits through the new view-model action
- `Cancel` and outside dismissal discard the draft

- [ ] **Step 4: Add or update string resources**

Add strings for:

- `Apply`
- favorites row label
- recents row label
- add/remove favorite content description

- [ ] **Step 5: Run a compile check**

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt app/src/main/res/values/strings.xml
git commit -m "feat: redesign editor color picker dialog"
```

## Task 5: Verify The Full Change Set

**Files:**
- Verify: `app/src/test/java/com/muding/android/presentation/editor/EditorColorCollectionsTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/editor/ColorPickerDraftStateTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/editor/AnnotationViewModelColorSelectionTest.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/editor/EditorScreen.kt`

- [ ] **Step 1: Run focused editor-color tests**

Run:

- `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.EditorColorCollectionsTest`
- `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.ColorPickerDraftStateTest`
- `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.editor.AnnotationViewModelColorSelectionTest`

Expected: PASS

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`

Expected: PASS

- [ ] **Step 3: Record residual validation needs**

Document that device-side validation is still required for:

- dialog visual balance on small screens
- drag feel in the visual picker area
- whether three favorites plus three recents remain legible in Chinese strings

- [ ] **Step 4: Commit final verification updates if needed**

```bash
git add app/src/main/java/com/muding/android/presentation/editor app/src/test/java/com/muding/android/presentation/editor app/src/main/java/com/muding/android/data/settings app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt app/src/main/res/values/strings.xml
git commit -m "test: verify editor color picker redesign"
```
