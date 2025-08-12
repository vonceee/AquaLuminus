@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.aqualuminus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.aqualuminus.data.auth.AuthStateManager
import com.example.aqualuminus.ui.navigation.AquariumNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val authStateManager = remember { AuthStateManager() }
                val authState by authStateManager.authState.collectAsState()

                AquariumNavGraph(
                    authState = authState,
                    onSignOut = { authStateManager.signOut() }
                )
            }
        }
    }
}