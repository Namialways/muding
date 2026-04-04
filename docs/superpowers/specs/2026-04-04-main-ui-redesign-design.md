# Main UI Redesign Design

Date: 2026-04-04
Project: androidprintScreen
Scope: Main app shell, home page, records page, settings overview, and settings detail pages

## 1. Goal

Redesign the app's main UI into a cleaner, more premium mobile experience without hurting runtime performance or future maintainability.

The target feeling is:

- warm white paper-like base
- concise and modern
- premium but not decorative
- tool-oriented, not marketing-oriented
- consistent across home, records, and settings

This redesign must also solve current usability problems:

- too many redundant hints and secondary descriptions
- records page spreads search, filter, sort, counters, and results across too many blocks
- settings overview is noisy and repeats data that is not actionable
- screenshot and floating-ball settings pages look dated and inconsistent
- current structure makes future theme expansion harder than it should be

## 2. Non-goals

- No feature expansion in this phase beyond UI and interaction restructuring
- No migration of records persistence architecture unless current UI work proves blocked by it
- No visually heavy effects such as blur-heavy glassmorphism, layered gradients everywhere, or animation-first interactions

## 3. Design References

The redesign direction is informed by these product and platform references:

- Linear Mobile: compact workflows, low-noise hierarchy, fast interaction
  - https://linear.app/mobile
- Todoist: restrained minimalism with clear emphasis and high scanability
  - https://www.todoist.com/features
- Android Compose SearchBar guidance: search as primary entry, secondary controls can be grouped around it
  - https://developer.android.com/develop/ui/compose/components/search-bar
- Apple HIG Menus: secondary controls should be grouped into menus instead of occupying permanent space
  - https://developer.apple.com/design/human-interface-guidelines/menus
- Material 3 in Compose: unified theming and expressive but disciplined component language
  - https://developer.android.com/develop/ui/compose/designsystems/material3

This project should not copy any one product. It should borrow the shared traits:

- low noise
- obvious hierarchy
- compact control grouping
- intentional spacing
- minimal persistent explanation text

## 4. Product Direction

Chosen direction: warm-white premium utility UI.

This means:

- warm off-white background instead of pure white
- subtle tonal surfaces instead of many large heavy cards
- small, controlled accent usage
- strong typography hierarchy instead of many helper texts
- forms and lists should feel precise and quiet

This is intentionally not:

- flat system-settings plainness
- bold gradient-heavy creative UI
- dense dashboard-style control walls

## 5. Information Architecture

### Main shell

Keep the existing three-tab structure:

- Home
- Records
- Settings

Do not add more tabs.

Top app bar behavior:

- Keep titles simple
- Keep back navigation only for settings detail pages
- Remove decorative or explanatory subheaders from the shell level

Bottom navigation behavior:

- Keep three items only
- Use a clean active state with stronger contrast and less visual weight than the current implementation

### Home

Home should answer three questions only:

1. Is the app ready to use right now
2. What are the four fastest actions I can take
3. What are the current key defaults

Home should not behave like a settings summary page.

### Records

Records should behave like a searchable work list, not a dashboard.

Top-level focus:

- search
- filter/sort
- result count
- record list

All non-essential storage metrics and repeated counters should be removed from the main records page.

### Settings

Settings overview should behave like a section launcher, not a report.

Top-level focus:

- open a category
- see only the minimum useful state

Repeated counts like pin history count and project record count should not appear in the overview unless they support an immediate decision.

## 6. Visual System

### Color direction

Base palette:

- background: warm off-white
- surface: slightly stronger warm neutral
- outline: soft beige-gray
- text: dark neutral, not pure black
- accent: restrained cool tone for active states and CTAs

Guideline:

- background carries warmth
- accent carries precision
- error and danger states remain obvious but contained

### Surface strategy

Use fewer cards overall.

When a surface is needed:

- prefer thin borders + soft elevation
- reduce nested cards
- avoid full-page card stacking

### Typography

Limit to three functional hierarchy levels on most screens:

- page title
- section title or item title
- body/meta text

Remove most helper paragraphs. Explanatory copy should appear only where a user could make a damaging or confusing decision.

### Shape and spacing

Use a consistent shape language:

- medium rounded corners for inputs and grouped surfaces
- slightly larger corners for primary action tiles
- compact vertical rhythm

Spacing should rely on a small token set:

- page gutter
- section gap
- row gap
- control gap

This is important for future theme switching because shape and spacing tokens should be theme-aware without changing layout logic.

## 7. Component Strategy

Create or refactor toward reusable main-UI components rather than page-specific one-offs.

Core reusable pieces:

- `MainSectionHeader`: title with optional terse trailing action, no default description
- `ActionTile`: icon + title only, optional one-line subtitle if strictly needed
- `SettingEntryRow`: icon, title, optional concise trailing value, chevron
- `SettingGroup`: titled grouped surface for related controls
- `InlineValueRow`: title on left, current value on right
- `RecordsToolbar`: search, filter menu, sort menu, refresh
- `FilterMenuButton` and `SortMenuButton`: compact dropdown triggers
- `StatBadge`: only for places where a number supports a decision

Components that should be reduced or removed:

- verbose summary pills everywhere
- metrics cards on pages where the metric is not the task
- cards that only exist to hold a single button

## 8. Page-by-Page Design

### 8.1 Home page

Structure:

1. readiness hero
2. quick actions grid
3. current defaults strip

#### Readiness hero

Purpose:

- show whether overlay permission/service state is ready
- expose the one required action if not ready

Rules:

