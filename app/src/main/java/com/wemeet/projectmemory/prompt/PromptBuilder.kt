package com.wemeet.projectmemory.prompt

import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Purpose

/**
 * Builds every prompt sent to the on-device model (Phase 4). Every prompt
 * ends with an explicit "JSON only" instruction plus a matching example,
 * because a small on-device model left to freeform-answer will happily
 * wrap its JSON in commentary otherwise.
 */
class PromptBuilder {

    fun buildIntentClassificationPrompt(sttText: String): String = """
        |당신은 음성 메모 앱의 의도 분류기입니다. 아래 발화가 "문서 반영"(project document_update)인지,
        |"용어집에 새 용어를 추가해달라"는 명령(glossary_command)인지 판단하세요.
        |
        |${PurposePromptTemplates.STT_CORRECTION_INSTRUCTION}
        |
        |반드시 아래 JSON 형식으로만 답하세요. 다른 설명은 출력하지 마세요.
        |예시: {"intent": "glossary_command", "glossaryTerm": "트랜스포머 아키텍처"}
        |예시: {"intent": "document_update", "glossaryTerm": null}
        |
        |발화: "$sttText"
    """.trimMargin()

    fun buildDocumentUpdatePrompt(
        projectName: String,
        purpose: Purpose,
        glossary: List<GlossaryTerm>,
        availableDocs: List<DocType>,
        existingDocsContent: Map<DocType, String>,
        sttText: String,
    ): String {
        val docsSection = availableDocs.joinToString("\n\n") { docType ->
            val content = existingDocsContent[docType].orEmpty().ifBlank { "(비어 있음)" }
            "### ${docType.fileName}\n$content"
        }
        val glossarySection = if (glossary.isEmpty()) {
            "(등록된 용어 없음)"
        } else {
            glossary.joinToString(", ") { it.term }
        }

        return """
            |당신은 "$projectName" 프로젝트의 Markdown 문서를 관리하는 어시스턴트입니다.
            |
            |${PurposePromptTemplates.STT_CORRECTION_INSTRUCTION}
            |
            |용어집(오인식 보정용 힌트): $glossarySection
            |
            |문서 구조 규칙: ${PurposePromptTemplates.structureInstruction(purpose)}
            |
            |현재 문서들:
            |$docsSection
            |
            |아래 발화 내용을 가장 적절한 문서 하나에 반영하세요. 기존 내용은 유지하고 중복 없이 필요한 부분만
            |추가/수정하세요. 이미 있는 내용과 명백히 모순되면 hasConflict를 true로 표시하고 이유를 적으세요.
            |
            |반드시 아래 JSON 형식으로만 답하세요. summary는 targetFile에 실제로 반영될 전체 Markdown 본문입니다.
            |예시: {"targetFile": "todo.md", "summary": "# Todo\n\n## Phase 1.\n\n- [ ] 예시 항목", "hasConflict": false, "conflictReason": null}
            |
            |발화: "$sttText"
        """.trimMargin()
    }

    fun buildGlossaryScanPrompt(documentsText: String, existingTerms: List<String>): String = """
        |아래는 한 프로젝트의 Markdown 문서 전체입니다. 기술 용어, 라이브러리/프레임워크 이름,
        |고유명사 후보를 찾아주세요. 이미 등록된 용어(${existingTerms.joinToString(", ").ifEmpty { "없음" }})는 제외하세요.
        |
        |반드시 아래 JSON 형식으로만 답하세요.
        |예시: {"terms": ["Kafka", "LangGraph"]}
        |
        |문서:
        |$documentsText
    """.trimMargin()
}
