package com.roninsoulkh.mappingop.presentation.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.GeoPrecision
import com.roninsoulkh.mappingop.domain.models.GeoSource
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    // Если здесь будет ошибка, убедись, что BatchGeocodingService импортирован
    private val batchGeocodingService = BatchGeocodingService(repository)

    // Данные для UI
    val worksheets = repository.getAllWorksheetsFlow()

    private val _currentConsumers = MutableStateFlow<List<Consumer>>(emptyList())
    val currentConsumers: StateFlow<List<Consumer>> = _currentConsumers.asStateFlow()

    val isGeocoding = batchGeocodingService.isGeocoding
    val geocodingProgress = batchGeocodingService.progress

    // --- ФУНКЦИИ ---

    fun selectWorksheet(worksheetId: String) {
        viewModelScope.launch {
            repository.getConsumersFlow(worksheetId).collect { list ->
                _currentConsumers.value = list
            }
        }
    }

    fun deleteWorksheet(worksheet: Worksheet) {
        viewModelScope.launch {
            repository.deleteWorksheet(worksheet)
            _currentConsumers.value = emptyList()
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            // ТУТ БУДЕТ ТВОЙ ПАРСЕР ПОЗЖЕ
            // Пока просто заглушка, чтобы не было ошибок
            Log.d("MainViewModel", "Нужно подключить ExcelParser для: $uri")
        }
    }

    // ТА САМАЯ ФУНКЦИЯ ДЛЯ РУЧНОЙ ПРИВЯЗКИ
    fun updateConsumerLocationManually(consumer: Consumer, lat: Double, lon: Double) {
        viewModelScope.launch {
            val updatedConsumer = consumer.copy(
                latitude = lat,
                longitude = lon,
                geoSource = GeoSource.FIELD_CONFIRMED,
                geoPrecision = GeoPrecision.HOUSE,
                geoSourceCategory = "FIELD_CONFIRMED",
                needsManualPin = false,
                geoMessage = "Підтверджено вручну"
            )
            repository.updateConsumer(updatedConsumer)
        }
    }

    suspend fun getWorkResult(consumerId: String): WorkResult? {
        return repository.getWorkResultByConsumerId(consumerId)
    }
}