package com.example.aqualuminus.ui.screens.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            delay(1500)
            if (username.trim().lowercase() == "admin" && password == "aquarium123") {
                onSuccess()
            } else {
                _errorMessage.value = "Invalid username or password"
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }

}
