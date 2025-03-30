package com.abspi.pitvexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.models.ImageItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.File

class ImageTileAdapter(
    private val context: Context,
    private val imageList: ArrayList<ImageItem>,
    private val listener: ImageTileClickListener
) : RecyclerView.Adapter<ImageTileAdapter.TileViewHolder>() {

    interface ImageTileClickListener {
        fun onImageTileClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val imageItem = imageList[position]

        // Show loading indicator
        holder.progressBar.visibility = View.VISIBLE

        // Get the file name without path
        val fileName = File(imageItem.name).name
        holder.textView.text = if (fileName.length > 15) {
            fileName.take(12) + "..."
        } else {
            fileName
        }

        // Load thumbnail using Glide
        Glide.with(context)
            .load(imageItem.path)
            .apply(
                RequestOptions()
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_image)
            )
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(holder.imageView)

        // Set click listener
        holder.itemView.setOnClickListener {
            listener.onImageTileClick(position)
        }
    }

    override fun getItemCount(): Int = imageList.size

    class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewTile)
        val textView: TextView = itemView.findViewById(R.id.textViewTileTitle)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarTile)
    }
}