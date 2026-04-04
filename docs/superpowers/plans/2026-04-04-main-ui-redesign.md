# Main UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the main app shell, home page, records page, and settings pages into a warm-paper, low-noise UI without regressing performance or maintainability.

**Architecture:** Keep the current `MainScreen` state flow, but introduce a small main-UI token layer plus reusable grouped-setting and records-toolbar components. Move view-only label and summary logic into pure helpers in `MainUiModels.kt`, keep filtering and precomputation off the UI thread, and refactor pages to consume the new shared visual primitives.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Coil, JUnit4, Gradle

---

## File Map

### Create

- `app/src/main/java/com/muding/android/presentation/main/MainUiDesignTokens.kt`
  - Warm-paper palette, spacing, corner, and surface helpers for the main shell pages
- `app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt`
  - Unit tests for new pure summary and toolbar helper functions in `MainUiModels.kt`
- `docs/superpowers/plans/2026-04-04-main-ui-redesign.md`
  - This implementation plan

### Modify

- `app/src/main/java/com/muding/android/presentation/theme/Color.kt`
  - Replace the current default purple-forward light palette with warm-paper-friendly base tokens while keeping dark palette intact
- `app/src/main/java/com/muding/android/presentation/theme/Theme.kt`
  - Ensure main shell screens can consume the updated palette cleanly and leave room for future theme families
- `app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt`
  - Polish top app bar, bottom navigation, and shared page padding and containers
- `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
  - Replace noisy metrics, pills, and card patterns with reusable low-noise components
- `app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt`
  - Add pure helper functions for records toolbar summaries and settings trailing labels
- `app/src/main/java/com/muding/android/presentation/main/HomeDashboardScreen.kt`
  - Rebuild into readiness hero, quick actions, and compact defaults section
- `app/src/main/java/com/muding/android/presentation/main/RecordsScreen.kt`
  - Rebuild into search-first toolbar plus dropdown filter/sort and quieter result header and list
- `app/src/main/java/com/muding/android/presentation/main/RecordDetailScreen.kt`
  - Align detail page styling with the new records visual language
- `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
  - Rebuild overview and detail pages into grouped modern settings forms

### Verify Only

- `app/src/main/java/com/muding/android/MainActivity.kt`
  - Confirm no API changes are needed from the activity entry point

## Scope Guardrails

- Theme work in this plan means tokenizing the current theme and shipping Warm Paper.
- Theme switching UI is explicitly out of scope for this implementation.
- Do not migrate persistence or storage architecture in this plan.
- Do not add new settings categories or new product features in this plan.

## Task 1: Establish Main UI Design Tokens

**Files:**
- Create: `app/src/main/java/com/muding/android/presentation/main/MainUiDesignTokens.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/theme/Color.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/theme/Theme.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Verify: `app/src/main/java/com/muding/android/MainActivity.kt`

- [ ] **Step 1: Add the token file**

Create `MainUiDesignTokens.kt` with focused, reusable tokens instead of page-local magic values:

```kotlin
package com.muding.android.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class MainUiPalette(
    val pageBackground: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val surfaceMuted: Color,
    val outline: Color,
    val title: Color,
    val body: Color,
    val accent: Color,
    val accentMuted: Color,
    val danger: Color
)

data class MainUiSpacing(
    val pageGutter: Dp = 20.dp,
    val sectionGap: Dp = 18.dp,
    val groupGap: Dp = 14.dp,
    val rowGap: Dp = 10.dp
)

@Composable
fun rememberMainUiTokens(): MainUiTokens = ...
```

- [ ] **Step 2: Update the base palette to warm-paper values**

Modify `Color.kt` so the light theme stops looking default-purple-heavy. Keep accent capability, but move the page foundation to warm neutrals:

```kotlin
val Background = Color(0xFFF6F2EA)
val Surface = Color(0xFFFFFCF6)
val OnSurface = Color(0xFF201B17)
val SecondaryContainer = Color(0xFFEFE6D7)
```

Keep dark colors functional; do not redesign dark mode in this pass.

- [ ] **Step 3: Wire the theme file to expose the new palette cleanly**

Update `Theme.kt` only enough to support the refined light palette and avoid baking main-page magic numbers into page code:

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    secondaryContainer = SecondaryContainer
)
```

Do not add theme-switching state here yet.

- [ ] **Step 4: Replace ad-hoc surface styling in shared components**

Start consuming the new token helpers in `MainUiComponents.kt` so cards, grouped rows, and icon containers stop using repeated patterns like `surfaceVariant.copy(alpha = 0.32f)`.

Target replacements:

- grouped setting surface helper
- icon container helper
- compact badge surface helper
- page-level section spacing helper

