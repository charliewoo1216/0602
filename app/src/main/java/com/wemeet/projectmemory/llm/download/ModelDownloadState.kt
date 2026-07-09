package com.wemeet.projectmemory.llm.download

sealed interface ModelDownloadState {
    data object NotStarted : ModelDownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : ModelDownloadState
    data object Verifying : ModelDownloadState
    data object Completed : ModelDownloadState
    data class Failed(val message: String) : ModelDownloadState
}
