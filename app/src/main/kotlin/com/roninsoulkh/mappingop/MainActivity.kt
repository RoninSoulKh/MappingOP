package com.roninsoulkh.mappingop

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

import com.roninsoulkh.mappingop.data.parser.ExcelParser
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.screens.*
import com.roninsoulkh.mappingop.ui.theme.MappingOPTheme
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.utils.AddressHelper
import com.roninsoulkh.mappingop.utils.SettingsManager
import com.roninsoulkh.mappingop.presentation.viewmodels.BatchGeocodingService
import com.roninsoulkh.mappingop.ui.components.*
import com.roninsoulkh.mappingop.ui.theme.CyanAction
import com.roninsoulkh.mappingop.ui.theme.StatusGreen
import com.roninsoulkh.mappingop.ui.theme.StatusRed

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        org.osmdroid.config.Configuration.getInstance().load(
            this,
            android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        )
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var currentThemeSetting by remember { mutableStateOf(SettingsManager.getTheme(context)) }
            val useDarkTheme = when (currentThemeSetting) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as ComponentActivity).window
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                }
            }
            MappingOPTheme(darkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        repository = repository,
                        currentTheme = currentThemeSetting,
                        onThemeChanged = { newTheme ->
                            SettingsManager.saveTheme(context, newTheme)
                            currentThemeSetting = newTheme
                        }
                    )
                }
            }
        }
    }

    companion object {
        init {
            System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
        }
    }
}

