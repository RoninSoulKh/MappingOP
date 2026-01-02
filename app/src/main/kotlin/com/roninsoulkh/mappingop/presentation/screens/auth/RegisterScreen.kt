package com.roninsoulkh.mappingop.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roninsoulkh.mappingop.ui.components.MappingGradientButton
import com.roninsoulkh.mappingop.ui.components.MappingTextField
import com.roninsoulkh.mappingop.ui.theme.DarkBackground

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                text = "Реєстрація",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        MappingTextField(
            value = username,
            onValueChange = { username = it },
            label = "Ім'я користувача",
            icon = Icons.Default.Person
        )

        Spacer(modifier = Modifier.height(16.dp))

        MappingTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email (Робочий)",
            icon = Icons.Default.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        MappingTextField(
            value = password,
            onValueChange = { password = it },
            label = "Пароль",
            icon = Icons.Default.Lock
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            MappingGradientButton(
                text = "Створити акаунт",
                onClick = { onRegisterClick(username, email, password) }
            )
        }
    }
}