package com.example.aqualuminus.ui.screens.auth.register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus.data.repository.FirebaseAuthRepository
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _uiState = mutableStateOf(RegisterUiState())
    val uiState: State<RegisterUiState> = _uiState

    fun onUsernameChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(username = newValue, errorMessage = "", successMessage = "")
    }

    fun onEmailChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(email = newValue, errorMessage = "", successMessage = "")
    }

    fun onPasswordChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(password = newValue, errorMessage = "", successMessage = "")
    }

    fun onConfirmPasswordChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = newValue, errorMessage = "", successMessage = "")
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(passwordVisible = !_uiState.value.passwordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(confirmPasswordVisible = !_uiState.value.confirmPasswordVisible)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = "", successMessage = "")
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validation = validateInputs(
            state.username,
            state.email,
            state.password,
            state.confirmPassword
        )
        if (validation != null) {
            _uiState.value = state.copy(errorMessage = validation)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = "", successMessage = "")

        viewModelScope.launch {
            try {
                val result = authRepository.createUserWithEmailAndPassword(
                    state.email.trim(),
                    state.password
                )

                result.fold(
                    onSuccess = { user ->
                        val profileUpdates = userProfileChangeRequest {
                            displayName = state.username
                        }

                        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _uiState.value = _uiState.value.copy(
                                    successMessage = "Account created successfully!",
                                    isLoading = false
                                )
                                viewModelScope.launch {
                                    delay(1500)
                                    onSuccess()
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = "Failed to update profile. Please try again.",
                                    isLoading = false
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = authRepository.getFirebaseErrorMessage(exception as Exception),
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Registration failed. Please try again.",
                    isLoading = false
                )
            }
        }
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        if (username.isBlank()) return "Username is required"
        if (username.length < 3) return "Username must be at least 3 characters long"
        if (username.length > 20) return "Username must be less than 20 characters"
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) return "Username can only contain letters, numbers, and underscores"

        if (email.isBlank()) return "Email is required"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email address"

        if (password.isBlank()) return "Password is required"
        if (password.length < 6) return "Password must be at least 6 characters long"

        if (confirmPassword.isBlank()) return "Please confirm your password"
        if (password != confirmPassword) return "Passwords do not match"

        return null
    }
}
