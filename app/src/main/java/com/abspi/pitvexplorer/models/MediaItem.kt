package com.abspi.pitvexplorer.models

import java.io.Serializable

data class MediaItem(
    val name: String,
    val path: String
) : Serializable