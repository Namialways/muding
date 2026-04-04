# Translation Settings Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild translation settings into a compact single-page mobile settings flow and simplify OCR result translation into one settings-driven action.

**Architecture:** Keep the existing settings repository and translation engines, but move translation UI behavior behind a small route/state layer and split the screen into focused local and cloud settings groups. Replace the OCR result screen's per-engine buttons with one translation entry point that reads current translation settings and routes to the correct engine at runtime.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android Activity + ViewModel patterns, JUnit4, Gradle

---

## File Map

### Create

- `app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsViewModel.kt`
  - Owns translation settings UI state, local model state refresh, provider-specific draft state, and update actions
- `app/src/test/java/com/muding/android/presentation/translation/TranslationSettingsViewModelTest.kt`
  - Tests summary/status logic and provider-specific visibility behavior for the new route state
- `docs/superpowers/plans/2026-04-04-translation-settings-redesign.md`
  - This implementation plan

### Modify

- `app/src/main/java/com/muding/android/feature/translation/TranslationModels.kt`
  - Normalize translation language and provider display labels used by the redesigned UI
- `app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsActivity.kt`
  - Replace the current long-form page with a compact single-page grouped settings layout wired to the new view model
- `app/src/main/java/com/muding/android/presentation/ocr/OcrResultActivity.kt`
  - Replace local/cloud/settings controls with a single translation action driven by saved settings
- `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
  - Keep only one concise translation entry row summary in main settings
- `app/src/test/java/com/muding/android/feature/translation/CloudTranslationEngineRouterTest.kt`
  - Update any test fakes affected by the new UI state access patterns if needed

## Scope Guardrails

- Do not add new providers or auto-translate-on-open behavior.
- Do not change repository persistence keys or translation engine implementations unless required to support the UI contract.
- Keep the redesign mobile-first and compact; avoid multi-page drilling unless implementation uncovers a hard blocker.

## Task 1: Add Translation Settings Route State

**Files:**
- Create: `app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsViewModel.kt`
- Create: `app/src/test/java/com/muding/android/presentation/translation/TranslationSettingsViewModelTest.kt`
- Modify: `app/src/main/java/com/muding/android/feature/translation/TranslationModels.kt`

- [ ] **Step 1: Write failing tests for translation settings UI state**

Add tests for:

- selected language summary
- local model status for the selected language
- cloud provider visibility rules
- credential form hidden when provider is `NONE`

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.translation.TranslationSettingsViewModelTest
```

Expected: `FAIL` because the view model and its UI state types do not exist yet.

- [ ] **Step 3: Implement the minimal route state**

Create focused state for:

- selected language
- Wi-Fi only toggle
- downloaded model tags
- selected provider
- provider-specific drafts
- transient feedback/loading flags

- [ ] **Step 4: Normalize display labels**

Update `TranslationModels.kt` so language and provider labels consumed by the new UI are clean and consistent.

- [ ] **Step 5: Run the targeted test again**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.translation.TranslationSettingsViewModelTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsViewModel.kt app/src/main/java/com/muding/android/feature/translation/TranslationModels.kt app/src/test/java/com/muding/android/presentation/translation/TranslationSettingsViewModelTest.kt
git commit -m "refactor(translation): add settings route state"
```

## Task 2: Rebuild Translation Settings Activity

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsActivity.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
- Verify: `app/src/main/java/com/muding/android/data/settings/AppSettingsRepository.kt`

- [ ] **Step 1: Write a failing test for provider-specific field visibility**

Extend `TranslationSettingsViewModelTest.kt` with cases covering:

- `NONE` hides cloud credential fields and action
- `YOUDAO` shows only Youdao fields
- `BAIDU` shows only Baidu fields

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.translation.TranslationSettingsViewModelTest
```

Expected: `FAIL` on the new visibility assertions.

- [ ] **Step 3: Replace the long-form translation page**

Refactor `TranslationSettingsActivity.kt` into:

- simple header
- `Local Translation` group
- `Cloud Translation` group
- no page-level `Save / Close` pair

The local group must use:

- language dropdown
- Wi-Fi only switch
- current model status row
- one action button for the selected model

The cloud group must use:

- service dropdown with `NONE / YOUDAO / BAIDU`
- provider-specific credentials only
- `Save and Verify` button only when provider is not `NONE`

- [ ] **Step 4: Keep main settings summary compact**

Ensure `SettingsScreen.kt` still opens translation settings from one concise entry row and does not regrow detailed translation state.

- [ ] **Step 5: Run debug compile**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsActivity.kt app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt
git commit -m "refactor(translation): rebuild settings page"
```

## Task 3: Simplify OCR Result Translation Flow

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/ocr/OcrResultActivity.kt`
- Test: `app/src/test/java/com/muding/android/feature/translation/CloudTranslationEngineRouterTest.kt`

- [ ] **Step 1: Write a failing test for settings-driven translation routing**

Add or extend tests to prove:

- cloud provider `NONE` no longer implies a visible cloud-translation choice in OCR UI
- OCR translation uses saved settings instead of a per-tap local/cloud mode choice

- [ ] **Step 2: Run the targeted tests to verify they fail**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.feature.translation.CloudTranslationEngineRouterTest
```

Expected: `FAIL` or missing behavior coverage that requires implementation.

- [ ] **Step 3: Replace OCR translation controls**

Refactor `OcrResultActivity.kt` so the screen exposes:

- one `Translate` button
- no separate `Local Translation` and `Cloud Translation` buttons
- no separate `Translation Settings` button in the action area

Translation behavior must:

- read current settings
- translate using local engine when cloud provider is `NONE`
- use the configured cloud engine when a provider is enabled

- [ ] **Step 4: Run translation tests**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.feature.translation.CloudTranslationEngineRouterTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/ocr/OcrResultActivity.kt app/src/test/java/com/muding/android/feature/translation/CloudTranslationEngineRouterTest.kt
git commit -m "refactor(ocr): use one settings-driven translate action"
```

## Task 4: Final Verification

**Files:**
- Verify only: `app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsActivity.kt`
- Verify only: `app/src/main/java/com/muding/android/presentation/ocr/OcrResultActivity.kt`
- Verify only: `app/src/test/java/com/muding/android/presentation/translation/TranslationSettingsViewModelTest.kt`

- [ ] **Step 1: Run focused unit tests**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.translation.TranslationSettingsViewModelTest --tests com.muding.android.feature.translation.CloudTranslationEngineRouterTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Run debug build verification**

Run:

```powershell
./gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit final integration changes**

```bash
git add app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsActivity.kt app/src/main/java/com/muding/android/presentation/ocr/OcrResultActivity.kt app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt app/src/main/java/com/muding/android/feature/translation/TranslationModels.kt app/src/main/java/com/muding/android/presentation/translation/TranslationSettingsViewModel.kt app/src/test/java/com/muding/android/presentation/translation/TranslationSettingsViewModelTest.kt app/src/test/java/com/muding/android/feature/translation/CloudTranslationEngineRouterTest.kt
git commit -m "refactor(translation): simplify translation settings flow"
```
