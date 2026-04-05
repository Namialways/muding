# App Icon Design: Crop Tile

Date: 2026-04-06
Status: Approved for implementation planning

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

The selected direction is `Crop Tile`.

This concept uses a rounded card or tile as the primary shape, then removes one corner to create a crop-cut gesture. Two restrained guide lines reinforce the idea of cropping without making the icon feel busy or technical.

This direction was chosen because it balances product meaning and visual restraint better than the more abstract concepts explored earlier.

## Visual Structure

The icon is composed of three layers:

1. Background field
   A dark, clean adaptive-icon background that gives the icon a stable, premium base.

2. Main tile
   A soft warm-light rounded card that reads as a screenshot, canvas, or pinned visual panel.

3. Crop cue
   A cut-out corner plus two guide lines that indicate editing, trimming, or framing.

The silhouette should remain simple enough to survive launcher scaling and masked adaptive icon shapes.

## Style Rules

- Rounded geometry throughout
- Low shape count
- No tiny decorative details
- No literal camera body
- No hard pure-black versus pure-white slab contrast
- No gradients inside the symbol unless they are extremely subtle

The overall look should feel calm, sharp, and intentional rather than flashy.

## Color Direction

The palette should move away from the current orange accent.

Use:

- Deep graphite or charcoal for the background
- Warm off-white for the main tile
- A low-saturation blue-gray as the accent for crop guides

The accent color should support recognition without turning the icon into a bright consumer-app badge.

## Adaptive Icon Breakdown

### Foreground

The foreground should contain the `Crop Tile` symbol:

- rounded tile
- corner cut
- crop guide lines

### Background

The background should be a simple deep graphite fill, optionally with a very soft tonal shift if needed for depth.

The icon must remain strong in both circular and rounded-square masks.

## Monochrome Variant

Provide a dedicated monochrome version for themed icons on modern Android launchers.

The monochrome variant should preserve:

- the rounded tile silhouette
- the cut corner
- the crop-guide reading

It should simplify tonal relationships but keep the icon legible when flattened into a single-color system-driven theme.

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

- the icon feels noticeably more refined than the current orange camera-like mark
- the icon still suggests screenshot, cropping, or editable visual content
- the symbol remains understandable at launcher size
- the monochrome version remains recognizable
- the icon does not feel generic, playful, or over-engineered
