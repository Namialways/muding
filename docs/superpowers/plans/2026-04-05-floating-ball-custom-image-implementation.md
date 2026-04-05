# Floating Ball Custom Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a custom-image appearance mode for the floating ball while preserving the existing theme-based mode and keeping the floating ball circular, lightweight, and recoverable.

**Architecture:** Extend floating-ball settings so appearance source becomes first-class state, then add a small image-processing/cache path that normalizes one selected gallery image into an app-owned floating-ball asset. Rebuild the settings UI and runtime rendering around a dual-mode appearance model with explicit fallback to built-in theme mode if custom image state becomes invalid.

**Tech Stack:** Kotlin, Jetpack Compose Material3, SharedPreferences, Android Activity Result APIs, Bitmap/Canvas processing, JUnit4, Gradle

---

## File Map

### Create

- `app/src/main/java/com/muding/android/domain/usecase/FloatingBallAppearanceMode.kt`
  - Enum for `THEME` and `CUSTOM_IMAGE`
- `app/src/main/java/com/muding/android/feature/floatingball/FloatingBallImageProcessor.kt`
  - Decode, centered square crop, scale, and cache one selected custom image
- `app/src/main/java/com/muding/android/presentation/source/FloatingBallImagePickerActivity.kt`
  - Lightweight gallery picker flow for selecting a custom floating-ball image
- `app/src/test/java/com/muding/android/feature/floatingball/FloatingBallImageProcessingPlanTest.kt`
  - Unit tests for pure image-processing plan math and fallback decisions
- `app/src/test/java/com/muding/android/presentation/main/FloatingBallAppearanceSourceStateTest.kt`
  - Unit tests for settings-side appearance source behavior
- `docs/superpowers/plans/2026-04-05-floating-ball-custom-image-implementation.md`
  - This implementation plan

### Modify

- `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
  - Extend `FloatingBallSettings` and repository methods for appearance mode and custom image reference
- `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
  - Persist and expose the new floating-ball appearance fields
- `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
  - Store and retrieve floating-ball appearance mode and custom image URI
- `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
  - Add appearance source selection, custom image preview/actions, and restore-default action
- `app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt`
  - Thread custom image settings state and callbacks through the main settings screen
- `app/src/main/java/com/muding/android/MainActivity.kt`
  - Wire choose-image and restore-default actions to persistence, processing, and floating-ball refresh
- `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
  - Extend floating-ball preview composable to render either theme mode or custom image mode
- `app/src/main/java/com/muding/android/service/FloatingBallService.kt`
  - Load and render custom image appearance with theme fallback if asset loading fails
- `app/src/main/res/values/strings.xml`
  - Add strings for appearance source selection, choose image, restore default, and failure messages

### Verify Only

- `docs/superpowers/specs/2026-04-05-floating-ball-custom-image-design.md`
  - Approved design source of truth for plan alignment
- `app/src/main/java/com/muding/android/domain/usecase/CacheImageStore.kt`
  - Existing cache-writing pattern to reuse rather than duplicating ad hoc file logic
- `app/src/main/java/com/muding/android/presentation/source/GalleryPinActivity.kt`
  - Existing image-picking pattern to follow for the new floating-ball picker activity

## Task 1: Extend Floating-Ball Appearance Persistence With TDD

**Files:**
- Create: `app/src/main/java/com/muding/android/domain/usecase/FloatingBallAppearanceMode.kt`
- Modify: `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
- Modify: `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
- Modify: `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
- Modify: `app/src/test/java/com/muding/android/domain/usecase/CaptureFlowSettingsTest.kt`

- [ ] **Step 1: Write failing persistence tests**

Cover:

