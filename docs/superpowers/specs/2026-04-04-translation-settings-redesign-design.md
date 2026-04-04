# Translation Settings Redesign Design

Date: 2026-04-04
Project: androidprintScreen
Scope: Translation settings entry in main settings and `TranslationSettingsActivity`

## 1. Goal

Redesign the translation settings experience into a modern, low-noise mobile settings flow that feels aligned with the app's warm-paper visual direction.

The redesign must solve these current problems:

- the page behaves like a long configuration form instead of a settings flow
- local translation, cloud translation, and credential management are mixed into one screen
- the user must scan too many controls before understanding current state
- two providers' credential fields appear at the same time, which creates visual noise
- the current page does not match the cleaner grouped-settings structure already established in the main settings redesign

## 2. Non-goals

- No new translation providers in this phase
- No new translation engines in this phase
- No change to credential storage format in this phase
- No redesign of OCR result screen behavior beyond navigation into translation settings

## 3. Design References

The redesign follows mainstream mobile settings patterns rather than dashboard or form-builder patterns.

- Jetpack Compose menus: use temporary menus for compact option picking instead of permanently expanded controls
  - https://developer.android.com/develop/ui/compose/components/menu
- Jetpack Compose lists: prefer efficient list-based structures for vertically scrolling collections
  - https://developer.android.com/develop/ui/compose/lists
- Apple HIG menus: keep option groups compact, prioritize important items first, and avoid long noisy menus
  - https://developer.apple.com/design/human-interface-guidelines/menus

Shared traits borrowed from mainstream tool apps:

- overview first, detail second
- one decision per row whenever possible
- short trailing state values
- only show configuration fields when they are relevant
- action labels should be concise and task-oriented

## 4. Product Direction

Chosen direction: grouped utility settings with warm-paper styling.

This means:

- translation settings should feel like a focused settings flow, not a setup wizard
- overview screen should emphasize current status over explanation text
- heavy forms should be pushed into detail screens
- only one provider's credentials should be visible at a time
- visual hierarchy should come from spacing, grouping, and trailing values, not from many large cards

This is intentionally not:

- a single endless page with all controls expanded
- a tabbed configuration surface
- a modal-heavy workflow with many interruptions

## 5. Information Architecture

Use one translation settings activity with three internal destinations:

- `Overview`
- `Local Translation`
- `Cloud Translation`

Do not create separate Android activities for local and cloud settings. Keep the flow inside the existing translation settings entry so navigation remains simple and maintainable.

### 5.1 Overview

Purpose:

- show current translation defaults at a glance
- expose only the settings that matter most often
- route the user into deeper management pages

Content:

- page title
- `Default Translation` group
- `Local Translation` entry row
- `Cloud Translation` entry row

### 5.2 Local Translation

Purpose:

- manage target language and local model download state

Content:

- target language picker
- Wi-Fi only download switch
- model list with per-language status and actions

### 5.3 Cloud Translation

Purpose:

- select provider
- edit only the active provider's credentials
- save and validate cloud configuration

Content:

- provider picker
- provider-specific credentials form
- validation or save status
- `Save and Test` primary action

## 6. Screen Design

### 6.1 Overview Screen

Structure:

1. simple page header
2. `Default Translation` group
3. `Local Translation` grouped entry
4. `Cloud Translation` grouped entry

#### Default Translation group

Rows:

- `Target Language` -> trailing value like `English`
- `Download on Wi-Fi Only` -> trailing switch

This group is intentionally small. The user should be able to confirm the app's default translation behavior in one glance.

#### Local Translation entry

Use a compact `SettingEntryRow`.

Trailing value examples:

- `Downloaded 2`
- `None downloaded`
- `Japanese ready`

Tap opens the local translation page.

#### Cloud Translation entry

Use a compact `SettingEntryRow`.

Trailing value examples:

- `Youdao / Configured`
- `Baidu / Missing key`
- `Disabled`

Tap opens the cloud translation page.

### 6.2 Local Translation Screen

Structure:

1. header with back action
2. `Language` group
3. `Download Rules` group
4. `Models` group

#### Language group

Use a menu or compact single-choice list, not a permanently expanded radio wall.

The selected language is shown as one current value row, such as:

- `Target Language` -> `English`

