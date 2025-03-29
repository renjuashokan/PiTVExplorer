package com.abspi.pitvexplorer.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abspi.pitvexplorer.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class LoginViewModel(private val preferenceManager: PreferenceManager) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val client = OkHttpClient()

    fun getLastServerIp(): String? {
        return preferenceManager.getLastServerIp()
    }

    fun login(serverIp: String): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://$serverIp:8080/api/v1/files")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        // Save the server IP on successful connection
                        preferenceManager.saveServerIp(serverIp)

                        // Update UI state
                        _isLoading.postValue(false)
                        result.postValue(true)
                    } else {
                        _error.postValue("Server returned status code: ${response.code}")
                        _isLoading.postValue(false)
                        result.postValue(false)
                    }
                }
            } catch (e: IOException) {
                Log.e("LoginViewModel", "Connection error", e)
                _error.postValue("Unable to connect to the server: ${e.message}")
                _isLoading.postValue(false)
                result.postValue(false)
            }
        }

        return result
    }
}