package com.wemeet.projectmemory.data.local.room

import androidx.room.TypeConverter
import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.MemoStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun docTypesToString(value: List<DocType>): String = value.joinToString(",") { it.id }

    @TypeConverter
    fun stringToDocTypes(value: String): List<DocType> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull(DocType::fromId)

    @TypeConverter
    fun memoStatusToString(value: MemoStatus): String = value.name

    @TypeConverter
    fun stringToMemoStatus(value: String): MemoStatus = MemoStatus.valueOf(value)

    @TypeConverter
    fun glossaryToString(value: List<GlossaryTerm>): String =
        if (value.isEmpty()) "" else json.encodeToString(value)

    @TypeConverter
    fun stringToGlossary(value: String): List<GlossaryTerm> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
