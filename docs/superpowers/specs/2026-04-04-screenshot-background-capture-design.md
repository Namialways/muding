# Screenshot Background Capture Design

Date: 2026-04-04
Project: androidprintScreen
Scope: `ScreenshotManager` capture pipeline and screenshot request scheduling

## Goal

Move screen-frame waiting and bitmap extraction off the main thread so screenshot capture produces less UI hitching while preserving the current floating-ball, crop, OCR, and idle-timeout behavior.

## Current Problem

The current implementation polls `ImageReader.acquireLatestImage()` from a main-thread `Handler`, then performs full-screen `Bitmap.createBitmap()` and `copyPixelsFromBuffer()` work on the same path. This makes screenshot capture more likely to stall UI work, especially on high-resolution devices.

## Chosen Direction

Use a dedicated background `HandlerThread` plus `ImageReader.OnImageAvailableListener` for capture delivery.

The new pipeline will:

- create a dedicated background thread owned by `ScreenshotManager`
- register `ImageReader.setOnImageAvailableListener(..., backgroundHandler)`
- replace main-thread polling with a single pending-capture request model
- perform `acquireLatestImage()`, optional first-frame drop, and `imageToBitmap()` on the background thread
- resume the suspended capture coroutine once the bitmap is ready

## Non-goals

- No UI or floating-ball interaction redesign
- No change to the 2-minute MediaProjection idle-timeout behavior
- No bitmap pool or aggressive memory reuse work in this phase
- No change to OCR or crop behavior after a bitmap is delivered

## State Model

Only one pending screenshot request may exist at a time.

Each request tracks:

- whether the first frame should be dropped
- whether it has already completed or failed
- a timeout callback

This keeps the Android-facing code small and ensures timeouts, cancellation, and release all converge on one cleanup path.

## Error Handling

The new pipeline must:

- fail fast when capture is requested without an active projection
- fail the pending request on timeout
- fail the pending request when `release()` or `MediaProjection.Callback.onStop()` runs
- prevent double completion when timeout, cancellation, and image arrival race each other

## Testing

Unit coverage should focus on the new pure request controller:

- starting a request schedules a timeout
- starting a second request while one is pending is rejected
- first-frame drop requires a second frame before completion
- timeout cancels the request
- release clears pending work and prevents late completion

Project verification remains `./gradlew.bat testDebugUnitTest`.