Tapping the row opens a menu anchored to the row trigger.

#### Download Rules group

Rows:

- `Wi-Fi only download` -> switch

This setting is instant-apply.

#### Models group

Show each supported language as a row item with:

- language name
- current status
- trailing action

Status examples:

- `Downloaded`
- `Not downloaded`
- `Downloading`

Action examples:

- `Download`
- `Delete`

The current target language should be visually marked, but not with oversized emphasis.

### 6.3 Cloud Translation Screen

Structure:

1. header with back action
2. `Provider` group
3. `Credentials` group
4. sticky or bottom-aligned primary action area

#### Provider group

Use a compact menu or segmented choice row. Do not expand all providers as a radio wall unless there are only two and the layout remains quiet.

Recommended default:

- a row showing `Provider` -> `Youdao / Baidu / Disabled`
- tapping opens a dropdown menu

Provider selection is instant-apply.

#### Credentials group

Only render fields for the currently selected provider.

Examples:

- Baidu:
  - `App ID`
  - `Secret Key`
- Youdao:
  - `App Key`
  - `App Secret`

If provider is `None`, hide the credentials form and show a short empty-state line such as `Cloud translation is disabled`.

#### Primary action

Only this screen keeps an explicit save action:

- `Save and Test`

Rationale:

- picker and switch changes are low-risk and should feel immediate
- credential edits are draft-like and need explicit confirmation
- testing immediately after save reduces uncertainty

## 7. Interaction Rules

### Immediate-apply settings

The following apply immediately when changed:

- target language
- Wi-Fi only download
- cloud provider selection

### Deferred-save settings

The following remain local draft state until explicit save:

- Baidu App ID
- Baidu Secret Key
- Youdao App Key
- Youdao App Secret

### Feedback

Use short inline status text or snackbar-style toast feedback.

Examples:

- `Model downloaded`
- `Model deleted`
- `Credentials saved`
- `Connection test failed`

Avoid verbose helper paragraphs under every group.

## 8. State and Architecture

Introduce a small route-state layer for translation settings instead of keeping the entire screen as one large composable with local mutable state.

Recommended structure:

- `TranslationSettingsRoute`
- `TranslationSettingsViewModel`
- `TranslationSettingsUiState`
- `TranslationSettingsDestination`

Responsibilities:

- repository-backed persisted state for current settings
- transient UI state for loading, download progress, save state, and validation message
- internal destination state for `Overview / Local / Cloud`
- provider-specific credential draft state isolated to the cloud settings screen

This keeps the flow maintainable when more target languages, providers, or status indicators are added later.

## 9. Visual Language

The translation settings redesign must inherit the main UI redesign tokens.

Rules:

- warm off-white background
- soft bordered grouped surfaces
- limited accent usage
- concise trailing value text
- no repeated decorative cards
- no large explanatory hero blocks

Typography:

- one page title
- group titles
- body text only where needed

The page should feel quieter than the current implementation.

## 10. Performance and Maintainability

- prefer `LazyColumn` over a fully expanded `Column` with all content rendered at once
- avoid rendering unrelated credential fields
- keep option collections immutable and stable
- isolate provider-specific forms into separate composables
- keep state writes scoped so changing a switch does not force unrelated form fields to recompute

This redesign should reduce UI complexity and recomposition noise compared with the current all-in-one screen.

## 11. Error Handling

### Local models

- download failure should leave current state intact
- delete failure should surface brief feedback and keep current list state

### Cloud settings

- missing credentials should be validated before save and test
- failed provider test should surface provider-specific error text
- selecting `None` as provider should not require any credentials

## 12. Testing Strategy

Add or update tests to cover:

- overview summary formatting
- provider-specific field visibility
- disabled cloud provider state
- immediate-apply behavior for target language and Wi-Fi-only switch
- save gating for cloud credentials
- local model status rendering and action availability

Manual verification should cover:

- mobile-height devices with narrow width
- switching providers repeatedly
- opening the page with existing saved credentials
- model download/delete feedback paths

## 13. Implementation Notes

The existing main settings overview should keep only one concise entry into translation settings.

Recommended summary value in main settings:

- `Local + Cloud`

Do not mirror detailed provider state on the main settings overview, because that makes the overview noisy again. The details belong inside the translation settings flow.
