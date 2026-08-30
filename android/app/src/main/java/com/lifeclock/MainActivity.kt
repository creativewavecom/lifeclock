package com.lifeclock

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifeclock.ui.MainViewModel
import com.lifeclock.ui.screens.AddWidgetDialog
import com.lifeclock.ui.screens.CitiesScreen
import com.lifeclock.ui.screens.HomeScreen
import com.lifeclock.ui.screens.SettingsScreen
import com.lifeclock.ui.theme.LifeClockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(application) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* No-op: user can always add cities manually */ }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // User denied notification permission — notification just won't show, app still works.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Soft-ask for location permission (user can deny and still use the app)
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        setContent {
            LifeClockTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavigation(viewModel: MainViewModel) {
    val nav = rememberNavController()
    var showAddWidgetDialog by remember { mutableStateOf(false) }

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { nav.navigate("settings") },
                onNavigateToCities = { nav.navigate("cities") },
                onAddWidget = { showAddWidgetDialog = true }
            )
        }
        composable("settings") {
            SettingsScreen(viewModel, onNavigateBack = { nav.popBackStack() })
        }
        composable("cities") {
            CitiesScreen(viewModel, onNavigateBack = { nav.popBackStack() })
        }
    }

    if (showAddWidgetDialog) {
        AddWidgetDialog(onDismiss = { showAddWidgetDialog = false })
    }
}
