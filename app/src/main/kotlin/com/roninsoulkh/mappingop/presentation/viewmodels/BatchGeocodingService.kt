package com.roninsoulkh.mappingop.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.utils.GeocodingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BatchGeocodingService(private val repository: AppRepository) : ViewModel() {

    private val _isGeocoding = MutableStateFlow(false)
    val isGeocoding: StateFlow<Boolean> = _isGeocoding

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress: StateFlow<Pair<Int, Int>?> = _progress

    private val _failedList = MutableStateFlow<List<String>>(emptyList())
    val failedList: StateFlow<List<String>> = _failedList

    fun startForWorksheet(worksheetId: String) {
        viewModelScope.launch {
            _isGeocoding.value = true
            _failedList.value = emptyList()

            try {
                val allConsumers = repository.getConsumersByWorksheetId(worksheetId)
                // Ищем только тех, у кого latitude == null.
                // Если там 0.0 - значит мы уже искали и не нашли, пропускаем.
                val targets = allConsumers.filter { it.latitude == null }

                if (targets.isEmpty()) {
                    _isGeocoding.value = false
                    return@launch
                }

                val results = GeocodingManager.geocodingBatch(targets) { curr, total ->
                    _progress.value = curr to total
                }

                val failed = mutableListOf<String>()

                targets.forEach { consumer ->
                    val res = results[consumer.id]
                    if (res != null && res.found) {
                        // НАШЛИ
                        val updated = consumer.copy(latitude = res.lat, longitude = res.lon)
                        repository.updateConsumer(updated)
                    } else {
                        // НЕ НАШЛИ (или ошибка)
                        // Сохраняем 0.0, чтобы в следующий раз не искать этот адрес снова
                        val updated = consumer.copy(latitude = 0.0, longitude = 0.0)
                        repository.updateConsumer(updated)

                        failed.add(consumer.rawAddress)
                        Log.e("BATCH", "❌ Не найдено: ${consumer.rawAddress}")
                    }
                }
                _failedList.value = failed

            } catch (e: Exception) {
                Log.e("BATCH", "Error: ${e.message}")
            } finally {
                _isGeocoding.value = false
                _progress.value = null
            }
        }
    }

    fun clearErrors() {
        _failedList.value = emptyList()
    }
}