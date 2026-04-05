# App Icon Design: Reference Note Tile

Date: 2026-04-06
Status: Implemented

## Goal

Replace the current launcher icon with a direction that feels simpler, more refined, and more premium while still being understandable at a glance.

The icon should read as a creative screenshot or annotation tool, not as a generic camera app or a purely abstract brand mark.

## Product Intent

The icon should communicate these ideas in priority order:

1. A captured or editable visual surface
2. Cropping or framing
3. A refined creative-tool feel

The icon should avoid these impressions:

- Heavy black-and-white block graphics with no clear meaning
- Generic system utility styling
- Camera lens symbolism
- Overly playful or toy-like color treatment

## Chosen Direction

The selected direction is `Reference Note Tile`.

This direction follows the final user-provided visual reference directly instead of the earlier crop-bracket explorations.

The icon uses:

- a light silver-gray launcher field
- a warm tilted note card
- two rounded text-like bars
- a dark circular action marker
- a soft offset shadow

This direction was chosen because it reads more like a polished application icon and less like a raw editing glyph.

## Visual Structure

The icon is composed of three layers:

1. Background field
   A soft silver-gray field with a subtle tonal shift so the icon feels bright and premium on the launcher.

2. Main note card
   A warm rounded card, slightly rotated, that reads as a pinned or editable visual panel.

3. Content and marker
   Two rounded bars suggest content or annotation, and a dark circular marker anchors the lower-right corner.

The silhouette should remain simple enough to survive launcher scaling and masked adaptive icon shapes.

## Style Rules

- Rounded geometry throughout
- Low shape count
- No tiny decorative details
- No literal camera body
- No heavy dark slab around a small center symbol
- No bracket-only composition
- No hard pure-black versus pure-white slab contrast
- No gradients inside the symbol unless they are extremely subtle

The overall look should feel calm, sharp, and intentional rather than flashy.

## Color Direction

The palette should move away from the current orange accent and from the dark bracket-heavy look of the previous attempts.

Use:

- Mist silver and pale blue-gray for the adaptive icon background
- Warm cream for the main card
- Deep graphite for the primary line and circular marker
- Medium warm gray for the secondary line
- A soft blue-gray shadow under the card

The palette should look calm and mature at launcher size rather than dramatic in isolation.

## Adaptive Icon Breakdown

### Foreground

The foreground should contain the `Reference Note Tile` symbol:

- tilted rounded note card
- two rounded content lines
- lower-right circular marker
- subtle card shadow

### Background

The background should be a light silver-gray field with a subtle gradient. The icon should feel brighter and more app-like on the launcher than the earlier dark-background attempt.

The icon must remain strong in both circular and rounded-square masks.

## Monochrome Variant

Provide a dedicated monochrome version for themed icons on modern Android launchers.

The monochrome variant should preserve:

- the rounded tile silhouette
- the cut corner
- the crop-guide reading

It should simplify tonal relationships but keep the note-card silhouette, two-line reading, and circular marker legible when flattened into a single-color system-driven theme.

## Implementation Notes

The current launcher assets that will need replacement are:

- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

The implementation should prefer vector assets so the launcher icon remains crisp and maintainable.

## Acceptance Criteria

The redesign is successful if:

- the icon feels noticeably more refined than the old orange camera-like mark
- the icon suggests editable content, annotation, or pinned visual material
- the icon no longer feels visually hollow or overly dark on the launcher
- the symbol remains understandable at launcher size
- the monochrome version remains recognizable
- the icon does not feel generic, playful, or over-engineered
