# App Icon Reference Note Tile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current launcher icon with the approved reference-based note-tile direction across adaptive and monochrome Android icon assets.

**Architecture:** Keep the existing adaptive icon entrypoints, switch the adaptive background from a flat color to a drawable gradient, and replace the launcher foreground and monochrome vectors with a note-card composition derived from the approved reference. Do not change app behavior or introduce raster assets; keep the icon fully vector-based and compatible with themed icons.

**Tech Stack:** Android XML vector drawables, adaptive icons, Gradle Android build

---

### Task 1: Replace the launcher icon assets

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

- [ ] **Step 1: Replace the foreground vector with the approved note-card symbol**

Create a tilted rounded note card with two rounded content bars, a lower-right circular marker, and a soft shadow using the existing 108 viewport.

- [ ] **Step 2: Replace the monochrome vector**

Flatten the same symbol into a single-color themed-icon-safe variant while preserving the note-card, two-bar, and circular-marker reading.

- [ ] **Step 3: Update the icon background and palette**

Set the adaptive icon background to a light silver-gray gradient, the card to warm cream, and the foreground accents to graphite and warm gray. Add or rename color resources if needed.

### Task 2: Verify the launcher assets compile

**Files:**
- Verify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Verify: `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Verify: `app/src/main/res/values/colors.xml`

- [ ] **Step 1: Run a fresh Android build**

Run: `./gradlew.bat assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Review the worktree**

Run: `git status --short`

Expected: only the icon asset files and this plan/spec documentation appear as modified or added.
