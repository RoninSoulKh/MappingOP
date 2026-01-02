package com.roninsoulkh.mappingop.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import com.roninsoulkh.mappingop.ui.theme.StatusRed

@Composable
fun ChangePasswordScreen(
    email: String,
    onChangeClick: (String, String) -> Unit, // oldPass, newPass
    isLoading: Boolean = false
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isReady = oldPassword.isNotEmpty() && isMatch

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LockReset,
            contentDescription = null,
            tint = CyanAction,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Зміна пароля",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Система вимагає змінити тимчасовий пароль.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        MappingTextField(
            value = oldPassword,
            onValueChange = { oldPassword = it },
            label = "Старий пароль",
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(16.dp))

        MappingTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "Новий пароль",
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(16.dp))

        MappingTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Підтвердіть пароль",
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password
        )

        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !isMatch) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = StatusRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Паролі не співпадають", color = StatusRed, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = CyanAction)
        } else {
            MappingGradientButton(
                text = "Змінити пароль",
                onClick = {
                    if (isReady) onChangeClick(oldPassword, newPassword)
                }
            )
        }
    }
}