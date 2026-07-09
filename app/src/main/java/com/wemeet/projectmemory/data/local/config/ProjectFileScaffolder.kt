package com.wemeet.projectmemory.data.local.config

import android.net.Uri
import com.wemeet.projectmemory.data.model.ProjectConfig
import com.wemeet.projectmemory.data.model.Purpose
import com.wemeet.projectmemory.document.PurposeDocumentTemplates
import com.wemeet.projectmemory.document.StorageManager
import java.util.UUID

data class ScaffoldResult(val folderUri: Uri, val config: ProjectConfig)

/**
 * Creates a new project's folder + default `.md` files + `project.json`
 * under a user-chosen parent SAF folder (Phase 1, "새 프로젝트 생성").
 */
class ProjectFileScaffolder(
    private val storageManager: StorageManager,
    private val configStore: ProjectConfigStore,
) {
    suspend fun scaffold(
        parentFolderUri: Uri,
        projectName: String,
        colorHex: String,
        purpose: Purpose,
    ): ScaffoldResult {
        val folderUri = storageManager.createSubFolder(parentFolderUri, projectName)

        val config = ProjectConfig(
            id = UUID.randomUUID().toString(),
            name = projectName,
            colorHex = colorHex,
            purposeId = purpose.id,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        configStore.save(folderUri, config)

        purpose.defaultDocTypes.forEach { docType ->
            if (!storageManager.fileExists(folderUri, docType.fileName)) {
                storageManager.writeFile(
                    folderUri = folderUri,
                    fileName = docType.fileName,
                    content = PurposeDocumentTemplates.initialContent(docType, projectName, purpose),
                )
            }
        }

        return ScaffoldResult(folderUri, config)
    }
}
