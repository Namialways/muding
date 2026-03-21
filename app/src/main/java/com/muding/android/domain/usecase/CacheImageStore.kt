package com.muding.android.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 统一的缓存图片写入器：将 Bitmap 写入 app cacheDir，并返回可共享的 content Uri。
 *
 * 目的：避免在 Intent/Service 间直接传 Bitmap（Binder 限制），以及集中管理缓存目录。
 */
class CacheImageStore(private val context: Context) {

    fun writePngToCache(bitmap: Bitmap, subDir: String, prefix: String): Uri {
        val dir = File(context.cacheDir, subDir)
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

