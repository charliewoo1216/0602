package com.wemeet.projectmemory.llm

/**
 * On-device inference boundary (Phase 3). Kept as a small interface —
 * separate from LlmSessionManager, which owns lazy loading/timeout — so a
 * different runtime (a bigger/smaller Gemma variant, a different backend)
 * can be swapped in without touching PromptBuilder/DocumentManager.
 */
interface LlmEngine : AutoCloseable {
    suspend fun generateResponse(prompt: String): String
}
