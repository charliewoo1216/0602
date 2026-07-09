package com.wemeet.projectmemory.data.model

/**
 * Lifecycle of a captured voice memo. Written PENDING immediately after STT
 * so nothing is lost even if the LLM step fails; the file (not this row) is
 * still the source of truth once APPLIED.
 */
enum class MemoStatus {
    PENDING,
    APPLIED,
    FAILED,
    CONFLICT,
}
