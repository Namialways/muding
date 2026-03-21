package com.muding.android.domain.usecase

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ClosedPinRecord(
    val imageUri: String,
    val annotationSessionId: String?
)

object RecentPinStore {
    private const val PREFS_NAME = "muding_recent_pins"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 20

    fun push(context: Context, record: ClosedPinRecord) {
        val items = load(context).toMutableList()
        items.removeAll { it.imageUri == record.imageUri && it.annotationSessionId == record.annotationSessionId }
        items.add(0, record)
        save(context, items.take(MAX_ITEMS))
    }

    fun popMostRecent(context: Context): ClosedPinRecord? {
        val items = load(context).toMutableList()
        val first = items.firstOrNull() ?: return null
        items.removeAt(0)
        save(context, items)
        return first
    }

    fun hasRecent(context: Context): Boolean = load(context).isNotEmpty()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ITEMS)
            .apply()
    }

    fun count(context: Context): Int = load(context).size

    private fun load(context: Context): List<ClosedPinRecord> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    ClosedPinRecord(
                        imageUri = item.getString("imageUri"),
                        annotationSessionId = item.optString("annotationSessionId").ifBlank { null }
                    )
                )
            }
        }
    }

    private fun save(context: Context, items: List<ClosedPinRecord>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("imageUri", item.imageUri)
                    put("annotationSessionId", item.annotationSessionId ?: "")
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
