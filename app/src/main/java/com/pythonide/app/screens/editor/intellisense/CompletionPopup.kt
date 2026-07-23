package com.pythonide.app.screens.editor.intellisense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.Diagnostic
import com.pythonide.domain.model.intellisense.DiagnosticSeverity

@Composable
fun CompletionPopup(
    completions: List<CompletionItem>,
    selectedIndex: Int,
    isVisible: Boolean,
    onItemSelected: (Int) -> Unit,
    onItemClicked: (CompletionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && completions.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.4f)
                .heightIn(max = 300.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val listState = rememberLazyListState()
            LaunchedEffect(selectedIndex) {
                listState.animateScrollToItem(selectedIndex)
            }
            LazyColumn(state = listState, modifier = Modifier.padding(4.dp)) {
                itemsIndexed(completions) { index, item ->
                    CompletionItemRow(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = { onItemClicked(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionItemRow(
    item: CompletionItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF264F78) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getIconText(item.kind),
            color = getColorForKind(item.kind),
            fontSize = 12.sp,
            modifier = Modifier.width(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = Color(0xFFD4D4D4),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (item.kind == CompletionKind.KEYWORD) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.detail?.let { detail ->
                Text(
                    text = detail,
                    color = Color(0xFF858585),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item.returnType?.let { type ->
            Text(
                text = ": $type",
                color = Color(0xFF858585),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getIconText(kind: CompletionKind): String = when (kind) {
    CompletionKind.KEYWORD -> "K"
    CompletionKind.BUILTIN -> "B"
    CompletionKind.MODULE -> "M"
    CompletionKind.FUNCTION -> "f"
    CompletionKind.CLASS -> "C"
    CompletionKind.VARIABLE -> "v"
    CompletionKind.SNIPPET -> "S"
    CompletionKind.IMPORT -> "I"
    else -> "?"
}

private fun getColorForKind(kind: CompletionKind): Color = when (kind) {
    CompletionKind.KEYWORD -> Color(0xFF569CD6)
    CompletionKind.BUILTIN -> Color(0xFF4FC1FF)
    CompletionKind.MODULE -> Color(0xFF4EC9B0)
    CompletionKind.FUNCTION -> Color(0xFFDCDCAA)
    CompletionKind.CLASS -> Color(0xFF4EC9B0)
    CompletionKind.VARIABLE -> Color(0xFF9CDCFE)
    CompletionKind.SNIPPET -> Color(0xFFCE9178)
    CompletionKind.IMPORT -> Color(0xFFC586C0)
    else -> Color(0xFFD4D4D4)
}

fun getDiagnosticColor(severity: DiagnosticSeverity): Color = when (severity) {
    DiagnosticSeverity.ERROR -> Color(0xFFF44747)
    DiagnosticSeverity.WARNING -> Color(0xFFFFA500)
    DiagnosticSeverity.INFO -> Color(0xFF75BEFF)
    DiagnosticSeverity.HINT -> Color(0xFF858585)
}
