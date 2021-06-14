package com.example.minecraft.ui.main

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.minecraft.BuildConfig
import com.example.minecraft.ui.util.DownloadDialogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Provider


private const val TAG = "DownloadAddon"
class DownloadAddon(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
companion object{
    const val FILE_NAME = "addon.file.name"
    const val URI_DOWNLOAD = "uri.download"

    const val DIRECTORY = "ui.main.public"
    const val DIR_PUBLIC = "ui.main.public.directory"
    const val DIR_EXT_STORAGE = "ui.main.private.directory"
    const val DIR_CACHE = "ui.main.cache.directory"

    const val DOWNLOAD_FLAG_CACHE = "ui.main.download.flag.cache"
    const val DOWNLOAD_FLAG_PRIVATE= "ui.main.download.flag.private"

    const val FOLDER_DOWNLOAD_ADDONS = "MineCraftAddons"
}
    override fun doWork(): Result {
        return try {
            val uri = inputData.getString(URI_DOWNLOAD)
            val title = inputData.getString(FILE_NAME)
            val directory = inputData.getString(DIRECTORY)

            var outputId: Long = -2
//            makeDirAddons()
            // Check download directory
            if (uri != null && title != null) {
                when (directory) {
                    DIR_EXT_STORAGE -> {
//                           outputId = downloadFileDir(uri, title)
                           outputId = downloadPublicDir(uri, title)
//                        outputId = downloadCacheDir(uri, title)
                    }
//                    DIR_PUBLIC -> {
//                        outputId = downloadSdPublicDir(uri, title)
//                    }
                    DIR_CACHE -> {
                        outputId = downloadCacheDir(uri, title)
                    }
                }
            }

            val outputData = Data.Builder()
                .putLong(DOWNLOAD_FLAG_CACHE, outputId)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "DownloadAddon: Error Download Addon ${e.message}")
            Result.failure()
        }
    }
    // Private directory download
    private fun downloadFileDir(uri: String, fileName: String): Long {
        val path = context.getExternalFilesDir(
            Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        val uriPath = File(path, fileName).toUri()

        val request = downloadManagerBuilder(uri, fileName)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    // Public directory download
        @Suppress("DEPRECATION")
        private fun downloadPublicDir(uri: String, fileName: String): Long {
        val request = downloadManagerBuilder(uri, fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            request.setDestinationUri(file.toUri())
//            makeDirAddons()
//            val resolver = context.contentResolver
//            val contentValue = ContentValues().apply {
//                put(MediaStore.DownloadColumns.DISPLAY_NAME, "fileName")
//                put(MediaStore.DownloadColumns.MIME_TYPE, "application/octet-stream")
//                put(MediaStore.DownloadColumns.RELATIVE_PATH, "Download")
//                put(MediaStore.Downloads.IS_PENDING, 1)
//            }
//
//
//            val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValue)
//            fileUri?.let {
//                resolver.openOutputStream(fileUri)?.use { outputStream ->
//                    val encoded = Files.readAllBytes(Paths.get(file.toURI()))
//                    outputStream.write(encoded)
//                    contentValue.put(MediaStore.Video.Media.IS_PENDING, 0)
//                }
//            }

        }else{
            val file = File(Environment.DIRECTORY_DOWNLOADS + File.separator + fileName)
            val path = Environment.getExternalStoragePublicDirectory(file.absolutePath)
//            request.setDestinationInExternalPublicDir(path.absolutePath, fileName)
            request.setDestinationUri(path.toUri())
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    // Copy file from internal storage to Shared storage
    fun saveFilePublicDownload(file: File, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValue = ContentValues().apply {
                put(MediaStore.DownloadColumns.DISPLAY_NAME, name)
                put(MediaStore.DownloadColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.DownloadColumns.RELATIVE_PATH, "Download")
//                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValue
            )?.also { uri ->
                context.contentResolver.openOutputStream(uri).use { output ->
                    val encoded = Files.readAllBytes(Paths.get(file.toURI()))
                    output?.write(encoded)
//                    contentValue.put(MediaStore.Downloads.IS_PENDING, 0)
                }
            }
        }
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
            .setAllowedOverMetered(true)


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun makeDirAddons() {
        val f = File(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI.path,
            FOLDER_DOWNLOAD_ADDONS
        )
        if (!f.exists()) { f.mkdirs() }
    }
}