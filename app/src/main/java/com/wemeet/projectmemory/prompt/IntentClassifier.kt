package com.wemeet.projectmemory.prompt

import com.wemeet.projectmemory.llm.LlmSessionManager

data class ClassifiedIntent(val intent: MemoIntent, val glossaryTerm: String?)

/** Phase 4 step 1: is this STT text a document update, or a glossary command? */
class IntentClassifier(
    private val promptBuilder: PromptBuilder,
    private val llmSessionManager: LlmSessionManager,
    private val responseParser: ResponseParser,
) {
    suspend fun classify(sttText: String): ClassifiedIntent {
        val quickMatch = GLOSSARY_COMMAND_REGEX.find(sttText)
        if (quickMatch != null) {
            return ClassifiedIntent(MemoIntent.GLOSSARY_COMMAND, quickMatch.groupValues[1].trim())
        }

        val result = responseParser.parseWithRetry<IntentClassificationResult>(
            generate = { llmSessionManager.generateResponse(promptBuilder.buildIntentClassificationPrompt(sttText)) },
        )
        val intent = if (result?.intent == "glossary_command") MemoIntent.GLOSSARY_COMMAND else MemoIntent.DOCUMENT_UPDATE
        return ClassifiedIntent(intent, result?.glossaryTerm)
    }

    companion object {
        // Cheap pre-check for the common phrasing ("용어집에 ... 추가해줘") so the
        // obvious case doesn't need a full model round-trip.
        private val GLOSSARY_COMMAND_REGEX = Regex("용어집에\\s*(.+?)\\s*(추가|등록)")
    }
}
