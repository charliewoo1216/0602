package com.wemeet.projectmemory.document

import com.wemeet.projectmemory.data.local.config.ProjectConfigStore
import com.wemeet.projectmemory.data.model.GlossarySource
import com.wemeet.projectmemory.data.model.GlossaryStatus
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.data.repository.MemoRepository
import com.wemeet.projectmemory.data.repository.ProjectRepository
import com.wemeet.projectmemory.llm.LlmSessionManager
import com.wemeet.projectmemory.prompt.IntentClassifier
import com.wemeet.projectmemory.prompt.MemoApplyResult
import com.wemeet.projectmemory.prompt.MemoIntent
import com.wemeet.projectmemory.prompt.PromptBuilder
import com.wemeet.projectmemory.prompt.ResponseParser

data class MemoProcessingResult(val success: Boolean, val userMessage: String)

/**
 * The Phase 4/5 pipeline end to end: STT text in, an applied (or failed)
 * Markdown file out. The model is handed the *entire* existing target file
 * and asked to return the entire corrected file back — that full-file
 * rewrite is deliberately the "merge" strategy (dedup/keep-existing/edit-
 * only-what's-needed lives in the prompt, not in a separate diff algorithm),
 * since only the model has enough context to do it well.
 */
class DocumentManager(
    private val storageManager: StorageManager,
    private val projectRepository: ProjectRepository,
    private val memoRepository: MemoRepository,
    private val promptBuilder: PromptBuilder,
    private val responseParser: ResponseParser,
    private val intentClassifier: IntentClassifier,
    private val llmSessionManager: LlmSessionManager,
    private val notifier: MemoNotifier,
) {
    suspend fun processVoiceMemo(projectId: String, sttText: String): MemoProcessingResult {
        val project = projectRepository.getProject(projectId)
            ?: return MemoProcessingResult(false, "프로젝트를 찾을 수 없습니다.")
        val memoId = memoRepository.recordPending(projectId, sttText)

        return try {
            val classified = intentClassifier.classify(sttText)
            when (classified.intent) {
                MemoIntent.GLOSSARY_COMMAND -> applyGlossaryCommand(project, memoId, classified.glossaryTerm ?: sttText)
                MemoIntent.DOCUMENT_UPDATE -> applyDocumentUpdate(project, memoId, sttText)
            }
        } catch (e: Exception) {
            memoRepository.markFailed(memoId, e.message ?: "처리 중 오류가 발생했습니다.")
            MemoProcessingResult(false, "메모 처리에 실패했어요. 잠시 후 다시 시도해주세요.")
        }
    }

    private suspend fun applyGlossaryCommand(project: Project, memoId: String, term: String): MemoProcessingResult {
        val updatedGlossary = project.glossary + GlossaryTerm(
            term = term,
            status = GlossaryStatus.APPROVED,
            source = GlossarySource.VOICE_COMMAND,
        )
        projectRepository.saveSettings(project.id, project.purpose, updatedGlossary)
        memoRepository.markApplied(memoId, correctedSummary = "용어집에 '$term' 추가", targetFile = ProjectConfigStore.CONFIG_FILE_NAME)
        notifier.notifyApplied(project.name, "용어집에 '$term' 추가됨")
        return MemoProcessingResult(true, "용어집에 '$term'을(를) 추가했어요.")
    }

    private suspend fun applyDocumentUpdate(project: Project, memoId: String, sttText: String): MemoProcessingResult {
        val availableDocs = project.purpose.defaultDocTypes
        val existingDocsContent = availableDocs.associateWith { docType ->
            storageManager.readFile(project.folderUri, docType.fileName).orEmpty()
        }

        val result = responseParser.parseWithRetry<MemoApplyResult>(
            onRawTextFallback = { raw -> memoRepository.markFailed(memoId, "JSON 파싱 실패. 원문: ${raw.take(500)}") },
            generate = {
                llmSessionManager.generateResponse(
                    promptBuilder.buildDocumentUpdatePrompt(
                        projectName = project.name,
                        purpose = project.purpose,
                        glossary = project.glossary,
                        availableDocs = availableDocs,
                        existingDocsContent = existingDocsContent,
                        sttText = sttText,
                    ),
                )
            },
        ) ?: return MemoProcessingResult(false, "메모를 문서에 반영하지 못했어요. 다시 시도해주세요.")

        val docType = availableDocs.find { it.fileName == result.targetFile } ?: availableDocs.first()
        storageManager.writeFile(project.folderUri, docType.fileName, result.summary)

        val recentTypes = (listOf(docType) + project.recentDocTypes.filterNot { it == docType }).take(3)
        projectRepository.updatePreview(project.id, previewOf(result.summary), recentTypes)

        return if (result.hasConflict) {
            memoRepository.markConflict(memoId, result.summary, docType.fileName, result.conflictReason ?: "기존 내용과 상충할 수 있습니다.")
            notifier.notifyApplied(project.name, "${docType.fileName}에 반영됨 (확인 필요)")
            MemoProcessingResult(true, "${docType.label}에 반영했지만 기존 내용과 상충할 수 있어요.")
        } else {
            memoRepository.markApplied(memoId, result.summary, docType.fileName)
            notifier.notifyApplied(project.name, "${docType.fileName}에 반영됨")
            MemoProcessingResult(true, "${docType.label}에 반영했어요.")
        }
    }

    private fun previewOf(fullContent: String): String =
        fullContent.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") }?.take(80)
            ?: fullContent.take(80)
}
