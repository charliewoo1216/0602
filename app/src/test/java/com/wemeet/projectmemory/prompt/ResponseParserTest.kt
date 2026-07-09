package com.wemeet.projectmemory.prompt

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ResponseParserTest {

    private val parser = ResponseParser()

    @Test
    fun `extractJsonBlock returns a plain object as-is`() {
        assertEquals("""{"a":1}""", parser.extractJsonBlock("""{"a":1}"""))
    }

    @Test
    fun `extractJsonBlock strips a markdown json fence`() {
        val fenced = "여기 결과입니다:\n```json\n{\"targetFile\": \"todo.md\", \"summary\": \"x\"}\n```\n감사합니다."
        assertEquals("""{"targetFile": "todo.md", "summary": "x"}""", parser.extractJsonBlock(fenced))
    }

    @Test
    fun `extractJsonBlock strips leading and trailing prose without a fence`() {
        val prosed = "Sure, here's the answer: {\"intent\": \"document_update\"} - hope that helps!"
        assertEquals("""{"intent": "document_update"}""", parser.extractJsonBlock(prosed))
    }

    @Test
    fun `extractJsonBlock balances nested braces correctly`() {
        val nested = """{"targetFile":"a.md","summary":"# H\n\n{nested-looking text}","hasConflict":false}"""
        assertEquals(nested, parser.extractJsonBlock(nested))
    }

    @Test
    fun `extractJsonBlock handles a top-level array`() {
        assertEquals("""["Kafka", "MCP"]""", parser.extractJsonBlock("prefix [\"Kafka\", \"MCP\"] suffix"))
    }

    @Test
    fun `parseWithRetry succeeds once the model returns valid JSON`() = runBlocking {
        var callCount = 0
        var fallbackCalled = false
        val result = parser.parseWithRetry<IntentClassificationResult>(
            maxAttempts = 2,
            onRawTextFallback = { fallbackCalled = true },
            generate = {
                callCount++
                if (callCount == 1) "this is not json at all" else """{"intent": "glossary_command", "glossaryTerm": "Kafka"}"""
            },
        )
        assertEquals("glossary_command", result?.intent)
        assertEquals(2, callCount)
        assertFalse(fallbackCalled)
    }

    @Test
    fun `parseWithRetry returns null and logs the raw text once attempts are exhausted`() = runBlocking {
        var fallbackText: String? = null
        val result = parser.parseWithRetry<MemoApplyResult>(
            maxAttempts = 2,
            onRawTextFallback = { raw -> fallbackText = raw },
            generate = { "still not json" },
        )
        assertNull(result)
        assertEquals("still not json", fallbackText)
    }

    @Test
    fun `parseWithRetry decodes a glossary scan result`() = runBlocking {
        val result = parser.parseWithRetry<GlossaryScanResult>(
            onRawTextFallback = {},
            generate = { """{"terms": ["Kafka", "LangGraph"]}""" },
        )
        assertEquals(listOf("Kafka", "LangGraph"), result?.terms)
    }
}
