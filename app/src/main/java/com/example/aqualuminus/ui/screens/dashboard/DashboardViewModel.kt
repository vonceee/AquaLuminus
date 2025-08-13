package com.example.aqualuminus.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class DashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    // Observable state for the UI
    var userName by mutableStateOf("User")
        private set

    var userPhotoUrl by mutableStateOf<String?>(null)
        private set

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Get display name, fallback to email username, then to "User"
            userName = user.displayName
                ?: user.email?.substringBefore("@")
                        ?: "User"

            // Get profile photo URL if available
            userPhotoUrl = user.photoUrl?.toString()
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}