package com.keran.appoptmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

@Composable
fun SelectionHeader(
    selectedCount: Int,
    isAllSelected: Boolean,
    onExitSelectionMode: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onBatchEnableRules: () -> Unit,
    onBatchDisableRules: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val hasSelection = selectedCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = outlineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        HeaderNavBtn(
            icon = Icons.Default.Close,
            description = "Close Selection Mode",
            onClick = onExitSelectionMode
        )
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "已选择 $selectedCount 项",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        HeaderNavBtn(
            icon = Icons.Default.SwapHoriz,
            description = "Invert Selection",
            onClick = onInvertSelection
        )
        Spacer(modifier = Modifier.width(8.dp))

        HeaderNavBtn(
            icon = Icons.Filled.ToggleOn,
            description = "Batch enable rules",
            onClick = onBatchEnableRules,
            enabled = hasSelection
        )
        Spacer(modifier = Modifier.width(8.dp))

        HeaderNavBtn(
            icon = Icons.Filled.ToggleOff,
            description = "Batch disable rules",
            onClick = onBatchDisableRules,
            enabled = hasSelection
        )
        Spacer(modifier = Modifier.width(8.dp))

        HeaderNavBtn(
            icon = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
            description = if (isAllSelected) "Deselect All" else "Select All",
            onClick = onToggleSelectAll
        )
        Spacer(modifier = Modifier.width(8.dp))

        HeaderNavBtn(
            icon = Icons.Default.Delete,
            description = "Delete Selected",
            onClick = onDeleteSelected,
            enabled = hasSelection
        )
    }
}

@Composable
fun HeaderNavBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    RoundedIconButton(
        icon = icon,
        contentDescription = description,
        onClick = onClick,
        enabled = enabled,
        size = 32.dp,
        iconSize = 20.dp
    )
}
