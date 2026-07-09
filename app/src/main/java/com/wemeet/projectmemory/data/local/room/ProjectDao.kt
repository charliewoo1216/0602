package com.wemeet.projectmemory.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wemeet.projectmemory.data.model.DocType
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM project_index ORDER BY pinned DESC, lastUpdatedEpochMillis DESC")
    fun observeAll(): Flow<List<ProjectIndexEntity>>

    @Query(
        "SELECT * FROM project_index WHERE name LIKE '%' || :query || '%' " +
            "OR previewText LIKE '%' || :query || '%' " +
            "ORDER BY pinned DESC, lastUpdatedEpochMillis DESC",
    )
    fun search(query: String): Flow<List<ProjectIndexEntity>>

    @Query("SELECT * FROM project_index WHERE id = :id")
    suspend fun getById(id: String): ProjectIndexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectIndexEntity)

    @Update
    suspend fun update(entity: ProjectIndexEntity)

    @Query("UPDATE project_index SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query(
        "UPDATE project_index SET previewText = :previewText, " +
            "lastUpdatedEpochMillis = :updatedAt, recentDocTypes = :recentDocTypes WHERE id = :id",
    )
    suspend fun updatePreview(id: String, previewText: String, updatedAt: Long, recentDocTypes: List<DocType>)

    @Delete
    suspend fun delete(entity: ProjectIndexEntity)

    @Query("DELETE FROM project_index")
    suspend fun clear()
}
