package com.example.tapblocker.screens.appsettingsscreen

import SettingsViewModel
import SettingsViewModel.Companion.provideFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tapblocker.data.RegionEntity
import com.example.tapblocker.data.RegionOrientation

// Screen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    appId: String
) {
    // obtain ViewModel for this app
    val viewModel: SettingsViewModel = viewModel(
        factory = provideFactory(LocalContext.current, appId)
    )
    val uiState by viewModel.uiState.collectAsState()

    // state for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Main layout container
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.appName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete app")
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { viewModel.addRegion() }) { Icon(Icons.Default.Add, contentDescription = "Add region") } },
    ) { paddingValues ->
        // Settings list overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if(uiState.regions.isEmpty()) {
                Text("No regions configured. Press the + button to add one.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.regions, key = { it.regionId }) { region ->
                        RegionCard(
                            region = region,
                            onUpdate = { updated -> viewModel.updateRegion(updated) },
                            onDelete = { viewModel.deleteRegion(region.regionId) }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen preview background
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.1f))
    ) {
        val fullWidth = maxWidth
        val fullHeight = maxHeight
        uiState.regions.forEach { region ->
            val w: Dp = region.xSize.dp
            val h: Dp = region.ySize.dp
            val xOff: Dp = region.xOffset.dp
            val yOff: Dp = region.yOffset.dp

            val orientation = RegionOrientation.valueOf(region.orientation)
            // compute position based on orientation
            val xPos: Dp = when (orientation) {
                RegionOrientation.TOPLEFT,
                RegionOrientation.LEFT,
                RegionOrientation.BOTTOMLEFT -> xOff

                RegionOrientation.TOP,
                RegionOrientation.BOTTOM -> (fullWidth - w) / 2 + xOff

                RegionOrientation.TOPRIGHT,
                RegionOrientation.RIGHT,
                RegionOrientation.BOTTOMRIGHT -> fullWidth - w - xOff
            }

            val yPos: Dp = when (orientation) {
                RegionOrientation.TOPLEFT,
                RegionOrientation.TOP,
                RegionOrientation.TOPRIGHT -> yOff

                RegionOrientation.LEFT,
                RegionOrientation.RIGHT -> (fullHeight - h) / 2 + yOff

                RegionOrientation.BOTTOMLEFT,
                RegionOrientation.BOTTOM,
                RegionOrientation.BOTTOMRIGHT -> fullHeight - h - yOff
            }

            Box(
                modifier = Modifier
                    .offset(x = xPos, y = yPos)
                    .size(w, h)
                    .background(Color.Red.copy(alpha = 0.2f))
            )
        }
    }

    // Confirmation dialog for deleting the app
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete App") },
            text = { Text("Are you sure you want to delete this app and all its regions?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteApp()
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RegionCard(
    region: RegionEntity,
    onUpdate: (RegionEntity) -> Unit,
    onDelete: () -> Unit
) {
    // saveable states reset when region changes
    var expanded by rememberSaveable { mutableStateOf(false) }
    var orientationString by rememberSaveable(region.orientation) { mutableStateOf(region.orientation) }
    var xOffset by rememberSaveable(region.xOffset) { mutableStateOf(region.xOffset.toString()) }
    var yOffset by rememberSaveable(region.yOffset) { mutableStateOf(region.yOffset.toString()) }
    var xSize by rememberSaveable(region.xSize) { mutableStateOf(region.xSize.toString()) }
    var ySize by rememberSaveable(region.ySize) { mutableStateOf(region.ySize.toString()) }
    val orientation = RegionOrientation.valueOf(orientationString)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alignment:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = orientation.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RegionOrientation.values().forEach { dir ->
                            DropdownMenuItem(
                                text = { Text(dir.name) },
                                onClick = {
                                    expanded = false
                                    orientationString = dir.name
                                    onUpdate(region.copy(orientation = dir.name))
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = xOffset,
                    onValueChange = { xOffset = it },
                    label = { Text("xOffset") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onUpdate(region.copy(xOffset = xOffset.toIntOrNull() ?: 0))
                        }
                    )
                )
                OutlinedTextField(
                    value = yOffset,
                    onValueChange = { yOffset = it },
                    label = { Text("yOffset") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onUpdate(region.copy(yOffset = yOffset.toIntOrNull() ?: 0))
                        }
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = xSize,
                    onValueChange = { xSize = it },
                    label = { Text("width") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            onUpdate(region.copy(xSize = xSize.toIntOrNull() ?: 0))
                        }
                    )
                )
                OutlinedTextField(
                    value = ySize,
                    onValueChange = { ySize = it },
                    label = { Text("height") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onUpdate(region.copy(ySize = ySize.toIntOrNull() ?: 0))
                        }
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete region")
                }
            }
        }
    }
}

