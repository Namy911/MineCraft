package com.example.minecraft.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.minecraft.BuildConfig
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.ui.main.DownloadAddon
import com.example.minecraft.ui.main.DownloadAddon.Companion.Progress
import com.example.minecraft.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.io.File


private const val TAG = "DownloadDialogUtil"

@AndroidEntryPoint
abstract class DownloadDialogUtil : Fragment(), NetworkUtil {
    companion object{
        val TAG_RESOURCE = "resource"
        val TAG_BEHAVIOR = "behavior"

        const val RECORD_REQUEST_CODE = 101
        const val packageName = "com.mojang.minecraftpe"
        const val TAG_WORK_MANAGER = "ui.util.tag.work.manager"
    }

    private val viewModel: MainViewModel by viewModels()

    var toast: Toast? = null
    var flagPermission = false

    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted -> flagPermission = isGranted }
    }
    // Config name of downloaded file
    fun getPackFileName(segment: String, tag: String): String {
        var term = ".mcpack"
        if (segment.endsWith(".mcaddon")) {
            term = ".mcaddon"
        }
        return if (tag == TAG_RESOURCE) {
            "$TAG_RESOURCE${segment.hashCode()}$term"
        }
        else {
            "$TAG_BEHAVIOR${segment.hashCode()}$term"
        }
    }
    //
    private fun checkInstallation(uri: String, tag: String, flagDir: String) {
        if (flagDir == DownloadAddon.DIR_CACHE) {
            if (isAppInstalled()) {
                val cacheLink =
                    requireActivity().externalCacheDir?.path + File.separator + getPackFileName(uri, tag)
                // install packs
                if (File(cacheLink).exists()) {
                    if (tag == TAG_BEHAVIOR) {
                        viewModel.setCachePathBehavior(cacheLink)
                    } else {
                        viewModel.setCachePathResource(cacheLink)
                    }
                } else {
                    Log.d(TAG, "checkInstallation: No file $cacheLink")
                }
                installAddon(File(cacheLink))
            } else {
                dialogDownloadApp()
            }
        }
    }
    @SuppressLint("ShowToast")
    fun workDownloadMultiple(fileNameList: List<String?>, model: AddonModel){

        val data: Data = Data.Builder()
            .putString(DownloadAddon.URI_DOWNLOAD, model.resource)
            .putString(DownloadAddon.FILE_NAME, fileNameList[0])
            .putString(DownloadAddon.DIRECTORY, DownloadAddon.DIR_CACHE)
            .build()

        val data2: Data = Data.Builder()
            .putString(DownloadAddon.URI_DOWNLOAD, model.behavior)
            .putString(DownloadAddon.FILE_NAME, fileNameList[1])
            .putString(DownloadAddon.DIRECTORY, DownloadAddon.DIR_CACHE)
            .build()

        val request = OneTimeWorkRequest.Builder(DownloadAddon::class.java)
            .setInputData(data)
            .build()

        val request2 = OneTimeWorkRequest.Builder(DownloadAddon::class.java)
            .setInputData(data2)
            .build()

        // Get result and observe response from manager

        val workManager = WorkManager.getInstance(requireContext())
        with(workManager) {
            when {
//                fileNameList[0] != null && fileNameList[1] != null -> { enqueue(mutableListOf(request, request2)) }
                fileNameList[0] != null && fileNameList[1] != null -> { enqueue(mutableListOf(request, request2)) }
                fileNameList[0] != null && fileNameList[1] == null -> { enqueue(request) }
                fileNameList[0] == null && fileNameList[1] != null -> { enqueue(request2) }
            }

            getWorkInfoByIdLiveData(request.id).observe(viewLifecycleOwner) { workInfo ->
//            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                val progress = workInfo.progress
                val value = progress.getInt(Progress, 0)
                val view = requireActivity().findViewById<ProgressBar>(R.id.progress_download)
                view.visibility = View.VISIBLE
                view.progress = value

                if (workInfo != null && workInfo.state.isFinished) {
                    view.visibility = View.GONE
                    downloadShareFiles(model)
                }
            }
        }
    }
    // WorkManager config, download file
    @SuppressLint("ShowToast")
    fun workDownloadSingle(
        uri: String, fileName: String, flagDir: String,
        model: AddonModel, flagBtnShare: Boolean = false
    ){
        val workManager = WorkManager.getInstance(requireContext())

        val data: Data = Data.Builder()
            .putString(DownloadAddon.URI_DOWNLOAD, uri)
            .putString(DownloadAddon.FILE_NAME, fileName)
            .putString(DownloadAddon.DIRECTORY, flagDir)
            .build()

        val request = OneTimeWorkRequest.Builder(DownloadAddon::class.java).addTag(TAG_WORK_MANAGER)
            .setInputData(data)
            .build()
        // Get result and observe response from manager
        with(workManager){
            enqueue(request)
            getWorkInfoByIdLiveData(request.id).observe(viewLifecycleOwner){ workInfo ->
                val progress = workInfo.progress
                val value = progress.getInt(Progress, 0)
                val view = requireActivity().findViewById<ProgressBar>(R.id.progress_download)
                view.visibility = View.VISIBLE
                view.progress = value

//            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                if (workInfo != null && workInfo.state.isFinished) {
                    view.visibility = View.GONE
                    /**
                     * Download addon type(resource or behavior)
                     * @param [flagBtnShare] click on btn share
                     * Dialog from button share can have  one link to download or two
                     */
                    if (flagBtnShare){
                        when {
                            model.resource.isNotEmpty() && model.behavior.isNotEmpty() -> { downloadShareFiles(model) }
                            model.resource.isNotEmpty() -> { downloadShareFile(model, TAG_RESOURCE) }
                            model.behavior.isNotEmpty() -> { downloadShareFile(model, TAG_BEHAVIOR) }
                        }
                    } else {
                        // button install
                        when (uri) {
                            model.resource -> { checkInstallation(uri, TAG_RESOURCE, flagDir) }
                            model.behavior -> { checkInstallation(uri, TAG_BEHAVIOR, flagDir) }
                        }
                    }
                    // check if needed toast message, from cache do not needed
                    if (flagDir == DownloadAddon.DIR_EXT_STORAGE){
                        toast = Toast.makeText(context, getString(R.string.msg_finish_download), Toast.LENGTH_SHORT)
                        toast?.show()
                    }
                }
            }
        }
    }
    // Install addon
    private fun installAddon(path: File) {
        val uri = try {
            FileProvider.getUriForFile(
                requireContext().applicationContext, BuildConfig.APPLICATION_ID + ".fileProvider", path
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
    // Permissions Setup
    fun checkPermission(): Boolean {
        onRequestCheckPermission()
        return flagPermission
    }
    // Check permission: WRITE_EXTERNAL_STORAGE
    private fun onRequestCheckPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                flagPermission = true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                dialogRequest()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    // Dialog Info about permission
    private fun dialogRequest(){
        val builder = AlertDialog.Builder(requireActivity())
        builder.apply {
            setMessage(getString(R.string.dialog_requestPerm_msg))
            setTitle(getString(R.string.dialog_requestPerm_title))
            setPositiveButton(getString(R.string.dialog_requestPerm_butt_OK)){ dialog, _ ->
                dialog.dismiss()
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
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

        val temp1 = viewModel.getCachePathBehavior()
        val temp2 = viewModel.getCachePathResource()
        // Check if link is not empty
        val resourceLink: String? = if (model.resource.isNotBlank()){ getPackFileName(model.resource, TAG_RESOURCE)} else { null }
        val behaviorLink: String? = if (model.behavior.isNotBlank()){ getPackFileName(model.behavior, TAG_BEHAVIOR) } else { null }
        // Button resource on dialog config
        if (behaviorLink != null) {
            val behavior = dialogView.findViewById<Button>(R.id.btn_behavior)
            val stubView = dialogView.findViewById<View>(R.id.view2)
            stubView.visibility = View.VISIBLE
            behavior.apply {
                visibility = View.VISIBLE
                if(flagDir == DownloadAddon.DIR_CACHE) { text = getString(R.string.btn_install_behavior) }

                setOnClickListener {
                    if (temp1 != null && flagDir == DownloadAddon.DIR_CACHE) {
                        checkInstallation(model.behavior, TAG_BEHAVIOR, flagDir)
                        dialog.dismiss()
                        return@setOnClickListener
                    }
                    if (checkInternetConnection(requireContext())) {
                        workDownloadSingle(model.behavior, behaviorLink, flagDir, model, false)
                    }else{
                        Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
            }
        } else {
            Log.d(TAG, "isFileResource Install: no file")
        }
        // Button behavior on dialog config
        if (resourceLink != null) {
            val resource = dialogView.findViewById<Button>(R.id.btn_pack)
            val stubView = dialogView.findViewById<View>(R.id.view)
            stubView.visibility = View.VISIBLE
            resource.apply {
                visibility = View.VISIBLE
                if(flagDir == DownloadAddon.DIR_CACHE) {
                    text = getString(R.string.btn_install_resource)
                }

                setOnClickListener {
                    if (temp2 != null && flagDir == DownloadAddon.DIR_CACHE) {
                        dialog.dismiss()
                        checkInstallation(model.resource, TAG_RESOURCE, flagDir)
                        return@setOnClickListener
                    }
                    if (checkInternetConnection(requireContext())) {
                        workDownloadSingle(model.resource, resourceLink, flagDir, model, false)
                    }else{
                        Toast.makeText(requireActivity(), getString(R.string.msg_no_internet), Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
            }
        } else {
            Log.d(TAG, "isFileBehavior Install: no file")
        }
    }
    // Download file and create chooser
    private fun downloadShareFile(modelType: AddonModel, tag: String){
        var item: Uri? = null

        val cacheLink = if (tag == TAG_RESOURCE){
            requireActivity().externalCacheDir?.path + File.separator + getPackFileName(modelType.resource, tag)
        } else{
            requireActivity().externalCacheDir?.path + File.separator + getPackFileName(modelType.behavior, tag)
        }

        item = FileProvider.getUriForFile(requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", File(cacheLink))

        if (item != null  && checkPermission()) {
            val sendIntent: Intent = Intent().apply {
                putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_share_addon))
                type = "file/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, item)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            requireActivity().startActivity(shareIntent)
        }
    }
    // Download files and create chooser
    private fun downloadShareFiles(model: AddonModel){

        val list = arrayListOf<Uri?>(null, null)
        val cacheResourceLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.resource, TAG_RESOURCE)
        val cacheBehaviorLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.behavior, TAG_BEHAVIOR)

        if (File(cacheBehaviorLink).exists()) {viewModel.setCachePathBehavior(cacheBehaviorLink) }
        if (File(cacheResourceLink).exists()) {viewModel.setCachePathResource(cacheResourceLink) }

        list[1] = FileProvider.getUriForFile(requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", File(cacheBehaviorLink))

        list[0] = FileProvider.getUriForFile(requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", File(cacheResourceLink))

        if (list[0] != null && list[1] != null && checkPermission()) {
            val sendIntent: Intent = Intent().apply {
                putExtra(Intent.EXTRA_TEXT, getString(R.string.msg_share_addon))
                type = "file/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            requireActivity().startActivity(shareIntent)
        }
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

    fun checkFilesExists(model: AddonModel){
        val cacheResourceLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.resource, TAG_RESOURCE)
        val cacheBehaviorLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.behavior, TAG_BEHAVIOR)

        if (File(cacheResourceLink).exists()) { viewModel.setCachePathResource(cacheResourceLink);  }
        if (File(cacheBehaviorLink).exists()) { viewModel.setCachePathBehavior(cacheBehaviorLink);  }
    }

    fun checkFileExist(model: String, tag: String){
        val cacheLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model, tag)

        if (tag == TAG_RESOURCE) {
            if (File(cacheLink).exists()) { viewModel.setCachePathResource(cacheLink) }
        }else {
            if (File(cacheLink).exists()) { viewModel.setCachePathBehavior(cacheLink) }
        }
    }

    fun checkFileExists(model: String , tag: String){
        val cacheLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model, tag)
        if (tag == TAG_BEHAVIOR) {
            if (File(cacheLink).exists()) { viewModel.setCachePathBehavior(cacheLink) }
        } else {
            if (File(cacheLink).exists()) { viewModel.setCachePathResource(cacheLink) }
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

    override fun onDetach() {
        super.onDetach()
        requestPermissionLauncher.unregister()
    }

    override fun onStop() {
        super.onStop()
        if (toast != null) toast = null
    }
}