package com.example.aqualuminus.ui.screens.register

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

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _successMessage = mutableStateOf("")
    val successMessage: State<String> = _successMessage

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        // Validate inputs first
        val validation = validateInputs(username, email, password, confirmPassword)
        if (validation != null) {
            _errorMessage.value = validation
            return
        }

        _isLoading.value = true
        _errorMessage.value = ""
        _successMessage.value = ""

        viewModelScope.launch {
            try {
                val result = authRepository.createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )

                result.fold(
                    onSuccess = { user ->
                        // Update user profile with username
                        val profileUpdates = userProfileChangeRequest {
                            displayName = username
                        }

                        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _successMessage.value = "Account created successfully!"
                                _isLoading.value = false

                                // Auto-navigate after showing success message
                                viewModelScope.launch {
                                    delay(1500)
                                    onSuccess()
                                }
                            } else {
                                _errorMessage.value = "Failed to update profile. Please try again."
                                _isLoading.value = false
                            }
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = authRepository.getFirebaseErrorMessage(exception as Exception)
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Registration failed. Please try again."
                _isLoading.value = false
            }
        }
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        // Username validation
        if (username.isBlank()) {
            return "Username is required"
        }
        if (username.length < 3) {
            return "Username must be at least 3 characters long"
        }
        if (username.length > 20) {
            return "Username must be less than 20 characters"
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return "Username can only contain letters, numbers, and underscores"
        }

        // Email validation
        if (email.isBlank()) {
            return "Email is required"
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Please enter a valid email address"
        }

        // Password validation
        if (password.isBlank()) {
            return "Password is required"
        }
        if (password.length < 6) {
            return "Password must be at least 6 characters long"
        }

        // Confirm password validation
        if (confirmPassword.isBlank()) {
            return "Please confirm your password"
        }
        if (password != confirmPassword) {
            return "Passwords do not match"
        }

        return null
    }

    fun clearMessages() {
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}