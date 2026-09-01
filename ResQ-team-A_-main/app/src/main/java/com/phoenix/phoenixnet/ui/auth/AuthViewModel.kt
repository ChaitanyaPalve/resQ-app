package com.phoenix.phoenixnet.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.phoenixnet.db.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var firstName by mutableStateOf("")
    var middleName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var triggerWord by mutableStateOf("")

    private val _authState = MutableStateFlow<AuthResult?>(null)
    val authState: StateFlow<AuthResult?> = _authState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = authRepository.getActiveSession()
            if (session != null) {
                _authState.value = AuthResult.Authenticated
            }
        }
    }

    fun onRegister(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.register(firstName, middleName, lastName, triggerWord)
            if (result.isSuccess) {
                _authState.value = AuthResult.Success
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } else {
                _authState.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun onLogin(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.login(firstName, middleName, lastName, triggerWord)
            if (result.isSuccess) {
                _authState.value = AuthResult.Success
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            } else {
                _authState.value = AuthResult.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun onLogout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
            _authState.value = null
            // Clear input fields
            firstName = ""
            middleName = ""
            lastName = ""
            triggerWord = ""
            viewModelScope.launch(Dispatchers.Main) { onComplete() }
        }
    }

    fun clearState() {
        _authState.value = null
    }
}

sealed class AuthResult {
    object Authenticated : AuthResult()
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
