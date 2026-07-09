package com.wemeet.projectmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.ui.theme.BgCard
import com.wemeet.projectmemory.ui.theme.BgCardBorder
import com.wemeet.projectmemory.ui.theme.TextFaint
import com.wemeet.projectmemory.ui.theme.TextPrimary
import com.wemeet.projectmemory.ui.theme.TextSecondary
import java.util.concurrent.TimeUnit

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onTogglePin)
            .padding(12.dp, 12.dp, 12.dp, 10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = project.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 14.dp),
            )
            if (project.recentDocTypes.isNotEmpty()) {
                DiffStripe(project.recentDocTypes, modifier = Modifier.padding(top = 7.dp, bottom = 6.dp))
            }
            if (project.previewText.isNotBlank()) {
                Text(
                    text = project.previewText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = metaLine(project),
                color = TextFaint,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (project.pinned) {
            Text(
                text = "📌",
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

private fun metaLine(project: Project): String {
    val relative = relativeTime(project.lastUpdatedEpochMillis)
    val recentFile = project.recentDocTypes.firstOrNull()?.fileName
    return if (recentFile != null) "$relative · $recentFile 반영" else relative
}

private fun relativeTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val diffMs = System.currentTimeMillis() - epochMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        minutes < 60 * 24 -> "${TimeUnit.MILLISECONDS.toHours(diffMs)}시간 전"
        minutes < 60 * 24 * 2 -> "어제"
        minutes < 60 * 24 * 7 -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}일 전"
        else -> "${TimeUnit.MILLISECONDS.toDays(diffMs) / 7}주 전"
    }
}
