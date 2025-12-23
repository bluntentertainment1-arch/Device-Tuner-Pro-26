@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
package com.kidblunt.cleanerguru.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import com.kidblunt.cleanerguru.R

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.sp
import com.kidblunt.cleanerguru.ui.theme.CloudBlue
import com.kidblunt.cleanerguru.ui.theme.SuccessGreen
import com.kidblunt.cleanerguru.ui.theme.WarningOrange
import com.kidblunt.cleanerguru.ui.theme.CardBackground
import com.kidblunt.cleanerguru.ui.theme.ErrorRed
import com.kidblunt.cleanerguru.data.manager.GamingModeManager
import kotlinx.coroutines.delay

@Composable
fun GamingModeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Initialize GamingModeManager
    val gamingModeManager = remember { GamingModeManager(context) }
    val gamingModeState by gamingModeManager.gamingModeState.collectAsState()

    var showOptimizationDialog by remember { mutableStateOf(false) }
    var activeDuration by remember { mutableStateOf("") }

    val memoryInfo = remember { getMemoryInfo(context) }

    // Update active duration every second when gaming mode is active
    LaunchedEffect(gamingModeState.isEnabled) {
        while (gamingModeState.isEnabled) {
            activeDuration = gamingModeManager.formatActiveDuration()
            delay(1000)
        }
        if (!gamingModeState.isEnabled) {
            activeDuration = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Gaming Mode")
                        if (gamingModeState.isEnabled && activeDuration.isNotEmpty()) {
                            Text(
                                text = activeDuration,
                                style = MaterialTheme.typography.caption,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                backgroundColor = CloudBlue,
                contentColor = Color.White,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (gamingModeState.isEnabled) {
                                gamingModeManager.disableGamingMode()
                            } else {
                                gamingModeManager.enableGamingMode()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (gamingModeState.isEnabled) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (gamingModeState.isEnabled) "Disable Gaming Mode" else "Enable Gaming Mode",
                            tint = if (gamingModeState.isEnabled) SuccessGreen else Color.White
                        )
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
                isGamingModeActive = gamingModeState.isEnabled,
                isCpuBoostActive = gamingModeState.cpuBoostEnabled,
                activeDuration = activeDuration
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
                description = "Enable high-performance settings for gaming (stays active until disabled)",
                icon = Icons.Default.Gamepad,
                isEnabled = gamingModeState.isEnabled,
                onToggle = { 
                    if (it) {
                        gamingModeManager.enableGamingMode()
                    } else {
                        gamingModeManager.disableGamingMode()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            GamingSettingCard(
                title = "CPU Performance Boost",
                description = "Maximize CPU performance independently (works with or without gaming mode)",
                icon = Icons.Default.Speed,
                isEnabled = gamingModeState.cpuBoostEnabled,
                onToggle = { 
                    gamingModeManager.toggleCpuBoost(it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    showOptimizationDialog = true
                },
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
                tip = "CPU boost can work independently from gaming mode",
                icon = Icons.Default.Info
            )

            Spacer(modifier = Modifier.height(8.dp))

            GamingTipCard(
                tip = "Gaming mode will stay active until you manually turn it off",
                icon = Icons.Default.Timer
            )

            Spacer(modifier = Modifier.height(8.dp))

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
            backgroundColor = CardBackground,
            title = { 
                Text(
                    "Optimize for Gaming",
                    color = Color.White
                ) 
            },
            text = { 
                Text(
                    "This will close all non-essential background apps to free up memory and improve gaming performance.",
                    color = Color.White.copy(alpha = 0.8f)
                ) 
            },
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
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun PerformanceStatusCard(
    memoryUsage: Int,
    availableMemory: String,
    isGamingModeActive: Boolean,
    isCpuBoostActive: Boolean,
    activeDuration: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = 4.dp,
        backgroundColor = when {
            isGamingModeActive && isCpuBoostActive -> SuccessGreen.copy(alpha = 0.15f)
            isGamingModeActive || isCpuBoostActive -> SuccessGreen.copy(alpha = 0.1f)
            else -> CloudBlue.copy(alpha = 0.1f)
        }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Gaming Mode Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGamingModeActive) SuccessGreen.copy(alpha = 0.2f)
                                else CloudBlue.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = if (isGamingModeActive) SuccessGreen else CloudBlue
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isGamingModeActive) "Gaming Active" else "Gaming Off",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = if (isGamingModeActive) SuccessGreen else CloudBlue
                    )
                }

                // CPU Boost Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCpuBoostActive) SuccessGreen.copy(alpha = 0.2f)
                                else CloudBlue.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = if (isCpuBoostActive) SuccessGreen else CloudBlue
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isCpuBoostActive) "CPU Boosted" else "CPU Normal",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium,
                        color = if (isCpuBoostActive) SuccessGreen else CloudBlue
                    )
                }
            }

            if (isGamingModeActive && activeDuration.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = activeDuration,
                    style = MaterialTheme.typography.caption,
                    color = SuccessGreen,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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