- [ ] **Step 5: Run compile verification**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/MainUiDesignTokens.kt app/src/main/java/com/muding/android/presentation/theme/Color.kt app/src/main/java/com/muding/android/presentation/theme/Theme.kt app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt
git commit -m "refactor(main): add warm-paper UI design tokens"
```

## Task 2: Rebuild Settings Overview And Detail Pages

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt`
- Test: `app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt`

- [ ] **Step 1: Write failing tests for settings summary helpers**

Add tests for new pure helper functions that produce concise trailing values for the overview rows:

```kotlin
class MainUiModelsTest {

    @Test
    fun captureSettingsSummary_prefers_direct_pin_label() {
        val summary = captureSettingsSummary(
            action = CaptureResultAction.PIN_DIRECTLY,
            permissionGranted = true
        )

        assertEquals("Pin directly", summary)
    }

    @Test
    fun storageSettingsSummary_prefers_retention_state_not_counts() {
        val summary = storageSettingsSummary(pinHistoryEnabled = true)

        assertEquals("Auto cleanup on", summary)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.main.MainUiModelsTest
```

Expected: `FAIL` because the helper functions and test package do not exist yet.

- [ ] **Step 3: Implement the pure helper functions**

Add focused summary helpers in `MainUiModels.kt`:

```kotlin
fun captureSettingsSummary(
    action: CaptureResultAction,
    permissionGranted: Boolean
): String = ...

fun pinInteractionSettingsSummary(scaleMode: PinScaleMode): String = ...

fun storageSettingsSummary(pinHistoryEnabled: Boolean): String = ...
```

Do not pull snapshot counts into these summaries.

- [ ] **Step 4: Replace the overview page with entry rows**

Refactor `SettingsScreen.kt` overview into a compact launcher list:

- remove top metrics cards
- remove chip clusters and duplicated counts
- use `SettingEntryRow`
- show a terse trailing value only where helpful

Expected structure:

```kotlin
LazyColumn {
    item { MainSectionHeader(title = "Settings") }
    item { SettingEntryRow(...) }
    item { SettingEntryRow(...) }
    item { SettingEntryRow(...) }
    item { SettingEntryRow(...) }
}
```

- [ ] **Step 5: Rebuild settings detail pages as grouped forms**

Refactor these sections in `SettingsScreen.kt`:

- `CaptureAndFloatingSettingsSection`
- `PinAndInteractionSettingsSection`
- `OcrAndTranslationSettingsSection`
- `StorageAndRecordsSettingsSection`

Rules:

- fewer cards, more grouped sections
- helper text only where a destructive or confusing action needs it
- storage page keeps usage data but removes repeated reminder text
- floating-ball page keeps preview, but rows should feel modern and aligned

- [ ] **Step 6: Add the shared grouped settings components**

In `MainUiComponents.kt`, add reusable pieces used by the refactor:

```kotlin
@Composable
fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) { ... }

@Composable
fun SettingEntryRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) { ... }

@Composable
fun InlineValueRow(label: String, value: String) { ... }
```

- [ ] **Step 7: Run the new settings tests**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.main.MainUiModelsTest
```

Expected: `PASS`

- [ ] **Step 8: Run compile verification**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt
git commit -m "refactor(settings): rebuild main settings screens"
```

## Task 3: Rebuild The Records Page Around Search And Menus

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/main/RecordsScreen.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/RecordDetailScreen.kt`
- Test: `app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt`

- [ ] **Step 1: Write failing tests for records toolbar helper logic**

Add tests for the compact toolbar and result-header summary helpers:

```kotlin
@Test
fun recordsCriteriaSummary_returns_null_for_default_state() {
    val summary = recordsCriteriaSummary(
        filter = RecordsFilter.ALL,
        sort = RecordsSortOrder.NEWEST,
        query = ""
    )

    assertNull(summary)
}

