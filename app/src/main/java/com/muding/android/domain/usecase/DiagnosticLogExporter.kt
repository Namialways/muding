package com.muding.android.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class DiagnosticLogExporter(
    private val context: Context
) {

    fun share(content: String) {
        val outputDir = File(context.cacheDir, DIAGNOSTICS_DIR).apply { mkdirs() }
        val outputFile = File(outputDir, DIAGNOSTIC_FILE_NAME).apply {
            writeText(content)
        }
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Muding diagnostic log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出诊断日志"))
    }

    companion object {
        private const val DIAGNOSTICS_DIR = "diagnostics"
        private const val DIAGNOSTIC_FILE_NAME = "muding-diagnostic-log.txt"
    }
}
