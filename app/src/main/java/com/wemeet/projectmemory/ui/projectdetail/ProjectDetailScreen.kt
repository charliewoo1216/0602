package com.wemeet.projectmemory.ui.projectdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.DocType
import com.wemeet.projectmemory.ui.components.MarkdownPreview
import com.wemeet.projectmemory.ui.theme.AccentRedEnd
import com.wemeet.projectmemory.ui.theme.AccentRedStart
import com.wemeet.projectmemory.ui.theme.BgCardBorder
import com.wemeet.projectmemory.ui.theme.BgInputSurface
import com.wemeet.projectmemory.ui.theme.TextHeading
import com.wemeet.projectmemory.ui.theme.TextMuted
import com.wemeet.projectmemory.ui.theme.TextSecondary

@Composable
fun ProjectDetailScreen(
    viewModel: ProjectDetailViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Color(0xFF05070A)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                ) {
                    Text(
                        text = "←",
                        color = TextSecondary,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable(onClick = onBack).padding(end = 10.dp),
                    )
                    Text(
                        text = uiState.project?.name.orEmpty(),
                        color = TextHeading,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "⚙",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable(onClick = onOpenSettings),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                ) {
                    uiState.availableDocTypes.forEachIndexed { index, docType ->
                        DocTab(
                            docType = docType,
                            selected = docType == uiState.selectedDocType,
                            onClick = { viewModel.selectDocType(docType) },
                            modifier = Modifier.padding(end = if (index == uiState.availableDocTypes.lastIndex) 0.dp else 6.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0E1013))
                        .border(1.dp, Color(0xFF1B1F25), RoundedCornerShape(12.dp)),
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        if (uiState.docContent.isBlank()) {
                            Text("아직 내용이 없습니다. 음성으로 첫 메모를 남겨보세요.", color = TextMuted, fontSize = 11.5.sp)
                        } else {
                            MarkdownPreview(uiState.docContent)
                        }
                    }
                }
            }

            RecordButton(
                isRecording = uiState.isRecording,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 22.dp, top = 130.dp),
                onClick = {
                    val next = !uiState.isRecording
                    viewModel.setRecording(next)
                    onToggleOverlay(next)
                },
            )
        }
    }
}

@Composable
private fun DocTab(docType: DocType, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1A1E25) else BgInputSurface)
            .border(1.dp, if (selected) Color(0xFF333B47) else BgCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(docType.colorHex))),
        )
        Text(
            text = docType.label,
            color = if (selected) TextHeading else TextSecondary,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(
                if (isRecording) {
                    androidx.compose.ui.graphics.Brush.radialGradient(listOf(AccentRedStart, AccentRedEnd))
                } else {
                    androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF1A1D22), Color(0xFF1A1D22)))
                },
            )
            .border(2.dp, if (isRecording) Color(0xFFFF6B6B) else Color(0xFF3A4048), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("●", color = if (isRecording) Color.White else TextSecondary, fontSize = 20.sp)
    }
}
