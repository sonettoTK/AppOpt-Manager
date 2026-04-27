package com.keran.appoptmanager.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolbarItem(val id: String, val label: String, val icon: ImageVector) {
    Refresh("refresh", "刷新", Icons.Default.Refresh),
    Save("save", "保存", Icons.Default.Save),
    Import("import", "导入", Icons.Default.Description),
    Share("share", "分享", Icons.Default.Share),
    Search("search", "搜索", Icons.Default.Search),
    Settings("settings", "设置", Icons.Default.Settings);

    companion object {
        fun fromId(id: String): ToolbarItem? = entries.find { it.id == id }
        
        val DEFAULT_ORDER = listOf(Refresh, Save, Import, Share, Search, Settings)
    }
}
