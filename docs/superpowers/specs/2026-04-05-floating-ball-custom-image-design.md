# Floating Ball Custom Image Design

Date: 2026-04-05
Project: androidprintScreen
Scope: floating ball appearance mode, custom floating ball image import, floating ball image rendering

## 1. Goal

Allow users to replace the current gradient-based floating ball appearance with a custom image while keeping the floating ball recognizable, lightweight, and stable.

The feature must:

- preserve the current three built-in theme appearances
- add a user-controlled custom image mode
- keep the floating ball circular and size-controlled by the existing size setting
- avoid turning floating ball appearance settings into a full image editor

## 2. Non-goals

- No support for multiple saved custom floating ball images
- No support for GIF or animated assets
- No freeform crop editor or drag-to-reposition crop UI
- No per-image overlays, badges, or dual-layer icon composition
- No cloud sync or export/import of floating ball image settings

## 3. Existing Implementation Context

Current floating ball appearance is defined across these boundaries:

- `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`
  - exposes `FloatingBallSettings`
- `app/src/main/java/com/muding/android/data/settings/SharedPreferencesAppSettingsRepository.kt`
  - maps stored settings into `FloatingBallSettings`
- `app/src/main/java/com/muding/android/domain/usecase/CaptureFlowSettings.kt`
  - persists floating ball size, opacity, and theme in shared preferences
- `app/src/main/java/com/muding/android/presentation/theme/FloatingBallAppearance.kt`
  - resolves the three theme gradients
- `app/src/main/java/com/muding/android/service/FloatingBallService.kt`
  - builds and renders the actual floating ball composable
- `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
  - lets the user select floating ball appearance settings

Today, theme selection only changes gradient colors. The content structure of the floating ball remains fixed: circular background plus camera icon.

## 4. Product Direction

Chosen direction: keep the current theme-based floating ball as the default path, but add one alternative appearance source: a single user-selected custom image.

This means:

- appearance is no longer just a theme choice
- users can switch between built-in theme appearance and custom image appearance
- custom image is treated as content inside the same circular floating ball footprint
- the custom image path remains highly constrained and easy to recover from

This is intentionally not:

- a media browser
- a multi-slot avatar system
- a highly configurable sticker engine for the floating ball itself

## 5. Data Model

Introduce one new conceptual layer above theme:

- `FloatingBallAppearanceMode`
  - `THEME`
  - `CUSTOM_IMAGE`

Extend `FloatingBallSettings` so it keeps:

- existing `sizeDp`
- existing `opacity`
- existing `theme`
- new `appearanceMode`
- new `customImageUri` or equivalent persistent identifier for the processed custom image

Repository and settings APIs should be extended so floating ball appearance mode and custom image reference are first-class settings, not ad hoc extras.

## 6. Storage Strategy

The app must not rely on the original gallery URI long-term.

Storage rules:

- user picks an image from the system picker
- the app decodes and processes it immediately
- the processed result is written into app-controlled cache storage under a dedicated floating-ball directory
- settings persist only the resulting processed image URI or local reference
- only one active custom floating ball image is retained
- choosing a new custom image replaces the previous stored one

When the user restores default appearance:

- `appearanceMode` becomes `THEME`
- the custom image reference is cleared
- the processed cached image may be deleted immediately as part of cleanup

This keeps the feature self-contained and prevents long-lived dependence on outside content permissions.

## 7. Image Processing

The first version should use a lightweight and deterministic processing path.

Processing pipeline:

1. open the selected image
2. crop to a centered square using the shortest side
3. scale to a fixed output size such as `256x256`
4. preserve alpha when present
5. write the processed image to floating ball cache storage

The goal is visual consistency, not editability.

Design constraints:

- no interactive crop tool in this phase
- no manual pan/zoom adjustment in this phase
- no history stack for previous picks

If the selected image is extremely large, decode should be sampled down before final scaling to control memory use.

## 8. Rendering

The floating ball remains circular regardless of appearance source.

### 8.1 Theme Mode

Keep current behavior:

- circular surface
- gradient background from the selected theme
- camera icon centered inside
- size and opacity controlled by existing floating ball settings

### 8.2 Custom Image Mode

Use the same outer circle and interaction behavior, but swap the visual content:

- render the processed image clipped to a circle
- use `ContentScale.Crop`
- keep the existing `sizeDp` setting as the overall floating ball size
- keep the existing opacity setting applied to the whole floating ball

The custom image mode should not add a second camera icon on top of the image in this phase.

### 8.3 Fallback Behavior

If custom image loading fails for any reason:

- automatically fall back to `THEME` mode
- keep the rest of floating ball settings intact
- avoid rendering an empty or broken floating ball

## 9. Settings UI

The floating ball settings page should stay compact and not become visually noisy.

### 9.1 Appearance Source Controls

In the existing floating ball appearance section, add an appearance source selector with two choices:

- default style
- custom image

Behavior:

- choosing default style reveals the existing theme choices
- choosing custom image reveals custom-image-specific controls

### 9.2 Custom Image Controls

When custom image mode is active, show:

- current image preview
- `Choose image`
- `Restore default style`

Recommended interaction:

- if the user switches to custom image and no image exists yet, immediately launch image selection rather than presenting a dead empty state first

### 9.3 Existing Controls

These controls remain shared by both appearance modes:

- floating ball size
- floating ball opacity

Theme choices remain relevant only in `THEME` mode.

## 10. User Flow

### 10.1 First-time Custom Image

1. user opens floating ball settings
2. user selects `custom image`
3. app launches image picker immediately if no current custom image exists
4. app processes the chosen image and stores it
5. app switches appearance mode to `CUSTOM_IMAGE`
6. app refreshes floating ball appearance

### 10.2 Replace Current Custom Image

1. user opens floating ball settings
2. current custom image preview is shown
3. user taps `Choose image`
4. app replaces the processed image and refreshes the floating ball

### 10.3 Restore Default

1. user taps `Restore default style`
2. app switches back to `THEME`
3. custom image reference is cleared
4. processed image cache may be deleted
5. floating ball refreshes immediately

## 11. Error Handling

Expected failure cases:

- picker returns no image
- image decoding fails
- processing fails due to memory or file I/O
- processed image cache is missing later at render time

Handling rules:

- cancellation should leave current appearance unchanged
- import failure should show a short error message and leave current appearance unchanged
- render-time failure should fall back to theme mode rather than displaying a blank control

## 12. Performance

The feature should not add a heavy rendering or persistence path to ordinary floating ball usage.

Performance principles:

- do processing once at selection time
- store a small normalized output image
- avoid decoding original full-size image during every floating ball render
- keep runtime rendering as simple as loading one small cached image

## 13. Testing Strategy

Focus on state transitions, storage behavior, and image-processing fallbacks.

Required coverage:

- appearance mode persistence
- custom image reference persistence
- switching to custom image with a valid processed result
- replacing an existing custom image
- restoring default style clears custom image settings
- render fallback when custom image reference is invalid
- image processing produces normalized square output metadata or expected storage result

Reasonable test split:

- pure logic tests for appearance mode state and fallback behavior
- repository/settings tests for persistence
- bounded unit tests for image processing helpers where practical

## 14. Implementation Boundaries

Recommended implementation split:

- settings/storage extension
  - add appearance mode and custom image reference support
- image import/processing helper
  - decode, square-crop, scale, cache, replace
- settings UI integration
  - source selector, preview, choose image, restore default
- floating ball rendering integration
  - render either theme mode or custom image mode with fallback behavior

This keeps storage, image processing, UI, and rendering separate enough to evolve independently.