- floating-ball settings default to `THEME` mode with no custom image URI
- stored appearance mode loads correctly
- stored custom image URI loads correctly
- clearing custom image URI preserves other floating-ball settings

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.CaptureFlowSettingsTest`

Expected: FAIL because appearance mode and custom image settings do not exist yet.

- [ ] **Step 3: Add the new persistence types and APIs**

Implement:

- `FloatingBallAppearanceMode`
- `FloatingBallSettings(appearanceMode, customImageUri)`
- repository getters/setters for floating-ball appearance mode and custom image URI
- shared-preference keys and read/write logic in `CaptureFlowSettings`

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.CaptureFlowSettingsTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/domain/usecase/FloatingBallAppearanceMode.kt app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt app/src/test/java/com/muding/android/domain/usecase/CaptureFlowSettingsTest.kt
git commit -m "feat: persist floating ball appearance source"
```

## Task 2: Add Custom Image Processing And Storage With TDD

**Files:**
- Create: `app/src/main/java/com/muding/android/feature/floatingball/FloatingBallImageProcessor.kt`
- Create: `app/src/test/java/com/muding/android/feature/floatingball/FloatingBallImageProcessingPlanTest.kt`
- Verify: `app/src/main/java/com/muding/android/domain/usecase/CacheImageStore.kt`

- [ ] **Step 1: Write failing processing tests**

Cover:

- rectangular source dimensions produce a centered square crop plan
- output size is normalized to the fixed target dimension
- replacing an existing custom image keeps only one current image reference
- invalid custom image input produces a recoverable failure result

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.feature.floatingball.FloatingBallImageProcessingPlanTest`

Expected: FAIL because the processor and its pure planning helpers do not exist yet.

- [ ] **Step 3: Implement the image processor**

Implement a focused helper that:

- opens a selected image URI
- samples large images down before decode if needed
- center-crops to a square
- scales to the normalized output size
- writes the processed image to app cache under a floating-ball-specific directory
- deletes the previously stored floating-ball custom image when replacing it

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.feature.floatingball.FloatingBallImageProcessingPlanTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/feature/floatingball/FloatingBallImageProcessor.kt app/src/test/java/com/muding/android/feature/floatingball/FloatingBallImageProcessingPlanTest.kt
git commit -m "feat: add floating ball image processor"
```

## Task 3: Add Settings-Side Appearance Source Behavior With TDD

**Files:**
- Create: `app/src/test/java/com/muding/android/presentation/main/FloatingBallAppearanceSourceStateTest.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt`
- Modify: `app/src/main/java/com/muding/android/MainActivity.kt`

- [ ] **Step 1: Write failing appearance-state tests**

Cover:

- selecting custom image mode with no stored image requests image picking
- restoring default appearance clears custom image settings
- successful image processing switches state to `CUSTOM_IMAGE`
- cancellation leaves committed appearance unchanged

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.main.FloatingBallAppearanceSourceStateTest`

Expected: FAIL because custom-image appearance behavior is not wired into the settings flow yet.

- [ ] **Step 3: Implement the activity/settings orchestration**

Add minimal orchestration that:

- exposes choose-image and restore-default callbacks from the main settings screen
- launches a dedicated picker activity for custom floating-ball image selection
- updates settings only after a processed result succeeds
- refreshes the floating-ball appearance after successful mode changes

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.presentation.main.FloatingBallAppearanceSourceStateTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt app/src/main/java/com/muding/android/MainActivity.kt app/src/test/java/com/muding/android/presentation/main/FloatingBallAppearanceSourceStateTest.kt
git commit -m "feat: wire floating ball custom image settings flow"
```

## Task 4: Add Picker Activity For Floating-Ball Custom Images

**Files:**
- Create: `app/src/main/java/com/muding/android/presentation/source/FloatingBallImagePickerActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Verify: `app/src/main/java/com/muding/android/presentation/source/GalleryPinActivity.kt`

- [ ] **Step 1: Reuse the existing gallery-picker pattern**

Follow the same launcher pattern as `GalleryPinActivity`, but route the selected URI into the floating-ball image processor instead of the pin flow.

- [ ] **Step 2: Process and persist the selected image**

On success:

- write the processed image
- store the new custom image URI
- set appearance mode to `CUSTOM_IMAGE`
- refresh the floating ball

On cancel/failure:

- keep current appearance unchanged
- restore floating-ball visibility if it was hidden

- [ ] **Step 3: Add manifest registration**

Register the new picker activity in the manifest using the same lightweight, transient-activity style as the other picker helpers.

- [ ] **Step 4: Run a compile check**

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/source/FloatingBallImagePickerActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add floating ball image picker flow"
```

