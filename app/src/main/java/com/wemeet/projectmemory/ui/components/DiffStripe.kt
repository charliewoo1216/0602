package com.wemeet.projectmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wemeet.projectmemory.data.model.DocType

/** Small colored bars showing which doc types were touched most recently. */
@Composable
fun DiffStripe(docTypes: List<DocType>, modifier: Modifier = Modifier) {
    if (docTypes.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        docTypes.forEach { docType ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(docType.colorHex)),
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}
