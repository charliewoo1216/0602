package com.wemeet.projectmemory.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wemeet.projectmemory.data.model.MemoStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MemoEntity)

    @Query("SELECT * FROM memos WHERE projectId = :projectId ORDER BY timestampEpochMillis DESC")
    fun observeForProject(projectId: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE projectId = :projectId ORDER BY timestampEpochMillis DESC LIMIT :limit")
    suspend fun getRecent(projectId: String, limit: Int): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE status = :status ORDER BY timestampEpochMillis DESC")
    fun observeByStatus(status: MemoStatus): Flow<List<MemoEntity>>

    @Query(
        "SELECT * FROM memos WHERE projectId = :projectId AND " +
            "(rawSttText LIKE '%' || :query || '%' OR correctedSummary LIKE '%' || :query || '%') " +
            "ORDER BY timestampEpochMillis DESC",
    )
    suspend fun search(projectId: String, query: String): List<MemoEntity>

    @Query(
        "UPDATE memos SET status = :status, correctedSummary = :correctedSummary, " +
            "targetFile = :targetFile, failureReason = :failureReason WHERE id = :id",
    )
    suspend fun updateResult(
        id: String,
        status: MemoStatus,
        correctedSummary: String?,
        targetFile: String?,
        failureReason: String?,
    )

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: String): MemoEntity?
}
