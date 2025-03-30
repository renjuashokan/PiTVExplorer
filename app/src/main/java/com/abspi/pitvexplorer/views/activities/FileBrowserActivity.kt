package com.abspi.pitvexplorer.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.adapters.FileItemAdapter
import com.abspi.pitvexplorer.models.FileItem
import com.abspi.pitvexplorer.models.ImageItem
import com.abspi.pitvexplorer.models.MediaItem
import com.abspi.pitvexplorer.utils.PreferenceManager
import com.abspi.pitvexplorer.viewmodels.FileBrowserViewModel
import com.abspi.pitvexplorer.viewmodels.FileBrowserViewModelFactory
import com.abspi.pitvexplorer.R
import java.util.ArrayList

class FileBrowserActivity : FragmentActivity() {

    private lateinit var viewModel: FileBrowserViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var pathBreadcrumb: TextView
    private lateinit var itemCountText: TextView
    private lateinit var loadingIndicator: View
    private lateinit var errorMessage: TextView
    private lateinit var fileAdapter: FileItemAdapter
    private lateinit var serverIp: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)

        // Get server IP from intent
        serverIp = intent.getStringExtra("SERVER_IP") ?: run {
            Toast.makeText(this, "Server IP not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewFiles)
        pathBreadcrumb = findViewById(R.id.textViewPath)
        itemCountText = findViewById(R.id.textViewItemCount)
        loadingIndicator = findViewById(R.id.progressLoading)
        errorMessage = findViewById(R.id.textViewError)

        // Set up ViewModel
        val factory = FileBrowserViewModelFactory(serverIp)
        viewModel = ViewModelProvider(this, factory)[FileBrowserViewModel::class.java]

        // Initialize adapter and RecyclerView
        setupRecyclerView()

        // Set up observers
        setupObservers()

        // Fetch files for initial directory
        viewModel.fetchFiles()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileItemAdapter(emptyList(),viewModel, object : FileItemAdapter.FileItemClickListener {
        override fun onFileItemClick(fileItem: FileItem) {
            if (fileItem.isDirectory) {
                // Use file.name instead of file.fullName for directory navigation
                viewModel.navigateToDirectory(fileItem.name)
            } else {
                if (viewModel.isVideo(fileItem.name)) {
                    openVideoPlayer(fileItem)
                } else if (viewModel.isPicture(fileItem.name)) {
                    openImageViewer(fileItem)
                }
            }
        }
    })

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@FileBrowserActivity, 4)
            adapter = fileAdapter
            setHasFixedSize(true)

            // Handle pagination with scroll listener
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadNextPageIfNeeded()
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        // Observe files list
        viewModel.files.observe(this) { files ->
            fileAdapter.updateData(files)
            itemCountText.text = "${files.size} items in total"
        }

        // Observe current path
        viewModel.currentPath.observe(this) { path ->
            val segments = viewModel.pathSegments.value ?: listOf()
            val breadcrumb = segments.joinToString(" / ")
            pathBreadcrumb.text = breadcrumb
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(this) { error ->
            if (error != null) {
                errorMessage.visibility = View.VISIBLE
                errorMessage.text = error
            } else {
                errorMessage.visibility = View.GONE
            }
        }
    }

    private fun openVideoPlayer(fileItem: FileItem) {
        // Get all video items from current directory
        val videoFiles = viewModel.files.value?.filter { viewModel.isVideo(it.fullName) } ?: listOf()
        val currentIndex = videoFiles.indexOf(fileItem)

        if (currentIndex >= 0) {
            // Create MediaItem list for the playlist
            val mediaItems = ArrayList<MediaItem>()
            videoFiles.forEach { file ->
                val path = getCurrentFullPath(file.fullName)
                mediaItems.add(MediaItem(name = file.fullName, path = path))
            }

            // Launch video player activity
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("SERVER_ADDRESS", serverIp)
                putExtra("PLAYLIST", mediaItems)
                putExtra("INITIAL_INDEX", currentIndex)
            }
            startActivity(intent)
        }
    }

    private fun getCurrentFullPath(fileName: String): String {
        val currentPath = viewModel.currentPath.value ?: "."
        return if (currentPath == "." || currentPath == "\$") {
            fileName
        } else {
            "$currentPath/$fileName"
        }
    }

    private fun openImageViewer(fileItem: FileItem) {
        // Get all image items from current directory
        val imageFiles = viewModel.files.value?.filter { viewModel.isPicture(it.fullName) } ?: listOf()
        val currentIndex = imageFiles.indexOf(fileItem)

        if (currentIndex >= 0) {
            // Create ImageItem list for the gallery
            val imageItems = ArrayList<ImageItem>()
            imageFiles.forEach { file ->
                val fullPath = viewModel.getFileFullPath(file.fullName)
                val normalizedPath = viewModel.normalizeServerPath(fullPath)
                val imageUrl = "http://$serverIp:8080/api/v1/file/${normalizedPath}"
                imageItems.add(ImageItem(name = file.fullName, path = imageUrl))
            }

            // Launch image viewer activity
            val intent = Intent(this, ImageViewerActivity::class.java).apply {
                putExtra("SERVER_ADDRESS", serverIp)
                putExtra("IMAGES", imageItems)
                putExtra("INITIAL_INDEX", currentIndex)
            }
            startActivity(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle back button to navigate up directory hierarchy
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.currentPath.value != "." && viewModel.currentPath.value != "\$") {
                viewModel.navigateUp()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}