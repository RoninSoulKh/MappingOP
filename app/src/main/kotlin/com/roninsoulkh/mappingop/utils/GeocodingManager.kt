package com.roninsoulkh.mappingop.utils

import android.location.Location
import android.util.Log
import com.roninsoulkh.mappingop.BuildConfig
import com.roninsoulkh.mappingop.domain.models.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeocodingManager {
    private val API_KEY = BuildConfig.VISICOM_KEY

    // Если "Дом" больше 150 метров по диагонали — это вранье, это улица или квартал.
    private const val MAX_HOUSE_DIAGONAL = 150
    // Если "Улица" или "Село" больше 4 км — это слишком неточно.
    private const val MAX_SETTLEMENT_DIAGONAL = 4000

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class GeocodingResult(
        val lat: Double,
        val lon: Double,
        val type: String,
        val found: Boolean,
        val isApproximate: Boolean = false
    )

    private data class VisicomFeature(
        val lat: Double,
        val lon: Double,
        val category: String,
        val bboxDiagonal: Float
    )

    suspend fun geocodingBatch(
        consumers: List<Consumer>,
        onProgress: (Int, Int) -> Unit
    ): Map<String, GeocodingResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, GeocodingResult>()

            consumers.forEachIndexed { index, consumer ->
                withContext(Dispatchers.Main) { onProgress(index + 1, consumers.size) }

                if (consumer.latitude != null && consumer.latitude != 0.0) return@forEachIndexed

                delay(800)

                // Определяем название области текстом (Харківська область)
                val regionName = getRegionName(consumer.rawAddress)

                val result = smartGeocode(consumer.rawAddress, regionName)
                results[consumer.id] = result
            }
            results
        }
    }

    suspend fun testGeocode(rawAddress: String): GeocodingResult {
        return smartGeocode(rawAddress, getRegionName(rawAddress))
    }

    private suspend fun smartGeocode(rawAddress: String, regionName: String): GeocodingResult {
        return withContext(Dispatchers.IO) {
            try {
                val comp = parseAddressToCleanParts(rawAddress)
                val settlement = if (comp.settlement.isNotEmpty()) comp.settlement else ""

                // Собираем строку базы: "62322 Безруки Харківська область"
                // Добавляем regionName в конец запроса — это ЖЕЛЕЗНО работает.
                val baseLocation = listOfNotNull(comp.zipCode, settlement, regionName)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")

                // --- 1. ТОЧНЫЙ АДРЕС ---
                if (comp.street.isNotEmpty() && comp.house.isNotEmpty()) {
                    val query = "$baseLocation ${comp.street} ${comp.house}".trim()
                    val feat = executeRequest(query, "adr_address")

                    if (feat != null) {
                        if (feat.category == "adr_address") {
                            // ФИКС СИНИХ КЛАСТЕРОВ:
                            // Если Visicom говорит "Это дом", но его размер > 150м — это не точный дом.
                            if (feat.bboxDiagonal > MAX_HOUSE_DIAGONAL) {
                                return@withContext GeocodingResult(feat.lat, feat.lon, "street", true, isApproximate = true)
                            }
                            return@withContext GeocodingResult(feat.lat, feat.lon, "house", true)
                        } else if (feat.category == "adr_street") {
                            // Искали дом, нашли улицу -> Оранжевый
                            return@withContext GeocodingResult(feat.lat, feat.lon, "street", true, isApproximate = true)
                        }
                    }
                }

                // --- 2. УЛИЦА ---
                if (comp.street.isNotEmpty()) {
                    val query = "$baseLocation ${comp.street}".trim()
                    val feat = executeRequest(query, "adr_street")

                    if (feat != null) {
                        if (feat.bboxDiagonal > MAX_SETTLEMENT_DIAGONAL) {
                            Log.w("GEO", "Улица '${comp.street}' слишком длинная.")
                            return@withContext GeocodingResult(0.0, 0.0, "street_too_big", false)
                        }
                        return@withContext GeocodingResult(feat.lat, feat.lon, "street", true, isApproximate = true)
                    }
                }

                // --- 3. НАСЕЛЕННЫЙ ПУНКТ ---
                if (baseLocation.isNotEmpty()) {
                    val feat = executeRequest(baseLocation, "adm_settlement")
                    if (feat != null) {
                        if (feat.bboxDiagonal > MAX_SETTLEMENT_DIAGONAL) {
                            Log.w("GEO", "НП '$settlement' слишком большой.")
                            return@withContext GeocodingResult(0.0, 0.0, "city_too_big", false)
                        }
                        return@withContext GeocodingResult(feat.lat, feat.lon, "settlement_center", true, isApproximate = true)
                    }
                }

                GeocodingResult(0.0, 0.0, "none", false)

            } catch (e: Exception) {
                GeocodingResult(0.0, 0.0, "error", false)
            }
        }
    }

    private fun executeRequest(queryText: String, category: String): VisicomFeature? {
        try {
            val urlBuilder = HttpUrl.Builder()
                .scheme("https")
                .host("api.visicom.ua")
                .addPathSegments("data-api/5.0/uk/geocode.json")
                .addQueryParameter("text", queryText) // Теперь здесь "Безруки Харківська область"
                .addQueryParameter("key", API_KEY)
                .addQueryParameter("limit", "1")
                .addQueryParameter("categories", category)
            // intersect_with УБРАЛИ, он не работает надежно с ISO кодами

            val request = Request.Builder().url(urlBuilder.build()).header("User-Agent", "MappingOP/1.0").build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string()

            if (response.code != 200 || bodyString == null) return null

            val jsonObject = JSONObject(bodyString)
            var feature: JSONObject? = null

            if (jsonObject.optString("type") == "Feature") {
                feature = jsonObject
            } else {
                val features = jsonObject.optJSONArray("features")
                if (features != null && features.length() > 0) feature = features.getJSONObject(0)
            }

            if (feature != null) {
                val coords = feature.getJSONObject("geo_centroid").getJSONArray("coordinates")
                val props = feature.optJSONObject("properties")
                val foundCategory = props?.optString("categories") ?: category

                var diagonal = 0f
                val bbox = feature.optJSONArray("geo_bbox")
                if (bbox != null && bbox.length() == 4) {
                    val res = FloatArray(1)
                    Location.distanceBetween(
                        bbox.getDouble(1), bbox.getDouble(0),
                        bbox.getDouble(3), bbox.getDouble(2), res
                    )
                    diagonal = res[0]
                }

                return VisicomFeature(
                    lat = coords.getDouble(1),
                    lon = coords.getDouble(0),
                    category = foundCategory,
                    bboxDiagonal = diagonal
                )
            }
        } catch (e: Exception) {
            Log.e("GEO_NET", "Fail: ${e.message}")
        }
        return null
    }

    private data class CleanAddress(val settlement: String, val street: String, val house: String, val zipCode: String)

    private fun parseAddressToCleanParts(raw: String): CleanAddress {
        val cleanRaw = raw.replace("\"", "").trim()
        val parts = cleanRaw.split(",").map { it.trim() }

        var settlement = ""
        var street = ""
        var house = ""
        var zipCode = ""

        parts.forEach { part ->
            val lower = part.lowercase()
            if (part.matches(Regex("\\d{5}"))) { zipCode = part; return@forEach }
            if (lower.contains("обл") || lower.contains("р-н")) return@forEach

            if (lower.startsWith("с.") || lower.startsWith("м.") || lower.startsWith("смт")) {
                settlement = part.replace(Regex("^(с\\.|м\\.|смт\\.|сел\\.|селище)\\s*"), "").trim()
            } else if (lower.contains("вул") || lower.contains("пров") || lower.contains("просп")) {
                street = part.replace(Regex("^(вул\\.|вул|пров\\.|пров|просп\\.|просп)\\s*"), "").trim()
            } else if (part.any { it.isDigit() } && !lower.contains("кв")) {
                house = part.replace(Regex("^(буд\\.|буд)\\s*"), "").trim()
            }
        }

        return CleanAddress(settlement, street, house, zipCode)
    }

    // Возвращает ПОЛНОЕ название области для уточнения поиска
    private fun getRegionName(rawAddress: String): String {
        val lower = rawAddress.lowercase().replace('i', 'і').replace('y', 'у')
        return when {
            lower.contains("харків") -> "Харківська область"
            lower.contains("київ") -> "Київська область"
            lower.contains("полтав") -> "Полтавська область"
            lower.contains("сумськ") -> "Сумська область"
            lower.contains("дніпр") -> "Дніпропетровська область"
            lower.contains("донецьк") -> "Донецька область"
            lower.contains("львів") -> "Львівська область"
            lower.contains("одес") -> "Одеська область"
            lower.contains("запоріж") -> "Запорізька область"
            else -> ""
        }
    }
}