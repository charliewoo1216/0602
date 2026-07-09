package com.wemeet.projectmemory.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LiteRT-LM (Gemma-4-E4B) inference via MediaPipe's `tasks-genai`. Fully
 * on-device, no network calls — this is the whole point of the app (see
 * decisions log: "STT 오류를 LLM이 문맥/용어집으로 보정").
 */
class GemmaLlmEngine private constructor(
    private val llmInference: LlmInference,
) : LlmEngine {

    override suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.Default) {
        llmInference.generateResponse(prompt)
    }

    override fun close() {
        llmInference.close()
    }

    companion object {
        private const val DEFAULT_MAX_TOKENS = 1024

        suspend fun create(context: Context, modelFile: File): GemmaLlmEngine = withContext(Dispatchers.Default) {
            check(modelFile.exists()) { "Model file not found at ${modelFile.absolutePath}; download it first." }
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(DEFAULT_MAX_TOKENS)
                .build()
            GemmaLlmEngine(LlmInference.createFromOptions(context, options))
        }
    }
}
