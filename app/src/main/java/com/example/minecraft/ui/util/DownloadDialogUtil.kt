package com.example.minecraft.ui.util

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.work.*
import com.example.minecraft.BuildConfig
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.ui.main.DownloadAddon
import com.example.minecraft.ui.main.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File


private const val TAG = "DownloadDialogUtil"

abstract class DownloadDialogUtil : Fragment(){
    companion object{
//        val FILE_NAME = "addon.file.name"
//        val URI_DOWNLOAD = "uri.download"
//        private val URI_RESOURCE = "resource"
//        private val URI_BEHAVIOR = "behavior"

        const val RECORD_REQUEST_CODE = 101
        const val packageName = "com.mojang.minecraftpe"
    }
    private val viewModel: MainViewModel by viewModels()
    // Config name of downloaded file
    fun getPackFileName(resource: String): String {
        var term = ".mcpack"
        if (resource.endsWith(".mcaddon")) {
            term = ".mcaddon"
        }
        return "A" + resource.hashCode() + term
    }
    //
    private fun checkInstallation(model: AddonModel, tag: String) {
        if (isAppInstalled()) {
            // Cache Dir
            if (tag == DownloadAddon.DIR_CACHE) {
                val cacheResourceLink =
                    requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.resource)
                val cacheBehaviorLink =
                    requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.behavior)
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
            }
            // Private Dir
            if (tag == DownloadAddon.DIR_PRIVATE) {
                val pathResourceLink = requireActivity().applicationContext
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + getPackFileName(model.resource)
                val pathBehaviorLink = requireActivity().applicationContext
                    .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + File.separator + getPackFileName(model.behavior)
                // install packs
                if (File(pathResourceLink).exists()) {
                    viewModel.setPrivatePathResource(pathResourceLink)
                    installAddon(File(pathResourceLink), DownloadAddon.DIR_PRIVATE)
                } else {
                    Log.d(TAG, "checkInternetConnection: No $pathResourceLink")
                }
                if (File(pathBehaviorLink).exists()) {
                    viewModel.setPrivatePathBehavior(pathBehaviorLink)
                    installAddon(File(pathBehaviorLink), DownloadAddon.DIR_PRIVATE)
                } else {
                    Log.d(TAG, "checkInternetConnection: No $pathBehaviorLink")
                }
            }
        } else {
            dialogDownloadApp()
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
                val id = result.getLong(DownloadAddon.DOWNLOAD_FLAG, -3)
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
        val premWrite = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (premWrite != PackageManager.PERMISSION_GRANTED) {
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
    fun dialogInstall(model: AddonModel, flagDir: String ){
        val dialogView = layoutInflater.inflate(R.layout.item_dialog, null)
        val builder = AlertDialog.Builder(requireActivity()).apply {
            setView(dialogView)
        }
        val dialog = builder.create()
        dialog.show()

        val closeView = dialogView.findViewById<ImageView>(R.id.img_close)
        closeView.setOnClickListener {
            dialog.cancel()
        }
        // Check if link is not empty
        val resourceLink: String? = if (model.resource.isNotBlank()){ getPackFileName(model.resource)} else { null }
        val behaviorLink: String? = if (model.behavior.isNotBlank()){ getPackFileName(model.behavior) } else { null }

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
                text = getString(R.string.btn_install_pack)
            }
        } else {
            Log.d(TAG, "isFileResource Install: no file")
        }

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
                text = getString(R.string.btn_install_behavior)
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
}