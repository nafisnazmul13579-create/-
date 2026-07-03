package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.HMStudioApp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val viewModel: MainViewModel = viewModel()
                
                // Lightweight, bulletproof state navigation stack
                var currentScreen by remember { mutableStateOf(Screen.Home) }
                val backStack = remember { mutableStateListOf<Screen>() }

                fun navigateTo(screen: Screen) {
                    if (currentScreen != screen) {
                        backStack.add(currentScreen)
                        currentScreen = screen
                    }
                }

                fun navigateBack() {
                    if (backStack.isNotEmpty()) {
                        currentScreen = backStack.removeAt(backStack.size - 1)
                    }
                }

                // Handle system back buttons elegantly
                BackHandler(enabled = currentScreen != Screen.Home) {
                    if (currentScreen == Screen.Generating) {
                        // Prompt abort or just go home
                        viewModel.resetGeneration()
                        currentScreen = Screen.Home
                        backStack.clear()
                    } else {
                        navigateBack()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    HMStudioApp(
                        viewModel = viewModel,
                        currentScreen = currentScreen,
                        onNavigate = { screen -> navigateTo(screen) },
                        onNavigateWithProject = { screen, projectId ->
                            navigateTo(screen)
                        }
                    )
                }
            }
        }
    }
}
