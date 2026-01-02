package com.roninsoulkh.mappingop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roninsoulkh.mappingop.data.local.TokenManager
import com.roninsoulkh.mappingop.data.repository.AuthRepository
import com.roninsoulkh.mappingop.domain.models.ChangePasswordRequest
import com.roninsoulkh.mappingop.domain.models.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _savedCredentials = MutableStateFlow<Pair<String, String>>(Pair("", ""))
    val savedCredentials: StateFlow<Pair<String, String>> = _savedCredentials.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.savedCredentials.collect { pair ->
                _savedCredentials.value = pair
            }
        }
    }

    // --- ОБЫЧНЫЙ ВХОД ---
    fun login(login: String, pass: String, rememberMe: Boolean) {
        if (login.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Заповніть всі поля")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val request = LoginRequest(email = login, password = pass)
            val result = repository.login(request)

            result.fold(
                onSuccess = { response ->
                    if (response.status == "password_change_required") {
                        _uiState.value = AuthUiState.PasswordChangeRequired(email = login, tempPass = pass)
                    } else {
                        if (rememberMe) tokenManager.saveCredentials(login, pass)
                        else tokenManager.clearCredentials()

                        tokenManager.setLoggedIn(true) // Сохраняем сессию
                        _uiState.value = AuthUiState.Success
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Помилка входу")
                }
            )
        }
    }

    // --- 🔥 НОВАЯ ФУНКЦИЯ: ГОСТЕВОЙ РЕЖИМ ---
    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // Сохраняем фейковые данные, чтобы приложение думало, что мы вошли
            tokenManager.setLoggedIn(true)
            _uiState.value = AuthUiState.Success
        }
    }

    // --- 🔥 НОВАЯ ФУНКЦИЯ: ВЫХОД ---
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearSession() // Удаляем "галочку" входа
            _uiState.value = AuthUiState.Idle // Сбрасываем состояние
        }
    }

    fun changePassword(email: String, oldPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val request = ChangePasswordRequest(email, oldPass, newPass)
            val result = repository.changePassword(request)
            result.fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { error -> _uiState.value = AuthUiState.Error(error.message ?: "Помилка") }
            )
        }
    }

    fun clearError() { _uiState.value = AuthUiState.Idle }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class PasswordChangeRequired(val email: String, val tempPass: String) : AuthUiState()
}

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}