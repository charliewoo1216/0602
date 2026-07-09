package com.wemeet.projectmemory.ui.projectsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.ui.components.GlossaryEditor
import com.wemeet.projectmemory.ui.components.PurposeSelector
import com.wemeet.projectmemory.ui.theme.AccentBlueEnd
import com.wemeet.projectmemory.ui.theme.AccentBlueStart
import com.wemeet.projectmemory.ui.theme.BgCard
import com.wemeet.projectmemory.ui.theme.BgCardBorder
import com.wemeet.projectmemory.ui.theme.TextHeading
import com.wemeet.projectmemory.ui.theme.TextMuted
import com.wemeet.projectmemory.ui.theme.TextSecondary

@Composable
fun ProjectSettingsScreen(
    viewModel: ProjectSettingsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Color(0xFF05070A)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
            ) {
                Text(
                    text = "←",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable(onClick = onBack).padding(end = 10.dp),
                )
                Text(
                    text = "설정 · 음성",
                    color = TextHeading,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            SectionLabel("목적 (Purpose)", topPadding = 20.dp)
            PurposeSelector(selected = uiState.purpose, onSelect = viewModel::onPurposeSelected)

            SectionLabel("용어집 (Glossary)")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                GlossaryEditor(
                    terms = uiState.glossary,
                    onAddTerm = viewModel::addTerm,
                    onRemoveTerm = viewModel::removeTerm,
                    onApproveSuggestion = viewModel::approveSuggestion,
                )

                Text(
                    text = if (uiState.isScanning) "🔍 스캔 중..." else "🔍 문서에서 용어 다시 스캔하기",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF333B47), RoundedCornerShape(10.dp))
                        .clickable(enabled = !uiState.isScanning, onClick = viewModel::scanDocumentsForTerms)
                        .padding(10.dp),
                )
            }

            Text(
                text = if (uiState.isSaved) "저장됨 ✓" else "저장",
                color = TextHeading,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(AccentBlueStart, AccentBlueEnd)))
                    .clickable(onClick = viewModel::save)
                    .padding(13.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 20.dp) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = topPadding, bottom = 10.dp),
    )
}