fun constructSmartAddress(raw: String): String {
    val parts = raw.split(",").map { it.trim() }

    var zipCode = ""
    var settlement = ""
    var street = ""
    var houseNumber = ""
    var region = ""

    for (part in parts) {
        val lower = part.lowercase()
        if (part.matches(Regex("\\d{5}"))) { zipCode = part; continue }
        if (lower.contains("обл")) { region = part; continue }
        if (lower.contains("вул") || lower.contains("пров") || lower.contains("просп") ||
            lower.contains("в'їзд") || lower.contains("майдан") || lower.contains("бульвар") ||
            lower.contains("наб") || lower.contains("узвіз") || lower.contains("тупик")) {
            street = part
            continue
        }
        if (lower.contains("с.") || lower.contains("м.") || lower.contains("смт") || lower.contains("селище")) {
            settlement = part.replace("с.", "").replace("м.", "").replace("смт", "").replace("селище", "").trim()
            continue
        }
        if ((lower.contains("буд") || (part.isNotEmpty() && part[0].isDigit())) && part.length < 6) {
            houseNumber = part.replace("буд.", "").replace("буд", "").replace("кв.", "").trim()
            continue
        }
    }

    val addressParts = mutableListOf<String>()
    if (street.isNotEmpty()) addressParts.add(street)
    if (houseNumber.isNotEmpty()) addressParts.add(houseNumber)
    if (settlement.isNotEmpty()) addressParts.add(settlement)
    if (region.isNotEmpty()) addressParts.add(region)
    if (zipCode.isNotEmpty()) addressParts.add(zipCode)

    if (addressParts.size < 2) return raw
    return addressParts.joinToString(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: AppRepository,
    currentTheme: String,
    onThemeChanged: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    var currentWorksheetsScreen by remember { mutableStateOf<AppScreen>(AppScreen.Worksheets) }

    var isNavigatedFromMap by remember { mutableStateOf(false) }

    var selectedWorksheetId by remember { mutableStateOf<String?>(null) }
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }
    var selectedMapWorksheetId by remember { mutableStateOf<String?>(null) }

    var mapCenterState by remember { mutableStateOf(GeoPoint(50.0, 36.23)) }
    var mapZoomState by remember { mutableStateOf(13.0) }
    var mapCameraTarget by remember { mutableStateOf<GeoPoint?>(null) }

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var notificationColor by remember { mutableStateOf(StatusGreen) }

    BackHandler(enabled = currentTab != BottomTab.HOME || currentWorksheetsScreen != AppScreen.Worksheets) {
        if (currentTab == BottomTab.TASKS) {
            when (currentWorksheetsScreen) {
                is AppScreen.EditLocation,
                AppScreen.ProcessConsumer -> currentWorksheetsScreen = AppScreen.ConsumerDetail
                AppScreen.ConsumerDetail -> {
                    if (isNavigatedFromMap) {
                        currentTab = BottomTab.MAP
                    } else {
                        currentWorksheetsScreen = AppScreen.ConsumerList
                    }
                }
                AppScreen.ConsumerList -> currentWorksheetsScreen = AppScreen.Worksheets
                AppScreen.WorkResults -> currentWorksheetsScreen = AppScreen.Worksheets
                else -> currentTab = BottomTab.HOME
            }
        } else {
            currentTab = BottomTab.HOME
        }
    }

    val geoService = remember { BatchGeocodingService(repository) }
    val isGeocoding by geoService.isGeocoding.collectAsState()
    val geoProgress by geoService.progress.collectAsState()
    val failedList by geoService.failedList.collectAsState()

    val mapConsumersState = remember(selectedMapWorksheetId) {
        if (selectedMapWorksheetId != null) {
            repository.getConsumersFlow(selectedMapWorksheetId!!)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val mapConsumersList = mapConsumersState.value
    val totalConsumersCount = mapConsumersList.size
    val foundCoordinatesCount = mapConsumersList.count { it.latitude != null && it.latitude != 0.0 }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var launchImport by remember { mutableStateOf(false) }

    var worksheets by remember { mutableStateOf<List<Worksheet>>(emptyList()) }
    LaunchedEffect(Unit) { repository.getAllWorksheetsFlow().collect { worksheets = it } }

    var worksheetConsumers by remember { mutableStateOf<List<Consumer>>(emptyList()) }
    LaunchedEffect(selectedWorksheetId) {
        selectedWorksheetId?.let { id -> repository.getConsumersFlow(id).collect { worksheetConsumers = it } } ?: run { worksheetConsumers = emptyList() }
    }

    var workResultForSelectedConsumer by remember { mutableStateOf<WorkResult?>(null) }
    LaunchedEffect(selectedConsumer, currentWorksheetsScreen) {
        workResultForSelectedConsumer = selectedConsumer?.let { repository.getWorkResultByConsumerId(it.id) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { fileUri ->
            coroutineScope.launch {
                kotlin.runCatching {
                    context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                        val rawFileName = getFileNameFromUri(context, fileUri)
                        val fileName = rawFileName.replace(".xlsx", "", ignoreCase = true)
                        val worksheetId = "worksheet_${System.currentTimeMillis()}"
                        val consumers = ExcelParser().parseWorkbook(inputStream, worksheetId)
                        if (consumers.isNotEmpty()) {
                            repository.addWorksheet(fileName, consumers)
                            notificationMessage = "Файл успішно завантажено"
                            notificationColor = StatusGreen

                            currentTab = BottomTab.TASKS
                            currentWorksheetsScreen = AppScreen.Worksheets
                        } else {
                            notificationMessage = "Файл пустий або пошкоджений"
                            notificationColor = StatusRed
                        }
                    }
                }.onFailure {
                    notificationMessage = "Помилка: ${it.message}"
                    notificationColor = StatusRed
                }
            }
        }
        launchImport = false
    }

    if (launchImport) {
        LaunchedEffect(Unit) { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
    }

    if (failedList.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { geoService.clearErrors() },
            title = { Text("Звіт пошуку") },
            text = {
                Column {
                    Text("Не знайдено адрес: ${failedList.size}")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(failedList) { addr ->
                            Text("- $addr", fontSize = 12.sp)
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { geoService.clearErrors() }) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            floatingActionButtonPosition = FabPosition.Start,
            bottomBar = {
                if (currentTab != BottomTab.TASKS || (currentWorksheetsScreen != AppScreen.ConsumerDetail && currentWorksheetsScreen != AppScreen.ProcessConsumer && currentWorksheetsScreen !is AppScreen.EditLocation)) {

                    // 🔥 ОБНОВЛЕННАЯ ПАНЕЛЬ (Compact & High Contrast)
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomTab.values().forEach { tab ->
                                val isSelected = currentTab == tab

                                // Цвета для активного/неактивного состояния
                                val iconColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                val textColor = if (isSelected) CyanAction else MaterialTheme.colorScheme.onSurfaceVariant
                                val backgroundShape = if (isSelected) CyanAction else Color.Transparent

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (tab == BottomTab.TASKS) {
                                                currentWorksheetsScreen = AppScreen.Worksheets
                                                selectedConsumer = null
                                                isNavigatedFromMap = false
                                            }
                                            currentTab = tab
                                        }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(backgroundShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                AnimatedContent(targetState = currentTab, label = "TabAnim") { targetTab ->
                    when (targetTab) {
                        BottomTab.HOME -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = "Привіт, Владислав",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Гарного дня для роботи!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val totalConsumers = worksheets.sumOf { it.totalConsumers }

                                    StatCard(
                                        title = "Завантажено точок",
                                        value = "$totalConsumers",
                                        modifier = Modifier.weight(1f)
                                    )

                                    StatCard(
                                        title = "Загальний борг",
                                        value = "--- грн",
                                        modifier = Modifier.weight(1f),
                                        valueColor = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                MappingGradientButton(
                                    text = "ЗАВАНТАЖИТИ EXCEL",
                                    icon = Icons.Default.FileUpload,
                                    onClick = { launchImport = true }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    MenuGridButton(
                                        title = "Карта",
                                        icon = Icons.Default.Map,
                                        modifier = Modifier.weight(1f),
                                        onClick = { currentTab = BottomTab.MAP }
                                    )

                                    MenuGridButton(
                                        title = "Звіти",
                                        icon = Icons.Default.Description,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            currentTab = BottomTab.TASKS
                                            currentWorksheetsScreen = AppScreen.WorkResults
                                        }
                                    )
                                }
                            }
                        }

                        BottomTab.MAP -> {
                            MapScreen(
                                worksheets = worksheets,
                                consumers = mapConsumersList,
                                totalCount = totalConsumersCount,
                                foundCount = foundCoordinatesCount,
                                initialCenter = mapCenterState,
                                initialZoom = mapZoomState,
                                cameraTarget = mapCameraTarget,
                                onCameraTargetSettled = { mapCameraTarget = null },
                                onMapStateChanged = { center, zoom ->
                                    mapCenterState = center
                                    mapZoomState = zoom
                                },
                                onWorksheetSelected = { worksheet ->
                                    selectedMapWorksheetId = worksheet.id
                                    geoService.startForWorksheet(worksheet.id)
                                    notificationMessage = "Запуск пошуку адрес..."
                                    notificationColor = CyanAction
                                },
                                onConsumerClick = { consumer ->
                                    selectedConsumer = consumer
                                    isNavigatedFromMap = true
                                    currentTab = BottomTab.TASKS
                                    currentWorksheetsScreen = AppScreen.ConsumerDetail
                                },
                                isGeocoding = isGeocoding,
                                progress = geoProgress
                            )
                        }

                        BottomTab.PROFILE -> ProfileScreen(currentTheme = currentTheme, onThemeSelected = onThemeChanged)

                        BottomTab.TASKS -> {
                            AnimatedContent(targetState = currentWorksheetsScreen, label = "ListAnim") { targetScreen ->
                                when (targetScreen) {
                                    AppScreen.Worksheets -> WorksheetsScreen(
                                        worksheets = worksheets,
                                        onWorksheetClick = { ws ->
                                            selectedWorksheetId = ws.id
                                            isNavigatedFromMap = false
                                            currentWorksheetsScreen = AppScreen.ConsumerList
                                        },
                                        onAddWorksheet = { launchImport = true },
                                        onDeleteWorksheet = { ws -> coroutineScope.launch { repository.deleteWorksheet(ws) } },
                                        onRenameWorksheet = { ws, name -> coroutineScope.launch { repository.renameWorksheet(ws, name) } },
                                        onViewResults = { currentWorksheetsScreen = AppScreen.WorkResults },
                                        onBackClick = {}
                                    )
                                    AppScreen.ConsumerList -> ConsumerListScreen(
                                        consumers = worksheetConsumers,
                                        onConsumerClick = { c ->
                                            selectedConsumer = c
                                            isNavigatedFromMap = false
                                            currentWorksheetsScreen = AppScreen.ConsumerDetail
                                        },
                                        onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets }
                                    )

                                    AppScreen.ConsumerDetail -> ConsumerDetailScreen(
                                        consumer = selectedConsumer!!,
                                        workResult = workResultForSelectedConsumer,
                                        onBackClick = {
                                            if (isNavigatedFromMap) {
                                                currentTab = BottomTab.MAP
                                            } else {
                                                currentWorksheetsScreen = AppScreen.ConsumerList
                                            }
                                        },
                                        onProcessClick = { currentWorksheetsScreen = AppScreen.ProcessConsumer },
                                        onManualLocationClick = {
                                            currentWorksheetsScreen = AppScreen.EditLocation(selectedConsumer!!)
                                        },
                                        onMapClick = {
                                            selectedConsumer?.let { consumer ->
                                                if (consumer.latitude != null && consumer.latitude != 0.0) {
                                                    mapCameraTarget = GeoPoint(consumer.latitude!!, consumer.longitude!!)
                                                    currentTab = BottomTab.MAP
                                                    notificationMessage = "Перехід на карту..."
                                                    notificationColor = StatusGreen
                                                } else {
                                                    notificationMessage = "Пошук координат..."
                                                    notificationColor = CyanAction
                                                    val smartAddress = constructSmartAddress(consumer.rawAddress)
                                                    AddressHelper.searchAddress(smartAddress) { location ->
                                                        coroutineScope.launch(Dispatchers.Main) {
                                                            if (location == null) {
                                                                notificationMessage = "Не знайдено! Перевірте адресу"
                                                                notificationColor = StatusRed
                                                            } else {
                                                                consumer.latitude = location.lat
                                                                consumer.longitude = location.lng
                                                                repository.updateConsumer(consumer)
                                                                notificationMessage = "Знайдено! Натисніть ще раз"
                                                                notificationColor = StatusGreen
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    is AppScreen.EditLocation -> EditLocationScreen(
                                        consumer = targetScreen.consumer,
                                        onSave = { lat, lng ->
                                            val c = targetScreen.consumer
                                            c.latitude = lat
                                            c.longitude = lng
                                            coroutineScope.launch {
                                                repository.updateConsumer(c)
                                                notificationMessage = "Координати збережено вручну"
                                                notificationColor = StatusGreen
                                                currentWorksheetsScreen = AppScreen.ConsumerDetail
                                            }
                                        },
                                        onCancel = { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                    )

                                    AppScreen.ProcessConsumer -> ProcessConsumerScreen(
                                        consumer = selectedConsumer!!,
                                        initialResult = workResultForSelectedConsumer,
                                        onSave = { result ->
                                            coroutineScope.launch {
                                                repository.saveWorkResult(selectedConsumer!!.id, result)
                                                withContext(Dispatchers.Main) {
                                                    workResultForSelectedConsumer = result
                                                    currentWorksheetsScreen = AppScreen.ConsumerDetail
                                                    notificationMessage = "ОР ${selectedConsumer!!.orNumber} - опрацьовано!"
                                                    notificationColor = StatusGreen
                                                }
                                            }
                                        },
                                        onCancel = { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                    )

                                    AppScreen.WorkResults -> WorkResultsScreen(
                                        worksheets = worksheets,
                                        onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets },
                                        onExportClick = { worksheet ->
                                            notificationMessage = "Генерую звіт..."
                                            notificationColor = CyanAction
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val consumers = repository.getConsumersFlow(worksheet.id).first()
                                                    val dataForExport = consumers.map { consumer ->
                                                        val result = repository.getWorkResultByConsumerId(consumer.id)
                                                        consumer to result
                                                    }
                                                    val file = ExcelParser().exportWorksheet(context, worksheet.fileName, dataForExport)
                                                    withContext(Dispatchers.Main) {
                                                        shareExcelFile(context, file)
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        notificationMessage = "Помилка експорту: ${e.message}"
                                                        notificationColor = StatusRed
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }

        TopSuccessNotification(
            message = notificationMessage ?: "",
            isVisible = notificationMessage != null,
            onDismiss = { notificationMessage = null },
            backgroundColor = notificationColor
        )
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) result = result?.substring(cut + 1)
    }
    return result ?: "imported_file.xlsx"
}

fun shareExcelFile(context: Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = android.content.Intent.createChooser(intent, "Надіслати звіт через...")
    context.startActivity(chooser)
}

sealed class AppScreen {
    object Worksheets : AppScreen()
    object ConsumerList : AppScreen()
    object ConsumerDetail : AppScreen()
    object ProcessConsumer : AppScreen()
    object ImportExcel : AppScreen()
    object WorkResults : AppScreen()
    data class EditLocation(val consumer: Consumer) : AppScreen()
}

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Головна", Icons.Filled.Home),
    TASKS("Задачі", Icons.Filled.Assignment),
    MAP("Мапа", Icons.Filled.Map),
    PROFILE("Профіль", Icons.Filled.Person)
}