package com.abspi.pitvexplorer.models

data class FilePiResponse(
    val totalFiles: Int,
    val files: List<FileItem>,
    val skip: Int,
    val limit: Int
) {
    companion object {
        fun fromJson(json: Map<String, Any>): FilePiResponse {
            return FilePiResponse(
                totalFiles = json["total_files"] as Int,
                files = (json["files"] as List<*>).map { FileItem.fromJson(it as Map<String, Any>) },
                skip = json["skip"] as Int,
                limit = json["limit"] as Int
            )
        }
    }
}
