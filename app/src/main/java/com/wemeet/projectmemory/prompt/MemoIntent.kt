package com.wemeet.projectmemory.prompt

import kotlinx.serialization.Serializable

/** Phase 4 step 1: is this memo meant to update a document, or add a glossary term? */
enum class MemoIntent {
    DOCUMENT_UPDATE,
    GLOSSARY_COMMAND,
}

@Serializable
data class IntentClassificationResult(
    val intent: String,
    val glossaryTerm: String? = null,
)

@Serializable
data class MemoApplyResult(
    val targetFile: String,
    val summary: String,
    val hasConflict: Boolean = false,
    val conflictReason: String? = null,
)

@Serializable
data class GlossaryScanResult(
    val terms: List<String> = emptyList(),
)
