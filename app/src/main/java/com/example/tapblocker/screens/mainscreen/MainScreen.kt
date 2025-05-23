package com.example.tapblocker.screens.mainscreen

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.tapblocker.ForegroundService

@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val apps by viewModel.settings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val svcIntent = Intent(context, ForegroundService::class.java)
        ContextCompat.startForegroundService(context, svcIntent)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add app")
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            if(apps.isEmpty()) {
                Text("No apps configured. Press the + button to add one.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps) { item ->
                        val app = item.app
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("settings/${app.id}") }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(app.name, style = MaterialTheme.typography.titleLarge)
                                    Text(app.id, style = MaterialTheme.typography.bodyMedium)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { navController.navigate("settings/${app.id}") }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = app.activated,
                                    onCheckedChange = { newValue ->
                                        viewModel.toggleApp(app.id, newValue)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Der „Add App“-Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add new app") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Display name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newId,
                        onValueChange = { newId = it },
                        label = { Text("Packet (ID)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addApp(newName.trim(), newId.trim())
                    newName = ""
                    newId = ""
                    showAddDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
