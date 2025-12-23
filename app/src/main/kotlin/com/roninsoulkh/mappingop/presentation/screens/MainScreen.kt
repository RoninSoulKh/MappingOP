package com.roninsoulkh.mappingop.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen() {
    var excelFileSelected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📊 Mapping OP",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (excelFileSelected)
                "✅ Файл загружен"
            else
                "Выберите Excel файл с ведомостью",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { excelFileSelected = true },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("📁 Загрузить Excel файл")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { /* TODO: Переход к карте */ },
            enabled = excelFileSelected,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("🗺️ Открыть карту")
        }
    }
}