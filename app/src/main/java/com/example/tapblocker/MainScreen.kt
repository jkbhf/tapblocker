package com.example.tapblocker

import android.content.Intent
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.tapblocker.ui.theme.TapBlockerTheme

@Composable
fun MainScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val svcIntent = Intent(context, ForegroundService::class.java)
        ContextCompat.startForegroundService(context, svcIntent)
    }
    Scaffold(Modifier.padding(all = 16.dp)) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Test")
            Spacer(Modifier.height(32.dp))

            val apps = listOf("com.instagram.android", "com.tiktok.android")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps) { it ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.padding(16.dp)) {
                                Text(it.split(".")[1].replaceFirstChar { it.uppercaseChar() }, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {}) { Icon(Icons.Default.KeyboardArrowDown, "Arrow down") }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    TapBlockerTheme {
        MainScreen()
    }
}