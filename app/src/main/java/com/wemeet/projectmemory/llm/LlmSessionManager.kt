package com.wemeet.projectmemory.llm

import android.content.Context
import com.wemeet.projectmemory.llm.download.ModelDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the (heavy) LlmEngine lifecycle: loads it lazily on first use
 * (Phase 3-2), and unloads it after a period of inactivity to give the
 * memory/battery back — a personal on-device app has no reason to keep a
 * multi-GB model resident indefinitely.
 */
class LlmSessionManager(
    private val context: Context,
    private val modelDownloadManager: ModelDownloadManager,
) {
    sealed interface State {
        data object Idle : State
        data object Loading : State
        data object Ready : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var engine: LlmEngine? = null
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun generateResponse(prompt: String): String = mutex.withLock {
        val activeEngine = ensureLoadedLocked()
        scheduleTimeoutLocked()
        activeEngine.generateResponse(prompt)
    }

    private suspend fun ensureLoadedLocked(): LlmEngine {
        engine?.let { return it }
        check(modelDownloadManager.isModelReady()) { "모델이 아직 다운로드되지 않았습니다." }

        _state.value = State.Loading
        return try {
            val created = GemmaLlmEngine.create(context, ModelDownloadManager.modelFile(context))
            engine = created
            _state.value = State.Ready
            created
        } catch (e: Exception) {
            _state.value = State.Error(e.message ?: "모델 로딩에 실패했습니다.")
            throw e
        }
    }

    private fun scheduleTimeoutLocked() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(SESSION_TIMEOUT_MS)
            mutex.withLock { unloadLocked() }
        }
    }

    fun unload() {
        scope.launch { mutex.withLock { unloadLocked() } }
    }

    private fun unloadLocked() {
        engine?.close()
        engine = null
        _state.value = State.Idle
    }

    companion object {
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
