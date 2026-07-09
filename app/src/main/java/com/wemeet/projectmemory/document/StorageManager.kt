package com.wemeet.projectmemory.document

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thin SAF (Storage Access Framework) wrapper. This is the only place that
 * touches `DocumentFile`/`ContentResolver` directly — everything else (the
 * config store, the document merge logic) goes through here so the storage
 * backend could be swapped later without touching business logic.
 */
class StorageManager(private val context: Context) {

    fun folder(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)

    suspend fun createSubFolder(parentUri: Uri, name: String): Uri = withContext(Dispatchers.IO) {
        val parent = folder(parentUri) ?: error("Invalid parent folder URI: $parentUri")
        val sanitized = sanitizeFileName(name)
        val existing = parent.findFile(sanitized)
        val target = if (existing != null && existing.isDirectory) {
            existing
        } else {
            parent.createDirectory(sanitized) ?: error("Could not create folder '$sanitized'")
        }
        target.uri
    }

    suspend fun readFile(folderUri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        val file = folder(folderUri)?.findFile(fileName) ?: return@withContext null
        context.contentResolver.openInputStream(file.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    suspend fun writeFile(
        folderUri: Uri,
        fileName: String,
        content: String,
        mimeType: String = "text/markdown",
    ): Uri = withContext(Dispatchers.IO) {
        val dir = folder(folderUri) ?: error("Invalid project folder URI: $folderUri")
        val file = dir.findFile(fileName) ?: dir.createFile(mimeType, fileName)
            ?: error("Could not create file '$fileName'")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }
        file.uri
    }

    suspend fun fileExists(folderUri: Uri, fileName: String): Boolean = withContext(Dispatchers.IO) {
        folder(folderUri)?.findFile(fileName) != null
    }

    suspend fun listMarkdownFiles(folderUri: Uri): List<DocumentFile> = withContext(Dispatchers.IO) {
        folder(folderUri)?.listFiles()?.filter { it.isFile && it.name?.endsWith(".md") == true }.orEmpty()
    }

    private fun sanitizeFileName(name: String): String =
        name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "untitled" }
}
