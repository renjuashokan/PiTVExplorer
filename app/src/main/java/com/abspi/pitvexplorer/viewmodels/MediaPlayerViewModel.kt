package com.abspi.pitvexplorer.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.abspi.pitvexplorer.models.MediaItem

class MediaPlayerViewModel(private val serverAddress: String) : ViewModel() {

    private val _playlist = MutableLiveData<List<MediaItem>>(emptyList())
    val playlist: LiveData<List<MediaItem>> = _playlist

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _currentMedia = MutableLiveData<MediaItem?>(null)
    val currentMedia: LiveData<MediaItem?> = _currentMedia

    val streamUrl: String?
        get() = _currentMedia.value?.path?.let {
            "http://$serverAddress:8080/api/v1/stream/${Uri.encode(it)}"
        }

    val hasNext: Boolean
        get() = (_currentIndex.value ?: 0) < (_playlist.value?.size ?: 0) - 1

    val hasPrevious: Boolean
        get() = (_currentIndex.value ?: 0) > 0

    fun setPlaylist(playlist: List<MediaItem>, initialIndex: Int) {
        _playlist.value = playlist
        _currentIndex.value = initialIndex
        updateCurrentMedia()
        prepareStream()
    }

    fun nextVideo() {
        if (hasNext) {
            _currentIndex.value = (_currentIndex.value ?: 0) + 1
            updateCurrentMedia()
            prepareStream()
        }
    }

    fun previousVideo() {
        if (hasPrevious) {
            _currentIndex.value = (_currentIndex.value ?: 0) - 1
            updateCurrentMedia()
            prepareStream()
        }
    }

    private fun updateCurrentMedia() {
        val index = _currentIndex.value ?: 0
        val list = _playlist.value ?: emptyList()
        _currentMedia.value = if (list.isNotEmpty() && index < list.size) list[index] else null
    }

    private fun prepareStream() {
        if (_currentMedia.value == null) {
            setError("No media selected")
            return
        }

        _isLoading.value = true
        _error.value = null

        // The streamUrl property now handles URL construction
        _isLoading.value = false
    }

    fun setError(error: String) {
        _error.value = error
        _isLoading.value = false
    }
}