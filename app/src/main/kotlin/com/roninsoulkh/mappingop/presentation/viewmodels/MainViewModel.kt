package com.roninsoulkh.mappingop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _excelLoaded = MutableStateFlow(false)
    val excelLoaded: StateFlow<Boolean> = _excelLoaded

    fun loadExcelFile() {
        viewModelScope.launch {
            _excelLoaded.value = true
        }
    }
}