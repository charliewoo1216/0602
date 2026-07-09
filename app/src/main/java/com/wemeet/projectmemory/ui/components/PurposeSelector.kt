package com.wemeet.projectmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.Purpose
import com.wemeet.projectmemory.ui.theme.AccentBlueStart
import com.wemeet.projectmemory.ui.theme.BgCard
import com.wemeet.projectmemory.ui.theme.BgCardBorder
import com.wemeet.projectmemory.ui.theme.TextPrimary
import com.wemeet.projectmemory.ui.theme.TextSecondary

/** Maps 1:1 to 화면 3's "목적 (Purpose)" radio list. */
@Composable
fun PurposeSelector(
    selected: Purpose,
    onSelect: (Purpose) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Purpose.entries.forEach { purpose ->
            val isSelected = purpose == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCard)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AccentBlueStart else BgCardBorder,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(purpose) }
                    .padding(14.dp),
            ) {
                RadioDot(selected = isSelected)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = purpose.displayName,
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = purpose.description,
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val borderColor = if (selected) AccentBlueStart else androidx.compose.ui.graphics.Color(0xFF3A4048)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .padding(3.dp),
    ) {
        if (selected) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(AccentBlueStart),
            )
        }
    }
}
