package com.keran.appoptmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType

@Composable
fun RuleItem(
    rule: Rule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnToggle by rememberUpdatedState(onToggle)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RuleTypeTag(type = rule.type)

            Text(
                text = rule.target,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = rule.cores,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RoundedIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit Rule",
                    onClick = currentOnEdit,
                    size = 28.dp,
                    iconSize = 14.dp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                RoundedIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Rule",
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    onClick = currentOnDelete,
                    size = 28.dp,
                    iconSize = 14.dp
                )
                StatusToggleBtn(
                    checked = rule.enabled,
                    onCheckedChange = currentOnToggle,
                    size = 28.dp
                )
            }
        }
    }
}

@Composable
fun RuleTypeTag(type: RuleType) {
    val bgColor = when (type) {
        RuleType.MAIN -> MaterialTheme.colorScheme.tertiaryContainer
        RuleType.SUB -> MaterialTheme.colorScheme.inverseSurface
        RuleType.THREAD -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when (type) {
        RuleType.MAIN -> MaterialTheme.colorScheme.onTertiaryContainer
        RuleType.SUB -> MaterialTheme.colorScheme.inverseOnSurface
        RuleType.THREAD -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val ruleLabel = when (type) {
        RuleType.MAIN -> "主进程"
        RuleType.SUB -> "子进程"
        RuleType.THREAD -> "线程"
    }

    Text(
        text = ruleLabel,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
