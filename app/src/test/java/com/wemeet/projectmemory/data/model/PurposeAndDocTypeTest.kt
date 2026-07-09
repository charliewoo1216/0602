package com.wemeet.projectmemory.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurposeAndDocTypeTest {

    @Test
    fun `default purpose matches the mockup's pre-selected option`() {
        assertEquals(Purpose.CODING_PLAN, Purpose.default)
    }

    @Test
    fun `Purpose fromId round-trips for every entry`() {
        Purpose.entries.forEach { purpose ->
            assertEquals(purpose, Purpose.fromId(purpose.id))
        }
    }

    @Test
    fun `Purpose fromId falls back to the default for an unknown id`() {
        assertEquals(Purpose.default, Purpose.fromId("nonexistent"))
    }

    @Test
    fun `DocType fromId round-trips for every entry`() {
        DocType.entries.forEach { docType ->
            assertEquals(docType, DocType.fromId(docType.id))
        }
    }

    @Test
    fun `DocType fromId returns null for an unknown id`() {
        assertNull(DocType.fromId("nonexistent"))
    }

    @Test
    fun `CODING_PLAN default docs match todo md's Phase 1-1 structure`() {
        assertTrue(
            Purpose.CODING_PLAN.defaultDocTypes.containsAll(
                listOf(DocType.TODO, DocType.ARCHITECTURE, DocType.DECISIONS),
            ),
        )
    }

    @Test
    fun `TASK_PLANNING default docs are checkbox-only`() {
        assertEquals(listOf(DocType.TODO), Purpose.TASK_PLANNING.defaultDocTypes)
    }
}
