package com.example.minecraft.ui.util

import android.Manifest
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.minecraft.BuildConfig
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.ui.main.DownloadAddon
import com.example.minecraft.ui.main.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


private const val TAG = "DownloadDialogUtil"

abstract class DownloadDialogUtil : Fragment(){
    companion object{
//        val FILE_NAME = "addon.file.name"
//        val URI_DOWNLOAD = "uri.download"
        val TAG_RESOURCE = "resource"
        val TAG_BEHAVIOR = "behavior"

        const val RECORD_REQUEST_CODE = 101
        const val packageName = "com.mojang.minecraftpe"
    }
    private val viewModel: MainViewModel by viewModels()
    // Config name of downloaded file
    fun getPackFileName(resource: String, tag: String): String {
        var term = ".mcpack"
        if (resource.endsWith(".mcaddon")) {
            term = ".mcaddon"
        }

        return if (tag == TAG_RESOURCE) {
            "$TAG_RESOURCE${resource.hashCode()}$term"
        } else {
            "$TAG_RESOURCE${resource.hashCode()}$term"
        }
    }
    //
    private fun checkInstallation(model: AddonModel, tag: String) {

            if (tag == DownloadAddon.DIR_CACHE) {
                if (isAppInstalled()) {
                    // Cache Dir
                    val cacheResourceLink =
                        requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.resource, TAG_RESOURCE)
                    val cacheBehaviorLink =
                        requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.behavior, TAG_BEHAVIOR)
                    // install packs
                    if (File(cacheResourceLink).exists()) {
                        viewModel.setCachePathResource(cacheResourceLink)
                        installAddon(File(cacheResourceLink), DownloadAddon.DIR_CACHE)
                    } else {
                        Log.d(TAG, "checkInstallation: No $cacheResourceLink")
                    }
                    if (File(cacheBehaviorLink).exists()) {
                        viewModel.setCachePathBehavior(cacheBehaviorLink)
                        installAddon(File(cacheBehaviorLink), DownloadAddon.DIR_CACHE)
                    } else {
                        Log.d(TAG, "checkInstallation: No $cacheBehaviorLink")
                    }
                } else {
                    dialogDownloadApp()
                }
            }
            // Private Dir
            if (tag == DownloadAddon.DIR_EXT_STORAGE) {
                val pathResourceLink = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + getPackFileName(model.resource, TAG_RESOURCE)
                val pathBehaviorLink = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + getPackFileName(model.behavior, TAG_BEHAVIOR)
                // install packs
                if (File(pathResourceLink).exists()) {
                    viewModel.setPrivatePathResource(pathResourceLink)
                    CoroutineScope(Dispatchers.IO).launch {
                        val name = getPackFileName(model.resource, TAG_RESOURCE)
                        saveFilePublicDownload(File(pathResourceLink), name)
                    }
                } else {
                    Log.d(TAG, "checkInternetConnection: No $pathResourceLink")
                }
                if (File(pathBehaviorLink).exists()) {
                    viewModel.setPrivatePathBehavior(pathBehaviorLink)
                    CoroutineScope(Dispatchers.IO).launch {
                        val name = getPackFileName(model.behavior, TAG_BEHAVIOR)
                        saveFilePublicDownload(File(pathBehaviorLink), name)
                    }
                } else {
                    Log.d(TAG, "checkInternetConnection: No $pathBehaviorLink")
                }
            }
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

            requireActivity().contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValue
            )?.also { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { output ->
                    val encoded = Files.readAllBytes(Paths.get(file.toURI()))
                    output?.write(encoded)
//                    contentValue.put(MediaStore.Downloads.IS_PENDING, 0)
                }
            }
        }
    }
    // WorkManager config, download file
    private fun workDownloadAddon(uri: String, fileName: String, flagDir: String, model: AddonModel){
        val workManager = WorkManager.getInstance(requireContext())

        val data: Data = Data.Builder()
            .putString(DownloadAddon.URI_DOWNLOAD, uri)
            .putString(DownloadAddon.FILE_NAME, fileName)
            .putString(DownloadAddon.DIRECTORY, flagDir)
            .build()

        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequest.Builder(DownloadAddon::class.java)
            .setConstraints(constrains)
            .setInputData(data)
            .build()
        // Get result and observe response from manager
        workManager.enqueue(request)
        workManager.getWorkInfoByIdLiveData(request.id).observe(viewLifecycleOwner){
            if (it.state.isFinished){
                // Download addon type(resource or behavior)
                val result = it.outputData
                val id = result.getLong(DownloadAddon.DOWNLOAD_FLAG_CACHE, -3)
                receiverComplete(id, model, flagDir)
            }
        }
    }

    private fun installAddon(path: File, flagDir: String) {
        val uri = try {
                FileProvider.getUriForFile(
                    requireContext().applicationContext,
                    BuildConfig.APPLICATION_ID + ".fileProvider",
                    path
                )
            } catch (e: IllegalArgumentException) {
                Log.d("File Selector", "The selected file not funded: $path")
                null
            }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addCategory(Intent.CATEGORY_DEFAULT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        requireActivity().startActivity(intent)
    }
    // Internet connection
    private fun checkInternetConnection(): Boolean{
        val connectivityManager = requireActivity().applicationContext
            .getSystemService(ConnectivityManager::class.java)
        val currentNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    }
    // Permissions Setup
    fun checkPermission(): Boolean{
        setupPermissions()
        return if(ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permission: WRITE_EXTERNAL_STORAGE")
            false
        } else {
            true
        }
    }
    // Check permission: WRITE_EXTERNAL_STORAGE
    private fun setupPermissions() {
        val permWrite = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permWrite != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                dialogRequest()
            } else {
                makeRequestWriteStorage()
            }
        }
    }
    private fun makeRequestWriteStorage() {
        ActivityCompat.requestPermissions(requireActivity(),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            RECORD_REQUEST_CODE)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    dialogRequest()
                }
            }
        }
    }
    // Dialog Info about permission
    private fun dialogRequest(){
        val builder = AlertDialog.Builder(requireActivity())
        builder.apply {
            setMessage(getString(R.string.dialog_requestPerm_msg))
            setTitle(getString(R.string.dialog_requestPerm_title))
            setPositiveButton(getString(R.string.dialog_requestPerm_butt_OK)){ _, _ -> makeRequestWriteStorage()}
            create()
        }
        builder.show()
    }
    // Dialog download Addon
    fun dialogDownload(model: AddonModel, flagDir: String ){
        val dialogView = layoutInflater.inflate(R.layout.item_dialog, null)
        val builder = AlertDialog.Builder(requireActivity()).apply { setView(dialogView) }
        val dialog = builder.create()
        dialog.show()

        val closeView = dialogView.findViewById<ImageView>(R.id.img_close)
        closeView.setOnClickListener { dialog.cancel() }
        // Check if link is not empty
        val resourceLink: String? = if (model.resource.isNotBlank()){ getPackFileName(model.resource, TAG_RESOURCE)} else { null }
        val behaviorLink: String? = if (model.behavior.isNotBlank()){ getPackFileName(model.behavior, TAG_BEHAVIOR) } else { null }
        // Button resource on dialog config
        if (resourceLink != null) {
            val behavior = dialogView.findViewById<Button>(R.id.btn_behavior)
            behavior.apply {
                setOnClickListener {
                    if (checkInternetConnection()) {
                        workDownloadAddon(model.resource, resourceLink, flagDir, model)
                    }else{
                        Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                    }
                }
                visibility = View.VISIBLE
                if(flagDir == DownloadAddon.DIR_CACHE) {
                    text = getString(R.string.btn_install_behavior)
                }
            }
        } else {
            Log.d(TAG, "isFileResource Install: no file")
        }
        // Button behavior on dialog config
        if (behaviorLink != null ) {
            val resource = dialogView.findViewById<Button>(R.id.btn_pack)
            resource.apply {
                setOnClickListener {
                    if (checkInternetConnection()) {
                        workDownloadAddon(model.behavior, behaviorLink, flagDir, model)
                    }else{
                        Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                    }
                }
                visibility = View.VISIBLE
                if(flagDir == DownloadAddon.DIR_CACHE) {
                    text = getString(R.string.btn_install_resource)
                }
            }
        } else {
            Log.d(TAG, "isFileBehavior Install: no file")
        }
    }
    // Receiver, check download completed by id and install addon
    private fun receiverComplete(idEnqueue: Long, model:AddonModel, flagDir: String){
        val receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == idEnqueue){
                    checkInstallation(model, flagDir)
                    Toast.makeText(context, getString(R.string.msg_finish_download), Toast.LENGTH_SHORT).show()
                }
            }
        }
        requireActivity().registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE) )
    }
    // Check if app is installed, otherwise go to install
    private fun dialogDownloadApp() {
        AlertDialog.Builder(requireActivity()).apply {
            setTitle(getString(R.string.title_no_found_app))
            setMessage(getString(R.string.msg_go_download_app))
            setPositiveButton("OK") { _, _ -> }
            show()
        }
    }

    private fun isAppInstalled(): Boolean {
        return try {
            val packageManager = requireActivity().packageManager
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    inline fun <T> sdk29(sdk29: () -> T): T? {
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sdk29()
        } else return null
    }
}