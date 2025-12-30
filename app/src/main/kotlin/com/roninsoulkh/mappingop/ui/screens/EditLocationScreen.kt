package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.roninsoulkh.mappingop.domain.models.Consumer
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay

@Composable
fun EditLocationScreen(
    consumer: Consumer,
    onSave: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    // Начальная позиция: или текущие координаты, или Харьков
    var centerPoint by remember {
        mutableStateOf(
            if (consumer.latitude != null && consumer.latitude != 0.0) {
                GeoPoint(consumer.latitude!!, consumer.longitude!!)
            } else {
                GeoPoint(49.9935, 36.2304) // Харьков
            }
        )
    }

    DisposableEffect(Unit) {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0) // Зум поближе для точности
        mapView.controller.setCenter(centerPoint)

        // Слушатель событий (нужен для корректной работы OSMDroid в Compose)
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        mapView.overlays.add(mapEventsOverlay)

        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. КАРТА
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. ПРИЦЕЛ ПО ЦЕНТРУ (Красный плюсик)
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Прицел",
            tint = Color.Red,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
        )

        // 3. КНОПКИ УПРАВЛЕНИЯ
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Кнопка Отмена
            FloatingActionButton(
                onClick = onCancel,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }

            // Кнопка Сохранить
            FloatingActionButton(
                onClick = {
                    val center = mapView.mapCenter
                    onSave(center.latitude, center.longitude)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
        }

        // Подсказка сверху
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ) {
            Text(
                text = "Наведіть хрестик на будинок",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}