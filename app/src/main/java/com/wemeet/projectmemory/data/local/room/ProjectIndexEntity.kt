package com.wemeet.projectmemory.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Purpose

/**
 * Fast-query cache of a project's card info. Rebuildable at any time by
 * re-scanning each project folder's `project.json`; never treated as the
 * source of truth for purpose/glossary (those live only in project.json).
 */
@Entity(tableName = "project_index")
@TypeConverters(RoomConverters::class)
data class ProjectIndexEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val folderUri: String,
    val pinned: Boolean,
    val createdAtEpochMillis: Long,
    val lastUpdatedEpochMillis: Long,
    val previewText: String,
    val recentDocTypes: List<DocType>,
    val purposeId: String = Purpose.default.id,
    val glossary: List<GlossaryTerm> = emptyList(),
)
