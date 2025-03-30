package com.abspi.pitvexplorer.views.activities

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.models.MediaItem
import com.abspi.pitvexplorer.viewmodels.MediaPlayerViewModel
import com.abspi.pitvexplorer.viewmodels.MediaPlayerViewModelFactory
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
    private var player: ExoPlayer? = null
    private var skipDuration: Int = 15 // Default skip duration in seconds

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

        // Set up ViewModel
        val factory = MediaPlayerViewModelFactory(serverAddress)
        viewModel = ViewModelProvider(this, factory)[MediaPlayerViewModel::class.java]
        viewModel.setPlaylist(playlist, initialIndex)

        // Set up observers
        setupObservers()

        // Set up player
        initializePlayer()
    }

    private fun setupObservers() {
        // Observe current media
        viewModel.currentMedia.observe(this) { mediaItem ->
            playerView.player?.let { player ->
                player.setMediaItem(ExoMediaItem.fromUri(Uri.parse(viewModel.streamUrl)))
                player.prepare()
                player.playWhenReady = true
            }
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
                playerView.player = exoPlayer
                
                // Set up player listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            // Auto-advance to next video when current one ends
                            if (viewModel.hasNext) {
                                viewModel.nextVideo()
                            }
                        }
                    }
                })
                
                // Set initial media if available
                viewModel.streamUrl?.let { url ->
                    exoPlayer.setMediaItem(ExoMediaItem.fromUri(Uri.parse(url)))
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }
    }
    
    private fun skipForward() {
        player?.let {
            val newPosition = it.currentPosition + (skipDuration * 1000)
            it.seekTo(newPosition)
        }
    }
    
    private fun skipBackward() {
        player?.let {
            val newPosition = it.currentPosition - (skipDuration * 1000)
            it.seekTo(Math.max(0, newPosition))
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
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
                    skipDuration = input.text.toString().toInt()
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
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            exoPlayer.release()
        }
        player = null
    }
}