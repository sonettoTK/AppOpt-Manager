package com.keran.appoptmanager.viewmodel.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SelectionViewModel : ViewModel() {

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedAppIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedAppIds: StateFlow<Set<Int>> = _selectedAppIds.asStateFlow()

    fun enterSelectionMode(initialId: Int? = null) {
        _selectionMode.value = true
        _selectedAppIds.value = if (initialId != null) setOf(initialId) else emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedAppIds.value = emptySet()
    }

    fun toggleSelection(id: Int) {
        _selectedAppIds.update { current ->
            if (id in current) current - id else current + id
        }
    }
    
    fun selectAll(visibleIds: Set<Int>) {
        _selectedAppIds.value = visibleIds
    }
    
    fun deselectAll() {
        _selectedAppIds.value = emptySet()
    }
    
    fun invertSelection(visibleIds: Set<Int>) {
        _selectedAppIds.value = visibleIds.symmetricDifference(_selectedAppIds.value)
    }

    private fun <T> Set<T>.symmetricDifference(other: Set<T>): Set<T> {
        return (this - other) + (other - this)
    }

    fun toggleApp(id: Int) {
        _selectedAppIds.update { current ->
            if (id in current) current - id else current + id
        }
    }
}
