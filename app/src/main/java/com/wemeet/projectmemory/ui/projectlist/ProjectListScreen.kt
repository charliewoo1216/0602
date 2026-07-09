package com.wemeet.projectmemory.ui.projectlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.ui.components.ProjectCard
import com.wemeet.projectmemory.ui.theme.AccentBlueEnd
import com.wemeet.projectmemory.ui.theme.AccentBlueStart
import com.wemeet.projectmemory.ui.theme.BgInputSurface
import com.wemeet.projectmemory.ui.theme.BgCardBorder
import com.wemeet.projectmemory.ui.theme.TextHeading
import com.wemeet.projectmemory.ui.theme.TextMuted

@Composable
fun ProjectListScreen(
    viewModel: ProjectListViewModel,
    onProjectClick: (Project) -> Unit,
    onCreateProjectClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFF05070A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProjectClick,
                containerColor = Color.Transparent,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AccentBlueStart, AccentBlueEnd))),
            ) {
                Text("＋", color = TextHeading, fontSize = 24.sp)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    text = "프로젝트 메모리",
                    color = TextHeading,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "// voice-driven project memory",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.pinned.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("📌 고정됨")
                    }
                    items(uiState.pinned, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectClick(project) },
                            onTogglePin = { viewModel.togglePinned(project) },
                        )
                    }
                }
                items(uiState.others, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectClick(project) },
                        onTogglePin = { viewModel.togglePinned(project) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF4B5563),
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BgInputSurface)
            .border(1.dp, BgCardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("🔍  프로젝트 검색...", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    inner()
                }
            },
        )
    }
}
