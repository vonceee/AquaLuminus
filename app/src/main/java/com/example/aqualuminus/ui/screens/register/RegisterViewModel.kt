package com.example.aqualuminus.ui.screens.register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class RegisterViewModel : ViewModel() {
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf("")
    val errorMessage: State<String> = _errorMessage

    private val _successMessage = mutableStateOf("")
    val successMessage: State<String> = _successMessage

    // email validation pattern
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        // validate inputs
        val validation = validateInputs(username, email, password, confirmPassword)
        if (validation != null) {
            _errorMessage.value = validation
            return
        }

        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            // simulate network delay
            delay(2000)

            // simulate registration logic
            // in a real app, this would call your backend API
            try {
                // check if username already exists (simulate)
                if (username.trim().lowercase() == "admin" || username.trim().lowercase() == "test") {
                    _errorMessage.value = "Username already exists. Please choose a different one."
                    _isLoading.value = false
                    return@launch
                }

                // check if email already exists (simulate)
                if (email.trim().lowercase() == "admin@example.com" || email.trim().lowercase() == "test@example.com") {
                    _errorMessage.value = "Email already registered. Please use a different email."
                    _isLoading.value = false
                    return@launch
                }

                // simulate successful registration
                _successMessage.value = "Account created successfully! Please sign in."

                // auto-navigate after showing success message
                delay(1500)
                onSuccess()

            } catch (e: Exception) {
                _errorMessage.value = "Registration failed. Please try again."
            } finally {
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
        // username validation
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

        // email validation
        if (email.isBlank()) {
            return "Email is required"
        }
        if (!emailPattern.matcher(email).matches()) {
            return "Please enter a valid email address"
        }

        // password validation
        if (password.isBlank()) {
            return "Password is required"
        }
        if (password.length < 6) {
            return "Password must be at least 6 characters long"
        }
        if (password.length > 50) {
            return "Password must be less than 50 characters"
        }

        // password strength validation
        if (!password.matches(Regex(".*[A-Za-z].*"))) {
            return "Password must contain at least one letter"
        }
        if (!password.matches(Regex(".*[0-9].*"))) {
            return "Password must contain at least one number"
        }

        // confirm password validation
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