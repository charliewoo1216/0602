package com.wemeet.projectmemory.ui.projectsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wemeet.projectmemory.data.model.GlossaryStatus
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.data.model.Purpose
import com.wemeet.projectmemory.data.repository.ProjectRepository
import com.wemeet.projectmemory.prompt.GlossaryScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectSettingsUiState(
    val projectName: String = "",
    val purpose: Purpose = Purpose.default,
    val glossary: List<GlossaryTerm> = emptyList(),
    val isScanning: Boolean = false,
    val isSaved: Boolean = false,
)

class ProjectSettingsViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    private val glossaryScanner: GlossaryScanner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectSettingsUiState())
    val uiState: StateFlow<ProjectSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getProject(projectId)?.let { project ->
                _uiState.value = ProjectSettingsUiState(
                    projectName = project.name,
                    purpose = project.purpose,
                    glossary = project.glossary,
                )
            }
        }
    }

    fun onPurposeSelected(purpose: Purpose) {
        _uiState.value = _uiState.value.copy(purpose = purpose, isSaved = false)
    }

    fun addTerm(term: String) {
        val current = _uiState.value.glossary
        if (current.any { it.term.equals(term, ignoreCase = true) }) return
        _uiState.value = _uiState.value.copy(
            glossary = current + GlossaryTerm(term = term, status = GlossaryStatus.APPROVED),
            isSaved = false,
        )
    }

    fun removeTerm(term: GlossaryTerm) {
        _uiState.value = _uiState.value.copy(
            glossary = _uiState.value.glossary.filterNot { it == term },
            isSaved = false,
        )
    }

    fun approveSuggestion(term: GlossaryTerm) {
        _uiState.value = _uiState.value.copy(
            glossary = _uiState.value.glossary.map {
                if (it == term) it.copy(status = GlossaryStatus.APPROVED) else it
            },
            isSaved = false,
        )
    }

    /** Phase 1-2: "문서에서 용어 스캔하기" — LLM reads the project's .md files for candidate terms. */
    fun scanDocumentsForTerms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val project = repository.getProject(projectId)
            if (project != null) {
                val suggestions = glossaryScanner.scan(project.folderUri, existingTerms = _uiState.value.glossary)
                _uiState.value = _uiState.value.copy(
                    glossary = _uiState.value.glossary + suggestions,
                    isScanning = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            repository.saveSettings(projectId, _uiState.value.purpose, _uiState.value.glossary)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
