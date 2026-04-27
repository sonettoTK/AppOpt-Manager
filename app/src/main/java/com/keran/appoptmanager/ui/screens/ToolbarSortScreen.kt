package com.keran.appoptmanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keran.appoptmanager.model.ToolbarItem
import com.keran.appoptmanager.ui.components.ToolbarButtons
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSortScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val toolbarOrder by settingsViewModel.toolbarOrder.collectAsStateWithLifecycle()
    
    DisposableEffect(Unit) {
        onDispose {
            settingsViewModel.saveToolbarOrder()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("工具栏排序") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "效果预览",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ToolbarButtons(
                            order = toolbarOrder,
                            onRefresh = {},
                            onSave = {},
                            onImport = {},
                            onShare = {},
                            onSearch = {},
                            onSettings = {}
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "长按并拖拽以调整顺序",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            DraggableSortableList(
                items = toolbarOrder,
                onReorder = { newOrder ->
                    settingsViewModel.updateToolbarOrder(newOrder)
                }
            )
        }
    }
}

@Composable
fun DraggableSortableList(
    items: List<ToolbarItem>,
    onReorder: (List<ToolbarItem>) -> Unit
) {
    val density = LocalDensity.current
    val itemHeight = 72.dp
    val spacing = 8.dp
    val itemHeightPx = with(density) { itemHeight.toPx() + spacing.toPx() }

    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var draggingItemY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((items.size * (itemHeight + spacing).value).dp)
    ) {
        items.forEachIndexed { index, item ->
            key(item.id) {
                val currentY = remember { Animatable(index * itemHeightPx) }
                
                val isDragging = draggingItemId == item.id

                val currentDragIndex = if (draggingItemId != null) {
                    (draggingItemY / itemHeightPx).roundToInt().coerceIn(0, items.size - 1)
                } else -1
                
                // 计算该 Item 的目标索引（Visual Index）
                val targetIndex = if (draggingItemId != null && !isDragging) {
                    val dragIndex = items.indexOfFirst { it.id == draggingItemId }
                    if (dragIndex != -1) {
                         if (dragIndex < currentDragIndex && index in (dragIndex + 1)..currentDragIndex) {
                            index - 1
                        } else if (dragIndex > currentDragIndex && index in currentDragIndex until dragIndex) {
                            index + 1
                        } else {
                            index
                        }
                    } else index
                } else {
                    index
                }

                val targetY = targetIndex * itemHeightPx

                LaunchedEffect(targetY, isDragging) {
                    if (!isDragging) {
                        currentY.animateTo(
                            targetValue = targetY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
                
                val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .offset { IntOffset(0, if (isDragging) draggingItemY.roundToInt() else currentY.value.roundToInt()) }
                        .zIndex(if (isDragging) 1f else 0f)
                        .scale(scale)
                        .shadow(elevation.dp, RoundedCornerShape(12.dp))
                        .pointerInput(items) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    draggingItemId = item.id
                                    draggingItemY = currentY.value // 从当前动画位置接管
                                },
                                onDragEnd = { 
                                    if (draggingItemId != null) {
                                        val fromIndex = items.indexOfFirst { it.id == draggingItemId }
                                        // 使用最新的拖拽位置计算最终目标索引
                                        val toIndex = (draggingItemY / itemHeightPx).roundToInt().coerceIn(0, items.size - 1)
                                        
                                        if (fromIndex != -1 && fromIndex != toIndex) {
                                            val newList = items.toMutableList()
                                            val movedItem = newList.removeAt(fromIndex)
                                            newList.add(toIndex, movedItem)
                                            onReorder(newList)
                                        }
                                    }
                                    draggingItemId = null
                                    draggingItemY = 0f
                                },
                                onDragCancel = { 
                                    draggingItemId = null
                                    draggingItemY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggingItemY += dragAmount.y
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
