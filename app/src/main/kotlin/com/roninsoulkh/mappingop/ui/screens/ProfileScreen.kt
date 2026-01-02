package com.roninsoulkh.mappingop.ui.screens

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roninsoulkh.mappingop.utils.SettingsManager
import com.roninsoulkh.mappingop.ui.theme.CyanAction
import com.roninsoulkh.mappingop.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Настройки
    var selectedLanguage by remember { mutableStateOf(SettingsManager.getLanguage(context)) }
    var syncEnabled by remember { mutableStateOf(true) }

    // Версия
    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: Exception) { "1.0.0" }
    }

    // Цвета
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
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
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = textColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
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
            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. АВАТАРКА И ИНФО ---
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(2.dp, CyanAction, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(cardColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(55.dp), tint = CyanAction)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Владислав",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "Administrator",
                style = MaterialTheme.typography.labelLarge,
                color = CyanAction,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- КОПИРОВАНИЕ ID ---
            val userId = "ID: 8800-555"
            Surface(
                color = cardColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    clipboardManager.setText(AnnotatedString("8800-555"))
                    Toast.makeText(context, "ID скопійовано", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(userId, style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ContentCopy, null, tint = textSecondary, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 2. СТАТИСТИКА ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CleanStatItem("12", "Опрацьовано", Icons.Default.CheckCircle, CyanAction, textColor)
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(textColor.copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically)
                )
                CleanStatItem("5", "В черзі", Icons.Default.Schedule, Color(0xFFFFA000), textColor)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 3. ТРИ ГЛАВНЫЕ КНОПКИ (НОВИЙ ПРЕМІУМ ДИЗАЙН) ---

            // 1. Оновлення
            ActionCard(
                icon = Icons.Outlined.SystemUpdate,
                title = "Перевірити оновлення",
                subtitle = "Встановлено: $appVersion",
                accentColor = CyanAction, // Фірмовий колір
                textColor = textColor,
                onClick = { /* Логика проверки */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Техподдержка
            ActionCard(
                icon = Icons.Outlined.SupportAgent,
                title = "Зв'язатися з підтримкою",
                subtitle = "Telegram / Телефон",
                accentColor = Color(0xFF7E57C2), // Фіолетовий акцент
                textColor = textColor,
                onClick = { /* Логика звонка */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Выход
            ActionCard(
                icon = Icons.Default.ExitToApp,
                title = "Вийти з акаунту",
                subtitle = null,
                accentColor = StatusRed, // Червоний акцент
                textColor = StatusRed, // Червоний текст
                onClick = onLogout
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- 4. ЛИЦЕНЗИЯ ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Ліцензія Активна",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAction,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RoninSoulKh Development © 2025",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }

    // --- ⚙️ ШТОРКА НАСТРОЕК ---
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    "Налаштування",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Синхронизация
                RowItem(
                    icon = Icons.Default.CloudSync,
                    text = "Авто-синхронізація",
                    textColor = textColor,
                    endContent = {
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { syncEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = CyanAction)
                        )
                    }
                )
                Divider(color = textColor.copy(alpha = 0.1f))

                // Тема
                RowItem(
                    icon = if (currentTheme == "Light") Icons.Default.LightMode else Icons.Default.DarkMode,
                    text = "Тема: ${if (currentTheme == "Light") "Світла" else "Темна"}",
                    textColor = textColor,
                    onClick = {
                        val next = if (currentTheme == "Light") "Dark" else "Light"
                        onThemeSelected(next)
                    }
                )

                // Язык
                RowItem(
                    icon = Icons.Default.Language,
                    text = "Мова: ${if (selectedLanguage == "Ukr") "Українська" else "English"}",
                    textColor = textColor,
                    onClick = {
                        val next = if (selectedLanguage == "Ukr") "Eng" else "Ukr"
                        selectedLanguage = next
                        SettingsManager.saveLanguage(context, next)
                    }
                )
                Divider(color = textColor.copy(alpha = 0.1f))

                // Кеш карт
                RowItem(
                    icon = Icons.Outlined.Map,
                    text = "Кеш карт (124 МБ)",
                    textColor = textColor,
                    onClick = {}
                )
            }
        }
    }
}

// --- КОМПОНЕНТЫ ---

@Composable
fun CleanStatItem(value: String, label: String, icon: ImageVector, accentColor: Color, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.6f)
        )
    }
}

// 🔥 НОВИЙ ПРЕМІУМ ДИЗАЙН КНОПКИ (Без сірих блоків)
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    accentColor: Color, // Акцентний колір (для рамки та іконки)
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Фон прозорий, але з тонкою кольоровою рамкою
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        color = Color.Transparent // Прибираємо "сірий блок"
    ) {
        // Додаємо легкий внутрішній тінт кольору
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.05f)) // Ледь помітний фон
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Іконка в кольоровому колі
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f)), // Напівпрозоре коло
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                if (subtitle != null) {
                    Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun RowItem(
    icon: ImageVector,
    text: String,
    textColor: Color,
    onClick: (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = textColor, modifier = Modifier.weight(1f))

        if (endContent != null) {
            endContent()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = textColor.copy(alpha = 0.3f))
        }
    }
}