package com.roninsoulkh.mappingop.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.roninsoulkh.mappingop.R
import com.roninsoulkh.mappingop.domain.models.Consumer
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun EditLocationScreen(
    consumer: Consumer,
    onSave: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Ссылка на карту для управления
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // 1. Логика стартовой точки
    // Если у потребителя есть координаты (даже неточные, типа улицы) - идем туда.
    // Если нет - ставим дефолт (Харьков), но позже кнопка "Моя локация" спасет.
    val startPoint = remember(consumer) {
        if (consumer.latitude != null && consumer.latitude != 0.0) {
            GeoPoint(consumer.latitude!!, consumer.longitude!!)
        } else {
            GeoPoint(49.9935, 36.2304) // Центр Харькова
        }
    }

    // Лаунчер для прав GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted && mapViewRef != null) {
            enableMyLocationOnEdit(mapViewRef!!, context, centerOnUser = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 2. КАРТА
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false) // Отключаем встроенные, рисуем свои

                    controller.setZoom(18.0)
                    controller.setCenter(startPoint)

                    mapViewRef = this
                }
            },
            update = { map ->
                // Если у нас нет координат дома, попробуем сразу включить GPS (если есть права)
                if ((consumer.latitude == null || consumer.latitude == 0.0) &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Не форсируем центрирование сразу, чтобы не дергало, но слой включаем
                    enableMyLocationOnEdit(map, context, centerOnUser = false)
                }
            }
        )

        // 3. ПРИЦЕЛ (КРАСНЫЙ ПИН)
        // Поднимаем чуть выше (padding bottom), чтобы "острие" пина смотрело в центр экрана
        Icon(
            painter = painterResource(id = R.drawable.ic_pin_red),
            contentDescription = "Приціл",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 24.dp)
        )

        // 4. ИНФО-ПАНЕЛЬ СВЕРХУ
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Text(
                text = "Наведіть маркер на точний будинок",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 5. КНОПКИ УПРАВЛЕНИЯ (СПРАВА)
        // Zoom In, Zoom Out, My Location
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопка "Моя локация"
            FloatingActionButton(
                onClick = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mapViewRef?.let { enableMyLocationOnEdit(it, context, centerOnUser = true) }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF06B6D4) // CyanAction
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_my_location_24),
                    contentDescription = "My Location",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Группа Zoom
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column {
                    IconButton(
                        onClick = { mapViewRef?.controller?.zoomIn() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, "Zoom In")
                    }
                    Divider(modifier = Modifier.width(32.dp).align(Alignment.CenterHorizontally))
                    IconButton(
                        onClick = { mapViewRef?.controller?.zoomOut() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, "Zoom Out")
                    }
                }
            }
        }

        // 6. КНОПКИ СОХРАНЕНИЯ (СНИЗУ)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Кнопка Скасувати
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Скасувати", fontWeight = FontWeight.Bold)
            }

            // Кнопка Зберегти
            Button(
                onClick = {
                    val center = mapViewRef?.mapCenter
                    if (center != null) {
                        onSave(center.latitude, center.longitude)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06B6D4) // CyanAction
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зберегти", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Вспомогательная функция для включения слоя локации
private fun enableMyLocationOnEdit(mapView: MapView, context: Context, centerOnUser: Boolean) {
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    locationOverlay.enableMyLocation()

    // Удаляем старые слои локации, чтобы не дублировать
    mapView.overlays.removeIf { it is MyLocationNewOverlay }
    mapView.overlays.add(locationOverlay)

    if (centerOnUser) {
        locationOverlay.enableFollowLocation()
        mapView.controller.animateTo(locationOverlay.myLocation, 18.0, 1000L)
    }
    mapView.invalidate()
}