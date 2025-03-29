package com.abspi.pitvexplorer.models

import java.util.Date

data class FileItem(
    val name: String,
    val size: Int,
    val isDirectory: Boolean,
    val createdTime: Date,
    val modifiedTime: Date,
    val owner: String,
    val fileType: String?,
    val fullName: String
) {
    companion object {
        fun fromJson(json: Map<String, Any>): FileItem {
            return FileItem(
                name = json["name"] as String,
                size = json["size"] as Int,
                isDirectory = json["is_directory"] as Boolean,
                createdTime = Date((json["created_time"] as Long)),
                modifiedTime = Date((json["modified_time"] as Long)),
                owner = json["owner"] as String,
                fileType = json["file_type"] as String?,
                fullName = json["full_name"] as String
            )
        }
    }
}