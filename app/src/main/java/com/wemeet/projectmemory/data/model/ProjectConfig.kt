package com.wemeet.projectmemory.data.model

import kotlinx.serialization.Serializable

/**
 * Canonical, portable project metadata persisted as `project.json` inside the
 * project's own folder (see ProjectConfigStore). This — together with the
 * `.md` files next to it — is the source of truth; the Room index
 * (ProjectIndexEntity) is only a rebuildable cache for fast list queries.
 *
 * Changing [purposeId] does not migrate already-written documents; it only
 * changes which template/prompt is used for memos applied afterwards
 * (per the "설정 변경 시... 이후 반영분부터 적용" decision).
 */
@Serializable
data class ProjectConfig(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val colorHex: String,
    val purposeId: String = Purpose.default.id,
    val glossary: List<GlossaryTerm> = emptyList(),
    val pinned: Boolean = false,
    val createdAtEpochMillis: Long,
) {
    val purpose: Purpose get() = Purpose.fromId(purposeId)
}
