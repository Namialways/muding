# Screenshot Background Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move screenshot frame waiting and bitmap extraction off the main thread without changing the current floating-ball capture flow.

**Architecture:** Add a small pure-Kotlin pending-capture controller that owns timeout, first-frame-drop, and single-request rules, then rework `ScreenshotManager` to use a dedicated background `HandlerThread` and `ImageReader.OnImageAvailableListener`. Keep `FloatingBallService` on the same API surface so the existing idle-timeout and crop flow remain intact.

**Tech Stack:** Kotlin, Android SDK `HandlerThread`/`ImageReader`, Coroutines, JUnit4, Gradle

---

## File Map

### Create

- `app/src/main/java/com/muding/android/domain/usecase/ScreenshotCaptureRequestController.kt`
  - Pure request state machine for single in-flight capture, timeout scheduling, and first-frame drop handling
- `app/src/test/java/com/muding/android/domain/usecase/ScreenshotCaptureRequestControllerTest.kt`
  - Unit tests for the request controller
- `docs/superpowers/specs/2026-04-04-screenshot-background-capture-design.md`
  - Approved design summary
- `docs/superpowers/plans/2026-04-04-screenshot-background-capture.md`
  - This implementation plan

### Modify

- `app/src/main/java/com/muding/android/domain/usecase/ScreenshotManager.kt`
  - Replace main-thread polling with background-thread listener-driven capture

### Verify Only

- `app/src/main/java/com/muding/android/service/FloatingBallService.kt`
  - Confirm existing call sites do not require API changes

## Task 1: Add Request Controller With TDD

**Files:**
- Create: `app/src/main/java/com/muding/android/domain/usecase/ScreenshotCaptureRequestController.kt`
- Test: `app/src/test/java/com/muding/android/domain/usecase/ScreenshotCaptureRequestControllerTest.kt`

- [ ] **Step 1: Write the failing tests**

Cover:

- request start schedules timeout
- second request is rejected while one is pending
- dropped first frame requires another frame
- timeout clears the request
- manual clear prevents later completion

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.ScreenshotCaptureRequestControllerTest`

Expected: FAIL because `ScreenshotCaptureRequestController` does not exist yet.

- [ ] **Step 3: Write the minimal controller implementation**

Implement a single pending request with:

- scheduler abstraction
- `startCapture`
- `onFrameAvailable`
- `complete`
- `fail`
- `clear`

- [ ] **Step 4: Run the targeted test to verify it passes**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.ScreenshotCaptureRequestControllerTest`

Expected: PASS

## Task 2: Move ScreenshotManager To Background Listener Flow

**Files:**
- Modify: `app/src/main/java/com/muding/android/domain/usecase/ScreenshotManager.kt`
- Verify: `app/src/main/java/com/muding/android/service/FloatingBallService.kt`

- [ ] **Step 1: Replace polling with a background handler thread**

Add a dedicated `HandlerThread` and background `Handler`, then attach `ImageReader.setOnImageAvailableListener` to that handler when the capture pipeline is created.

- [ ] **Step 2: Route image arrival through the request controller**

Use the controller to decide whether to drop a first frame, ignore late frames, or consume the current frame.

- [ ] **Step 3: Move bitmap extraction to the background thread**

Perform `acquireLatestImage()`, `imageToBitmap()`, and image closing entirely on the background handler thread, and only resume the suspended coroutine after the bitmap is ready.

- [ ] **Step 4: Unify cleanup**

Ensure timeout, `release()`, and `MediaProjection.Callback.onStop()` all clear listener state, cancel pending timeouts, fail any pending capture, and safely tear down the background thread resources.

- [ ] **Step 5: Verify no service API changes are required**

Confirm `FloatingBallService` can keep using `captureScreen(...)` unchanged.

## Task 3: Verify The Whole Project

**Files:**
- Verify: `app/src/main/java/com/muding/android/domain/usecase/ScreenshotManager.kt`
- Verify: `app/src/test/java/com/muding/android/domain/usecase/ScreenshotCaptureRequestControllerTest.kt`
- Verify: `app/src/test/java/com/muding/android/service/ProjectionSessionTimeoutControllerTest.kt`

- [ ] **Step 1: Run focused tests**

Run: `./gradlew.bat testDebugUnitTest --tests com.muding.android.domain.usecase.ScreenshotCaptureRequestControllerTest`

Expected: PASS

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`

Expected: PASS

- [ ] **Step 3: Summarize residual risks**

Document that device-side validation is still recommended for screenshot timing and visual smoothness.
