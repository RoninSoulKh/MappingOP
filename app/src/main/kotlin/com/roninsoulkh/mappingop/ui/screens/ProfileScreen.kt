package com.roninsoulkh.mappingop.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roninsoulkh.mappingop.utils.SettingsManager
import com.roninsoulkh.mappingop.ui.theme.CyanAction // Импортируем только акцентный цвет

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(SettingsManager.getLanguage(context)) }
    var syncEnabled by remember { mutableStateOf(true) }

    // Получение версии
    val appVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "v1.1.1"
        }
    }

    // Используем цвета темы, чтобы работала Light/Dark тема
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Профіль",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                actions = {
                    IconButton(onClick = { /* Настройки */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Налаштування", tint = CyanAction)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- КАРТОЧКА ПОЛЬЗОВАТЕЛЯ ---
            // Передаем цвета внутрь, чтобы они реагировали на тему
            UserCardSection(
                cardColor = cardColor,
                textColor = textColor,
                subTextColor = textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- НАСТРОЙКИ ---
            SectionLabel("МОВА", textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigToggleButton(
                    text = "Eng",
                    isSelected = selectedLanguage == "Eng",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    textColor = textSecondary,
                    onClick = {
                        selectedLanguage = "Eng"
                        SettingsManager.saveLanguage(context, "Eng")
                    }
                )
                BigToggleButton(
                    text = "Укр",
                    isSelected = selectedLanguage == "Ukr",
                    modifier = Modifier.weight(1f),
                    cardColor = cardColor,
                    textColor = textSecondary,
                    onClick = {
                        selectedLanguage = "Ukr"
                        SettingsManager.saveLanguage(context, "Ukr")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("ТЕМА", textSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BigToggleButton("Auto", currentTheme == "Auto", Modifier.weight(1f), cardColor, textSecondary) { onThemeSelected("Auto") }
                BigToggleButton("Light", currentTheme == "Light", Modifier.weight(1f), cardColor, textSecondary) { onThemeSelected("Light") }
                BigToggleButton("Dark", currentTheme == "Dark", Modifier.weight(1f), cardColor, textSecondary) { onThemeSelected("Dark") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- СИНХРОНИЗАЦИЯ ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardColor)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Синхронізація", color = textColor, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { syncEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyanAction,
                            uncheckedThumbColor = textSecondary,
                            uncheckedTrackColor = backgroundColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- ВЕРСИЯ (КНОПКА ПРОВЕРКИ ОБНОВЛЕНИЙ) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardColor)
                    .clickable { /* Логика проверки обновлений */ }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Версія додатку", color = textColor, fontWeight = FontWeight.Medium)
                    }

                    Text(
                        text = appVersion,
                        color = CyanAction,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Распорка, толкающая лицензию вниз
            Spacer(modifier = Modifier.weight(1f))

            // --- ЛИЦЕНЗИЯ ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Отступ снизу минимальный (4.dp), чтобы было почти в упор к навбару
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Ліцензія Активна",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanAction,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RoninSoulKh Development © 2025",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// --- КОМПОНЕНТЫ ---

@Composable
fun UserCardSection(cardColor: Color, textColor: Color, subTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(2.dp, CyanAction, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(cardColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, null, modifier = Modifier.size(50.dp), tint = subTextColor)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Владислав", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(color = CyanAction.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
            Text("Administrator", Modifier.padding(12.dp, 4.dp), style = MaterialTheme.typography.labelSmall, color = CyanAction, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("ID: 8800-555", style = MaterialTheme.typography.bodySmall, color = subTextColor)
    }
}

@Composable
fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun BigToggleButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanAction else cardColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            // Если выбрано - черный (на циане), если нет - цвет текста темы
            color = if (isSelected) Color.Black else textColor,
            fontWeight = FontWeight.Bold
        )
    }
}