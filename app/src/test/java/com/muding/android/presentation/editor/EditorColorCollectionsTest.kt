package com.muding.android.presentation.editor

import org.junit.Assert.assertEquals
import org.junit.Test

private val red = 0xFFFF0000.toInt()
private val green = 0xFF00FF00.toInt()
private val blue = 0xFF0000FF.toInt()
private val yellow = 0xFFFFFF00.toInt()

class EditorColorCollectionsTest {
  @Test
  fun `favorites deduplicate input list`() {
    val collections = EditorColorCollections(
      favorites = listOf(red, red, green, green)
    )

    assertEquals(listOf(red, green), collections.favorites)
  }

  @Test
  fun `favorites capped at three colors`() {
    val collections = EditorColorCollections()
      .toggleFavorite(red)
      .toggleFavorite(green)
      .toggleFavorite(blue)
      .toggleFavorite(yellow)

    assertEquals(listOf(green, blue, yellow), collections.favorites)
  }

  @Test
  fun `toggle existing favorite removes color`() {
    val collections = EditorColorCollections(favorites = listOf(red, green))
    val updated = collections.toggleFavorite(green)

    assertEquals(listOf(red), updated.favorites)
    assertEquals(collections.recents, updated.recents)
  }

  @Test
  fun `recents deduplicate when recording repeated color`() {
    val initial = EditorColorCollections(recents = listOf(red, red, green))
    assertEquals(listOf(red, green), initial.recents)

    val updated = initial.recordRecent(green)
    assertEquals(listOf(green, red), updated.recents)
  }

  @Test
  fun `recents capped at three colors`() {
    val collections = EditorColorCollections()
      .recordRecent(red)
      .recordRecent(green)
      .recordRecent(blue)
      .recordRecent(yellow)

    assertEquals(listOf(yellow, blue, green), collections.recents)
  }

  @Test
  fun `quick access prefers favorites and backfills from recents`() {
    val collections = EditorColorCollections(
      favorites = listOf(red),
      recents = listOf(red, green, yellow)
    )

    assertEquals(listOf(red, green, yellow), collections.quickAccessColors(limit = 3))
  }

  @Test
  fun `constructor caps and deduplicates favorites`() {
    val collections = EditorColorCollections(
      favorites = listOf(red, red, green, blue, yellow)
    )

    assertEquals(listOf(green, blue, yellow), collections.favorites)
  }

  @Test
  fun `constructor caps and deduplicates recents`() {
    val collections = EditorColorCollections(
      recents = listOf(yellow, red, yellow, green, blue)
    )

    assertEquals(listOf(yellow, red, green), collections.recents)
  }
}
