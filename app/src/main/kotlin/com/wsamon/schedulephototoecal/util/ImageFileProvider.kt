package com.wsamon.schedulephototoecal.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ImageFileProvider {

    fun createCaptureUri(context: Context): Uri {
        val directory = File(context.cacheDir, "captured_photos").apply { mkdirs() }
        val file = File(directory, "schedule_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
