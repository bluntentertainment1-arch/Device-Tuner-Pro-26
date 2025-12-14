@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
package com.kidblunt.cleanerguru.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import com.kidblunt.cleanerguru.R

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidblunt.cleanerguru.ui.theme.CloudBlue
import com.kidblunt.cleanerguru.ui.theme.SuccessGreen
import com.kidblunt.cleanerguru.ui.theme.WarningOrange

@Composable
fun GamingModeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var gamingModeEnabled by remember { mutableStateOf(true) } // Auto-enabled
    var cpuBoostEnabled by remember { mutableStateOf(true) } // Auto-enabled
    var showOptimizationDialog by remember { mutableStateOf(false) }

    val memoryInfo = remember { getMemoryInfo(context) }

    // Auto-enable gaming mode and CPU optimization when screen loads
    LaunchedEffect(Unit) {
        gamingModeEnabled = true
        cpuBoostEnabled = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gaming Mode") },
                backgroundColor = CloudBlue,
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            PerformanceStatusCard(
                memoryUsage = memoryInfo.first,
                availableMemory = memoryInfo.second,
                isGamingModeActive = gamingModeEnabled
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gaming Optimization",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GamingSettingCard(
                title = "Gaming Mode",
                description = "Enable high-performance settings for gaming",
                icon = Icons.Default.Gamepad,
                isEnabled = gamingModeEnabled,
                onToggle = { 
                    gamingModeEnabled = it
                    if (it) {
                        cpuBoostEnabled = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            GamingSettingCard(
                title = "CPU Performance Boost",
                description = "Maximize CPU performance for smoother gaming",
                icon = Icons.Default.Speed,
                isEnabled = cpuBoostEnabled,
                onToggle = { cpuBoostEnabled = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showOptimizationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WarningOrange),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Close Background Apps",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gaming Tips",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            GamingTipCard(
                tip = "Close all background apps before gaming",
                icon = Icons.Default.Apps
            )

            Spacer(modifier = Modifier.height(8.dp))

            GamingTipCard(
                tip = "Enable Do Not Disturb mode to avoid interruptions",
                icon = Icons.Default.DoNotDisturb
            )

            Spacer(modifier = Modifier.height(8.dp))

            GamingTipCard(
                tip = "Keep your device cool for optimal performance",
                icon = Icons.Default.AcUnit
            )

            Spacer(modifier = Modifier.height(8.dp))

            GamingTipCard(
                tip = "Ensure sufficient battery before long gaming sessions",
                icon = Icons.Default.BatteryChargingFull
            )
        }
    }

    if (showOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showOptimizationDialog = false },
            title = { Text("Optimize for Gaming") },
            text = { Text("This will close all non-essential background apps to free up memory and improve gaming performance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOptimizationDialog = false
                    }
                ) {
                    Text("Optimize", color = SuccessGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptimizationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PerformanceStatusCard(
    memoryUsage: Int,
    availableMemory: String,
    isGamingModeActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp,
        backgroundColor = if (isGamingModeActive) {
            SuccessGreen.copy(alpha = 0.1f)
        } else {
            CloudBlue.copy(alpha = 0.1f)
        }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGamingModeActive) SuccessGreen.copy(alpha = 0.2f)
                        else CloudBlue.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGamingModeActive) Icons.Default.Gamepad else Icons.Default.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isGamingModeActive) SuccessGreen else CloudBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isGamingModeActive) "Gaming Mode Active" else "Normal Mode",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = if (isGamingModeActive) SuccessGreen else CloudBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$memoryUsage%",
                        style = MaterialTheme.typography.h3,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Memory Used",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$availableMemory MB",
                        style = MaterialTheme.typography.h3,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun GamingSettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CloudBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SuccessGreen,
                    checkedTrackColor = SuccessGreen.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun GamingTipCard(
    tip: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CloudBlue,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tip,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

private fun getMemoryInfo(context: Context): Pair<Int, String> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
    val usedPercentage = ((usedMemory.toFloat() / memoryInfo.totalMem.toFloat()) * 100).toInt()
    val availableMB = (memoryInfo.availMem / (1024 * 1024)).toString()
    return Pair(usedPercentage, availableMB)
}