package com.pixpin.android.data.repository

import android.content.Context
import com.pixpin.android.domain.usecase.ClosedPinRecord
import com.pixpin.android.domain.usecase.RecentPinStore

interface RecentPinRepository {
    fun push(record: ClosedPinRecord)
    fun popMostRecent(): ClosedPinRecord?
    fun hasRecent(): Boolean
    fun count(): Int
    fun clear()
}

class SharedPreferencesRecentPinRepository(
    private val context: Context
) : RecentPinRepository {

    override fun push(record: ClosedPinRecord) {
        RecentPinStore.push(context, record)
    }

    override fun popMostRecent(): ClosedPinRecord? {
        return RecentPinStore.popMostRecent(context)
    }

    override fun hasRecent(): Boolean = RecentPinStore.hasRecent(context)

    override fun count(): Int = RecentPinStore.count(context)

    override fun clear() {
        RecentPinStore.clear(context)
    }
}
