package com.keran.appoptmanager.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.ui.components.AutoCompleteTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleDialog(
    rule: Triple<AppConfig, Int, Rule>,
    processSuggestions: List<String>,
    threadSuggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Rule) -> Unit
) {
    val (app, index, originalRule) = rule
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialTypeIndex = when (originalRule.type) {
        RuleType.MAIN -> 0
        RuleType.SUB -> 1
        RuleType.THREAD -> if (originalRule.target.contains(":")) 3 else 2
    }

    var selectedType by remember(originalRule.type, originalRule.target) { mutableStateOf(initialTypeIndex) }

    val (initialProcessName, initialThreadName) = remember(originalRule.target, originalRule.type) {
        when {
             originalRule.type == RuleType.MAIN -> "" to ""
             originalRule.type == RuleType.SUB -> originalRule.target to ""
             originalRule.target.contains(":") -> {
                 val parts = originalRule.target.split(":", limit = 2)
                 (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
             }
             else -> "" to originalRule.target
        }
    }

    var processName by remember(initialProcessName) { mutableStateOf(initialProcessName) }
    var threadName by remember(initialThreadName) { mutableStateOf(initialThreadName) }
    var cores by remember(originalRule.cores) { mutableStateOf(originalRule.cores) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "编辑规则配置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val types = listOf("主进程", "子进程", "主进程线程", "子进程线程")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "规则类型",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.take(2).forEachIndexed { idx, label ->
                            val realIdx = idx
                            FilterChip(
                                selected = selectedType == realIdx,
                                onClick = { selectedType = realIdx },
                                label = {
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                leadingIcon = if (selectedType == realIdx) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.drop(2).take(2).forEachIndexed { idx, label ->
                            val realIdx = idx + 2
                            FilterChip(
                                selected = selectedType == realIdx,
                                onClick = { selectedType = realIdx },
                                label = {
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                leadingIcon = if (selectedType == realIdx) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (selectedType) {
                        0 -> { }
                        1 -> {
                            AutoCompleteTextField(
                                value = processName,
                                onValueChange = { processName = it },
                                suggestions = processSuggestions,
                                label = { Text("子进程名") },
                                placeholder = { Text("例如: remote") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        2 -> {
                            AutoCompleteTextField(
                                value = threadName,
                                onValueChange = { threadName = it },
                                suggestions = threadSuggestions,
                                label = { Text("线程名") },
                                placeholder = { Text("例如: render") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        3 -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AutoCompleteTextField(
                                    value = processName,
                                    onValueChange = { processName = it },
                                    suggestions = processSuggestions,
                                    label = { Text("子进程名") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                AutoCompleteTextField(
                                    value = threadName,
                                    onValueChange = { threadName = it },
                                    suggestions = threadSuggestions,
                                    label = { Text("线程名") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cores,
                        onValueChange = { cores = it },
                        label = { Text("核心配置") },
                        placeholder = { Text("例如: 0-3") },
                        supportingText = { Text("指定 CPU 核心编号，用逗号分隔") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = {
                        val finalTarget = when (selectedType) {
                            0 -> "主进程"
                            1 -> processName.trim()
                            2 -> threadName.trim()
                            3 -> "${processName.trim()}:${threadName.trim()}"
                            else -> "主进程"
                        }

                        val finalType = when (selectedType) {
                            0 -> RuleType.MAIN
                            1 -> RuleType.SUB
                            2, 3 -> RuleType.THREAD
                            else -> RuleType.MAIN
                        }

                        onConfirm(originalRule.copy(target = finalTarget, cores = cores, type = finalType))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}