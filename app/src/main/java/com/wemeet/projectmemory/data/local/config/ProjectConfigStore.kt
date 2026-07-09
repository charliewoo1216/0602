package com.wemeet.projectmemory.data.local.config

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.wemeet.projectmemory.data.model.ProjectConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Reads/writes `project.json` (the canonical purpose+glossary settings file)
 * inside a project's SAF folder. Room's ProjectIndexEntity is only a cache
 * derived from this file; if they ever disagree, this file wins.
 */
class ProjectConfigStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun load(folderUri: Uri): ProjectConfig? = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext null
        val configFile = dir.findFile(CONFIG_FILE_NAME) ?: return@withContext null
        context.contentResolver.openInputStream(configFile.uri)?.use { input ->
            json.decodeFromString(ProjectConfig.serializer(), input.readBytes().toString(Charsets.UTF_8))
        }
    }

    suspend fun save(folderUri: Uri, config: ProjectConfig) = withContext(Dispatchers.IO) {
        val dir = DocumentFile.fromTreeUri(context, folderUri)
            ?: error("Invalid project folder URI: $folderUri")
        val file = dir.findFile(CONFIG_FILE_NAME)
            ?: dir.createFile("application/json", CONFIG_FILE_NAME)
            ?: error("Could not create $CONFIG_FILE_NAME under $folderUri")

        context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
            output.write(json.encodeToString(ProjectConfig.serializer(), config).toByteArray(Charsets.UTF_8))
        }
    }

    companion object {
        const val CONFIG_FILE_NAME = "project.json"
    }
}
