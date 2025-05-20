package com.example.tapblocker

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tapblocker.ui.theme.TapBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val permissionGranted = PermissionUtils.hasUsageStatsPermission(this) && Settings.canDrawOverlays(this)
        setContent {
            TapBlockerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = if(permissionGranted) "main_route" else "permissions_route"
                ) {
                    composable("permissions_route") {
                        PermissionsScreen(navController)
                    }
                    composable("main_route") {
                        MainScreen()
                    }
                }
            }
        }
    }
}