package com.keran.appoptmanager.viewmodel.sub

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SelectionViewModel : ViewModel() {

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    fun enterSelectionMode(initialPackage: String? = null) {
        _selectionMode.value = true
        _selectedPackages.value = if (initialPackage != null) setOf(initialPackage) else emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedPackages.value = emptySet()
    }

    fun toggleSelection(packageName: String) {
        _selectedPackages.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun selectAll(visiblePackages: Set<String>) {
        _selectedPackages.value = visiblePackages
    }

    fun deselectAll() {
        _selectedPackages.value = emptySet()
    }

    fun invertSelection(visiblePackages: Set<String>) {
        _selectedPackages.update { current ->
            (visiblePackages - current) + (current - visiblePackages)
        }
    }
}
