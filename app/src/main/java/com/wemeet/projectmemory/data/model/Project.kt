package com.wemeet.projectmemory.data.model

import android.net.Uri

/**
 * UI/domain-facing project model (Phase 1). Backed by ProjectIndexEntity
 * (fast Room cache) whose purpose/glossary fields are themselves synced from
 * project.json (see ProjectConfigStore) whenever settings are saved.
 */
data class Project(
    val id: String,
    val name: String,
    val colorHex: String,
    val folderUri: Uri,
    val pinned: Boolean,
    val lastUpdatedEpochMillis: Long,
    val previewText: String,
    val recentDocTypes: List<DocType>,
    val purpose: Purpose,
    val glossary: List<GlossaryTerm>,
)
