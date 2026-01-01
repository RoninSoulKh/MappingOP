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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

// --- DNA COLORS ---
private val CardColor = Color(0xFF1E293B)
private val CyanAction = Color(0xFF06B6D4)
private val TextWhite = Color(0xFFF8FAFC)
private val TextSlate = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    worksheets: List<Worksheet>,
    consumers: List<Consumer>,
    totalCount: Int,
    foundCount: Int,

    // === ПАРАМЕТРЫ КАМЕРЫ ===
    initialCenter: GeoPoint,
    initialZoom: Double,
    cameraTarget: GeoPoint? = null,
    onCameraTargetSettled: () -> Unit = {},
    onMapStateChanged: (GeoPoint, Double) -> Unit = { _, _ -> },

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
    var currentZoomLevel by remember { mutableStateOf(initialZoom) }

    // Состояния диалогов
    var clusterDialogList by remember { mutableStateOf<List<Consumer>?>(null) }
    var showNotFoundDialog by remember { mutableStateOf(false) }

    // === ЛОГИКА КАМЕРЫ ===
    LaunchedEffect(cameraTarget) {
        if (cameraTarget != null && mapViewRef != null) {
            mapViewRef?.controller?.animateTo(cameraTarget, 18.0, 1500L)
            onCameraTargetSettled()
        }
    }

    LaunchedEffect(consumers) {
        val validConsumers = consumers.filter { it.latitude != null && it.latitude != 0.0 }
        if (validConsumers.isNotEmpty() && mapViewRef != null && cameraTarget == null) {
            val avgLat = validConsumers.map { it.latitude!! }.average()
            val avgLon = validConsumers.map { it.longitude!! }.average()
            mapViewRef?.controller?.animateTo(GeoPoint(avgLat, avgLon), 14.0, 1000L)
        }
    }

    // === КЛАСТЕРИЗАЦИЯ ===
    val visibleMarkers = remember(consumers, currentZoomLevel, searchQuery) {
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

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. КАРТА (OSM)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 20.0
                    setBuiltInZoomControls(false)

                    controller.setZoom(initialZoom)
                    controller.setCenter(initialCenter)

                    mapViewRef = this

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            onMapStateChanged(mapCenter as GeoPoint, zoomLevelDouble)
                            return false
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            currentZoomLevel = this@apply.zoomLevelDouble
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

        // 2. ПЛАВАЮЩИЙ ПОИСК (ВЕРХ)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding() // 🔥 ИСПРАВЛЕНИЕ 1: Отступ от статус-бара
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                color = CardColor,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSlate,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Пошук (ОР, адреса)", color = TextSlate.copy(alpha = 0.7f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = CyanAction,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню", tint = CyanAction)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(CardColor)
                        ) {
                            worksheets.forEach { ws ->
                                DropdownMenuItem(
                                    text = { Text(ws.fileName, color = TextWhite) },
                                    onClick = {
                                        selectedWorksheetName = ws.fileName
                                        onWorksheetSelected(ws)
                                        expanded = false
                                    },
                                    colors = MenuDefaults.itemColors(textColor = TextWhite)
                                )
                            }
                        }
                    }
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = CardColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { showNotFoundDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = CyanAction,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$foundCount з $totalCount знайдено",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite
                        )
                    }
                }
            }
        }

        // 3. ПАНЕЛЬ УПРАВЛЕНИЯ (НИЗ-ПРАВО)
        MapControlsColumn(
            onZoomIn = { mapViewRef?.controller?.zoomIn() },
            onZoomOut = { mapViewRef?.controller?.zoomOut() },
            onMyLocation = {
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
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp)
        )

        // 4. ПРОГРЕСС ГЕОКОДИНГА
        if (isGeocoding) {
            Surface(
                modifier = Modifier.align(Alignment.Center).wrapContentSize(),
                shape = RoundedCornerShape(16.dp),
                color = CardColor,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, TextWhite.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = CyanAction, strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Пошук координат...", style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                    if (progress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Опрацьовано: ${progress.first} з ${progress.second}", style = MaterialTheme.typography.bodyMedium, color = TextSlate)
                    }
                }
            }
        }

        // 5. ДИАЛОГИ КЛАСТЕРА
        if (clusterDialogList != null) {
            AlertDialog(
                onDismissRequest = { clusterDialogList = null },
                containerColor = CardColor,
                titleContentColor = TextWhite,
                textContentColor = TextWhite,
                title = { Text("За цією локацією (${clusterDialogList!!.size}):") },
                text = {
                    val safeList = clusterDialogList ?: emptyList()
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(safeList) { consumer ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clusterDialogList = null
                                            onConsumerClick(consumer)
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconRes = if (consumer.isProcessed) R.drawable.ic_pin_green else R.drawable.ic_pin_red
                                    Icon(painter = painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(consumer.rawAddress, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                        Text(consumer.name, style = MaterialTheme.typography.labelSmall, color = TextSlate)
                                    }
                                }
                                Divider(color = TextSlate.copy(alpha = 0.2f))
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { clusterDialogList = null }) { Text("Закрити", color = CyanAction, fontWeight = FontWeight.Bold) } }
            )
        }

        // 6. 🔥 ДИАЛОГ "ЗВІТ ПОШУКУ" (НОВЫЙ СТИЛЬ)
        if (showNotFoundDialog) {
            val notFoundList = consumers.filter { it.latitude == null || it.latitude == 0.0 }
            AlertDialog(
                onDismissRequest = { showNotFoundDialog = false },
                containerColor = CardColor, // Темный фон
                titleContentColor = TextWhite, // Белый заголовок
                textContentColor = TextWhite, // Белый текст
                title = {
                    Column {
                        Text("Звіт пошуку", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Не знайдено адрес: ${notFoundList.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSlate
                        )
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(notFoundList) { consumer ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNotFoundDialog = false
                                            onConsumerClick(consumer)
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column {
                                        Text(
                                            consumer.rawAddress,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextWhite
                                        )
                                        Text(
                                            "Координати відсутні",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ErrorRed.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Divider(color = TextSlate.copy(alpha = 0.2f))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotFoundDialog = false }) {
                        Text("OK", color = CyanAction, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===

@Composable
fun MapControlsColumn(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = CardColor,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, TextWhite.copy(alpha = 0.1f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Кнопка "+"
            IconButton(onClick = onZoomIn, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextWhite)
            }

            Divider(color = TextWhite.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.width(32.dp))

            // Кнопка "-"
            IconButton(onClick = onZoomOut, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextWhite)
            }

            Divider(color = TextWhite.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.width(32.dp))

            // 🔥 ИСПРАВЛЕНИЕ 2: Уменьшенная иконка прицела
            IconButton(onClick = onMyLocation, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_my_location_24),
                    contentDescription = "My Location",
                    tint = CyanAction,
                    modifier = Modifier.size(24.dp) // Уменьшили размер иконки внутри кнопки
                )
            }
        }
    }
}

private fun enableMyLocation(mapView: MapView, context: Context) {
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    locationOverlay.enableMyLocation()
    mapView.overlays.add(locationOverlay)
    mapView.invalidate()
}

fun createBlueClusterIcon(context: Context, count: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (52 * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_cluster_bg)

    drawable?.let {
        it.setBounds(0, 0, sizePx, sizePx)
        it.draw(canvas)
    }

    val paintText = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 18f * density
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(3f, 0f, 0f, android.graphics.Color.DKGRAY)
    }

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