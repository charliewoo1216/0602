package com.wemeet.projectmemory.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.ui.theme.TextHeading
import com.wemeet.projectmemory.ui.theme.TextSecondary

/**
 * Deliberately simple line-based Markdown rendering (headings + checkboxes)
 * rather than a full parser — this view is a quick glance at what the LLM
 * just changed, not a Markdown editor. Open the file in a real editor (or
 * export it) for anything more.
 */
@Composable
fun MarkdownPreview(content: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        content.lineSequence().forEach { line ->
            when {
                line.isBlank() -> Text(" ", fontSize = 11.5.sp)
                line.startsWith("#") -> Text(
                    text = line.trimStart('#', ' '),
                    color = TextHeading,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                line.contains("- [x]", ignoreCase = true) -> Text(
                    text = "☑ " + line.substringAfter("]").trim(),
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 19.sp,
                )
                line.contains("- [ ]") -> Text(
                    text = "☐ " + line.substringAfter("]").trim(),
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 19.sp,
                )
                else -> Text(
                    text = line,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}
