package com.abspi.pitvexplorer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FileBrowserViewModelFactory(private val serverIp: String) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileBrowserViewModel::class.java)) {
            return FileBrowserViewModel(serverIp) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}