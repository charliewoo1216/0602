package com.wemeet.projectmemory.data.repository

import android.net.Uri
import com.wemeet.projectmemory.data.local.config.ProjectConfigStore
import com.wemeet.projectmemory.data.local.config.ProjectFileScaffolder
import com.wemeet.projectmemory.data.local.room.ProjectDao
import com.wemeet.projectmemory.data.local.room.ProjectIndexEntity
import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.data.model.ProjectConfig
import com.wemeet.projectmemory.data.model.Purpose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point the UI layer talks to for project CRUD. Combines the
 * fast Room index with the canonical project.json file: reads come from
 * Room (fast, list-friendly), every write goes to project.json first and
 * then mirrors into Room so the two never drift apart.
 */
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val configStore: ProjectConfigStore,
    private val scaffolder: ProjectFileScaffolder,
) {
    fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().map { list -> list.map(::toDomain) }

    fun searchProjects(query: String): Flow<List<Project>> =
        projectDao.search(query).map { list -> list.map(::toDomain) }

    suspend fun getProject(id: String): Project? = projectDao.getById(id)?.let(::toDomain)

    /** Re-reads project.json from disk in case it was edited externally (e.g. from a PC). */
    suspend fun refreshFromDisk(id: String): Project? {
        val cached = projectDao.getById(id) ?: return null
        val folderUri = Uri.parse(cached.folderUri)
        val config = configStore.load(folderUri) ?: return toDomain(cached)
        val refreshed = cached.copy(
            name = config.name,
            colorHex = config.colorHex,
            pinned = config.pinned,
            purposeId = config.purposeId,
            glossary = config.glossary,
        )
        projectDao.update(refreshed)
        return toDomain(refreshed)
    }

    suspend fun createProject(
        parentFolderUri: Uri,
        name: String,
        colorHex: String,
        purpose: Purpose,
    ): Project {
        val result = scaffolder.scaffold(parentFolderUri, name, colorHex, purpose)
        val entity = ProjectIndexEntity(
            id = result.config.id,
            name = result.config.name,
            colorHex = result.config.colorHex,
            folderUri = result.folderUri.toString(),
            pinned = result.config.pinned,
            createdAtEpochMillis = result.config.createdAtEpochMillis,
            lastUpdatedEpochMillis = result.config.createdAtEpochMillis,
            previewText = "",
            recentDocTypes = purpose.defaultDocTypes,
            purposeId = result.config.purposeId,
            glossary = result.config.glossary,
        )
        projectDao.upsert(entity)
        return toDomain(entity)
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        projectDao.setPinned(id, pinned)
        val cached = projectDao.getById(id) ?: return
        writeConfigFromCache(cached)
    }

    suspend fun saveSettings(id: String, purpose: Purpose, glossary: List<GlossaryTerm>) {
        val cached = projectDao.getById(id) ?: return
        val updated = cached.copy(purposeId = purpose.id, glossary = glossary)
        projectDao.update(updated)
        writeConfigFromCache(updated)
    }

    suspend fun updatePreview(id: String, previewText: String, recentDocTypes: List<DocType>) {
        projectDao.updatePreview(id, previewText, System.currentTimeMillis(), recentDocTypes)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.getById(project.id)?.let { projectDao.delete(it) }
        // Deliberately does not delete the on-disk folder/files — SAF storage
        // is user-owned; "removing" a project only removes it from the app's
        // index/list.
    }

    private suspend fun writeConfigFromCache(entity: ProjectIndexEntity) {
        val config = ProjectConfig(
            id = entity.id,
            name = entity.name,
            colorHex = entity.colorHex,
            purposeId = entity.purposeId,
            glossary = entity.glossary,
            pinned = entity.pinned,
            createdAtEpochMillis = entity.createdAtEpochMillis,
        )
        configStore.save(Uri.parse(entity.folderUri), config)
    }

    private fun toDomain(entity: ProjectIndexEntity): Project = Project(
        id = entity.id,
        name = entity.name,
        colorHex = entity.colorHex,
        folderUri = Uri.parse(entity.folderUri),
        pinned = entity.pinned,
        lastUpdatedEpochMillis = entity.lastUpdatedEpochMillis,
        previewText = entity.previewText,
        recentDocTypes = entity.recentDocTypes,
        purpose = Purpose.fromId(entity.purposeId),
        glossary = entity.glossary,
    )
}