@Test
fun recordsCriteriaSummary_includes_filter_and_query_when_active() {
    val summary = recordsCriteriaSummary(
        filter = RecordsFilter.TEXT,
        sort = RecordsSortOrder.NEWEST,
        query = "ocr"
    )

    assertEquals("Text - Search 'ocr'", summary)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.main.MainUiModelsTest
```

Expected: `FAIL` because `recordsCriteriaSummary()` does not exist yet.

- [ ] **Step 3: Implement the pure helper functions**

Add records-specific helpers in `MainUiModels.kt`:

```kotlin
fun recordsCriteriaSummary(
    filter: RecordsFilter,
    sort: RecordsSortOrder,
    query: String
): String? = ...

fun recordsFilterButtonLabel(filter: RecordsFilter): String = ...

fun recordsSortButtonLabel(sort: RecordsSortOrder): String = ...
```

Keep the existing background-thread record computation path intact.

- [ ] **Step 4: Replace the current top-of-page dashboard blocks**

Refactor `RecordsScreen.kt` to this order:

- section title
- integrated toolbar surface
- result header
- loading, empty, or list state

Remove:

- top metrics cards
- dedicated multi-row filter chip area
- dedicated multi-row sort chip area

- [ ] **Step 5: Add compact menu-based controls**

In `MainUiComponents.kt`, add reusable dropdown-trigger pieces using `DropdownMenu`:

```kotlin
@Composable
fun FilterMenuButton(...)

@Composable
fun SortMenuButton(...)

@Composable
fun RecordsToolbar(...)
```

Behavior requirements:

- button shows current selection
- menu dismisses on selection
- controls remain reachable on small screens

- [ ] **Step 6: Lightly restyle the record cells and detail page**

Update `PinHistoryRecordCard` and `RecordDetailScreen.kt` to match the calmer visual system:

- lighter surface
- fewer chips
- better spacing
- no extra explanatory noise

- [ ] **Step 7: Run the records helper tests**

Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests com.muding.android.presentation.main.MainUiModelsTest
```

Expected: `PASS`

- [ ] **Step 8: Run compile verification**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/RecordsScreen.kt app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt app/src/main/java/com/muding/android/presentation/main/RecordDetailScreen.kt app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt
git commit -m "refactor(records): rebuild records page toolbar and layout"
```

## Task 4: Rebuild The Home Page And Shell Chrome

**Files:**
- Modify: `app/src/main/java/com/muding/android/presentation/main/HomeDashboardScreen.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Modify: `app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt`

- [ ] **Step 1: Replace the hero with a concise readiness block**

Refactor the home top section to show:

- one status headline
- one short support line
- one primary action
- one secondary action

Do not re-add summary pill clusters.

- [ ] **Step 2: Simplify the quick action grid**

Refactor the four action tiles to be more compact:

```kotlin
ActionTile(
    icon = Icons.Default.PhotoLibrary,
    title = "Gallery Pin",
    subtitle = "Create from gallery"
)
```

Limit subtitles to one short line or omit them entirely.

- [ ] **Step 3: Replace the current defaults cards with a compact defaults strip**

Use 2 to 3 concise rows or badges for:

- capture result behavior
- scale mode
- floating-ball theme or size

Implement any formatting helpers in `MainUiModels.kt` rather than building strings inline.

- [ ] **Step 4: Polish shell padding and bottom navigation**

Refactor `MainShellScreen.kt`:

- reduce heaviness of the current top and bottom chrome
- align page padding with the new token layer
- keep current navigation logic intact

Do not change destination behavior or settings back-stack behavior.

- [ ] **Step 5: Run compile verification**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main/HomeDashboardScreen.kt app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt
git commit -m "refactor(home): rebuild dashboard and shell chrome"
```

## Task 5: Full Verification And QA

**Files:**
- Verify: `app/src/main/java/com/muding/android/presentation/main/MainShellScreen.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/HomeDashboardScreen.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/RecordsScreen.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/RecordDetailScreen.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/SettingsScreen.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/MainUiComponents.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/MainUiModels.kt`
- Verify: `app/src/main/java/com/muding/android/presentation/main/MainUiDesignTokens.kt`
- Test: `app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt`

- [ ] **Step 1: Run full compile and unit tests**

Run:

```powershell
./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --continue
```

Expected:

- `BUILD SUCCESSFUL`
- `MainUiModelsTest` passes
- existing translation tests remain green

- [ ] **Step 2: Run manual QA on the main screens**

Manual checks:

- Home page reads clearly at first glance
- Records page search feels primary
- Filter and sort are dropdown-based and dismiss immediately
- Settings overview no longer shows redundant record counts
- Capture and floating plus storage detail pages look like one family
- No card pile-up or crowded helper text returns

- [ ] **Step 3: Run targeted performance QA**

Manual checks:

- Records list still scrolls smoothly
- Search query changes do not freeze the page
- Settings page scroll remains stable
- No obvious animation jank from section transitions

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/muding/android/presentation/main app/src/main/java/com/muding/android/presentation/theme/Color.kt app/src/main/java/com/muding/android/presentation/theme/Theme.kt app/src/test/java/com/muding/android/presentation/main/MainUiModelsTest.kt
git commit -m "refactor(main): complete main UI redesign"
```

## Review Notes

Local plan review outcome: approved with one scope clarification.

- Keep theme tokenization in scope.
- Keep theme switching UI out of scope.
