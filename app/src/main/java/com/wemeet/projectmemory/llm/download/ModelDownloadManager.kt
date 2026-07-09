package com.wemeet.projectmemory.llm.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Orchestrates the Gemma model download (Phase 3-1) via WorkManager so it
 * survives process death, and reports progress/state to the UI as a Flow.
 */
class ModelDownloadManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun isModelReady(): Boolean = modelFile(context).let { it.exists() && it.length() > 0 }

    fun enqueueDownload(wifiOnly: Boolean = true) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelDownload() {
        workManager.cancelUniqueWork(WORK_TAG)
    }

    fun observeState(): Flow<ModelDownloadState> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_TAG).map(::toDownloadState)

    private fun toDownloadState(infos: List<WorkInfo>): ModelDownloadState {
        val info = infos.firstOrNull() ?: return ModelDownloadState.NotStarted
        return when (info.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ModelDownloadState.Downloading(0, 0, 0)
            WorkInfo.State.RUNNING -> {
                val progress = info.progress
                when (progress.getString(ModelDownloadWorker.KEY_STATE)) {
                    ModelDownloadWorker.STATE_VERIFYING -> ModelDownloadState.Verifying
                    else -> ModelDownloadState.Downloading(
                        progressPercent = progress.getInt(ModelDownloadWorker.KEY_PROGRESS_PERCENT, 0),
                        downloadedBytes = progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0),
                        totalBytes = progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, 0),
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> ModelDownloadState.Completed
            WorkInfo.State.FAILED -> ModelDownloadState.Failed(
                info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "다운로드에 실패했습니다.",
            )
            WorkInfo.State.CANCELLED -> ModelDownloadState.NotStarted
        }
    }

    companion object {
        private const val WORK_TAG = "gemma_model_download"
        private const val MODEL_FILE_NAME = "gemma-4-e4b-it.litertlm"

        // TODO(todo.md #52-53): confirm the exact asset name and whether a
        // gated Hugging Face login/token is required before wiring this up
        // to a real download in production.
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it-int4.litertlm"
        const val EXPECTED_SHA256 = ""

        fun modelFile(context: Context): File = File(context.getExternalFilesDir(null), MODEL_FILE_NAME)
    }
}
