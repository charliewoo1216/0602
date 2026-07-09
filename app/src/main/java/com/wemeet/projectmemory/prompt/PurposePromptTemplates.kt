package com.wemeet.projectmemory.prompt

import com.wemeet.projectmemory.data.model.Purpose

/**
 * Phase 4-1: a project's purpose decides which structural rules the LLM is told to enforce
 * when it rewrites a document. Only changes the *instruction text* — the
 * actual file chosen and content produced still come from the model.
 */
object PurposePromptTemplates {

    fun structureInstruction(purpose: Purpose): String = when (purpose) {
        Purpose.TASK_PLANNING ->
            "체크박스(- [ ]) 중심으로 정리하세요. 발화에 마감일(날짜/기한)이 언급되면 항목 끝에 표기하고, 없으면 표기하지 마세요."
        Purpose.CODING_PLAN ->
            "Phase와 Task 구조(## Phase N. 제목, 하위 체크박스)를 유지하세요. 우선순위, 의존관계(선행 작업), " +
                "완료조건이 드러나면 함께 반영하세요. 기존 Phase 번호 체계를 바꾸지 마세요."
        Purpose.MEETING_NOTES ->
            "일시, 참석자, 논의사항, 액션아이템 섹션 구조를 유지하세요. 발화에서 결정된 사항은 논의사항에, " +
                "누군가 해야 할 일은 액션아이템에 담당자와 함께 정리하세요."
        Purpose.BRAINSTORMING ->
            "자유 서술 형식으로 정리하되, 관련된 카테고리 태그를 함께 붙이세요. 구조를 강제하지 마세요."
        Purpose.FREEFORM ->
            "특별한 구조를 강제하지 마세요. 문맥에 맞게 가장 자연스러운 방식으로 정리하세요."
    }

    /** Shared across every purpose — STT is unreliable, so always say so explicitly. */
    const val STT_CORRECTION_INSTRUCTION =
        "입력 텍스트는 음성 인식(STT) 결과이므로 오타, 단어 누락, 발음 유사 오인식이 있을 수 있습니다. " +
            "문맥과 아래 용어집을 참고해 의도를 추론하고, 명백한 오인식은 자연스럽게 보정하세요. " +
            "확신이 없는 내용을 지어내지 마세요."
}
