package com.wemeet.projectmemory.llm.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Downloads the Gemma model file in the background with resume support
 * (Phase 3-1). A `.part` file is grown with HTTP Range requests so a killed
 * process / lost connection just picks back up instead of restarting a
 * multi-GB download from zero.
 */
class ModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val destination = ModelDownloadManager.modelFile(applicationContext)
        val partFile = File(destination.parentFile, "${destination.name}.part")

        if (destination.exists()) return@withContext Result.success()

        try {
            downloadWithResume(partFile)
        } catch (e: Exception) {
            return@withContext Result.retry()
        }

        setProgress(workDataOf(KEY_STATE to STATE_VERIFYING))
        val expectedSha256 = ModelDownloadManager.EXPECTED_SHA256
        if (expectedSha256.isNotBlank() && sha256(partFile) != expectedSha256) {
            partFile.delete()
            return@withContext Result.failure(workDataOf(KEY_ERROR to "체크섬이 일치하지 않습니다. 다시 시도해주세요."))
        }

        if (!partFile.renameTo(destination)) {
            return@withContext Result.failure(workDataOf(KEY_ERROR to "다운로드 파일을 이동하지 못했습니다."))
        }
        Result.success()
    }

    private suspend fun downloadWithResume(partFile: File) {
        val alreadyDownloaded = if (partFile.exists()) partFile.length() else 0L

        val request = Request.Builder()
            .url(ModelDownloadManager.MODEL_DOWNLOAD_URL)
            .header("Range", "bytes=$alreadyDownloaded-")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Unexpected download response: ${response.code}")
            val body = response.body ?: error("Empty response body")
            val totalBytes = alreadyDownloaded + body.contentLength().coerceAtLeast(0)

            RandomAccessFile(partFile, "rw").use { output ->
                output.seek(alreadyDownloaded)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = alreadyDownloaded
                    var lastReportedPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            setProgress(
                                workDataOf(
                                    KEY_STATE to STATE_DOWNLOADING,
                                    KEY_PROGRESS_PERCENT to percent,
                                    KEY_DOWNLOADED_BYTES to downloaded,
                                    KEY_TOTAL_BYTES to totalBytes,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_STATE = "state"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR = "error"

        const val STATE_DOWNLOADING = "downloading"
        const val STATE_VERIFYING = "verifying"
    }
}
