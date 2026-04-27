package com.keran.appoptmanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.keran.appoptmanager.data.InstalledApp
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.ui.components.AutoCompleteTextField
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleConfigScreen(
    app: InstalledApp,
    initialRules: List<Rule> = emptyList(),
    processSuggestions: List<String> = emptyList(),
    threadSuggestions: List<String> = emptyList(),
    onBack: () -> Unit,
    onSave: (List<Rule>) -> Unit
) {
    var rules by remember { 
        mutableStateOf(
            if (initialRules.isNotEmpty()) initialRules 
            else listOf(Rule("主进程", RuleType.MAIN, ""))
        )
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    val listState = rememberLazyListState()
    
    LaunchedEffect(rules.size) {
        if (rules.isNotEmpty() && !isSelectionMode) {
            listState.animateScrollToItem(rules.size) 
        }
    }

    val isSaveEnabled by remember(rules) {
        derivedStateOf {
            rules.isNotEmpty() && rules.all { rule ->
                val isTargetValid = when (rule.type) {
                    RuleType.MAIN -> true
                    else -> rule.target.isNotBlank() && 
                            !rule.target.startsWith(":") && 
                            !rule.target.endsWith(":") &&
                            !rule.target.contains("::")
                }
                val isCoresValid = rule.cores.isNotBlank()
                isTargetValid && isCoresValid
            }
        }
    }

    fun toggleSelection(index: Int) {
        selectedIndices = if (index in selectedIndices) {
            selectedIndices - index
        } else {
            selectedIndices + index
        }
        if (selectedIndices.isEmpty()) {
            isSelectionMode = false
        }
    }

    fun deleteSelected() {
        val sortedIndices = selectedIndices.sortedDescending()
        val mutableRules = rules.toMutableList()
        for (index in sortedIndices) {
            if (index in mutableRules.indices) {
                mutableRules.removeAt(index)
            }
        }
        rules = mutableRules
        isSelectionMode = false
        selectedIndices = emptySet()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIndices.size,
                    totalCount = rules.size,
                    onClose = {
                        isSelectionMode = false
                        selectedIndices = emptySet()
                    },
                    onSelectAll = {
                        if (selectedIndices.size == rules.size) {
                            selectedIndices = emptySet()
                        } else {
                            selectedIndices = rules.indices.toSet()
                        }
                    },
                    onDelete = { deleteSelected() }
                )
            } else {
                RuleConfigTopBar(
                    app = app,
                    onBack = onBack
                )
            }
        },
        bottomBar = {
            if (!isSelectionMode) {
                RuleConfigBottomBar(
                    rules = rules,
                    isSaveEnabled = isSaveEnabled,
                    onAddRule = {
                        val hasMain = rules.any { it.type == RuleType.MAIN }
                        val newRule = if (hasMain) {
                            Rule("", RuleType.THREAD, "")
                        } else {
                            Rule("主进程", RuleType.MAIN, "")
                        }
                        rules = rules + newRule
                    },
                    onBack = onBack,
                    onSave = { onSave(rules) }
                )
            }
        }
    ) { padding ->
        if (rules.isEmpty()) {
            EmptyRulesState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = if (isSelectionMode) 16.dp else 16.dp
                )
            ) {
                itemsIndexed(rules, key = { index, _ -> index }) { index, rule ->
                    ModernRuleCard(
                        rule = rule,
                        index = index,
                        totalRules = rules.size,
                        processSuggestions = processSuggestions,
                        threadSuggestions = threadSuggestions,
                        isSelectionMode = isSelectionMode,
                        isSelected = index in selectedIndices,
                        onToggleSelection = { toggleSelection(index) },
                        onSwipeLeft = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIndices = setOf(index)
                            }
                        },
                        onUpdate = { newRule ->
                            rules = rules.toMutableList().apply { set(index, newRule) }
                        },
                        onDelete = {
                            rules = rules.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                "$selectedCount 已选择",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ) 
        },
        navigationIcon = {
            FilledTonalIconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "取消选择")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "全选")
            }
            FilledTonalIconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleConfigTopBar(
    app: InstalledApp,
    onBack: () -> Unit
) {
    TopAppBar(
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconFile = File(app.iconPath)
                if (iconFile.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = iconFile),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "配置规则",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun RuleConfigBottomBar(
    rules: List<Rule>,
    isSaveEnabled: Boolean,
    onAddRule: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            OutlinedButton(
                onClick = onAddRule,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加规则")
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("返回")
                }
                Button(
                    onClick = onSave,
                    enabled = isSaveEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun EmptyRulesState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Rule,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "暂无规则",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "点击下方按钮添加第一条规则",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernRuleCard(
    rule: Rule,
    index: Int,
    totalRules: Int,
    processSuggestions: List<String>,
    threadSuggestions: List<String>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onSwipeLeft: () -> Unit,
    onUpdate: (Rule) -> Unit,
    onDelete: () -> Unit
) {
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnToggleSelection by rememberUpdatedState(onToggleSelection)
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200),
        label = "containerColor"
    )

    val initialTypeIndex = when (rule.type) {
        RuleType.MAIN -> 0
        RuleType.SUB -> 1
        RuleType.THREAD -> if (rule.target.contains(":")) 3 else 2
    }

    var selectedType by remember(rule.type, rule.target) { mutableStateOf(initialTypeIndex) }
    
    val (initialProcessName, initialThreadName) = remember(rule.target, rule.type) {
        when {
            rule.type == RuleType.MAIN -> "" to ""
            rule.type == RuleType.SUB -> rule.target to ""
            rule.target.contains(":") -> {
                val parts = rule.target.split(":", limit = 2)
                (parts.getOrNull(0) ?: "") to (parts.getOrNull(1) ?: "")
            }
            else -> "" to rule.target
        }
    }

    var processName by remember(initialProcessName) { mutableStateOf(initialProcessName) }
    var threadName by remember(initialThreadName) { mutableStateOf(initialThreadName) }
    var cores by remember(rule.cores) { mutableStateOf(rule.cores) }

    fun updateRule() {
        val newTarget = when (selectedType) {
            0 -> "主进程"
            1 -> processName.trim()
            2 -> threadName.trim()
            3 -> "${processName.trim()}:${threadName.trim()}"
            else -> "主进程"
        }

        val newType = when (selectedType) {
            0 -> RuleType.MAIN
            1 -> RuleType.SUB
            2, 3 -> RuleType.THREAD
            else -> RuleType.MAIN
        }

        onUpdate(rule.copy(target = newTarget, type = newType, cores = cores))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(isSelectionMode) {
                if (!isSelectionMode) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        if (totalDrag < -200) {
                            currentOnSwipeLeft()
                            totalDrag = 0f 
                        }
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { currentOnToggleSelection() },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    
                    RuleTypeIndicator(type = rule.type)
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Text(
                        text = "规则 ${index + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (!isSelectionMode) {
                    FilledTonalIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            val types = listOf("主进程", "子进程", "主线程", "子线程")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.take(2).forEachIndexed { idx, label ->
                        val realIdx = idx
                        ModernFilterChip(
                            selected = selectedType == realIdx,
                            onClick = {
                                selectedType = realIdx
                                updateRule()
                            },
                            label = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.drop(2).forEachIndexed { idx, label ->
                        val realIdx = idx + 2
                        ModernFilterChip(
                            selected = selectedType == realIdx,
                            onClick = { 
                                selectedType = realIdx
                                updateRule()
                            },
                            label = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            when (selectedType) {
                0 -> { }
                1 -> {
                    AutoCompleteTextField(
                        value = processName,
                        onValueChange = { 
                            processName = it
                            updateRule()
                        },
                        suggestions = processSuggestions,
                        label = { Text("子进程名 (例如: remote)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                2 -> {
                    AutoCompleteTextField(
                        value = threadName,
                        onValueChange = { 
                            threadName = it
                            updateRule()
                        },
                        suggestions = threadSuggestions,
                        label = { Text("线程名 (例如: render)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                3 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoCompleteTextField(
                            value = processName,
                            onValueChange = { 
                                processName = it
                                updateRule()
                            },
                            suggestions = processSuggestions,
                            label = { Text("子进程名") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        AutoCompleteTextField(
                            value = threadName,
                            onValueChange = { 
                                threadName = it
                                updateRule()
                            },
                            suggestions = threadSuggestions,
                            label = { Text("线程名") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = cores,
                onValueChange = { 
                    cores = it
                    updateRule()
                },
                label = { Text("核心绑定 (例如: 0-3)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun RuleTypeIndicator(type: RuleType) {
    val (bgColor, textColor, label) = when (type) {
        RuleType.MAIN -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "主"
        )
        RuleType.SUB -> Triple(
            MaterialTheme.colorScheme.inverseSurface,
            MaterialTheme.colorScheme.inverseOnSurface,
            "子"
        )
        RuleType.THREAD -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "线"
        )
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModernFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(200),
        label = "containerColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "contentColor"
    )
    
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
