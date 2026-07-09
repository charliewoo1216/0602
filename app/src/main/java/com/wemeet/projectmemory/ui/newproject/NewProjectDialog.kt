package com.wemeet.projectmemory.ui.newproject

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wemeet.projectmemory.data.model.Purpose
import com.wemeet.projectmemory.ui.components.PurposeSelector
import com.wemeet.projectmemory.ui.theme.AccentBlueStart
import com.wemeet.projectmemory.ui.theme.BgChip
import com.wemeet.projectmemory.ui.theme.BgChipBorder
import com.wemeet.projectmemory.ui.theme.BgInputSurface
import com.wemeet.projectmemory.ui.theme.TextHeading
import com.wemeet.projectmemory.ui.theme.TextMuted
import com.wemeet.projectmemory.ui.theme.TextPrimary

val PROJECT_COLOR_PRESETS = listOf("#4C7EF3", "#6FCF97", "#F2B84B", "#9B8CFF", "#FF8F6B", "#4FD1C5", "#63B3ED")

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (parentFolderUri: Uri, name: String, colorHex: String, purpose: Purpose) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var colorHex by rememberSaveable { mutableStateOf(PROJECT_COLOR_PRESETS.first()) }
    var purpose by remember { mutableStateOf(Purpose.default) }
    var parentUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            parentUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF12151A),
        title = {
            Text("새 프로젝트", color = TextHeading, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("이름", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgInputSurface)
                        .border(1.dp, BgChipBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        decorationBox = { inner ->
                            if (name.isEmpty()) {
                                Text("프로젝트 이름", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            inner()
                        },
                    )
                }

                Text("저장 위치", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = parentUri?.lastPathSegment ?: "폴더 선택...",
                    color = if (parentUri != null) TextPrimary else TextMuted,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgInputSurface)
                        .border(1.dp, BgChipBorder, RoundedCornerShape(8.dp))
                        .clickable { pickFolderLauncher.launch(null) }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                )

                Text("색상", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                ) {
                    PROJECT_COLOR_PRESETS.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (colorHex == hex) 2.dp else 0.dp,
                                    color = AccentBlueStart,
                                    shape = CircleShape,
                                )
                                .clickable { colorHex = hex },
                        )
                    }
                }

                Text("목적", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 8.dp))
                PurposeSelector(selected = purpose, onSelect = { purpose = it })
            }
        },
        confirmButton = {
            val enabled = name.isNotBlank() && parentUri != null
            Text(
                text = "생성",
                color = if (enabled) AccentBlueStart else TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable(enabled = enabled) {
                        onCreate(parentUri!!, name.trim(), colorHex, purpose)
                    },
            )
        },
        dismissButton = {
            Text(
                text = "취소",
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.padding(8.dp).clickable(onClick = onDismiss),
            )
        },
    )
}
