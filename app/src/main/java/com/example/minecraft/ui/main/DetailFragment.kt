package com.example.minecraft.ui.main

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.example.minecraft.BuildConfig
import com.example.minecraft.MainActivity
import com.example.minecraft.R
import com.example.minecraft.data.model.AddonModel
import com.example.minecraft.databinding.FragmentDetailBinding
import com.example.minecraft.ui.util.DownloadDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class DetailFragment : DownloadDialogUtil() {
    companion object{
        const val TAG = "DetailFragment"
    }

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val args: DetailFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        requireActivity().actionBar?.setDisplayShowTitleEnabled(false)
        requireActivity().actionBar?.setDisplayShowHomeEnabled(false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolBartTitle(getString(R.string.title_fragment_details))
        checkFileExists(args.model)

        binding.apply {
            val list: List<String> = args.model.preview
            imgContainer.adapter = DetailPageAdapter(list, args.title, requireActivity())
            pageIndicator.apply {
                count = list.size
                selection = 0
            }
            txtDesc.text = args.model.description

            btnShare.setOnClickListener { shareFile() }

            btnDownload.setOnClickListener {
                if (checkPermission()) {
                    dialogDownload(args.model, DownloadAddon.DIR_EXT_STORAGE)
                }
            }
            btnInstall.setOnClickListener {
                if (checkPermission()){ dialogDownload(args.model, DownloadAddon.DIR_CACHE) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupToolBartTitle(title: String){ (activity as MainActivity?)!!.setupToolBartTitle(title) }
    // Initialisation, Check file exist from share file
    private fun checkFileExists(model: AddonModel){
        val cacheResourceLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.resource, TAG_RESOURCE)
        val cacheBehaviorLink = requireActivity().externalCacheDir?.path + File.separator + getPackFileName(model.behavior, TAG_BEHAVIOR)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
            val projection = arrayOf(
                MediaStore.DownloadColumns._ID,
                MediaStore.DownloadColumns.DISPLAY_NAME,
                MediaStore.DownloadColumns.RELATIVE_PATH)
            val selection = "${MediaStore.DownloadColumns.DISPLAY_NAME} like ?"
            val selectionArgsBehavior = arrayOf(getPackFileName(model.behavior, TAG_BEHAVIOR))
            val selectionArgsResource = arrayOf(getPackFileName(model.resource, TAG_RESOURCE))

            requireActivity().applicationContext.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgsBehavior,
                null
            )?.use { cursor ->
                val columnId = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID)
                val columnName = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME)
                val columnUri = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(columnName)
                    val id = cursor.getLong(columnId)
                    val uri = ContentUris.withAppendedId( MediaStore.Downloads.EXTERNAL_CONTENT_URI, columnId.toLong())
//                    if (name ==  getPackFileName(model.behavior, TAG_BEHAVIOR)){
                    viewModel.setPrivatePathBehavior(uri.toString())
                        Log.d(TAG, "While : ${name} ${uri}")
//                    }
                }
            }
            requireActivity().applicationContext.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgsResource,
                null
            )?.use { cursor ->
                val columnId = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID)
                val columnName = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.DISPLAY_NAME)
                val columnUri = cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(columnName)
                    val id = cursor.getLong(columnId)
                    val uri = ContentUris.withAppendedId( MediaStore.Downloads.EXTERNAL_CONTENT_URI, columnId.toLong())
//                    if (name ==  getPackFileName(model.behavior, TAG_BEHAVIOR)){
                    viewModel.setPrivatePathResource(uri.toString())
                        Log.d(TAG, "While : ${name} ${uri}")
//                    }
                }
            }
        }
        if (File(cacheResourceLink).exists()) { viewModel.setCachePathResource(cacheResourceLink) }
        if (File(cacheBehaviorLink).exists()) { viewModel.setCachePathBehavior(cacheBehaviorLink) }
    }
    // Button share logic
    private fun shareFile() {
        val temp1 = viewModel.getCachePathBehavior()
        val temp2 = viewModel.getCachePathResource()
        val temp3 = viewModel.getPrivatePathBehavior()
        val temp4 = viewModel.getPrivatePathResource()

//        Log.d(TAG, "shareFile temp: $temp1, $temp2, $temp3, $temp4, ")

        val sendIntent: Intent = Intent().apply {
            putExtra(Intent.EXTRA_TEXT, "Share Addon")
            type = "file/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val list = arrayListOf<Uri?>(null, null)
        // Behavior cell config
        when {
            temp1 != null -> { list[1] = getPath(File(temp1)) }
            temp3 != null -> { list[1] = getPath(File(temp3)) }
//            temp5 != null -> { list[1] = getPath(File(temp5)) }
            else -> { Log.d(TAG, "shareFile: No behavior file") }
        }
        // Resource cell config
        when {
            temp2 != null -> { list[0] = getPath(File(temp2)) }
            temp4 != null -> { list[0] = getPath(File(temp4)) }
//            temp6 != null -> { list[0] = getPath(File(temp6)) }
            else -> { Log.d(TAG, "shareFile: No resource file")}
        }
        Log.d(TAG, "shareFile: list ${list[0]} - ${list[1]}")
        // Intent share config, multiple or single
        if (list[0] != null && list[1] != null) {
            sendIntent.action = Intent.ACTION_SEND_MULTIPLE
            sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list) }
        else if (list[0] != null){
            sendIntent.putExtra(Intent.EXTRA_STREAM, list[0])
            sendIntent.action = Intent.ACTION_SEND
        }
        else if (list[1] != null){
            sendIntent.putExtra(Intent.EXTRA_STREAM, list[1])
            sendIntent.action = Intent.ACTION_SEND
        } else { Toast.makeText(requireActivity(), "Download Addon", Toast.LENGTH_SHORT).show()}

        val shareIntent = Intent.createChooser(sendIntent, "null")
        startActivity(shareIntent)
    }

    private fun getPath(file: File) = try {
        FileProvider.getUriForFile(
            requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".fileProvider", file)
    } catch (e: IllegalArgumentException) {
        Log.d("File Selector", "The selected file not funded: $file")
        null
    }

    inner class DetailPageAdapter(private val listOfLinks: List<String>, val title: String, private val ctx: Context) : PagerAdapter(){
        override fun getCount(): Int {
            return listOfLinks.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(ctx)
            val view = inflater.inflate(R.layout.item_pager, container, false)
            val image = view.findViewById<ImageView>(R.id.img)
            val txtTitle = view.findViewById<TextView>(R.id.txt_title)

            txtTitle.text = title
            Glide.with(requireActivity()).load(listOfLinks[position]).centerCrop().into(image)
            container.addView(view)

            return  view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

    }
}