package com.wemeet.projectmemory.data.model

import kotlinx.serialization.Serializable

/**
 * Where a glossary entry came from. [PENDING_APPROVAL] terms show up as
 * dashed "suggested" chips in the settings screen until the user taps them.
 */
@Serializable
enum class GlossarySource {
    MANUAL,
    LLM_DOCUMENT_SCAN,
    VOICE_COMMAND,
    AUTO_DETECTED_REPEAT,
}

@Serializable
enum class GlossaryStatus {
    APPROVED,
    PENDING_APPROVAL,
}

@Serializable
data class GlossaryTerm(
    val term: String,
    val status: GlossaryStatus = GlossaryStatus.APPROVED,
    val source: GlossarySource = GlossarySource.MANUAL,
)