- keep to one headline, one short supporting sentence max
- one primary action, one secondary action
- no extra pill wall

#### Quick actions grid

Show four main actions:

- gallery pin
- gallery OCR
- clipboard text pin
- translation settings

Visual treatment:

- two-column mobile grid
- icon-led tiles
- title first
- remove long descriptions unless shortened to one line

#### Current defaults strip

Show only 2 to 3 concise values that help users predict behavior:

- capture result action
- scale mode
- floating-ball theme or size

These should be compact, likely as inline list rows or small badges, not another grid of heavy metric cards.

### 8.2 Records page

Structure:

1. records toolbar
2. result header
3. results list

#### Records toolbar

Use one integrated toolbar surface:

- search field as primary control
- filter dropdown button
- sort dropdown button
- refresh button as tertiary action

Behavior:

- filter and sort should not occupy permanent multi-row chip sections
- selected filter/sort choice should be visible in the button label or menu summary
- menus should be lightweight and dismiss immediately on selection

#### Result header

Show:

- page title
- result count
- active query/filter summary only when needed

Do not show:

- top-of-page storage metrics
- multiple status cards
- repeated counters unrelated to the current search task

#### Result list

Each record cell should stay rich enough to be useful but quieter:

- thumbnail
- title
- timestamp
- one or two concise metadata chips max
- primary actions

The card style should be lighter and flatter than now.

### 8.3 Settings overview

Structure:

1. compact title area
2. settings section list

No top metrics cards.

Each settings entry should show:

- icon
- title
- optional terse trailing state
- chevron

Example trailing states:

- capture: `pin directly`
- floating ball: `enabled`
- pin interaction: `lock aspect`
- storage: `auto cleanup on`

Do not show explanatory chip clusters inside the overview.

### 8.4 Capture and floating settings

This page currently feels especially card-heavy and old-fashioned.

New structure:

1. behavior group
2. permission/runtime group
3. floating appearance group

Behavior group:

- capture result action as segmented choice or compact radio rows

Permission/runtime group:

- current status row
- primary action only when permission is missing
- restart action kept but visually secondary

Floating appearance group:

- compact preview
- size slider row
- opacity slider row
- theme selector row or small theme chips

The key improvement is that this page should look like a grouped settings form, not a stack of independent cards.

### 8.5 Pin and interaction settings

New structure:

1. resize behavior group
2. default appearance group

Resize behavior:

- one compact choice group

Default appearance:

- shadow switch
- corner radius slider

Remove helper paragraphs that only restate the setting in words.

### 8.6 OCR and translation

Keep this page minimal.

It should be mostly an entry page, not a content page:

- one grouped entry leading to translation settings
- optional concise note only if needed for context

### 8.7 Storage and records

This page should be task-based:

1. storage summary
2. retention policy
3. cleanup actions

Storage summary:

- keep actual storage usage by category because it supports cleanup decisions
- present as cleaner list rows or compact stat tiles

Retention policy:

- grouped controls for pin history and project records
- remove repeated reminder text
- show counts only if they directly support the policy controls

Cleanup actions:

- clearly grouped, with danger actions visually separated
- use concise labels

## 9. Theme Architecture

The redesign must prepare for future themes without requiring another layout rewrite.

Introduce a small main-UI token layer for:

- background colors
- surface colors
- border colors
- accent colors
- page padding
- corner sizes
- elevation strength
- icon container styles

Recommended future theme families:

- Warm Paper
- Cool Mist
- Dark Slate
- High Contrast

Theme switching should change:

- color set
- shape tone
- shadow/elevation intensity
- accent treatment

Theme switching should not change:

- navigation structure
- content hierarchy
- control placement
- basic interaction model

## 10. Performance Constraints

Performance is a hard requirement for this redesign.

Rules:

- no heavy calculations in Composables
- keep records filtering/search preprocessing off the main thread
- retain stable keys in lazy lists
- avoid overdraw from unnecessary nested cards and layered backgrounds
- use simple transitions only: alpha, size, offset, color
- avoid blur-heavy or shader-heavy effects
- avoid recomposition churn caused by giant shared UI state objects

UI quality should come from:

- spacing
- hierarchy
- restraint

Not from:

- expensive effects
- overly dense animation
- decorative surfaces everywhere

## 11. Maintainability Constraints

This redesign should improve code structure, not just visual output.

Rules:

- separate overview pages from detail section components cleanly
- move repeated visual patterns into reusable composables
- keep page-specific state local, but keep business state out of UI-only memory
- avoid embedding style decisions as magic numbers across many files
- centralize tokens and shared component styling

## 12. Implementation Outline

Recommended implementation order:

1. establish token layer and shared main-UI components
2. refactor settings overview and settings detail pages
3. refactor records page into integrated toolbar + list model
4. refine home page to match the new visual language
5. run visual polish pass across shell, spacing, and navigation states

This order minimizes duplicated work because components and tokens are defined before the page rewrite spreads.

## 13. Verification

The redesign is complete only if all of the following are true:

- compile passes
- unit tests pass
- records page still scrolls smoothly
- filter and sort work via dropdown controls
- settings overview no longer shows redundant count summaries
- screenshot/floating settings and storage/settings detail pages share one visual language
- theme token layer supports at least the initial warm-paper theme cleanly

## 14. Decision Summary

Approved direction:

- structure-level refactor
- warm white premium utility UI
- concise information hierarchy
- dropdown-based filter/sort on records page
- simplified settings overview
- grouped modern detail settings pages
- architecture ready for future theme variants
- performance preserved as a first-class constraint
