package com.example.tapblocker

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tapblocker.screens.PermissionsScreen
import com.example.tapblocker.screens.appsettingsscreen.SettingsScreen
import com.example.tapblocker.screens.mainscreen.MainScreen
import com.example.tapblocker.screens.mainscreen.MainViewModel
import com.example.tapblocker.ui.theme.TapBlockerTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController


@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val permissionGranted = PermissionUtils.hasUsageStatsPermission(this) && Settings.canDrawOverlays(this)
        setContent {
            TapBlockerTheme {
                val navController = rememberAnimatedNavController()
                AnimatedNavHost(
                    navController = navController,
                    startDestination = if (permissionGranted) "main_route" else "permissions_route"
                ) {
                    // PERMISSIONS SCREEN (ohne Animation)
                    composable("permissions_route") {
                        PermissionsScreen(navController)
                    }

                    // MAIN SCREEN (ohne Animation)
                    composable("main_route") {
                        MainScreen(navController, mainViewModel)
                    }

                    // SETTINGS SCREEN mit Slide-In/Out Transitions
                    composable(
                        "settings/{appId}",
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(300)
                            )
                        },

                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(300)
                            )
                        },

                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300)
                            )
                        }
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments!!.getString("appId")!!
                        SettingsScreen(navController, id)
                    }
                }
            }
        }
    }
}