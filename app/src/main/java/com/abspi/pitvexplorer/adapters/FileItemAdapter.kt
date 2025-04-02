package com.abspi.pitvexplorer.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.models.FileItem
import com.abspi.pitvexplorer.viewmodels.FileBrowserViewModel
import com.abspi.pitvexplorer.viewmodels.ViewMode
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class FileItemAdapter(
    private var fileItems: List<FileItem>,
    private val viewModel: FileBrowserViewModel,
    private val listener: FileItemClickListener
) : RecyclerView.Adapter<FileItemAdapter.FileViewHolder>() {

    interface FileItemClickListener {
        fun onFileItemClick(fileItem: FileItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileItem = fileItems[position]
        holder.bind(fileItem)
    }

    override fun getItemCount(): Int = fileItems.size

    fun updateData(newFileItems: List<FileItem>) {
        fileItems = newFileItems
        notifyDataSetChanged()
    }

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.imageViewFileIcon)
        private val nameView: TextView = itemView.findViewById(R.id.textViewFileName)
        private val infoView: TextView = itemView.findViewById(R.id.textViewFileInfo)

        private fun getThumbnailUrl(fileItem: FileItem): String {
            return viewModel.getThumbnailUrl(fileItem)
        }

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFileItemClick(fileItems[position])
                }
            }

            // Make item focusable for TV navigation
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true

            // Set focus change listener for visual feedback
            itemView.setOnFocusChangeListener { view, hasFocus ->
                view.isSelected = hasFocus
                // Add a subtle scale animation when focused
                view.animate()
                    .scaleX(if (hasFocus) 1.05f else 1.0f)
                    .scaleY(if (hasFocus) 1.05f else 1.0f)
                    .setDuration(150)
                    .start()
            }
        }

        fun bind(fileItem: FileItem) {
            // Use full_name for display if in videos-only mode, otherwise use name
            val isVideosMode = viewModel.viewMode.value == ViewMode.VIDEOS_ONLY
            val displayName = if (isVideosMode && fileItem.fullName.isNotEmpty()) fileItem.fullName else fileItem.name
            nameView.text = displayName

            // Format date as MM/dd/yy
            val dateFormat = SimpleDateFormat("M/d/yy", Locale.US)
            val formattedDate = dateFormat.format(fileItem.modifiedTime)

            // Set info text (size for files, item count for directories)
            infoView.text = if (fileItem.isDirectory) {
                "${fileItem.size} items | $formattedDate"
            } else {
                "${formatFileSize(fileItem.size)} | $formattedDate"
            }

            // Set icon based on file type
            val context = itemView.context
            if (fileItem.isDirectory) {
                iconView.setImageResource(R.drawable.ic_folder)
            } else if (isPicture(fileItem.name)) {
                // Load thumbnail for images with error handling
                val thumbnailUrl = getThumbnailUrl(fileItem)
                Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .timeout(3000) // 3 second timeout
                    .into(iconView)
            } else if (isVideo(fileItem.name)) {
                // Load thumbnail for videos with error handling
                val thumbnailUrl = getThumbnailUrl(fileItem)
                Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .timeout(3000) // 3 second timeout
                    .into(iconView)
            } else {
                iconView.setImageResource(R.drawable.ic_file)
            }
        }

        private fun formatFileSize(size: Int): String {
            val suffixes = arrayOf("B", "KB", "MB", "GB", "TB")
            var tempSize = size.toDouble()
            var suffixIndex = 0

            while (tempSize >= 1024 && suffixIndex < suffixes.size - 1) {
                tempSize /= 1024
                suffixIndex++
            }

            return String.format("%.1f %s", tempSize, suffixes[suffixIndex])
        }

        private fun isPicture(filename: String): Boolean {
            return viewModel.isPicture(filename)
        }

        private fun isVideo(filename: String): Boolean {
            return viewModel.isVideo(filename)
        }
    }
}