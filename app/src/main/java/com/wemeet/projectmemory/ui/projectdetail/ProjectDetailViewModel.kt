package com.wemeet.projectmemory.ui.projectdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wemeet.projectmemory.data.local.room.MemoEntity
import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.data.repository.MemoRepository
import com.wemeet.projectmemory.data.repository.ProjectRepository
import com.wemeet.projectmemory.document.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val project: Project? = null,
    val availableDocTypes: List<DocType> = emptyList(),
    val selectedDocType: DocType? = null,
    val docContent: String = "",
    val isRecording: Boolean = false,
)

class ProjectDetailViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val storageManager: StorageManager,
    memoRepository: MemoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    val recentMemos: StateFlow<List<MemoEntity>> = memoRepository.observeForProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId) ?: return@launch
            val availableDocTypes = project.purpose.defaultDocTypes
            _uiState.value = _uiState.value.copy(project = project, availableDocTypes = availableDocTypes)
            (project.recentDocTypes.firstOrNull() ?: availableDocTypes.firstOrNull())?.let(::selectDocType)
        }
    }

    fun selectDocType(docType: DocType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedDocType = docType)
            val project = _uiState.value.project ?: return@launch
            val content = storageManager.readFile(project.folderUri, docType.fileName).orEmpty()
            _uiState.value = _uiState.value.copy(docContent = content)
        }
    }

    fun setRecording(recording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = recording)
    }

    /** Called after the overlay/STT+LLM pipeline (Phase 2-5) applies a memo to disk. */
    fun refreshCurrentDocument() {
        _uiState.value.selectedDocType?.let(::selectDocType)
    }
}
