package com.wemeet.projectmemory.data.repository

import com.wemeet.projectmemory.data.local.room.MemoDao
import com.wemeet.projectmemory.data.local.room.MemoEntity
import com.wemeet.projectmemory.data.model.MemoStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Phase 5-1: memo history/index. The file the memo was applied to (if any)
 * is still authoritative — this table exists so the app has a fast, local
 * timeline/search/undo-source without re-parsing Markdown on every screen.
 */
class MemoRepository(private val memoDao: MemoDao) {

    /** Call immediately after STT finishes, before the LLM ever sees the text. */
    suspend fun recordPending(projectId: String, rawSttText: String, voiceFilePath: String? = null): String {
        val id = UUID.randomUUID().toString()
        memoDao.upsert(
            MemoEntity(
                id = id,
                projectId = projectId,
                timestampEpochMillis = System.currentTimeMillis(),
                rawSttText = rawSttText,
                correctedSummary = null,
                targetFile = null,
                status = MemoStatus.PENDING,
                voiceFilePath = voiceFilePath,
            ),
        )
        return id
    }

    suspend fun markApplied(memoId: String, correctedSummary: String, targetFile: String) {
        memoDao.updateResult(
            id = memoId,
            status = MemoStatus.APPLIED,
            correctedSummary = correctedSummary,
            targetFile = targetFile,
            failureReason = null,
        )
    }

    suspend fun markFailed(memoId: String, reason: String) {
        memoDao.updateResult(
            id = memoId,
            status = MemoStatus.FAILED,
            correctedSummary = null,
            targetFile = null,
            failureReason = reason,
        )
    }

    suspend fun markConflict(memoId: String, correctedSummary: String, targetFile: String, reason: String) {
        memoDao.updateResult(
            id = memoId,
            status = MemoStatus.CONFLICT,
            correctedSummary = correctedSummary,
            targetFile = targetFile,
            failureReason = reason,
        )
    }

    fun observeForProject(projectId: String): Flow<List<MemoEntity>> = memoDao.observeForProject(projectId)

    suspend fun getRecent(projectId: String, limit: Int): List<MemoEntity> = memoDao.getRecent(projectId, limit)

    fun observeFailed(): Flow<List<MemoEntity>> = memoDao.observeByStatus(MemoStatus.FAILED)

    suspend fun search(projectId: String, query: String): List<MemoEntity> = memoDao.search(projectId, query)

    suspend fun retry(memoId: String) {
        val memo = memoDao.getById(memoId) ?: return
        memoDao.updateResult(
            id = memo.id,
            status = MemoStatus.PENDING,
            correctedSummary = null,
            targetFile = null,
            failureReason = null,
        )
    }
}
