package com.wemeet.projectmemory.ui.projectlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectListUiState(
    val pinned: List<Project> = emptyList(),
    val others: List<Project> = emptyList(),
    val searchQuery: String = "",
)

class ProjectListViewModel(private val repository: ProjectRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val projectsFlow = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.observeProjects() else repository.searchProjects(query)
    }

    val uiState: StateFlow<ProjectListUiState> = combine(projectsFlow, searchQuery) { projects, query ->
        ProjectListUiState(
            pinned = projects.filter { it.pinned },
            others = projects.filterNot { it.pinned },
            searchQuery = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectListUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun togglePinned(project: Project) {
        viewModelScope.launch { repository.setPinned(project.id, !project.pinned) }
    }
}
