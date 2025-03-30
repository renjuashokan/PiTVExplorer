package com.abspi.pitvexplorer.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abspi.pitvexplorer.models.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

enum class SortCriteria { TIME_EDITED, SIZE, FILENAME, TYPE }
enum class ViewMode { ALL, VIDEOS_ONLY }

class FileBrowserViewModel(private val serverIp: String) : ViewModel() {

    private val _files = MutableLiveData<List<FileItem>>(emptyList())
    val files: LiveData<List<FileItem>> = _files

    private val _currentPath = MutableLiveData<String>(".")
    val currentPath: LiveData<String> = _currentPath

    private val _pathSegments = MutableLiveData<List<String>>(listOf("\$"))
    val pathSegments: LiveData<List<String>> = _pathSegments

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _viewMode = MutableLiveData<ViewMode>(ViewMode.ALL)
    val viewMode: LiveData<ViewMode> = _viewMode

    private var sortCriteria = SortCriteria.TIME_EDITED
    private var isAscending = false
    private var currentPage = 0
    private var totalFiles = 0
    private val itemsPerPage = 25
    private var hasMorePages = true
    private var isSearchMode = false
    private var searchQuery = ""

    private val client = OkHttpClient()

    fun fetchFiles(reset: Boolean = false) {
        if (reset) {
            currentPage = 0
            _files.value = emptyList()
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiPath = getApiPath()
                val queryParams = HashMap<String, String>()
                queryParams["path"] = apiPath
                queryParams["skip"] = (currentPage * itemsPerPage).toString()
                queryParams["limit"] = itemsPerPage.toString()

                if (sortCriteria != SortCriteria.TIME_EDITED) {
                    queryParams["sort_by"] = getSortByString(sortCriteria)
                    queryParams["order"] = if (isAscending) "asc" else "desc"
                }

                val endpoint = if (_viewMode.value == ViewMode.ALL) "files" else "videos"
                if (_viewMode.value == ViewMode.VIDEOS_ONLY) {
                    queryParams["recursive"] = "true"
                }

                val urlBuilder = StringBuilder("http://$serverIp:8080/api/v1/$endpoint?")
                queryParams.forEach { (key, value) ->
                    urlBuilder.append("$key=$value&")
                }
                val url = urlBuilder.toString().dropLast(1) // Remove trailing &

                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string()
                        if (jsonString != null) {
                            val json = JSONObject(jsonString)
                            val totalFiles = json.getInt("total_files")
                            this@FileBrowserViewModel.totalFiles = totalFiles
                            hasMorePages = (currentPage + 1) * itemsPerPage < totalFiles

                            val filesArray = json.getJSONArray("files")
                            val filesList = mutableListOf<FileItem>()

                            for (i in 0 until filesArray.length()) {
                                val fileJson = filesArray.getJSONObject(i)
                                filesList.add(
                                    FileItem(
                                        name = fileJson.getString("name"),
                                        size = fileJson.getInt("size"),
                                        isDirectory = fileJson.getBoolean("is_directory"),
                                        createdTime = Date(fileJson.getLong("created_time")),
                                        modifiedTime = Date(fileJson.getLong("modified_time")),
                                        owner = fileJson.getString("owner"),
                                        fileType = if (fileJson.has("file_type")) fileJson.getString("file_type") else null,
                                        fullName = fileJson.getString("full_name")
                                    )
                                )
                            }

                            // Update UI in main thread
                            val currentFiles = _files.value?.toMutableList() ?: mutableListOf()
                            if (reset) {
                                _files.postValue(filesList)
                            } else {
                                currentFiles.addAll(filesList)
                                _files.postValue(currentFiles)
                            }
                        }
                    } else {
                        throw Exception("Failed to fetch files. Status code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                _error.postValue("Error fetching files: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun getFileFullPath(fileName: String): String {
        val currentPath = _currentPath.value ?: "."
        return if (currentPath == "." || currentPath == "\$") {
            fileName
        } else {
            "$currentPath/$fileName"
        }
    }


    fun debugPaths() {
        Log.d("PathDebug", "Current path value: ${_currentPath.value}")
        Log.d("PathDebug", "Path segments: ${_pathSegments.value}")

        // Check if current path matches what's displayed in the UI
        val segmentsWithoutRoot = _pathSegments.value?.filterIndexed { index, _ -> index > 0 } ?: listOf()
        val pathFromSegments = segmentsWithoutRoot.joinToString("/")
        Log.d("PathDebug", "Path from segments: $pathFromSegments")
    }

    fun navigateToDirectory(dirName: String) {
        Log.d("PathDebug", "Navigating to directory: $dirName")
        Log.d("PathDebug", "Current path before: ${_currentPath.value}")
        val path = _currentPath.value ?: "."
        val newPath = if (path == "." || path == "\$") {
            dirName
        } else {
            "$path/$dirName"
        }
        _currentPath.value = normalizeServerPath(newPath)
        updatePathSegments()
        fetchFiles(reset = true)
        debugPaths()
        Log.d("PathDebug", "Current path after: ${_currentPath.value}")
    }

    fun navigateUp() {
        val path = _currentPath.value ?: "."
        if (path == "." || path == "\$") {
            return
        }

        val segments = path.split("/")
        _currentPath.value = if (segments.size <= 1) {
            "."
        } else {
            segments.dropLast(1).joinToString("/")
        }
        updatePathSegments()
        fetchFiles(reset = true)
    }

    fun navigateToPath(index: Int) {
        val segments = _pathSegments.value ?: listOf()
        if (index == 0) {
            _currentPath.value = "."
        } else if (index < segments.size) {
            _currentPath.value = segments.subList(1, index + 1).joinToString("/")
        }
        updatePathSegments()
        fetchFiles(reset = true)
    }

    fun loadNextPageIfNeeded() {
        if (!_isLoading.value!! && hasMorePages) {
            currentPage++
            if (isSearchMode) {
                searchFiles(searchQuery)
            } else {
                fetchFiles()
            }
        }
    }

    fun searchFiles(query: String) {
        searchQuery = query
        isSearchMode = query.isNotEmpty()
        currentPage = 0
        _files.value = emptyList()

        if (query.isEmpty()) {
            isSearchMode = false
            fetchFiles(reset = true)
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "http://$serverIp:8080/api/v1/search?path=${getApiPath()}&query=$query&skip=${currentPage * itemsPerPage}&limit=$itemsPerPage"

                val request = Request.Builder()
                    .url(url)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string()
                        if (jsonString != null) {
                            val json = JSONObject(jsonString)
                            val totalFiles = json.getInt("total_files")
                            this@FileBrowserViewModel.totalFiles = totalFiles
                            hasMorePages = (currentPage + 1) * itemsPerPage < totalFiles

                            val filesArray = json.getJSONArray("files")
                            val filesList = mutableListOf<FileItem>()

                            for (i in 0 until filesArray.length()) {
                                val fileJson = filesArray.getJSONObject(i)
                                filesList.add(
                                    FileItem(
                                        name = fileJson.getString("name"),
                                        size = fileJson.getInt("size"),
                                        isDirectory = fileJson.getBoolean("is_directory"),
                                        createdTime = Date(fileJson.getLong("created_time")),
                                        modifiedTime = Date(fileJson.getLong("modified_time")),
                                        owner = fileJson.getString("owner"),
                                        fileType = if (fileJson.has("file_type")) fileJson.getString("file_type") else null,
                                        fullName = fileJson.getString("full_name")
                                    )
                                )
                            }

                            _files.postValue(filesList)
                        }
                    } else {
                        throw Exception("Failed to search files. Status code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                _error.postValue("Error searching files: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun toggleViewMode() {
        val newMode = if (_viewMode.value == ViewMode.ALL) ViewMode.VIDEOS_ONLY else ViewMode.ALL
        _viewMode.value = newMode
        fetchFiles(reset = true)
    }

    private fun updatePathSegments() {
        val path = _currentPath.value ?: "."
        if (path == ".") {
            _pathSegments.value = listOf("\$")
        } else {
            val segments = mutableListOf("\$")
            segments.addAll(path.split("/").filter { it.isNotEmpty() })
            _pathSegments.value = segments
        }
    }

    private fun normalizeServerPath(path: String): String {
        // Convert $ to . for server requests
        if (path == "\$") return "."

        // Remove double slashes and leading/trailing slashes
        var normalizedPath = path
        normalizedPath = normalizedPath.replace(Regex("/+"), "/")
        normalizedPath = normalizedPath.replace(Regex("^/+|/+$"), "")

        return normalizedPath
    }

    private fun getApiPath(): String {
        val path = _currentPath.value ?: "."
        return if (path == "\$") "." else normalizeServerPath(path)
    }

    private fun getSortByString(criteria: SortCriteria): String {
        return when (criteria) {
            SortCriteria.TIME_EDITED -> "modified_time"
            SortCriteria.SIZE -> "size"
            SortCriteria.FILENAME -> "name"
            SortCriteria.TYPE -> "file_type"
        }
    }

    fun isPicture(filename: String): Boolean {
        val lower = filename.toLowerCase(Locale.ROOT)
        return lower.endsWith(".jpeg") || lower.endsWith(".jpg") ||
                lower.endsWith(".png") || lower.endsWith(".gif") ||
                lower.endsWith(".bmp") || lower.endsWith(".webp")
    }

    fun isVideo(filename: String): Boolean {
        val lower = filename.toLowerCase(Locale.ROOT)
        return lower.endsWith(".mp4") || lower.endsWith(".avi") ||
                lower.endsWith(".mkv")
    }


    fun getThumbnailUrl(file: FileItem): String {
        // Get the full path by combining current path with file name
        val fullPath = getFileFullPath(file.name)
        val normalizedPath = normalizeServerPath(fullPath)
        val baseUrl = "http://$serverIp:8080/api/v1"

        return if (isVideo(file.name)) {
            "$baseUrl/thumbnail/${Uri.encode(normalizedPath)}"
        } else if (isPicture(file.name)) {
            "$baseUrl/file/${Uri.encode(normalizedPath)}"
        } else {
            ""
        }
    }
}