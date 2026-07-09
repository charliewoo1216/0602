package com.wemeet.projectmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.GlossaryStatus
import com.wemeet.projectmemory.data.model.GlossaryTerm
import com.wemeet.projectmemory.ui.theme.AccentGreen
import com.wemeet.projectmemory.ui.theme.BgChip
import com.wemeet.projectmemory.ui.theme.BgChipBorder
import com.wemeet.projectmemory.ui.theme.BgInputSurface
import com.wemeet.projectmemory.ui.theme.TextDim
import com.wemeet.projectmemory.ui.theme.TextMuted
import com.wemeet.projectmemory.ui.theme.TextPrimary

/**
 * Approved-term chips (with a ✕ to remove) plus dashed "AI 추천" chips for
 * terms found by the doc scan / repeat-detection (Phase 1-2) — tapping one
 * promotes it to approved.
 */
@Composable
fun GlossaryEditor(
    terms: List<GlossaryTerm>,
    onAddTerm: (String) -> Unit,
    onRemoveTerm: (GlossaryTerm) -> Unit,
    onApproveSuggestion: (GlossaryTerm) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newTerm by remember { mutableStateOf("") }
    val approved = terms.filter { it.status == GlossaryStatus.APPROVED }
    val suggested = terms.filter { it.status == GlossaryStatus.PENDING_APPROVAL }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowChipRow {
            approved.forEach { term ->
                ApprovedChip(term = term, onRemove = { onRemoveTerm(term) })
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgInputSurface)
                    .border(1.dp, Color(0xFF22262D), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                BasicTextField(
                    value = newTerm,
                    onValueChange = { newTerm = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    decorationBox = { inner ->
                        if (newTerm.isEmpty()) {
                            Text("용어 추가...", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        inner()
                    },
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgChip)
                    .border(1.dp, BgChipBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        if (newTerm.isNotBlank()) {
                            onAddTerm(newTerm.trim())
                            newTerm = ""
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("＋", color = TextMuted, fontSize = 16.sp)
            }
        }

        Text(
            text = "STT가 잘못 인식해도 이 목록을 참고해 LLM이 문맥상 보정합니다.",
            color = TextDim,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 10.dp),
        )

        if (suggested.isNotEmpty()) {
            Text(
                text = "✦ AI 추천 (문서 스캔 결과)",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
            FlowChipRow {
                suggested.forEach { term ->
                    SuggestedChip(term = term, onApprove = { onApproveSuggestion(term) })
                }
            }
        }
    }
}

@Composable
private fun FlowChipRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun ApprovedChip(term: GlossaryTerm, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgChip)
            .border(1.dp, BgChipBorder, RoundedCornerShape(16.dp))
            .padding(start = 11.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Text(term.term, color = TextPrimary, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace)
        Text(
            "✕",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun SuggestedChip(term: GlossaryTerm, onApprove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF4C5560), RoundedCornerShape(16.dp))
            .clickable(onClick = onApprove)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text("＋", color = AccentGreen, fontSize = 11.sp)
        Text(term.term, color = TextMuted, fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(start = 4.dp))
    }
}
