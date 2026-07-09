package com.wemeet.projectmemory.document

import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.Purpose

/**
 * Initial skeleton content for each Markdown file created when a project is
 * scaffolded (Phase 1) or when the LLM decides a brand-new document is
 * needed (Phase 5's DocumentManager). Purpose only changes which files get
 * created by default (Purpose.defaultDocTypes); the per-file skeleton below
 * is shared across purposes.
 */
object PurposeDocumentTemplates {

    fun initialContent(docType: DocType, projectName: String, purpose: Purpose): String = when (docType) {
        DocType.README -> """
            |# $projectName
            |
            |> ${purpose.description}
            |
        """.trimMargin()

        DocType.REQUIREMENTS -> """
            |# Requirements
            |
        """.trimMargin()

        DocType.TODO -> """
            |# Todo
            |
            |## Phase 1.
            |
            |- [ ]
            |
        """.trimMargin()

        DocType.ARCHITECTURE -> """
            |# Architecture
            |
        """.trimMargin()

        DocType.IDEAS -> """
            |# Ideas
            |
        """.trimMargin()

        DocType.DECISIONS -> """
            |# Decisions Log
            |
        """.trimMargin()

        DocType.MEETING -> """
            |# Meeting Notes
            |
        """.trimMargin()
    }
}
