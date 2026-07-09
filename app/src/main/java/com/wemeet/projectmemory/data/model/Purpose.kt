package com.wemeet.projectmemory.data.model

/**
 * A project's stated purpose. Drives which Markdown files get scaffolded on
 * creation (see [PurposeDocumentTemplates]) and which system-prompt template
 * PromptBuilder picks in Phase 4.
 */
enum class Purpose(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultDocTypes: List<DocType>,
) {
    TASK_PLANNING(
        id = "task_planning",
        displayName = "할일 계획",
        description = "체크박스 중심, 마감일 판단",
        defaultDocTypes = listOf(DocType.TODO),
    ),
    CODING_PLAN(
        id = "coding_plan",
        displayName = "코딩용 plan.md",
        description = "Phase/Task, 의존관계, 완료조건 구조화",
        defaultDocTypes = listOf(
            DocType.README,
            DocType.REQUIREMENTS,
            DocType.TODO,
            DocType.ARCHITECTURE,
            DocType.DECISIONS,
        ),
    ),
    MEETING_NOTES(
        id = "meeting_notes",
        displayName = "회의록",
        description = "일시/참석자/논의사항/액션아이템",
        defaultDocTypes = listOf(DocType.MEETING, DocType.DECISIONS),
    ),
    BRAINSTORMING(
        id = "brainstorming",
        displayName = "아이디어 브레인스토밍",
        description = "자유 서술 + 카테고리 태그",
        defaultDocTypes = listOf(DocType.IDEAS),
    ),
    FREEFORM(
        id = "freeform",
        displayName = "자유 형식",
        description = "구조 강제 없이 LLM 판단에 위임",
        defaultDocTypes = listOf(DocType.README),
    );

    companion object {
        val default: Purpose = CODING_PLAN

        fun fromId(id: String): Purpose = entries.find { it.id == id } ?: default
    }
}
