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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.roninsoulkh.mappingop.data.parser.ExcelParser
import com.roninsoulkh.mappingop.domain.models.Consumer
import com.roninsoulkh.mappingop.ui.screens.*
import com.roninsoulkh.mappingop.domain.models.WorkResult
import com.roninsoulkh.mappingop.domain.models.Worksheet
import com.roninsoulkh.mappingop.ui.theme.MappingOPTheme
import com.roninsoulkh.mappingop.data.repository.AppRepository
import com.roninsoulkh.mappingop.utils.ExcelUtils
import com.roninsoulkh.mappingop.utils.SettingsManager
import com.roninsoulkh.mappingop.data.database.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val repository by lazy { AppRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ВНИМАНИЕ: System.setProperty отсюда УБРАЛИ. Он теперь внизу, в companion object.

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
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            MappingOPTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

    // --- ВОТ ГЛАВНЫЙ ФИКС ДЛЯ EXCEL ---
    companion object {
        init {
            // Эта настройка сработает самой первой, еще до загрузки Activity.
            // Мы ставим "пустой" логгер, чтобы Excel не искал Log4j.
            System.setProperty("org.apache.poi.util.POILogger", "org.apache.poi.util.NullLogger")
        }
    }
}

// --- ВСЕ ОСТАЛЬНЫЕ КЛАССЫ ТЕПЕРЬ СНАРУЖИ (КАК И ДОЛЖНО БЫТЬ) ---

