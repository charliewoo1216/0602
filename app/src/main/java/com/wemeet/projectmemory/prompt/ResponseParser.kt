package com.wemeet.projectmemory.prompt

import kotlinx.serialization.json.Json

/**
 * Turns "the model was asked to output JSON" into an actual typed value,
 * tolerating the model wrapping its answer in prose or a ```json fence
 * (Phase 4). Retrying re-runs the whole generation, not just the parse,
 * since a malformed response usually means the prompt needs another shot at
 * the model, not that the same broken text needs re-reading.
 */
class ResponseParser {

    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    suspend inline fun <reified T> parseWithRetry(
        maxAttempts: Int = 2,
        crossinline onRawTextFallback: suspend (String) -> Unit,
        crossinline generate: suspend () -> String,
    ): T? {
        var lastRaw: String? = null
        repeat(maxAttempts) {
            val raw = generate()
            lastRaw = raw
            val block = extractJsonBlock(raw)
            val parsed = try {
                json.decodeFromString<T>(block)
            } catch (e: Exception) {
                null
            }
            if (parsed != null) return parsed
        }
        lastRaw?.let { onRawTextFallback(it) }
        return null
    }

    fun extractJsonBlock(raw: String): String {
        val fenced = FENCE_REGEX.find(raw)?.groupValues?.get(1)
        val candidate = (fenced ?: raw).trim()
        val start = candidate.indexOfFirst { it == '{' || it == '[' }
        if (start == -1) return candidate

        val open = candidate[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        for (i in start until candidate.length) {
            when (candidate[i]) {
                open -> depth++
                close -> depth--
            }
            if (depth == 0) return candidate.substring(start, i + 1)
        }
        return candidate.substring(start)
    }

    companion object {
        private val FENCE_REGEX = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
    }
}
