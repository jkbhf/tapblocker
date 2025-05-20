package com.example.tapblocker

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tapblocker.ui.theme.TapBlockerTheme

@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var usageStatsGranted by remember {
        mutableStateOf(
            PermissionUtils.hasUsageStatsPermission(
                context
            )
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                usageStatsGranted = PermissionUtils.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return Scaffold(
        Modifier.padding(16.dp),
        floatingActionButton = {if(overlayGranted && usageStatsGranted) {
            FloatingActionButton(onClick = {
                navController.navigate("main_route") {
                    popUpTo("permissions_route") {
                        inclusive = true
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next screen")
            }
        }
        }) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(128.dp))
            Text(
                "Permissions required!",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(128.dp))
            PermissionsCard(
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                },
                title = "Display over other apps",
                isGranted = overlayGranted
            )
            Spacer(Modifier.height(12.dp))
            PermissionsCard(
                onClick = {
                    if (!PermissionUtils.hasUsageStatsPermission(context)) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                },
                title = "Usage access",
                isGranted = usageStatsGranted
            )
        }
    }
}

@Composable
private fun PermissionsCard(onClick: () -> Unit, title: String, isGranted: Boolean) {
    val containerColor =
        if (isGranted) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.errorContainer
    val textColor =
        if (isGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
    Card(Modifier
        .clickable {
            onClick()
        }
        .fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = textColor)
                Text(
                    if(isGranted) "Granted!" else "Tap to grant!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(if(isGranted) Icons.Default.Done else Icons.Default.Close, if(isGranted) "Done" else "Not done",)
            Spacer(Modifier.width(16.0.dp))
        }
    }
}

@Preview
@Composable
private fun PermissionsScreenPreview() {
    return TapBlockerTheme {
        PermissionsScreen(navController = rememberNavController())
    }
}