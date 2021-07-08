package com.example.minecraft.ui.main

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.File
import java.io.IOException

private const val TAG = "DownloadAddon"
class DownloadAddon(val context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {
    companion object {
        const val FILE_NAME = "addon.file.name"
        const val URI_DOWNLOAD = "uri.download"

        const val DIRECTORY = "ui.main.public"
        const val DIR_PUBLIC = "ui.main.public.directory"
        const val DIR_EXT_STORAGE = "ui.main.public.external.directory"
        const val DIR_CACHE = "ui.main.cache.directory"

        const val DOWNLOAD_FLAG = "ui.main.download.flag.cache"
        const val DOWNLOAD_FLAG_PRIVATE = "ui.main.download.flag.private"

        const val FOLDER_DOWNLOAD_ADDONS = "MineCraftAddons"
    }

    override suspend fun doWork(): Result {
        return try {
            val uri = inputData.getString(URI_DOWNLOAD)
            val title = inputData.getString(FILE_NAME)
            val directory = inputData.getString(DIRECTORY)

            var outputId: Long = -2
            // Check download directory
            if (uri != null && title != null) {
                when (directory) {
                    DIR_EXT_STORAGE -> { outputId = downloadPublicDir(uri, title) }
                    DIR_CACHE -> { outputId = downloadCacheDir(uri, title) }
                }
            }

            val outputData = Data.Builder()
                .putLong(DOWNLOAD_FLAG, outputId)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "DownloadAddon: Error Download Addon ${e.message}")
            Result.failure()
        }
    }
    // Public directory download
    @Suppress("DEPRECATION")
    private suspend fun downloadPublicDir(uri: String, fileName: String): Long {
        val request = downloadManagerBuilder(uri, fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadPublicMediaDir(uri, fileName)
        } else {
            val file = File(Environment.DIRECTORY_DOWNLOADS + File.separator + fileName)
            val path = Environment.getExternalStoragePublicDirectory(file.absolutePath)
            request.setDestinationUri(path.toUri())
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    // Cache directory download
    private fun downloadCacheDir(uri: String, fileName: String): Long {
        val path = File(context.externalCacheDir, fileName).toUri()
        val request = downloadManagerBuilder(uri, fileName)
            .setDestinationUri(path)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    private fun downloadManagerBuilder(uri: String, file_name: String) =
        DownloadManager.Request(Uri.parse(uri))
            .setTitle(file_name)
            .setDescription("Download $file_name")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun makeDirAddons() {
        val f = File(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY).path,
            FOLDER_DOWNLOAD_ADDONS
        )
        if (!f.exists()) { f.mkdirs() }
    }
    // Download in media/download with okhttp
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadPublicMediaDir(fileUri: String, fileName: String) {
        withContext(Dispatchers.IO) {
//            makeDirAddons()
            val request = Request.Builder()
                .url(fileUri)
                .build()
            val client = OkHttpClient.Builder().build()
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful) {
                            val values = ContentValues().apply {
                                put(MediaStore.DownloadColumns.TITLE, fileName)
                                put(MediaStore.DownloadColumns.RELATIVE_PATH, "Download")
                                put(MediaStore.DownloadColumns.MIME_TYPE, "application/octet-stream")
                                put(MediaStore.DownloadColumns.IS_PENDING, 1)
                            }

                            val resolver = context.contentResolver
                            val fileByte = response.body?.byteStream()

                            val insert = resolver.insert(
                                collection,
                                values
                            )
                            insert?.let { uri ->
                                resolver.openOutputStream(uri, "w").use { output ->
                                    val encoded = fileByte?.readBytes()
                                    output?.write(encoded)
                                }
                                values.clear()
                                values.put(MediaStore.DownloadColumns.IS_PENDING, 0)
                                values.put(MediaStore.DownloadColumns.DISPLAY_NAME, fileName)

                                try {
                                    resolver.update(insert, values, null, null)
                                }catch (e: Exception){
                                    Log.d(TAG, "onResponse: resolver.update ${e.message}")
                                }
                            }
                        }
                    }
                }
            })
        }
    }
    
    private fun scanMedia(uri: String) {
        Log.d(TAG, "scanMedia: $uri")
        MediaScannerConnection
            .scanFile(context, arrayOf(uri), null, null)
    }
}