package com.example.minecraft.ui.main

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

private const val TAG = "DownloadAddon"
class DownloadAddon(val context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
companion object{
    const val FILE_NAME = "addon.file.name"
    const val URI_DOWNLOAD = "uri.download"

    const val DIRECTORY = "ui.main.public"
    const val DIR_PUBLIC = "ui.main.public.directory"
    const val DIR_PRIVATE = "ui.main.private.directory"
    const val DIR_CACHE = "ui.main.cache.directory"

    const val DOWNLOAD_FLAG ="ui.main.download.flag.cache"
    const val DOWNLOAD_FLAG_PRIVATE="ui.main.download.flag.private"
}
    override fun doWork(): Result {
        return try {
            val uri = inputData.getString(URI_DOWNLOAD)
            val title = inputData.getString(FILE_NAME)
            val directory = inputData.getString(DIRECTORY)

            var outputId: Long = -2
            // Check download directory
            if (uri != null && title != null) {
                when (directory) {
                    DIR_PRIVATE -> {
                           outputId = downloadFileDir(uri, title)
                    }
//                    DIR_PUBLIC -> { downloadPublicDir(uri, title) }
                    DIR_CACHE -> {
                        outputId = downloadCacheDir(uri, title)
                    }
                }
            }

            val outputData = Data.Builder()
                .putLong(DOWNLOAD_FLAG, outputId)
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            Log.d(TAG, "DownloadAddon: Error Download Addon")
            Result.failure()
        }
    }
    // Private directory download
    private fun downloadFileDir(uri: String, fileName: String): Long {
        val request = downloadManagerBuilder(uri, fileName)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    // Public directory download
        private fun downloadPublicDir(uri: String, fileName: String): Long {
        val request = downloadManagerBuilder(uri, fileName)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                fileName)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    // Cache directory download
    private fun downloadCacheDir(uri: String, fileName: String): Long {
        val path = File(context.externalCacheDir, fileName)
        val request = downloadManagerBuilder(uri, fileName)
            .setDestinationUri(path.toUri())

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    private fun downloadManagerBuilder(uri: String, file_name: String) =
        DownloadManager.Request(Uri.parse(uri))
            .setTitle(file_name)
            .setDescription("Download $file_name")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)

//    private fun receiverComplete(idEnqueue: Long){
//        val receiver = object : BroadcastReceiver(){
//            override fun onReceive(context: Context?, intent: Intent?) {
//                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
//                if (id == idEnqueue){
//                    Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
//                } else {
//
//                }
//            }
//
//        }
//        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE) )
//    }

}