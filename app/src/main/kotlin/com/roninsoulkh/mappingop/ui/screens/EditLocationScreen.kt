package com.roninsoulkh.mappingop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.roninsoulkh.mappingop.R
import com.roninsoulkh.mappingop.domain.models.Consumer
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun EditLocationScreen(
    consumer: Consumer,
    onSave: (Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // 1. Обчислюємо стартову точку один раз при вході
    val startPoint = remember(consumer) {
        if (consumer.latitude != null && consumer.latitude != 0.0) {
            GeoPoint(consumer.latitude!!, consumer.longitude!!)
        } else {
            GeoPoint(50.0, 36.23) // Харків (центр)
        }
    }

    // Зберігаємо посилання на карту, щоб дістати координати при збереженні
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // 2. КАРТА
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Налаштовуємо ВІДРАЗУ при створенні
                    controller.setZoom(18.0)
                    controller.setCenter(startPoint)

                    mapViewRef = this
                }
            }
        )

        // 3. ПРИЦІЛ ПО ЦЕНТРУ
        // Використовуємо твій червоний пін, бо він точніший за плюсик
        Icon(
            painter = painterResource(id = R.drawable.ic_pin_red),
            contentDescription = "Приціл",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 24.dp) // Трохи піднімаємо, щоб вістря було в центрі
        )

        // 4. ІНФО-ПАНЕЛЬ ЗВЕРХУ
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Text(
                text = "Наведіть маркер на будинок",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // 5. КНОПКИ УПРАВЛІННЯ (Знизу)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Кнопка Скасувати
            FloatingActionButton(
                onClick = onCancel,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Скасувати")
            }

            // Кнопка Зберегти
            ExtendedFloatingActionButton(
                onClick = {
                    val center = mapViewRef?.mapCenter
                    if (center != null) {
                        onSave(center.latitude, center.longitude)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text("Зберегти") }
            )
        }
    }
}