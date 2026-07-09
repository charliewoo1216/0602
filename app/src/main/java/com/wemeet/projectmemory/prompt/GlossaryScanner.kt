package com.wemeet.projectmemory.prompt

import android.net.Uri
import com.wemeet.projectmemory.data.model.GlossarySource
import com.wemeet.projectmemory.data.model.GlossaryStatus
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.document.StorageManager
import com.wemeet.projectmemory.llm.LlmSessionManager

/** Phase 1-2: "문서에서 용어 스캔하기" — reads every .md file and asks the LLM for candidate glossary terms. */
class GlossaryScanner(
    private val storageManager: StorageManager,
    private val promptBuilder: PromptBuilder,
    private val llmSessionManager: LlmSessionManager,
    private val responseParser: ResponseParser,
) {
    suspend fun scan(folderUri: Uri, existingTerms: List<GlossaryTerm>): List<GlossaryTerm> {
        val files = storageManager.listMarkdownFiles(folderUri)
        val combinedText = files.mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            storageManager.readFile(folderUri, name)?.let { "### $name\n$it" }
        }.joinToString("\n\n")

        if (combinedText.isBlank()) return emptyList()

        val existingLower = existingTerms.map { it.term.lowercase() }.toSet()
        val result = responseParser.parseWithRetry<GlossaryScanResult>(
            generate = {
                llmSessionManager.generateResponse(
                    promptBuilder.buildGlossaryScanPrompt(combinedText, existingTerms.map { it.term }),
                )
            },
        )

        return result?.terms
            .orEmpty()
            .filter { it.isNotBlank() && it.lowercase() !in existingLower }
            .distinct()
            .map { term ->
                GlossaryTerm(term = term, status = GlossaryStatus.PENDING_APPROVAL, source = GlossarySource.LLM_DOCUMENT_SCAN)
            }
    }
}
