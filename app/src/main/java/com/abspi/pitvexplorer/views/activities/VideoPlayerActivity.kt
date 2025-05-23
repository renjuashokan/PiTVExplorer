package com.abspi.pitvexplorer.views.activities

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.adapters.VideoListAdapter
import com.abspi.pitvexplorer.models.MediaItem
import com.abspi.pitvexplorer.viewmodels.MediaPlayerViewModel
import com.abspi.pitvexplorer.viewmodels.MediaPlayerViewModelFactory
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util

class VideoPlayerActivity : FragmentActivity() {

    private lateinit var viewModel: MediaPlayerViewModel
    private lateinit var playerView: PlayerView
    private lateinit var loadingIndicator: View
    private lateinit var errorMessage: TextView
    private lateinit var videoListOverlay: ConstraintLayout
    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var videoAdapter: VideoListAdapter
    private var player: ExoPlayer? = null
    private var skipDuration: Int = 15 // Default skip duration in seconds
    private var isOverlayVisible = false
    private var playWhenReady = true
    private var currentPosition = 0L
    private var currentMediaItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Extract data from intent
        val serverAddress = intent.getStringExtra("SERVER_ADDRESS") ?: run {
            Toast.makeText(this, "Server address not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        @Suppress("UNCHECKED_CAST")
        val playlist = intent.getSerializableExtra("PLAYLIST") as? ArrayList<MediaItem> ?: run {
            Toast.makeText(this, "Playlist not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val initialIndex = intent.getIntExtra("INITIAL_INDEX", 0)

        // Initialize UI components
        playerView = findViewById(R.id.playerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        errorMessage = findViewById(R.id.errorMessage)
        videoListOverlay = findViewById(R.id.videoListOverlay)
        videosRecyclerView = findViewById(R.id.videosRecyclerView)

        // Set up ViewModel
        val factory = MediaPlayerViewModelFactory(serverAddress)
        viewModel = ViewModelProvider(this, factory)[MediaPlayerViewModel::class.java]
        viewModel.setPlaylist(playlist, initialIndex)

        // Set up video list
        setupVideoList(serverAddress, playlist, initialIndex)

        // Set up observers
        setupObservers()

        // Request focus for player view
        playerView.requestFocus()
    }

    private fun setupVideoList(serverAddress: String, playlist: List<MediaItem>, initialIndex: Int) {
        // Initialize the RecyclerView for videos
        videosRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        videoAdapter = VideoListAdapter(playlist, serverAddress, initialIndex) { position ->
            // Handle video selection
            viewModel.playVideoAt(position)
            hideOverlay()
        }

        videosRecyclerView.adapter = videoAdapter
    }

    private fun setupObservers() {
        // Observe current media
        viewModel.currentMedia.observe(this) { mediaItem ->
            mediaItem?.let {
                // Clear any previous media
                player?.let { exoPlayer ->
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()

                    // Set new media
                    val mediaUri = Uri.parse(viewModel.streamUrl)
                    val exoMediaItem = ExoMediaItem.Builder()
                        .setUri(mediaUri)
                        .setMediaId(it.path)
                        .build()

                    exoPlayer.setMediaItem(exoMediaItem)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true

                    // Show loading indicator
                    loadingIndicator.visibility = View.VISIBLE
                }
            }

            // Update selected item in the video list
            val currentIndex = viewModel.currentIndex.value ?: 0
            videoAdapter.updateData(viewModel.playlist.value ?: emptyList(), currentIndex)
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

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                // Set seek increments after player creation
                exoPlayer.setSeekParameters(com.google.android.exoplayer2.SeekParameters.EXACT)

                // Set the player on the view
                playerView.player = exoPlayer

                // Add controller customization
                playerView.setShowNextButton(true)
                playerView.setShowPreviousButton(true)
                playerView.setShowFastForwardButton(true)
                playerView.setShowRewindButton(true)

                // Set up player listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        // Hide loading indicator when playback starts
                        if (state == Player.STATE_READY) {
                            loadingIndicator.visibility = View.GONE
                        }

                        if (state == Player.STATE_ENDED) {
                            // Auto-advance to next video when current one ends
                            if (viewModel.hasNext) {
                                viewModel.nextVideo()
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            loadingIndicator.visibility = View.GONE
                        }
                    }
                })

                // Restore state
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentMediaItemIndex, currentPosition)

                // Set initial media if available
                viewModel.streamUrl?.let { url ->
                    val mediaUri = Uri.parse(url)
                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(mediaUri)
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }
            }
    }

    private fun skipForward() {
        player?.let {
            // Manually seek forward by the skip duration
            val newPosition = it.currentPosition + (skipDuration * 1000)
            it.seekTo(newPosition)
        }
    }

    private fun skipBackward() {
        player?.let {
            // Manually seek backward by the skip duration
            val newPosition = it.currentPosition - (skipDuration * 1000)
            it.seekTo(Math.max(0, newPosition))
        }
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun showOverlay() {
        Log.d("VideoPlayer", "Showing overlay")
        videoListOverlay.visibility = View.VISIBLE
        isOverlayVisible = true
        player?.playWhenReady = false // Pause video when overlay is shown

        // Ensure the current video is visible in the list
        val currentIndex = viewModel.currentIndex.value ?: 0
        videosRecyclerView.scrollToPosition(currentIndex)

        // Request focus on the current item after a brief delay
        videosRecyclerView.postDelayed({
            val viewHolder = videosRecyclerView.findViewHolderForAdapterPosition(currentIndex)
            viewHolder?.itemView?.requestFocus()
        }, 100)
    }

    private fun hideOverlay() {
        Log.d("VideoPlayer", "Hiding overlay")
        videoListOverlay.visibility = View.GONE
        isOverlayVisible = false
        player?.playWhenReady = true // Resume video when overlay is hidden
        playerView.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("VideoPlayer", "Key pressed: $keyCode")

        // If overlay is visible, handle its navigation first
        if (isOverlayVisible) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    hideOverlay()
                    return true
                }
                // Let other keys be handled by the RecyclerView for navigation
                else -> return super.onKeyDown(keyCode, event)
            }
        }

        // Handle normal video player controls when overlay is not visible
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Properly cleanup and finish activity
                releasePlayer()
                finish()
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                toggleOverlay()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                skipForward()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_DPAD_LEFT -> {
                skipBackward()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_N -> {
                if (viewModel.hasNext) {
                    viewModel.nextVideo()
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_P -> {
                if (viewModel.hasPrevious) {
                    viewModel.previousVideo()
                }
                return true
            }
            KeyEvent.KEYCODE_S -> {
                showSkipDurationDialog()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showSkipDurationDialog() {
        val input = EditText(this)
        input.setText(skipDuration.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(this)
            .setTitle("Set Skip Duration")
            .setMessage("Enter skip duration in seconds")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                try {
                    val newDuration = input.text.toString().toInt()
                    skipDuration = newDuration

                    // The skip duration is used directly in the skipForward() and skipBackward() methods
                    // No need to update ExoPlayer settings here since we're using manual seeking

                } catch (e: Exception) {
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        // Force landscape orientation for video playback
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            savePlayerState()
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            savePlayerState()
            releasePlayer()
        }
    }

    private fun savePlayerState() {
        player?.let { exoPlayer ->
            playWhenReady = exoPlayer.playWhenReady
            currentPosition = exoPlayer.currentPosition
            currentMediaItemIndex = exoPlayer.currentMediaItemIndex
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            // Make sure to pause playback before releasing
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make absolutely sure the player is released
        releasePlayer()
    }
}