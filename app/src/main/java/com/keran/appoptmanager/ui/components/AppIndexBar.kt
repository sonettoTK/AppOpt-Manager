package com.keran.appoptmanager.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun AppIndexBar(
    visibleIndexKeys: List<String>,
    shouldShowIndexBar: Boolean,
    currentIndexKey: String?,
    onIndexSelected: (String) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val indexBarVerticalPadding = 16.dp
    val indexBarHorizontalPadding = 6.dp
    val indexBarVerticalPaddingPx = with(density) { indexBarVerticalPadding.toPx() }
    
    var indexBarHeightPx by remember { mutableStateOf(0f) }

    fun calculateIndex(positionY: Float): String? {
        if (indexBarHeightPx <= 0f || visibleIndexKeys.isEmpty()) return null
        
        val availableHeight = indexBarHeightPx - indexBarVerticalPaddingPx * 2
        if (availableHeight <= 0f) return null
        
        val itemHeight = availableHeight / visibleIndexKeys.size
        if (itemHeight <= 0f) return null
        
        val relativeY = (positionY - indexBarVerticalPaddingPx).coerceIn(0f, availableHeight - 1f)
        val keyIndex = (relativeY / itemHeight).toInt().coerceIn(0, visibleIndexKeys.lastIndex)
        return visibleIndexKeys[keyIndex]
    }

    Box(
        modifier = modifier
            .onSizeChanged { indexBarHeightPx = it.height.toFloat() }
            .pointerInput(visibleIndexKeys, indexBarHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onDragStateChange(true)
                        calculateIndex(offset.y)?.let(onIndexSelected)
                    },
                    onVerticalDrag = { change, _ ->
                        calculateIndex(change.position.y)?.let(onIndexSelected)
                    },
                    onDragEnd = {
                        onDragStateChange(false)
                    },
                    onDragCancel = {
                        onDragStateChange(false)
                    }
                )
            }
            .padding(vertical = indexBarVerticalPadding, horizontal = indexBarHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            val selectedIndex = if (currentIndexKey != null) {
                visibleIndexKeys.indexOf(currentIndexKey)
            } else {
                -1
            }

            visibleIndexKeys.forEachIndexed { index, key ->
                val baseAlpha = if (shouldShowIndexBar) 1f else 0f
                val distance = if (selectedIndex != -1) abs(index - selectedIndex) else -1
                
                val scale = if (shouldShowIndexBar && distance != -1) {
                    when (distance) {
                        0 -> 2.2f
                        1 -> 1.5f
                        2 -> 1.1f
                        else -> 0.8f
                    }
                } else {
                    1f
                }

                val translationX = if (shouldShowIndexBar && distance != -1) {
                    when (distance) {
                        0 -> -40f
                        1 -> -20f
                        else -> 0f
                    }
                } else {
                    0f
                }
                
                val isSelected = distance == 0

                Box(
                    modifier = Modifier
                        .size(width = 20.dp, height = 20.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.translationX = translationX
                            this.alpha = baseAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
