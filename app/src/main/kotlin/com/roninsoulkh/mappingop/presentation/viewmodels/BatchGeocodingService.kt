package com.roninsoulkh.mappingop.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.GeoPrecision
import com.roninsoulkh.mappingop.domain.models.GeoSource
import com.roninsoulkh.mappingop.utils.GeocodingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BatchGeocodingService(
    private val repository: AppRepository
) : ViewModel() {

    private val _isGeocoding = MutableStateFlow(false)
    val isGeocoding: StateFlow<Boolean> = _isGeocoding

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress: StateFlow<Pair<Int, Int>?> = _progress

    // 🔥 ИЗМЕНЕНИЕ: Было List<String>, стало List<Consumer>
    private val _failedList = MutableStateFlow<List<Consumer>>(emptyList())
    val failedList: StateFlow<List<Consumer>> = _failedList

    fun startForWorksheet(worksheetId: String) {
        viewModelScope.launch {
            _isGeocoding.value = true
            _failedList.value = emptyList()

            try {
                val allConsumers = repository.getConsumersByWorksheetId(worksheetId)
                val targets = allConsumers.filter { it.latitude == null || it.longitude == null }

                if (targets.isEmpty()) {
                    _isGeocoding.value = false
                    return@launch
                }

                val results = GeocodingManager.geocodingBatch(targets) { curr, total ->
                    _progress.value = curr to total
                }

                // 🔥 ИЗМЕНЕНИЕ: Список объектов Consumer
                val failed = mutableListOf<Consumer>()

                targets.forEach { consumer ->
                    val res = results[consumer.id]

                    if (res != null && res.found) {
                        val precision = when (res.type) {
                            "house" -> GeoPrecision.HOUSE
                            "street" -> GeoPrecision.STREET
                            "settlement_center" -> GeoPrecision.SETTLEMENT
                            else -> GeoPrecision.UNKNOWN
                        }

                        val message = when (res.type) {
                            "street" -> "Знайдено вулицю (центр). У базі немає точного номера."
                            "settlement_center" -> "Знайдено центр нас. пункту. Потрібне уточнення."
                            "street_too_big" -> "Вулиця занадто довга для автоматичної точки."
                            "city_too_big" -> "Місто занадто велике для автоматичної точки."
                            else -> null
                        }

                        val isTooBig = res.type == "street_too_big" || res.type == "city_too_big"

                        val finalLat = if (isTooBig) null else res.lat
                        val finalLon = if (isTooBig) null else res.lon
                        val finalPrecision = if (isTooBig) GeoPrecision.UNKNOWN else precision
                        val needsManual = (finalPrecision != GeoPrecision.HOUSE)

                        val updated = consumer.copy(
                            latitude = finalLat,
                            longitude = finalLon,
                            geoPrecision = finalPrecision,
                            geoSource = if (isTooBig) GeoSource.NONE else GeoSource.VISICOM,
                            geoSourceCategory = res.type,
                            geoMessage = message,
                            needsManualPin = if (isTooBig) false else needsManual
                        )

                        if (!isTooBig) {
                            repository.updateConsumer(updated)
                        } else {
                            // Если слишком большой - добавляем в список ошибок (сам объект)
                            failed.add(consumer)
                        }

                    } else {
                        // Не найдено вообще
                        val updated = consumer.copy(
                            latitude = null,
                            longitude = null,
                            geoPrecision = GeoPrecision.UNKNOWN,
                            geoSource = GeoSource.NONE,
                            geoMessage = "Не знайдено",
                            needsManualPin = false
                        )
                        repository.updateConsumer(updated)
                        // Добавляем в список ошибок
                        failed.add(consumer)
                    }
                }
                _failedList.value = failed

            } catch (e: Exception) {
                Log.e("BATCH", "Error: ${e.message}", e)
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