package com.roninsoulkh.mappingop.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roninsoulkh.mappingop.ui.components.MappingGradientButton
import com.roninsoulkh.mappingop.ui.components.MappingTextField
import com.roninsoulkh.mappingop.ui.theme.CyanAction
import com.roninsoulkh.mappingop.ui.theme.DarkBackground

@Composable
fun LoginScreen(
    initialLogin: String = "",
    initialPass: String = "",
    onLoginClick: (String, String, Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    isLoading: Boolean = false
) {
    var login by remember(initialLogin) { mutableStateOf(initialLogin) }
    var password by remember(initialPass) { mutableStateOf(initialPass) }
    var isRememberMe by remember(initialLogin) { mutableStateOf(initialLogin.isNotEmpty()) }

    // false = скрыто (точки), true = видно (текст)
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mapping OP",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Enterprise Solution",
            style = MaterialTheme.typography.labelMedium,
            color = CyanAction,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        MappingTextField(
            value = login,
            onValueChange = { login = it },
            label = "Логін або Email",
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔥 ФИКС ГЛАЗИКА
        MappingTextField(
            value = password,
            onValueChange = { password = it },
            label = "Пароль",
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = CyanAction
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isRememberMe = !isRememberMe }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isRememberMe,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanAction,
                    uncheckedColor = Color.White.copy(alpha = 0.5f),
                    checkmarkColor = DarkBackground
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Запам'ятати мене",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = CyanAction)
        } else {
            MappingGradientButton(
                text = "Увійти",
                onClick = { onLoginClick(login, password, isRememberMe) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Немає акаунту? Зареєструватися",
            style = MaterialTheme.typography.bodyMedium,
            color = CyanAction.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onRegisterClick() }
        )
    }
}