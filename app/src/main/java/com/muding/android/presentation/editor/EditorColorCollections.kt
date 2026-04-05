package com.muding.android.presentation.editor

/**
 * Tracks editor color preferences across favorites and recents.
 *
 * Favorites are stored from oldest to newest. Recents are stored from newest to oldest.
 * Both exposed lists return normalized read-only snapshots for downstream UI and state code.
 */
class EditorColorCollections(
  favorites: List<Int> = emptyList(),
  recents: List<Int> = emptyList()
) {
  /** Favorites ordered oldest-to-newest and copied to a read-only snapshot. */
  val favorites: List<Int> = normalizeFavorites(favorites)
  /** Recents ordered newest-to-oldest and copied to a read-only snapshot. */
  val recents: List<Int> = normalizeRecents(recents)

  fun toggleFavorite(color: Int): EditorColorCollections {
    val updatedFavorites = if (favorites.contains(color)) {
      favorites.filter { it != color }
    } else {
      val appended = favorites + color
      if (appended.size <= MAX_FAVORITES) appended else appended.takeLast(MAX_FAVORITES)
    }

    return EditorColorCollections(updatedFavorites, recents)
  }

  fun recordRecent(color: Int): EditorColorCollections {
    val withoutDuplicates = recents.filter { it != color }
    val updatedRecents = listOf(color) + withoutDuplicates
    val limitedRecents = if (updatedRecents.size <= MAX_RECENTS) {
      updatedRecents
    } else {
      updatedRecents.take(MAX_RECENTS)
    }

    return EditorColorCollections(favorites, limitedRecents)
  }

  fun quickAccessColors(limit: Int = MAX_QUICK_ACCESS): List<Int> {
    val seen = LinkedHashSet<Int>()
    val colors = mutableListOf<Int>()

    for (favorite in favorites) {
      if (colors.size >= limit) break
      if (seen.add(favorite)) colors += favorite
    }

    for (recent in recents) {
      if (colors.size >= limit) break
      if (seen.add(recent)) colors += recent
    }

    return colors
  }

  companion object {
    private const val MAX_FAVORITES = 3
    private const val MAX_RECENTS = 3
    private const val MAX_QUICK_ACCESS = 3

    private fun normalizeFavorites(input: List<Int>): List<Int> {
      val seen = LinkedHashSet<Int>()
      val ordered = mutableListOf<Int>()

      for (color in input.asReversed()) {
        if (seen.add(color)) {
          ordered += color
          if (ordered.size == MAX_FAVORITES) break
        }
      }

      return ordered.asReversed().toList()
    }

    private fun normalizeRecents(input: List<Int>): List<Int> {
      val seen = LinkedHashSet<Int>()
      val ordered = mutableListOf<Int>()

      for (color in input) {
        if (seen.add(color)) {
          ordered += color
          if (ordered.size == MAX_RECENTS) break
        }
      }

      return ordered.toList()
    }
  }
}