## Task 5: Rebuild Settings UI For Dual Appearance Sources

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add the appearance source controls**

In the floating-ball appearance section, add:

- `Default style`
- `Custom image`

Use the existing compact selection patterns already present in settings rather than introducing a large new control style.

- [ ] **Step 2: Add custom image preview and actions**

When `CUSTOM_IMAGE` is active, show:

- current preview card
- `Choose image`
- `Restore default style`

If custom image mode is chosen with no current image, the UI path should immediately trigger image selection rather than leaving the user in a dead state.

- [ ] **Step 3: Update the floating-ball preview composable**

Extend the preview component so it can render:

- existing gradient + icon theme mode
- circular custom image mode

Both modes must continue to respect the shared size and opacity settings.

- [ ] **Step 4: Add user-facing strings**

Add strings for:

- appearance source labels
- choose-image action
- restore-default action
- import failure or missing image fallback messages if they surface in the UI

- [ ] **Step 5: Run a compile check**

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt app/src/main/res/values/strings.xml
git commit -m "feat: add custom floating ball image settings UI"
```

## Task 6: Add Runtime Rendering And Fallback Behavior

**Files:**
- Modify: `app/src/main/java/com/muding/android/service/FloatingBallService.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/theme/FloatingBallAppearance.kt`

- [ ] **Step 1: Extend floating-ball appearance loading**

Load the full floating-ball appearance settings, including appearance mode and custom image URI, when building runtime appearance state.

- [ ] **Step 2: Render custom image mode**

Add runtime rendering support for:

- circular clipped image content
- existing click and long-press behavior
- existing opacity and size controls

- [ ] **Step 3: Add runtime fallback**

If the custom image cannot be decoded:

- fall back to theme mode immediately
- avoid rendering a blank or broken floating ball
- optionally clear invalid custom image state if that is the safest current-path recovery

- [ ] **Step 4: Run a compile check**

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/service/FloatingBallService.kt
git commit -m "feat: render custom floating ball image mode"
```

## Task 7: Verify The Full Change Set

**Files:**
- Verify: `app/src/test/java/com/muding/android/domain/usecase/CaptureFlowSettingsTest.kt`
- Verify: `app/src/test/java/com/muding/android/feature/floatingball/FloatingBallImageProcessingPlanTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/main/FloatingBallAppearanceSourceStateTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/main/DeferredFloatSettingDraftTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/main/FloatingBallAppearanceDraftTest.kt`
- Verify: `app/src/test/java/com/muding/android/presentation/main/FloatingBallSettingSliderMappingsTest.kt`

- [ ] **Step 1: Run focused floating-ball tests**

Run:

`./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.CaptureFlowSettingsTest --tests com.muding.android.feature.floatingball.FloatingBallImageProcessingPlanTest --tests com.muding.android.presentation.main.FloatingBallAppearanceSourceStateTest --tests com.muding.android.presentation.main.DeferredFloatSettingDraftTest --tests com.muding.android.presentation.main.FloatingBallAppearanceDraftTest --tests com.muding.android.presentation.main.FloatingBallSettingSliderMappingsTest`

Expected: PASS

- [ ] **Step 2: Run full unit tests**

Run: `./gradlew.bat testDebugUnitTest`

Expected: PASS

- [ ] **Step 3: Run final debug build**

Run: `./gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Review git diff and summarize residual risks**

Check:

- runtime fallback behavior if cached custom image disappears
- picker cancellation path
- whether the new cache directory is included in storage cleanup expectations

- [ ] **Step 5: Commit final verification if any polish changes were required**

```bash
git add -A
git commit -m "test: verify floating ball custom image flow"
```
