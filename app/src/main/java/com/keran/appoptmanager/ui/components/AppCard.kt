package com.keran.appoptmanager.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.keran.appoptmanager.model.AppConfig
import java.io.File

@Composable
fun AppCard(
    app: AppConfig,
    displayName: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    iconUpdateTrigger: Int = 0,
    onToggleSelection: (String) -> Unit = {},
    onSwipeRight: (String) -> Unit = {},
    onToggleApp: (String, Boolean) -> Unit,
    onAddRule: (AppConfig) -> Unit,
    onEditName: (AppConfig) -> Unit,
    onDeleteApp: (AppConfig) -> Unit,
    onEditRule: (AppConfig, Int) -> Unit,
    onDeleteRule: (AppConfig, Int) -> Unit,
    onToggleRule: (AppConfig, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    val currentOnToggleApp by rememberUpdatedState(onToggleApp)
    val currentOnAddRule by rememberUpdatedState(onAddRule)
    val currentOnEditName by rememberUpdatedState(onEditName)
    val currentOnDeleteApp by rememberUpdatedState(onDeleteApp)
    val currentOnEditRule by rememberUpdatedState(onEditRule)
    val currentOnDeleteRule by rememberUpdatedState(onDeleteRule)
    val currentOnToggleRule by rememberUpdatedState(onToggleRule)
    val currentOnToggleSelection by rememberUpdatedState(onToggleSelection)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)

    val displayNameFirstChar = remember(displayName) {
        displayName.firstOrNull()?.toString() ?: "?"
    }

    val ruleCountText by remember(app.rules) {
        androidx.compose.runtime.derivedStateOf {
            if (app.rules.all { it.enabled }) "${app.rules.size} 规则" else "${app.rules.count { it.enabled }}/${app.rules.size} 规则"
        }
    }

    val iconFile = remember(app.packageName, iconUpdateTrigger) {
        File(context.cacheDir, "app_icons/${app.packageName}.png")
    }

    val imageRequest = remember(app.packageName, iconUpdateTrigger, iconFile) {
        ImageRequest.Builder(context)
            .data(iconFile)
            .memoryCacheKey("${app.packageName}_$iconUpdateTrigger")
            .diskCacheKey(app.packageName)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    val swipeModifier = remember(app.packageName, isSelectionMode) {
        if (!isSelectionMode) {
            Modifier.pointerInput(app.packageName) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                    if (totalDrag > 300) {
                        currentOnSwipeRight(app.packageName)
                        totalDrag = 0f
                    }
                }
            }
        } else {
            Modifier
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (isSelectionMode) {
                    currentOnToggleSelection(app.packageName)
                } else {
                    expanded = !expanded
                }
            }
    ) {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = androidx.compose.animation.core.spring(
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp, 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = androidx.compose.animation.expandHorizontally(),
                    exit = androidx.compose.animation.shrinkHorizontally()
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { currentOnToggleSelection(app.packageName) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = ruleCountText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (!isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        RoundedIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add Rule",
                            onClick = { currentOnAddRule(app) }
                        )

                        RoundedIconButton(
                            icon = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            onClick = { currentOnEditName(app) }
                        )

                        RoundedIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete App",
                            contentColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            onClick = { currentOnDeleteApp(app) }
                        )

                        val isAppEnabled = app.rules.isNotEmpty() && app.rules.any { it.enabled }
                        StatusToggleBtn(
                            checked = isAppEnabled,
                            onCheckedChange = { currentOnToggleApp(app.packageName, !isAppEnabled) },
                            size = 32.dp
                        )
                    }
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (app.rules.isEmpty()) {
                        Text(
                            text = "暂无规则",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        app.rules.forEachIndexed { index, rule ->
                            RuleItem(
                                rule = rule,
                                onEdit = { currentOnEditRule(app, index) },
                                onDelete = { currentOnDeleteRule(app, index) },
                                onToggle = { currentOnToggleRule(app, index) }
                            )
                        }
                    }
                }
            }
        }
    }
}
