package com.wemeet.projectmemory.di

import android.content.Context
import com.wemeet.projectmemory.data.local.config.ProjectConfigStore
import com.wemeet.projectmemory.data.local.config.ProjectFileScaffolder
import com.wemeet.projectmemory.data.local.room.AppDatabase
import com.wemeet.projectmemory.data.repository.MemoRepository
import com.wemeet.projectmemory.data.repository.ProjectRepository
import com.wemeet.projectmemory.document.DocumentManager
import com.wemeet.projectmemory.document.MemoNotifier
import com.wemeet.projectmemory.document.StorageManager
import com.wemeet.projectmemory.llm.LlmSessionManager
import com.wemeet.projectmemory.llm.download.ModelDownloadManager
import com.wemeet.projectmemory.prompt.GlossaryScanner
import com.wemeet.projectmemory.prompt.IntentClassifier
import com.wemeet.projectmemory.prompt.PromptBuilder
import com.wemeet.projectmemory.prompt.ResponseParser

/**
 * Hand-rolled service locator. A small, single-user app doesn't need a DI
 * framework's build-time codegen (Hilt/KSP) on top of Room's/Compose's — one
 * object graph built once in Application.onCreate is enough and is trivial
 * to read top to bottom.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val storageManager: StorageManager by lazy { StorageManager(appContext) }
    val configStore: ProjectConfigStore by lazy { ProjectConfigStore(appContext) }
    val scaffolder: ProjectFileScaffolder by lazy { ProjectFileScaffolder(storageManager, configStore) }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepository(database.projectDao(), configStore, scaffolder)
    }
    val memoRepository: MemoRepository by lazy { MemoRepository(database.memoDao()) }

    val modelDownloadManager: ModelDownloadManager by lazy { ModelDownloadManager(appContext) }
    val llmSessionManager: LlmSessionManager by lazy { LlmSessionManager(appContext, modelDownloadManager) }

    val promptBuilder: PromptBuilder by lazy { PromptBuilder() }
    val responseParser: ResponseParser by lazy { ResponseParser() }
    val intentClassifier: IntentClassifier by lazy { IntentClassifier(promptBuilder, llmSessionManager, responseParser) }
    val glossaryScanner: GlossaryScanner by lazy {
        GlossaryScanner(storageManager, promptBuilder, llmSessionManager, responseParser)
    }

    val memoNotifier: MemoNotifier by lazy { MemoNotifier(appContext) }

    val documentManager: DocumentManager by lazy {
        DocumentManager(
            storageManager = storageManager,
            projectRepository = projectRepository,
            memoRepository = memoRepository,
            promptBuilder = promptBuilder,
            responseParser = responseParser,
            intentClassifier = intentClassifier,
            llmSessionManager = llmSessionManager,
            notifier = memoNotifier,
        )
    }
}
