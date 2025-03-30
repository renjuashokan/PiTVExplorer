package com.abspi.pitvexplorer.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.adapters.ImagePagerAdapter
import com.abspi.pitvexplorer.models.ImageItem
import java.util.Timer
import java.util.TimerTask

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var imageTitleText: TextView
    private lateinit var imageCountText: TextView
    private lateinit var slideshowButton: ImageButton
    private lateinit var adapter: ImagePagerAdapter
    private lateinit var serverIp: String
    private lateinit var imageList: ArrayList<ImageItem>

    private var slideshowTimer: Timer? = null
    private var isSlideshow = false
    private val slideshowDelay = 3000L // 3 seconds per image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image Viewer"

        // Get server IP and initial data from intent
        serverIp = intent.getStringExtra("SERVER_ADDRESS") ?: run {
            finish()
            return
        }

        imageList = intent.getSerializableExtra("IMAGES") as ArrayList<ImageItem>
        val initialIndex = intent.getIntExtra("INITIAL_INDEX", 0)

        // Initialize UI components
        viewPager = findViewById(R.id.viewPagerImages)
        progressBar = findViewById(R.id.progressLoading)
        errorText = findViewById(R.id.textViewError)
        imageTitleText = findViewById(R.id.textViewImageTitle)
        imageCountText = findViewById(R.id.textViewImageCount)
        slideshowButton = findViewById(R.id.buttonSlideshow)

        // Initialize adapter and ViewPager
        adapter = ImagePagerAdapter(this, imageList)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(initialIndex, false)

        // Set up event listeners
        setupEventListeners()

        // Update UI with initial image
        updateImageInfo(initialIndex)
    }

    private fun setupEventListeners() {
        // ViewPager page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateImageInfo(position)
            }
        })

        // Slideshow button click listener
        slideshowButton.setOnClickListener {
            toggleSlideshow()
        }

        // Set up swipe gesture for navigation between images
        viewPager.setOnTouchListener { _, _ ->
            if (isSlideshow) {
                stopSlideshow()
                return@setOnTouchListener false
            }
            false
        }
    }

    private fun updateImageInfo(position: Int) {
        if (imageList.isNotEmpty() && position < imageList.size) {
            val currentImage = imageList[position]
            imageTitleText.text = currentImage.name
            imageCountText.text = getString(R.string.image_count_format, position + 1, imageList.size)
        }
    }

    private fun toggleSlideshow() {
        if (isSlideshow) {
            stopSlideshow()
        } else {
            startSlideshow()
        }
    }

    private fun startSlideshow() {
        isSlideshow = true
        slideshowButton.setImageResource(R.drawable.ic_pause) // Change to your pause icon

        slideshowTimer = Timer()
        slideshowTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val currentItem = viewPager.currentItem
                    val nextItem = (currentItem + 1) % adapter.itemCount
                    viewPager.setCurrentItem(nextItem, true)
                }
            }
        }, slideshowDelay, slideshowDelay)
    }

    private fun stopSlideshow() {
        isSlideshow = false
        slideshowButton.setImageResource(R.drawable.ic_slideshow) // Change to your slideshow icon
        slideshowTimer?.cancel()
        slideshowTimer = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_image_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_tile_view -> {
                // Switch to the TileGalleryActivity
                switchToTileView()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun switchToTileView() {
        // Launch the TileGalleryActivity with the same data
        val intent = Intent(this, TileGalleryActivity::class.java).apply {
            putExtra("SERVER_ADDRESS", serverIp)
            putExtra("IMAGES", imageList)
            putExtra("CURRENT_POSITION", viewPager.currentItem)
        }
        startActivity(intent)
        finish() // Close this activity when switching to tile view
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle navigation with D-pad
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (viewPager.currentItem < adapter.itemCount - 1) {
                    viewPager.currentItem += 1
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (viewPager.currentItem > 0) {
                    viewPager.currentItem -= 1
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        // Stop slideshow when activity is paused
        if (isSlideshow) {
            stopSlideshow()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowTimer?.cancel()
    }
}