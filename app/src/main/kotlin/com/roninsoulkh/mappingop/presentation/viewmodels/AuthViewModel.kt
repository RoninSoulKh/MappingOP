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
            tokenManager.savedCredentials.collect { _savedCredentials.value = it }
        }
    }

    fun login(login: String, pass: String, rememberMe: Boolean) {
        if (login.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Заполните все поля")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(LoginRequest(login, pass))
            result.fold(
                onSuccess = { response ->
                    if (rememberMe) tokenManager.saveCredentials(login, pass)
                    else tokenManager.clearCredentials()

                    if (response.requires_password_change) {
                        _uiState.value = AuthUiState.PasswordChangeRequired(login, pass)
                    } else {
                        _uiState.value = AuthUiState.Success
                    }
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Ошибка входа")
                }
            )
        }
    }

    fun changePassword(email: String, oldPass: String, newPass: String, confirmPass: String) {
        if (newPass != confirmPass) {
            _uiState.value = AuthUiState.Error("Пароли не совпадают")
            return
        }
        if (newPass.length < 8) {
            _uiState.value = AuthUiState.Error("Пароль должен быть не менее 8 символов")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val request = ChangePasswordRequest(email, oldPass, newPass, confirmPass)
            val result = repository.changePassword(request)
            result.fold(
                onSuccess = { _uiState.value = AuthUiState.Success },
                onFailure = { error -> _uiState.value = AuthUiState.Error(error.message ?: "Ошибка") }
            )
        }
    }

    // --- 🔥 ДОБАВЛЕННЫЕ ФУНКЦИИ (которых не хватало) ---

    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            tokenManager.setLoggedIn(true)
            _uiState.value = AuthUiState.Success
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    // ---------------------------------------------------

    fun clearError() { _uiState.value = AuthUiState.Idle }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    data class PasswordChangeRequired(val email: String, val tempPass: String) : AuthUiState()
}

class AuthViewModelFactory(private val repository: AuthRepository, private val tokenManager: TokenManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository, tokenManager) as T
    }
}