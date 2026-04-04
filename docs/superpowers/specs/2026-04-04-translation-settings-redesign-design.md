# Translation Settings Redesign Design

Date: 2026-04-04
Project: androidprintScreen
Scope: Translation settings entry in main settings, `TranslationSettingsActivity`, and OCR result screen translation actions

## 1. Goal

Redesign translation settings into a cleaner mobile-first configuration page that matches the app's warm-paper UI direction and removes the current long-form clutter.

The redesign must solve these current problems:

- local translation and cloud translation settings are mixed into one long screen
- target languages and providers are always expanded instead of being selected compactly
- all cloud credential fields are shown at once, even when they are irrelevant
- the OCR result screen duplicates strategy choice with separate local/cloud buttons
- the overall experience feels more like a raw configuration form than a modern app settings surface

## 2. Non-goals

- No new translation providers in this phase
- No new translation engines in this phase
- No changes to how credentials are persisted in storage
- No automatic translation immediately after OCR in this phase

## 3. Design References

The redesign follows common mobile settings and compact form patterns rather than dashboard or wizard patterns.

- Jetpack Compose menus: compact option selection through anchored menus
  - https://developer.android.com/develop/ui/compose/components/menu
- Jetpack Compose lists: structured vertical content for mobile scrolling
  - https://developer.android.com/develop/ui/compose/lists
- Apple HIG menus: show only relevant choices and reduce persistent noise
  - https://developer.apple.com/design/human-interface-guidelines/menus

Shared traits borrowed from mainstream tool apps:

- single responsibility per section
- dropdown or menu selection for low-frequency choices
- only current provider fields are visible
- settings page defines behavior, task page executes behavior
- low-noise forms with short labels and clear primary action

## 4. Product Direction

Chosen direction: single-page grouped settings with compact controls.

This means:

- one translation settings page with two clean groups instead of multiple subpages
- local model choice and cloud provider choice use dropdown selection
- only the currently selected model or provider is actionable
- the visual language should resemble a refined mobile settings sheet, not a backend form
- OCR result page should offer a single translation action and follow saved settings

This is intentionally not:

- a multi-page translation control center
- a tabbed local/cloud settings UI
- a page with every supported model and provider expanded at once

## 5. Information Architecture

Keep one `TranslationSettingsActivity` page with two groups:

- `Local Translation`
- `Cloud Translation`

The main settings overview keeps one entry into this page.

The OCR result screen should no longer let the user choose translation mode each time. It should use the configured strategy from translation settings.

## 6. Screen Design

### 6.1 Translation Settings Page

Structure:

1. simple page header
2. `Local Translation` group
3. `Cloud Translation` group

There should be no extra hero block, no large explanatory card, and no bottom `Save / Close` pair for the whole page.

### 6.2 Local Translation Group

Purpose:

- define target language
- define local model download rule
- manage only the currently selected local model

Rows and controls:

- `Target Language`
  - dropdown field
  - shows the currently selected language
- `Wi-Fi only download`
  - switch
- `Model Status`
  - concise status text for the currently selected language
  - examples: `Downloaded`, `Not downloaded`
- primary action row
  - if current model is missing: `Download Current Model`
  - if current model is present: `Delete Current Model`

Design principle:

- do not render a full language list with one action per row
- the user picks one target language, then acts on that selected model only

### 6.3 Cloud Translation Group

Purpose:

- define whether cloud translation is used
- define which provider is active
- edit only the active provider's credentials

Rows and controls:

- `Translation Service`
  - dropdown field
  - options:
    - `Do not use cloud translation`
    - `Youdao`
    - `Baidu`
- provider-specific credential fields
  - only visible when a concrete provider is selected
  - Youdao:
    - `App Key`
    - `App Secret`
  - Baidu:
    - `App ID`
    - `Secret Key`
- action row
  - only shown when a concrete provider is selected
  - button: `Save and Verify`

When `Do not use cloud translation` is selected:

- hide provider credential fields
- hide the save-and-verify action
- keep the section visually compact

This selection replaces the need for a separate toggle like `Use custom API key`.

## 7. OCR Result Screen Changes

The OCR result screen should stop exposing strategy selection as two separate buttons.

Current duplicated controls to remove:

- `Local Translation`
- `Cloud Translation`
- `Translation Settings`

Replace them with one button:

- `Translate`

Behavior:

- the button executes translation using the settings currently saved in translation settings
- local target language is always taken from translation settings
- if cloud translation is disabled, the OCR result screen should not try to use a cloud engine
- if cloud translation is enabled, the app uses the configured provider according to existing translation routing rules

This change makes translation strategy a setting, not an OCR-session decision.

## 8. Interaction Rules

### Immediate-apply settings

The following apply immediately when changed:

- target language
- Wi-Fi only download
- translation service provider selection

### Deferred-save settings

The following remain draft state until explicit confirmation:

- Youdao App Key
- Youdao App Secret
- Baidu App ID
- Baidu Secret Key

Only provider credentials require an explicit action:

- `Save and Verify`

### Feedback

Use short status feedback only.

Examples:

- `Model downloaded`
- `Model deleted`
- `Credentials saved`
- `Verification failed`

Avoid helper paragraphs unless the user is in an error state.

## 9. State and Architecture

The screen should not stay as one large mutable composable with mixed local and cloud state.

Recommended structure:

- `TranslationSettingsRoute`
- `TranslationSettingsViewModel`
- `TranslationSettingsUiState`
- small sub-composables for:
  - local translation group
  - cloud translation group
  - provider credential form

Responsibilities:

- persisted current settings from repository
- transient UI state for model operations and credential validation
- provider-specific draft state isolated from local model state
- OCR result page consumes current translation strategy instead of deciding engine in UI

## 10. Visual Language

The page should inherit the main UI redesign tokens:

- warm off-white background
- grouped bordered surfaces
- restrained accent usage
- short labels and trailing values
- no stacked oversized cards

The visual feeling should be closer to a polished mobile utility settings page than a desktop admin form.

## 11. Performance and Maintainability

- prefer `LazyColumn` or lightweight grouped scrolling structure over a huge eagerly rendered form
- avoid composing irrelevant provider fields
- keep provider-specific forms isolated so changing local translation state does not force unrelated cloud fields to refresh
- keep dropdown option lists stable and immutable
- reduce OCR result screen branching by replacing dual engine buttons with one translation action

## 12. Error Handling

### Local translation

- download failure should keep current selection unchanged
- delete failure should keep current state and show brief feedback

### Cloud translation

- missing required fields should block `Save and Verify`
- verification failure should surface brief provider-specific feedback
- selecting `Do not use cloud translation` should require no credentials and no validation

### OCR result

- if translation cannot run under current settings, show the existing resolved error message path rather than exposing provider details in the layout

## 13. Testing Strategy

Add or update tests to cover:

- local target language selection summary
- current local model status visibility
- action label switching between download and delete for selected model
- cloud provider dropdown state
- provider-specific field visibility
- disabled cloud translation state
- single `Translate` action behavior on OCR result screen

Manual verification should cover:

- narrow mobile widths
- switching between `Do not use cloud translation`, `Youdao`, and `Baidu`
- changing target language and then downloading or deleting the selected model
- OCR translation behavior after changing settings

## 14. Implementation Notes

Main settings overview should keep one concise entry into translation settings.

Recommended overview summary value:

- `Local + Cloud`

Do not mirror provider credential state on the main settings overview. Detailed translation state belongs inside the translation settings page itself.
