package com.example.aqualuminus.ui.screens.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (!validateInputs(email, password)) return

        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmailAndPassword(
                    email.trim(),
                    password
                )

                result.fold(
                    onSuccess = { user ->
                        _isLoading.value = false
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = authRepository.getFirebaseErrorMessage(exception as Exception)
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Login failed. Please try again."
                _isLoading.value = false
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        when {
            email.isBlank() -> {
                _errorMessage.value = "Email is required"
                return false
            }
            password.isBlank() -> {
                _errorMessage.value = "Password is required"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _errorMessage.value = "Please enter a valid email address"
                return false
            }
        }
        return true
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}