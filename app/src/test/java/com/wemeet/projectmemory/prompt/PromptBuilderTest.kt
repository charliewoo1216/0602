package com.wemeet.projectmemory.prompt

import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Purpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val builder = PromptBuilder()

    @Test
    fun `every purpose produces a distinct structure instruction`() {
        val prompts = Purpose.entries.associateWith { purpose ->
            builder.buildDocumentUpdatePrompt(
                projectName = "테스트프로젝트",
                purpose = purpose,
                glossary = emptyList(),
                availableDocs = purpose.defaultDocTypes,
                existingDocsContent = emptyMap(),
                sttText = "발화 예시",
            )
        }
        assertEquals(Purpose.entries.size, prompts.size)
        assertEquals("purposes should not share an identical prompt", Purpose.entries.size, prompts.values.toSet().size)
    }

    @Test
    fun `document update prompt always includes the STT-correction instruction`() {
        Purpose.entries.forEach { purpose ->
            val prompt = builder.buildDocumentUpdatePrompt(
                projectName = "P",
                purpose = purpose,
                glossary = emptyList(),
                availableDocs = purpose.defaultDocTypes,
                existingDocsContent = emptyMap(),
                sttText = "x",
            )
            assertTrue(prompt.contains("음성 인식(STT) 결과"))
        }
    }

    @Test
    fun `document update prompt embeds glossary terms and existing doc content`() {
        val prompt = builder.buildDocumentUpdatePrompt(
            projectName = "P",
            purpose = Purpose.CODING_PLAN,
            glossary = listOf(GlossaryTerm("Kafka"), GlossaryTerm("LangGraph")),
            availableDocs = Purpose.CODING_PLAN.defaultDocTypes,
            existingDocsContent = mapOf(DocType.TODO to "# Todo\n\n- [ ] 기존 항목"),
            sttText = "발화",
        )
        assertTrue(prompt.contains("Kafka"))
        assertTrue(prompt.contains("LangGraph"))
        assertTrue(prompt.contains("기존 항목"))
    }

    @Test
    fun `intent classification prompt includes the utterance and both intent labels`() {
        val prompt = builder.buildIntentClassificationPrompt("용어집에 트랜스포머 추가해줘")
        assertTrue(prompt.contains("용어집에 트랜스포머 추가해줘"))
        assertTrue(prompt.contains("glossary_command"))
        assertTrue(prompt.contains("document_update"))
    }

    @Test
    fun `glossary scan prompt excludes already-known terms from the exclusion list text`() {
        val prompt = builder.buildGlossaryScanPrompt("some doc text mentioning Kafka and Redis", existingTerms = listOf("Kafka"))
        assertTrue(prompt.contains("Kafka"))
    }
}
