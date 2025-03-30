package com.abspi.pitvexplorer.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.adapters.ImageTileAdapter
import com.abspi.pitvexplorer.models.ImageItem

class TileGalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var errorText: TextView
    private lateinit var adapter: ImageTileAdapter
    private lateinit var serverIp: String
    private lateinit var imageList: ArrayList<ImageItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tile_gallery)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image Gallery"

        // Get server IP and image data from intent
        serverIp = intent.getStringExtra("SERVER_ADDRESS") ?: run {
            finish()
            return
        }

        imageList = intent.getSerializableExtra("IMAGES") as ArrayList<ImageItem>
                val currentPosition = intent.getIntExtra("CURRENT_POSITION", 0)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewImageTiles)
        errorText = findViewById(R.id.textViewTileError)

        // Set up adapter and RecyclerView
        setupRecyclerView(currentPosition)
    }

    private fun setupRecyclerView(currentPosition: Int) {
        adapter = ImageTileAdapter(this, imageList, object : ImageTileAdapter.ImageTileClickListener {
            override fun onImageTileClick(position: Int) {
                // Switch back to full-screen image view
                navigateToFullScreenView(position)
            }
        })

        recyclerView.apply {
            layoutManager = GridLayoutManager(this@TileGalleryActivity, 3)
            adapter = this@TileGalleryActivity.adapter
                    setHasFixedSize(true)
        }

        // Scroll to the current image
        recyclerView.post {
            recyclerView.scrollToPosition(currentPosition)
        }
    }

    private fun navigateToFullScreenView(position: Int) {
        // Navigate back to ImageViewerActivity with the selected image
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtra("SERVER_ADDRESS", serverIp)
            putExtra("IMAGES", imageList)
            putExtra("INITIAL_INDEX", position)
        }
        startActivity(intent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}