enum class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Головна", Icons.Filled.Home),
    LIST("Відомості", Icons.Filled.List),
    MAP("Мапа", Icons.Filled.Map),
    PROFILE("Профіль", Icons.Filled.Person)
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
    var selectedWorksheetId by remember { mutableStateOf<String?>(null) }
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var launchImport by remember { mutableStateOf(false) }

    var showSuccessToast by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var showErrorToast by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(showSuccessToast) { if (showSuccessToast) { Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show(); showSuccessToast = false } }
    LaunchedEffect(showErrorToast) { if (showErrorToast) { Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show(); showErrorToast = false } }

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

    val showBottomBar = when (currentTab) {
        BottomTab.LIST -> {
            currentWorksheetsScreen != AppScreen.ConsumerDetail &&
                    currentWorksheetsScreen != AppScreen.ProcessConsumer
        }
        else -> true
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
                            successMessage = "✅ Імпорт успішний: $fileName"; showSuccessToast = true
                            currentTab = BottomTab.LIST
                            currentWorksheetsScreen = AppScreen.Worksheets
                        } else {
                            errorMessage = "Файл пустий"; showErrorToast = true
                        }
                    }
                }.onFailure { errorMessage = "Помилка: ${it.message}"; showErrorToast = true }
            }
        }
        launchImport = false
    }

    if (launchImport) {
        LaunchedEffect(Unit) { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomTab.values().forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = currentTab == tab,
                            onClick = { currentTab = tab }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {

            // 1. Анимация переключения вкладок (Головна, Відомості, Мапа, Профіль)
            AnimatedContent(
                targetState = currentTab,
                label = "TabAnimation",
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { targetTab ->
                when (targetTab) {
                    BottomTab.HOME -> {
                        HomeScreen(
                            worksheets = worksheets,
                            onImportClick = { launchImport = true },
                            onExportClick = { worksheet ->
                                coroutineScope.launch {
                                    try {
                                        val db = AppDatabase.getDatabase(context)
                                        val results = db.workResultDao().getWorkResultsByWorksheetId(worksheet.id)
                                        val consumers = db.consumerDao().getConsumersByWorksheetIdSync(worksheet.id)
                                        val combinedData = consumers.map { c ->
                                            val res = results.find { it.consumerId == c.id }
                                            c to res
                                        }
                                        val sortedData = combinedData.sortedWith(
                                            compareByDescending<Pair<Consumer, WorkResult?>> { (_, res) -> res?.processedAt ?: 0L }
                                                .thenBy { (c, _) -> c.rawAddress }
                                        )
                                        ExcelUtils.exportReport(context, worksheet.fileName, sortedData)
                                    } catch (e: Exception) {
                                        errorMessage = "Помилка експорту: ${e.message}"; showErrorToast = true
                                    }
                                }
                            }
                        )
                    }
                    BottomTab.MAP -> MapScreen()
                    BottomTab.PROFILE -> ProfileScreen(
                        currentTheme = currentTheme,
                        onThemeSelected = onThemeChanged
                    )
                    BottomTab.LIST -> {
                        // 2. Анимация переключения экранов внутри Ведомостей
                        AnimatedContent(
                            targetState = currentWorksheetsScreen,
                            label = "ListScreenAnimation",
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            }
                        ) { targetScreen ->
                            when (targetScreen) {
                                AppScreen.Worksheets -> {
                                    WorksheetsScreen(
                                        worksheets = worksheets,
                                        onWorksheetClick = { ws -> selectedWorksheetId = ws.id; currentWorksheetsScreen = AppScreen.ConsumerList },
                                        onAddWorksheet = { launchImport = true },
                                        onBackClick = { },
                                        onViewResults = { currentWorksheetsScreen = AppScreen.WorkResults },
                                        onDeleteWorksheet = { ws -> coroutineScope.launch { repository.deleteWorksheet(ws) } },
                                        onRenameWorksheet = { ws, newName ->
                                            coroutineScope.launch {
                                                repository.renameWorksheet(ws, newName)
                                                successMessage = "Перейменовано"; showSuccessToast = true
                                            }
                                        }
                                    )
                                }
                                AppScreen.ConsumerList -> {
                                    BackHandler { currentWorksheetsScreen = AppScreen.Worksheets }
                                    ConsumerListScreen(
                                        consumers = worksheetConsumers,
                                        onConsumerClick = { c -> selectedConsumer = c; currentWorksheetsScreen = AppScreen.ConsumerDetail },
                                        onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets }
                                    )
                                }
                                AppScreen.ConsumerDetail -> {
                                    BackHandler { currentWorksheetsScreen = AppScreen.ConsumerList }
                                    if (selectedConsumer != null) {
                                        ConsumerDetailScreen(
                                            consumer = selectedConsumer!!,
                                            workResult = workResultForSelectedConsumer,
                                            onBackClick = { currentWorksheetsScreen = AppScreen.ConsumerList },
                                            onProcessClick = { currentWorksheetsScreen = AppScreen.ProcessConsumer }
                                        )
                                    }
                                }
                                AppScreen.ProcessConsumer -> {
                                    BackHandler { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                    if (selectedConsumer != null) {
                                        ProcessConsumerScreen(
                                            consumer = selectedConsumer!!,
                                            initialResult = workResultForSelectedConsumer,
                                            onSave = { result ->
                                                coroutineScope.launch {
                                                    repository.saveWorkResult(selectedConsumer!!.id, result)
                                                    withContext(Dispatchers.Main) {
                                                        workResultForSelectedConsumer = result
                                                        currentWorksheetsScreen = AppScreen.ConsumerDetail
                                                        successMessage = "✅ Дані збережено"; showSuccessToast = true
                                                    }
                                                }
                                            },
                                            onCancel = { currentWorksheetsScreen = AppScreen.ConsumerDetail }
                                        )
                                    }
                                }
                                AppScreen.WorkResults -> {
                                    BackHandler { currentWorksheetsScreen = AppScreen.Worksheets }
                                    WorkResultsScreen(onBackClick = { currentWorksheetsScreen = AppScreen.Worksheets })
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "imported_file.xlsx"
}

sealed class AppScreen {
    object Worksheets : AppScreen()
    object ConsumerList : AppScreen()
    object ConsumerDetail : AppScreen()
    object ProcessConsumer : AppScreen()
    object ImportExcel : AppScreen()
    object WorkResults : AppScreen()
}