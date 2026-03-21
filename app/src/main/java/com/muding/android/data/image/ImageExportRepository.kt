package com.muding.android.data.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.muding.android.domain.usecase.ImageSaver

interface ImageExportRepository {
    suspend fun saveToGallery(bitmap: Bitmap): Uri?
    suspend fun shareImage(bitmap: Bitmap)
}

class SystemImageExportRepository(
    context: Context
) : ImageExportRepository {

    private val imageSaver = ImageSaver(context)

    override suspend fun saveToGallery(bitmap: Bitmap): Uri? {
        return imageSaver.saveToGallery(bitmap)
    }

    override suspend fun shareImage(bitmap: Bitmap) {
        imageSaver.shareImage(bitmap)
    }
}
