package com.roninsoulkh.mappingop.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.roninsoulkh.mappingop.ui.components.MappingGradientButton
import com.roninsoulkh.mappingop.ui.components.MappingTextField
import com.roninsoulkh.mappingop.ui.theme.CyanAction
import com.roninsoulkh.mappingop.ui.theme.DarkBackground
import kotlinx.coroutines.delay

@Composable
fun VerifyCodeScreen(
    email: String,
    onVerifyClick: (String) -> Unit,
    isLoading: Boolean
) {
    var code by remember { mutableStateOf("") }
    var timer by remember { mutableStateOf(60) }

    LaunchedEffect(timer) {
        if (timer > 0) {
            delay(1000L)
            timer--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Перевірка пошти",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ми надіслали код на $email",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        MappingTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = "Код з листа (6 цифр)",
            icon = Icons.Default.Key,
            keyboardType = KeyboardType.NumberPassword
        )

        Spacer(modifier = Modifier.height(32.dp))

        MappingGradientButton(
            text = "Підтвердити",
            onClick = { onVerifyClick(code) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (timer > 0) {
            Text(
                text = "Надіслати код повторно через $timer сек",
                style = MaterialTheme.typography.bodySmall,
                color = CyanAction
            )
        } else {
            Text(
                text = "Надіслати код повторно",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { timer = 60 }
            )
        }
    }
}