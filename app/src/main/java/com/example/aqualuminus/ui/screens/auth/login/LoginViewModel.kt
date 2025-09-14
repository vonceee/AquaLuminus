package com.example.aqualuminus.ui.screens.auth.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> = _uiState

    fun onEmailChanged(newEmail: String) {
        _uiState.value = _uiState.value.copy(
            email = newEmail,
            errorMessage = ""
        )
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.value = _uiState.value.copy(
            password = newPassword,
            errorMessage = ""
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (!validateInputs(email, password)) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = ""
        )

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmailAndPassword(email, password)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = authRepository.getFirebaseErrorMessage(exception as Exception)
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Login failed. Please try again."
                )
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "Email is required")
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "Password is required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
                false
            }
            else -> true
        }
    }
}