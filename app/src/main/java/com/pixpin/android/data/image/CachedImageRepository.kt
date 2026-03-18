package com.pixpin.android.data.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.pixpin.android.domain.usecase.CacheImageStore

interface CachedImageRepository {
    fun writePngToCache(bitmap: Bitmap, subDir: String, prefix: String): Uri
}

class FileCachedImageRepository(
    context: Context
) : CachedImageRepository {

    private val cacheImageStore = CacheImageStore(context)

    override fun writePngToCache(bitmap: Bitmap, subDir: String, prefix: String): Uri {
        return cacheImageStore.writePngToCache(bitmap, subDir, prefix)
    }
}
