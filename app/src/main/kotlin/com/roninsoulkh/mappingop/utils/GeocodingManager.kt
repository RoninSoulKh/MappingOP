package com.roninsoulkh.mappingop.utils

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

    private const val CURRENT_REGION_ID = "UA-63"

    private val BIG_SETTLEMENTS = listOf(
        "харків", "лозова", "ізюм", "чугуїв", "первомайський", "балаклія", "куп'янськ",
        "мерефа", "люботин", "красноград", "вовчанськ", "дергачі", "богодухів", "зміїв", "валки", "барвінкове",
        "пісочин", "солоницівка", "високий", "безлюдівка", "мала данилівка",
        "циркуни", "липці", "руські тишки", "черкаські тишки", "слобожанське",
        "ківшарівка", "покотилівка", "рогань"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class GeocodingResult(
        val lat: Double,
        val lon: Double,
        val type: String,
        val found: Boolean
    )

    suspend fun geocodingBatch(
        consumers: List<Consumer>,
        onProgress: (Int, Int) -> Unit
    ): Map<String, GeocodingResult> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, GeocodingResult>()

            consumers.forEachIndexed { index, consumer ->
                withContext(Dispatchers.Main) { onProgress(index + 1, consumers.size) }

                if (consumer.latitude != null) {
                    return@forEachIndexed
                }

                delay(800) // Анти-бан

                val result = smartGeocode(consumer.rawAddress)
                results[consumer.id] = result
            }
            results
        }
    }

    suspend fun testGeocode(rawAddress: String): GeocodingResult {
        return smartGeocode(rawAddress)
    }

    private suspend fun smartGeocode(rawAddress: String): GeocodingResult {
        return withContext(Dispatchers.IO) {
            try {
                val comp = parseAddressToCleanParts(rawAddress)
                val settlement = if (comp.settlement.isNotEmpty()) comp.settlement else "Циркуни"

                // 1. Поиск точного адреса (Улица + Дом)
                if (comp.street.isNotEmpty() && comp.house.isNotEmpty()) {
                    val query = "$settlement, ${comp.street} ${comp.house}"
                    val res = executeRequest(query, "adr_address", settlement)
                    if (res != null) return@withContext GeocodingResult(res.first, res.second, "house", true)
                }

                // 2. Поиск улицы
                if (comp.street.isNotEmpty()) {
                    val query = "$settlement, ${comp.street}"
                    val res = executeRequest(query, "adr_street", settlement)
                    if (res != null) return@withContext GeocodingResult(res.first, res.second, "street", true)
                }

                // 3. Поиск населенного пункта
                val isBig = BIG_SETTLEMENTS.any { settlement.lowercase().contains(it) }
                if (isBig) {
                    Log.w("GEO_SKIP", "Большое село ($settlement), точный адрес не найден.")
                    return@withContext GeocodingResult(0.0, 0.0, "not_found_in_big_city", false)
                } else {
                    val query = settlement
                    val res = executeRequest(query, "adm_settlement", settlement)
                    if (res != null) {
                        return@withContext GeocodingResult(res.first, res.second, "settlement_center", true)
                    }
                }

                GeocodingResult(0.0, 0.0, "none", false)

            } catch (e: Exception) {
                Log.e("GEO_CRASH", "Ошибка: ${e.message}")
                GeocodingResult(0.0, 0.0, "error", false)
            }
        }
    }

    private fun executeRequest(queryText: String, category: String, expectedSettlement: String): Pair<Double, Double>? {
        try {
            val urlBuilder = HttpUrl.Builder()
                .scheme("https")
                .host("api.visicom.ua")
                .addPathSegments("data-api/5.0/uk/geocode.json")
                .addQueryParameter("text", queryText)
                .addQueryParameter("key", API_KEY)
                .addQueryParameter("limit", "1")
                .addQueryParameter("categories", category)

            // === ФИЛЬТР ПО ОБЛАСТИ ===
            if (CURRENT_REGION_ID.isNotEmpty()) {
                urlBuilder.addQueryParameter("intersect_with", CURRENT_REGION_ID)
            }

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
                if (!validateResult(feature, expectedSettlement)) return null
                val coords = feature.getJSONObject("geo_centroid").getJSONArray("coordinates")
                return Pair(coords.getDouble(1), coords.getDouble(0))
            }
        } catch (e: Exception) {
            Log.e("GEO_NET", "Сбой: ${e.message}")
        }
        return null
    }

    private fun validateResult(feature: JSONObject, expectedSettlement: String): Boolean {
        val props = feature.optJSONObject("properties") ?: return false

        // Доп. проверка, если Visicom вернет что-то странное
        val level1 = props.optString("level1").lowercase()
        // Если мы ищем в Харькове, а вернуло не Харьков - отбой (хотя intersect_with должен это решить)
        if (CURRENT_REGION_ID == "UA-63" && level1.isNotEmpty() && !level1.contains("харків")) return false

        val foundSettlement = props.optString("settlement").lowercase()
        val searchSettlement = expectedSettlement.lowercase()

        if (foundSettlement.isNotEmpty() &&
            !searchSettlement.contains(foundSettlement) &&
            !foundSettlement.contains(searchSettlement)) {
            return false
        }
        return true
    }

    private data class CleanAddress(val settlement: String, val street: String, val house: String)

    private fun parseAddressToCleanParts(raw: String): CleanAddress {
        val cleanRaw = raw.replace("\"", "").trim()
        val parts = cleanRaw.split(",").map { it.trim() }

        var settlement = ""
        var street = ""
        var house = ""

        parts.forEach { part ->
            val lower = part.lowercase()
            if (part.matches(Regex("\\d{5}"))) return@forEach
            if (lower.contains("обл") || lower.contains("р-н") || lower.contains("район")) return@forEach

            if (lower.startsWith("с.") || lower.startsWith("м.") || lower.startsWith("смт") || lower.startsWith("сел.") || lower.contains("селище")) {
                settlement = part
                    .replace("с.", "")
                    .replace("м.", "")
                    .replace("смт", "")
                    .replace("сел.", "")
                    .replace("селище", "")
                    .trim()
            } else if (settlement.isEmpty() && (lower == "циркуни" || lower == "липці" || lower == "кравцівка" || lower == "станіславівка")) {
                settlement = part
            } else if (lower.contains("вул") || lower.contains("пров") || lower.contains("просп") || lower.contains("майдан") || lower.contains("в'їзд")) {
                street = part
                    .replace("вул.", "").replace("вул", "")
                    .replace("пров.", "").replace("просп.", "")
                    .trim()
            } else if (part.any { it.isDigit() } && !lower.contains("кв")) {
                house = part.replace("буд.", "").replace("буд", "").trim()
            }
        }
        if (settlement.isEmpty() && cleanRaw.lowercase().contains("циркун")) settlement = "Циркуни"

        return CleanAddress(settlement, street, house)
    }
}