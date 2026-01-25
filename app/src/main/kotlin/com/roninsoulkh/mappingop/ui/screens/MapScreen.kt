package com.roninsoulkh.mappingop.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
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
import com.roninsoulkh.mappingop.domain.models.GeoPrecision
import com.roninsoulkh.mappingop.domain.models.GeoSource
import com.roninsoulkh.mappingop.domain.models.Worksheet
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import java.util.Locale

private val CardColor = Color(0xFF1E293B)
private val CyanAction = Color(0xFF06B6D4)
private val TextWhite = Color(0xFFF8FAFC)
private val TextSlate = Color(0xFF94A3B8)
private val ErrorRed = Color(0xFFEF4444)
private val WarningOrange = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    worksheets: List<Worksheet>,
    consumers: List<Consumer>,
    totalCount: Int,
    foundCount: Int,
    initialCenter: GeoPoint,
    initialZoom: Double,
    cameraTarget: GeoPoint? = null,
    onCameraTargetSettled: () -> Unit = {},
    onMapStateChanged: (GeoPoint, Double) -> Unit = { _, _ -> },
    onWorksheetSelected: (Worksheet) -> Unit,
    onConsumerClick: (Consumer) -> Unit,
    onManualLocationClick: (Consumer) -> Unit,
    isGeocoding: Boolean,
    progress: Pair<Int, Int>?
) {
    val context = LocalContext.current
    var selectedWorksheetName by remember { mutableStateOf("Оберіть відомість") }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentZoomLevel by remember { mutableStateOf(initialZoom) }
    var currentBoundingBox by remember { mutableStateOf<BoundingBox?>(null) }

    var clusterDialogList by remember { mutableStateOf<List<Consumer>?>(null) }
    var showNotFoundDialog by remember { mutableStateOf(false) }

    var approxConsumerDialog by remember { mutableStateOf<Consumer?>(null) }

    // Допоміжна функція навігації
    fun openNavigation(consumer: Consumer) {
        if (consumer.latitude != null && consumer.longitude != null && consumer.latitude != 0.0) {
            try {
                val label = Uri.encode("${consumer.rawAddress} (${consumer.orNumber})")
                val uri = Uri.parse("geo:${consumer.latitude},${consumer.longitude}?q=${consumer.latitude},${consumer.longitude}($label)")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Немає додатку для карт", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isValidCoord(lat: Double?, lon: Double?): Boolean {
        if (lat == null || lon == null) return false
        if (lat == 0.0 && lon == 0.0) return false
        if (lat < -90.0 || lat > 90.0) return false
        if (lon < -180.0 || lon > 180.0) return false
        return true
    }

    fun isAccurate(consumer: Consumer): Boolean {
        return consumer.geoPrecision == GeoPrecision.HOUSE ||
                consumer.geoSource == GeoSource.FIELD_CONFIRMED
    }

    LaunchedEffect(cameraTarget) {
        if (cameraTarget != null && mapViewRef != null) {
            mapViewRef?.controller?.animateTo(cameraTarget, 18.0, 1500L)
            onCameraTargetSettled()
        }
    }

    LaunchedEffect(consumers) {
        val validConsumers = consumers.filter { isValidCoord(it.latitude, it.longitude) }
        if (validConsumers.isNotEmpty() && mapViewRef != null && cameraTarget == null) {
            val avgLat = validConsumers.map { it.latitude!! }.average()
            val avgLon = validConsumers.map { it.longitude!! }.average()
            mapViewRef?.controller?.animateTo(GeoPoint(avgLat, avgLon), 14.0, 1000L)
        }
    }

    val visibleMarkers = remember(consumers, currentZoomLevel, searchQuery, currentBoundingBox) {
        fun isInsideBox(lat: Double, lon: Double, box: BoundingBox): Boolean {
            val latPad = (box.latNorth - box.latSouth) * 0.10
            val lonPad = (box.lonEast - box.lonWest) * 0.10
            val north = box.latNorth + latPad
            val south = box.latSouth - latPad
            val east = box.lonEast + lonPad
            val west = box.lonWest - lonPad
            return lat in south..north && lon in west..east
        }

        val baseFiltered = consumers
            .asSequence()
            .filter { isValidCoord(it.latitude, it.longitude) }
            .filter {
                searchQuery.isEmpty() ||
                        it.rawAddress.contains(searchQuery, true) ||
                        it.name.contains(searchQuery, true) ||
                        it.orNumber.contains(searchQuery, true)
            }
            .toList()

        val box = currentBoundingBox
        val filtered = if (box == null) {
            baseFiltered
        } else {
            baseFiltered.filter { c -> isInsideBox(c.latitude!!, c.longitude!!, box) }
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
                    currentBoundingBox = this.boundingBox

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            currentBoundingBox = this@apply.boundingBox
                            onMapStateChanged(mapCenter as GeoPoint, zoomLevelDouble)
                            return false
                        }

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            currentZoomLevel = this@apply.zoomLevelDouble
                            currentBoundingBox = this@apply.boundingBox
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
                } else if (
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    enableMyLocation(mapView, context)
                }

                visibleMarkers.forEach { group ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(group.lat, group.lon)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    val hasApproximate = group.consumers.any { !isAccurate(it) }

                    if (group.consumers.size > 1) {
                        marker.icon = createClusterIcon(context, group.consumers.size, hasApproximate)
                        marker.title = if (hasApproximate) "~${group.consumers.size}" else "${group.consumers.size}"

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
                        val accurate = isAccurate(consumer)

                        val iconRes = if (consumer.isProcessed) R.drawable.ic_pin_green else R.drawable.ic_pin_red
                        ContextCompat.getDrawable(context, iconRes)?.let {
                            marker.icon = it
                        }

                        marker.title = consumer.rawAddress
                        marker.subDescription = "ОР: ${consumer.orNumber}"

                        marker.setOnMarkerClickListener { _, _ ->
                            if (accurate) {
                                onConsumerClick(consumer)
                            } else {
                                approxConsumerDialog = consumer
                            }
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        // UI Controls ...
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
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
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 16.dp)
        )

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

        // --- ДІАЛОГ КЛАСТЕРІВ (з кнопкою навігації) ---
        if (clusterDialogList != null) {
            AlertDialog(
                onDismissRequest = { clusterDialogList = null },
                containerColor = CardColor,
                titleContentColor = TextWhite,
                textContentColor = TextWhite,
                title = { Text("У цій точці (${clusterDialogList!!.size}):") },
                text = {
                    val safeList = clusterDialogList ?: emptyList()
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(safeList) { consumer ->
                            val accurate = isAccurate(consumer)
                            val textColor = if (accurate) TextWhite else WarningOrange

                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clusterDialogList = null
                                            if (accurate) onConsumerClick(consumer)
                                            else approxConsumerDialog = consumer
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconRes = if (consumer.isProcessed) R.drawable.ic_pin_green else R.drawable.ic_pin_red
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        tint = if (!accurate) WarningOrange else Color.Unspecified,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(consumer.rawAddress, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                        if (!accurate) {
                                            Text("⚠️ Приблизна адреса", style = MaterialTheme.typography.labelSmall, color = WarningOrange)
                                        } else {
                                            Text(consumer.name, style = MaterialTheme.typography.labelSmall, color = TextSlate)
                                        }
                                    }

                                    // 🔥 МАЛЕНЬКА КНОПКА "ПОЇХАТИ" В СПИСКУ
                                    if (accurate) {
                                        IconButton(onClick = { openNavigation(consumer) }) {
                                            Icon(Icons.Default.DirectionsCar, contentDescription = "Поїхати", tint = CyanAction)
                                        }
                                    }
                                }
                                Divider(color = TextSlate.copy(alpha = 0.2f))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { clusterDialogList = null }) {
                        Text("Закрити", color = CyanAction, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ДІАЛОГ ПОПЕРЕДЖЕННЯ
        if (approxConsumerDialog != null) {
            val c = approxConsumerDialog!!
            AlertDialog(
                onDismissRequest = { approxConsumerDialog = null },
                containerColor = CardColor,
                titleContentColor = WarningOrange,
                textContentColor = TextWhite,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange) },
                title = { Text("Увага: Неточна адреса") },
                text = {
                    Column {
                        Text(
                            text = "Точність: ${c.geoPrecision.name}",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = c.geoMessage ?: "Visicom знайшов лише вулицю або населений пункт.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Рекомендується прив'язати точку вручну, щоб не їхати помилково.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSlate
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            approxConsumerDialog = null
                            onManualLocationClick(c)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAction)
                    ) {
                        Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Прив'язати вручну")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        approxConsumerDialog = null
                        onConsumerClick(c)
                    }) {
                        Text("Все одно відкрити", color = TextSlate)
                    }
                }
            )
        }

        if (showNotFoundDialog) {
            val notFoundList = consumers.filter { !isValidCoord(it.latitude, it.longitude) }

            AlertDialog(
                onDismissRequest = { showNotFoundDialog = false },
                containerColor = CardColor,
                titleContentColor = TextWhite,
                textContentColor = TextWhite,
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
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notFoundList) { consumer ->
                            Surface(
                                color = CardColor.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNotFoundDialog = false
                                        onConsumerClick(consumer)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {

                                    // ✅ ЖИРНЫМ: ОР
                                    Text(
                                        text = consumer.orNumber,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // ✅ Обычным: Адрес
                                    Text(
                                        text = consumer.rawAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextWhite
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // ✅ Обычным, но КАПСОМ: ФИО
                                    Text(
                                        text = consumer.name.uppercase(Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSlate
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // оставим твой индикатор
                                    Text(
                                        "Координати відсутні",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ErrorRed.copy(alpha = 0.85f)
                                    )
                                }
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

// ... [Інші функції: MapControlsColumn, enableMyLocation, createClusterIcon, groupConsumersByDistance - залишаються без змін] ...
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onZoomIn, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextWhite)
            }

            Divider(color = TextWhite.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.width(32.dp))

            IconButton(onClick = onZoomOut, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextWhite)
            }

            Divider(color = TextWhite.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.width(32.dp))

            IconButton(onClick = onMyLocation, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_my_location_24),
                    contentDescription = "My Location",
                    tint = CyanAction,
                    modifier = Modifier.size(24.dp)
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

fun createClusterIcon(context: Context, count: Int, hasApproximate: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (52 * density).toInt()

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_cluster_bg)
    drawable?.let {
        if (hasApproximate) {
            it.setTint(android.graphics.Color.parseColor("#F59E0B"))
        } else {
            it.setTintList(null)
        }
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

    val text = if (hasApproximate) "~$count" else "$count"
    canvas.drawText(text, xPos, yPos, paintText)

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
                abs(current.longitude!! - candidate.longitude!!) < thresholdDegrees
            ) {
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