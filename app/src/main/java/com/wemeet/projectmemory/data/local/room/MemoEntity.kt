package com.wemeet.projectmemory.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wemeet.projectmemory.data.model.MemoStatus

/**
 * One voice capture's processing record (Phase 5-1). This is history/index
 * only — the applied Markdown file is authoritative; on any mismatch the
 * file wins.
 */
@Entity(tableName = "memos")
@TypeConverters(RoomConverters::class)
data class MemoEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val timestampEpochMillis: Long,
    val rawSttText: String,
    val correctedSummary: String?,
    val targetFile: String?,
    val status: MemoStatus,
    val voiceFilePath: String? = null,
    val failureReason: String? = null,
)
