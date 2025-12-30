package com.roninsoulkh.mappingop.utils

import com.roninsoulkh.mappingop.BuildConfig
import com.roninsoulkh.mappingop.domain.models.TargetLocation
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object AddressHelper {
    private val API_KEY = BuildConfig.VISICOM_KEY
    private val client = OkHttpClient()

    fun searchAddress(fullRawAddress: String, callback: (TargetLocation?) -> Unit) {
        val cleanQuery = fullRawAddress.replace("\"", "").trim()
        val url = "https://api.visicom.ua/data-api/5.0/uk/geocode.json?text=$cleanQuery&key=$API_KEY&limit=1"

        Thread {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val jsonObject = JSONObject(jsonString)
                    val features = jsonObject.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val feat = features.getJSONObject(0)
                        val coords = feat.getJSONObject("geo_centroid").getJSONArray("coordinates")
                        // Visicom отдает (lon, lat), нам нужно (lat, lon)
                        val lon = coords.getDouble(0)
                        val lat = coords.getDouble(1)

                        val name = feat.getJSONObject("properties").optString("name") ?: cleanQuery

                        // Callback с правильными координатами
                        callback(TargetLocation(name, lat, lon, "manual", false))
                        return@Thread
                    }
                }
                callback(null)
            } catch (e: Exception) { e.printStackTrace(); callback(null) }
        }.start()
    }
}