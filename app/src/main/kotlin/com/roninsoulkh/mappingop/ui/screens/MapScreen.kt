package com.roninsoulkh.mappingop.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.roninsoulkh.mappingop.R
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.Worksheet
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    worksheets: List<Worksheet>,
    consumers: List<Consumer>,
    totalCount: Int,
    foundCount: Int,

    // === НОВЫЕ ПАРАМЕТРЫ ДЛЯ КАМЕРЫ ===
    initialCenter: GeoPoint,
    initialZoom: Double,
    cameraTarget: GeoPoint? = null,
    onCameraTargetSettled: () -> Unit = {},
    onMapStateChanged: (GeoPoint, Double) -> Unit = { _, _ -> },
    // ==================================

    onWorksheetSelected: (Worksheet) -> Unit,
    onConsumerClick: (Consumer) -> Unit,
    isGeocoding: Boolean,
    progress: Pair<Int, Int>?
) {
    val context = LocalContext.current
    var selectedWorksheetName by remember { mutableStateOf("Оберіть відомість") }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Ссылка на карту и зум
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentZoomLevel by remember { mutableStateOf(initialZoom) } // Используем переданный зум

    // Состояния диалогов
    var clusterDialogList by remember { mutableStateOf<List<Consumer>?>(null) }
    var showNotFoundDialog by remember { mutableStateOf(false) }

    // === ЛОГИКА КАМЕРЫ 1: Полет к цели (из карточки) ===
    LaunchedEffect(cameraTarget) {
        if (cameraTarget != null && mapViewRef != null) {
            // Летим к цели с зумом 18 (близко)
            mapViewRef?.controller?.animateTo(cameraTarget, 18.0, 1500L)
            onCameraTargetSettled() // Сбрасываем цель, чтобы не лететь снова
        }
    }

    // === ЛОГИКА КАМЕРЫ 2: Авто-фокус на ведомость ===
    LaunchedEffect(consumers) {
        val validConsumers = consumers.filter { it.latitude != null && it.latitude != 0.0 }
        // Если есть точки, карта готова и нет принудительной цели
        if (validConsumers.isNotEmpty() && mapViewRef != null && cameraTarget == null) {
            val avgLat = validConsumers.map { it.latitude!! }.average()
            val avgLon = validConsumers.map { it.longitude!! }.average()
            // Летим в центр ведомости с зумом 14
            mapViewRef?.controller?.animateTo(GeoPoint(avgLat, avgLon), 14.0, 1000L)
        }
    }

    // === УМНАЯ КЛАСТЕРИЗАЦИЯ ===
    val visibleMarkers = remember(consumers, currentZoomLevel, searchQuery) {
        // 1. Фильтрация поиска
        val filtered = if (searchQuery.isEmpty()) {
            consumers.filter { it.latitude != null && it.latitude != 0.0 }
        } else {
            consumers.filter {
                (it.latitude != null && it.latitude != 0.0) &&
                        (it.rawAddress.contains(searchQuery, true) ||
                                it.name.contains(searchQuery, true) ||
                                it.orNumber.contains(searchQuery, true))
            }
        }

        // 2. Алгоритм группировки
        val gridDistance = when {
            currentZoomLevel >= 18.5 -> 0.00001
            currentZoomLevel >= 16.0 -> 0.0005
            currentZoomLevel >= 14.0 -> 0.005
            else -> 0.03
        }
        groupConsumersByDistance(filtered, gridDistance)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted && mapViewRef != null) {
            enableMyLocation(mapViewRef!!, context)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mapViewRef?.overlayManager?.forEach {
                            if (it is MyLocationNewOverlay) {
                                it.enableFollowLocation()
                                mapViewRef?.controller?.setZoom(17.0)
                            }
                        }
                    } else {
                        locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_my_location_24),
                    contentDescription = "Моя локация",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. КАРТА
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        minZoomLevel = 4.0
                        maxZoomLevel = 20.0

                        // Устанавливаем начальную позицию из параметров
                        controller.setZoom(initialZoom)
                        controller.setCenter(initialCenter)

                        mapViewRef = this

                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                // Сохраняем новую позицию
                                onMapStateChanged(mapCenter as GeoPoint, zoomLevelDouble)
                                return false
                            }
                            override fun onZoom(event: ZoomEvent?): Boolean {
                                currentZoomLevel = this@apply.zoomLevelDouble
                                // Сохраняем новый зум
                                onMapStateChanged(mapCenter as GeoPoint, zoomLevelDouble)
                                return true
                            }
                        })
                    }
                },
                update = { mapView ->
                    val myLocationOverlay = mapView.overlays.find { it is MyLocationNewOverlay }
                    mapView.overlays.clear()

                    if (myLocationOverlay != null) {
                        mapView.overlays.add(myLocationOverlay)
                    } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        enableMyLocation(mapView, context)
                    }

                    visibleMarkers.forEach { group ->
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(group.lat, group.lon)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        if (group.consumers.size > 1) {
                            // КЛАСТЕР (ЗДЕСЬ ТЕПЕРЬ ИСПОЛЬЗУЕТСЯ ТВОЯ КАРТИНКА)
                            marker.icon = createBlueClusterIcon(context, group.consumers.size)
                            marker.title = "Об'єктів: ${group.consumers.size}"

                            marker.setOnMarkerClickListener { _, map ->
                                if (map != null && map.zoomLevelDouble < 17.5) {
                                    map.controller.animateTo(marker.position, map.zoomLevelDouble + 2.0, 1000L)
                                } else {
                                    clusterDialogList = group.consumers
                                }
                                true
                            }
                        } else {
                            // ОДИНОЧНЫЙ МАРКЕР
                            val consumer = group.consumers.first()
                            val iconRes = if (consumer.isProcessed) R.drawable.ic_pin_green else R.drawable.ic_pin_red
                            val iconDrawable = ContextCompat.getDrawable(context, iconRes)
                            if (iconDrawable != null) marker.icon = iconDrawable

                            marker.title = consumer.rawAddress
                            marker.subDescription = "ОР: ${consumer.orNumber}"

                            marker.setOnMarkerClickListener { _, _ ->
                                onConsumerClick(consumer)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            // 2. ВЕРХНЯЯ ПАНЕЛЬ
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { },
                        active = false,
                        onActiveChange = { },
                        placeholder = { Text("Пошук (ОР, адреса)", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {}

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 8.dp)
                        ) {
                            worksheets.forEach { ws ->
                                DropdownMenuItem(
                                    text = { Text(ws.fileName) },
                                    onClick = {
                                        selectedWorksheetName = ws.fileName
                                        onWorksheetSelected(ws)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showNotFoundDialog = true }
                    ) {
                        Text(
                            text = "$foundCount з $totalCount знайдено",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. ПРОГРЕСС БАР
            if (isGeocoding) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Пошук координат...")
                        progress?.let { (curr, total) -> Text("$curr з $total") }
                    }
                }
            }

            // 4. ДИАЛОГ КЛАСТЕРА
            if (clusterDialogList != null) {
                AlertDialog(
                    onDismissRequest = { clusterDialogList = null },
                    title = { Text("За цією локацією (${clusterDialogList!!.size}):") },
                    text = {
                        val safeList = clusterDialogList ?: emptyList()
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(safeList) { consumer ->
                                ListItem(
                                    headlineContent = { Text(consumer.rawAddress, style = MaterialTheme.typography.bodySmall) },
                                    supportingContent = { Text(consumer.name, style = MaterialTheme.typography.labelSmall) },
                                    leadingContent = {
                                        val iconRes = if (consumer.isProcessed) R.drawable.ic_pin_green else R.drawable.ic_pin_red
                                        Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                                    },
                                    modifier = Modifier.clickable {
                                        clusterDialogList = null
                                        onConsumerClick(consumer)
                                    }
                                )
                                Divider()
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { clusterDialogList = null }) { Text("Закрити") } }
                )
            }

            // 5. ДИАЛОГ "НЕ ЗНАЙДЕНО"
            if (showNotFoundDialog) {
                val notFoundList = consumers.filter { it.latitude == null || it.latitude == 0.0 }
                AlertDialog(
                    onDismissRequest = { showNotFoundDialog = false },
                    title = { Text("Не знайдено (${notFoundList.size})") },
                    text = {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(notFoundList) { consumer ->
                                ListItem(
                                    headlineContent = { Text(consumer.rawAddress, style = MaterialTheme.typography.bodySmall) },
                                    supportingContent = { Text("Координати: 0.0, 0.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.clickable {
                                        showNotFoundDialog = false
                                        onConsumerClick(consumer)
                                    }
                                )
                                Divider()
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showNotFoundDialog = false }) { Text("Закрити") } }
                )
            }
        }
    }
}

// === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===

private fun enableMyLocation(mapView: MapView, context: Context) {
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    locationOverlay.enableMyLocation()
    mapView.overlays.add(locationOverlay)
    mapView.invalidate()
}

// 🔥 Твоя функция с картинкой (ОСТАВЛЕНО БЕЗ ИЗМЕНЕНИЙ)
fun createBlueClusterIcon(context: Context, count: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (52 * density).toInt() // Размер иконки 52dp

    // 1. Создаем холст
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 2. Загружаем твою картинку ic_cluster_bg
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_cluster_bg)

    // Если картинка нашлась - рисуем её
    drawable?.let {
        it.setBounds(0, 0, sizePx, sizePx)
        it.draw(canvas)
    }

    // 3. Рисуем цифру поверх картинки
    val paintText = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 18f * density
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        // Добавил небольшую тень тексту для читаемости
        setShadowLayer(3f, 0f, 0f, android.graphics.Color.DKGRAY)
    }

    // Вычисляем центр для текста
    val xPos = sizePx / 2f
    val yPos = (sizePx / 2f) - ((paintText.descent() + paintText.ascent()) / 2f)

    canvas.drawText(count.toString(), xPos, yPos, paintText)

    return BitmapDrawable(context.resources, bitmap)
}

data class MarkerGroup(val lat: Double, val lon: Double, val consumers: List<Consumer>)

fun groupConsumersByDistance(consumers: List<Consumer>, thresholdDegrees: Double): List<MarkerGroup> {
    val groups = mutableListOf<MarkerGroup>()
    val remaining = consumers.toMutableList()

    while (remaining.isNotEmpty()) {
        val current = remaining.removeAt(0)
        val cluster = mutableListOf(current)

        val iterator = remaining.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (abs(current.latitude!! - candidate.latitude!!) < thresholdDegrees &&
                abs(current.longitude!! - candidate.longitude!!) < thresholdDegrees) {
                cluster.add(candidate)
                iterator.remove()
            }
        }

        val avgLat = cluster.map { it.latitude!! }.average()
        val avgLon = cluster.map { it.longitude!! }.average()
        groups.add(MarkerGroup(avgLat, avgLon, cluster))
    }
    return groups
}