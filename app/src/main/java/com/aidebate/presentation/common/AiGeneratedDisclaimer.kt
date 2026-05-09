package com.aidebate.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle

private const val DISCLAIMER_TEXT =
    "\u5185\u5bb9\u7531AI\u751f\u6210\uff0c\u4ec5\u4f9b\u53c2\u8003 AI-generated, for reference only"

@Composable
fun AiGeneratedDisclaimer(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
) {
    Text(
        text = DISCLAIMER_TEXT,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontStyle = FontStyle.Italic,
        color = color
    )
}
