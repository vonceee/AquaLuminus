package com.example.aqualuminus.data.manager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class UserManager {
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserDisplayName(): String {
        return auth.currentUser?.let { user ->
            user.displayName ?: user.email ?: "User"
        } ?: "User"
    }

    fun getUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    fun refreshUserData(onComplete: () -> Unit) {
        auth.currentUser?.reload()?.addOnCompleteListener {
            onComplete()
        }
    }
}