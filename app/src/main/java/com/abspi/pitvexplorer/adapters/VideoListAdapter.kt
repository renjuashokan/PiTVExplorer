package com.abspi.pitvexplorer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.models.MediaItem
import com.bumptech.glide.Glide

class VideoListAdapter(
    private var videos: List<MediaItem>,
    private val serverIp: String,
    private val currentIndex: Int,
    private val onVideoSelected: (Int) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnailView: ImageView = view.findViewById(R.id.videoThumbnail)
        val titleView: TextView = view.findViewById(R.id.videoTitle)
        val durationView: TextView = view.findViewById(R.id.videoDuration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_list_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]

        // Set title
        holder.titleView.text = video.name

        // Hide duration for now (we don't have this information yet)
        holder.durationView.visibility = View.GONE

        // Load thumbnail
        val thumbnailUrl = "http://$serverIp:8080/api/v1/thumbnail/${video.path}"
        Glide.with(holder.thumbnailView.context)
            .load(thumbnailUrl)
            .placeholder(R.drawable.video_placeholder)
            .error(R.drawable.video_placeholder)
            .into(holder.thumbnailView)

        // Highlight current video
        holder.itemView.isSelected = position == currentIndex

        // Set click listener
        holder.itemView.setOnClickListener {
            onVideoSelected(position)
        }

        // Set focus listener for TV navigation
        holder.itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus
        }

        // Request focus for the current video when list appears
        if (position == currentIndex) {
            holder.itemView.requestFocus()
        }
    }

    override fun getItemCount() = videos.size

    fun updateData(newVideos: List<MediaItem>, newCurrentIndex: Int) {
        videos = newVideos
        notifyDataSetChanged()
    }
}