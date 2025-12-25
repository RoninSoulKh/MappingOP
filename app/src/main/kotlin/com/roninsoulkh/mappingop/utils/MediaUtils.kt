package com.roninsoulkh.mappingop.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun openMediaFile(context: Context, path: String) {
    try {
        val file = File(path)
        if (!file.exists()) return

        // Определяем тип файла по расширению
        val isVideo = path.endsWith(".mp4", ignoreCase = true)
        val mimeType = if (isVideo) "video/*" else "image/*"

        // Получаем безопасный URI через FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Создаем Intent для открытия в Галерее/Google Photos
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}