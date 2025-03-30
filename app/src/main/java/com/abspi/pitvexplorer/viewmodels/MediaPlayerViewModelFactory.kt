package com.abspi.pitvexplorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MediaPlayerViewModelFactory(private val serverAddress: String) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaPlayerViewModel::class.java)) {
            return MediaPlayerViewModel(serverAddress) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}