@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
package com.kidblunt.cleanerguru.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import com.kidblunt.cleanerguru.R

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidblunt.cleanerguru.ui.theme.*
import com.kidblunt.cleanerguru.ui.components.TwinklingStarsBackground
import com.kidblunt.cleanerguru.ui.components.HeartRateMonitor
import com.kidblunt.cleanerguru.data.manager.GamingModeManager
import com.kidblunt.cleanerguru.data.manager.BatterySaverManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    onNavigateToPhotoCleanup: () -> Unit,
    onNavigateToBatterySaver: () -> Unit,
    onNavigateToGamingMode: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Initialize managers
    val gamingModeManager = remember { GamingModeManager(context) }
    val batterySaverManager = remember { BatterySaverManager(context) }
    
    // Collect states
    val gamingModeState by gamingModeManager.gamingModeState.collectAsState()
    val batterySaverState by batterySaverManager.batterySaverState.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    var isOptimizing by remember { mutableStateOf(false) }
    var storageInfo by remember { mutableStateOf(getStorageInfo(context)) }
    var batteryLevel by remember { mutableStateOf(getBatteryLevel(context)) }
    var memoryInfo by remember { mutableStateOf(getMemoryInfo(context)) }
    var lastOptimizeTime by remember { mutableStateOf(0L) }
    var canOptimize by remember { mutableStateOf(true) }
    
    // Track highest achieved score during app session
    var highestAchievedScore by remember { mutableStateOf(0) }
    var hasBeenQuickOptimized by remember { mutableStateOf(false) }
    
    // Calculate device score with new logic that prevents going backwards
    var deviceScore by remember { 
        mutableStateOf(calculateDeviceScore(
            storageInfo.first, 
            batteryLevel, 
            memoryInfo.first,
            isQuickOptimized = false,
            isBatterySaverOn = batterySaverState.isEnabled,
            isGamingModeOn = gamingModeState.isEnabled
        )) 
    }

    // Auto-refresh data every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            if (!isOptimizing) {
                storageInfo = getStorageInfo(context)
                batteryLevel = getBatteryLevel(context)
                memoryInfo = getMemoryInfo(context)
                
                val newScore = calculateDeviceScore(
                    storageInfo.first, 
                    batteryLevel, 
                    memoryInfo.first,
                    hasBeenQuickOptimized,
                    batterySaverState.isEnabled,
                    gamingModeState.isEnabled
                )
                
                // Only update score if it's higher than the highest achieved score
                if (newScore > highestAchievedScore) {
                    deviceScore = newScore
                    highestAchievedScore = newScore
                } else {
                    // Keep the highest achieved score
                    deviceScore = highestAchievedScore
                }
            }
        }
    }

    // Update score when modes change, but don't let it go backwards
    LaunchedEffect(batterySaverState.isEnabled, gamingModeState.isEnabled) {
        if (!isOptimizing) {
            val newScore = calculateDeviceScore(
                storageInfo.first, 
                batteryLevel, 
                memoryInfo.first,
                hasBeenQuickOptimized,
                batterySaverState.isEnabled,
                gamingModeState.isEnabled
            )
            
            // Only update if new score is higher
            if (newScore > highestAchievedScore) {
                deviceScore = newScore
                highestAchievedScore = newScore
            } else {
                // Keep the highest achieved score
                deviceScore = highestAchievedScore
            }
        }
    }

    // Check cooldown timer
    LaunchedEffect(lastOptimizeTime) {
        if (lastOptimizeTime > 0) {
            canOptimize = false
            delay(30000) // 30 seconds cooldown
            canOptimize = true
        }
    }

    // Handle quick optimization
    val performQuickOptimization: () -> Unit = {
        if (!isOptimizing && canOptimize) {
            lastOptimizeTime = System.currentTimeMillis()
            isOptimizing = true
        }
    }

    // Handle optimization completion
    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            // First, animate score to 100
            deviceScore = 100
            highestAchievedScore = 100
            delay(2000) // Show 100 for 2 seconds
            
            // Enable CPU boost during optimization
            gamingModeManager.toggleCpuBoost(true)
            
            // Enable power saver (not ultra saver) during optimization
            batterySaverManager.enableBatterySaver()
            
            // Mark as optimized
            hasBeenQuickOptimized = true
            
            // Simulate improved metrics
            val optimizedStorageInfo = Pair(
                maxOf(storageInfo.first - 15, 20), // Reduce storage usage by 15%
                (storageInfo.second.toFloat() - 1.5f).toString() // Free up 1.5GB
            )
            val optimizedMemoryInfo = Pair(
                maxOf(memoryInfo.first - 20, 30), // Reduce memory usage by 20%
                (memoryInfo.second.toInt() + 500).toString() // Add 500MB free memory
            )
            
            storageInfo = optimizedStorageInfo
            memoryInfo = optimizedMemoryInfo
            
            isOptimizing = false
            
            // Calculate final optimized score
            val finalScore = calculateDeviceScore(
                storageInfo.first, 
                batteryLevel, 
                memoryInfo.first,
                hasBeenQuickOptimized,
                batterySaverState.isEnabled,
                gamingModeState.isEnabled
            )
            
            // Slowly bring score down to final optimized score, but ensure it's still high
            val startScore = 100
            val endScore = maxOf(finalScore, 91) // Ensure minimum score of 91 after optimization
            val animationDuration = 5000L // 5 seconds
            val steps = 50
            val stepDelay = animationDuration / steps
            val scoreDecrement = (startScore - endScore).toFloat() / steps
            
            repeat(steps) { step ->
                delay(stepDelay)
                val newScore = (startScore - (scoreDecrement * (step + 1))).roundToInt().coerceAtLeast(endScore)
                deviceScore = newScore
                highestAchievedScore = newScore
            }
            
            deviceScore = endScore
            highestAchievedScore = endScore
            
            // Auto-navigate to gaming mode after optimization
            onNavigateToGamingMode()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vibrant Gen Z background
        TwinklingStarsBackground(
            modifier = Modifier.fillMaxSize(),
            starCount = 25,
            starColor = NeonPink.copy(alpha = 0.6f)
        )

        Scaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clean Booster Pro", fontWeight = FontWeight.Bold)
                        }
                    },
                    backgroundColor = NeonPink,
                    contentColor = Color.White,
                    actions = {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                storageInfo = getStorageInfo(context)
                                batteryLevel = getBatteryLevel(context)
                                memoryInfo = getMemoryInfo(context)
                                
                                val newScore = calculateDeviceScore(
                                    storageInfo.first, 
                                    batteryLevel, 
                                    memoryInfo.first,
                                    hasBeenQuickOptimized,
                                    batterySaverState.isEnabled,
                                    gamingModeState.isEnabled
                                )
                                
                                // Only update if new score is higher
                                if (newScore > highestAchievedScore) {
                                    deviceScore = newScore
                                    highestAchievedScore = newScore
                                } else {
                                    // Keep the highest achieved score
                                    deviceScore = highestAchievedScore
                                }
                                
                                isRefreshing = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = if (isRefreshing) {
                                    Modifier.scale(
                                        animateFloatAsState(
                                            targetValue = if (isRefreshing) 1.2f else 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000),
                                                repeatMode = RepeatMode.Reverse
                                            )
                                        ).value
                                    )
                                } else Modifier
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkBackground.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Device Score Card with new scoring logic
                AnimatedDeviceScoreCard(
                    score = deviceScore,
                    isOptimizing = isOptimizing,
                    hasBeenOptimized = hasBeenQuickOptimized,
                    isBatterySaverOn = batterySaverState.isEnabled,
                    isGamingModeOn = gamingModeState.isEnabled,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Quick optimization button
                AnimatedRocketOptimizeCard(
                    isOptimizing = isOptimizing,
                    isOptimized = hasBeenQuickOptimized,
                    canOptimize = canOptimize,
                    onClick = performQuickOptimization,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Device Health",
                    style = MaterialTheme.typography.h2,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Heart Rate Monitor with Gen Z colors
                HeartRateMonitor(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    heartRate = deviceScore,
                    isActive = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedMetricCard(
                        title = "Storage",
                        value = "${storageInfo.first}%",
                        subtitle = "${storageInfo.second} GB used",
                        icon = Icons.Default.Storage,
                        color = getStorageColor(storageInfo.first),
                        modifier = Modifier.weight(1f),
                        progress = storageInfo.first / 100f,
                        isOptimized = hasBeenQuickOptimized
                    )

                    AnimatedMetricCard(
                        title = "Battery",
                        value = "$batteryLevel%",
                        subtitle = getBatteryStatus(batteryLevel),
                        icon = getBatteryIcon(batteryLevel),
                        color = getBatteryColor(batteryLevel),
                        modifier = Modifier.weight(1f),
                        progress = batteryLevel / 100f,
                        isOptimized = false // Battery doesn't get optimized
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedMetricCard(
                        title = "Memory",
                        value = "${memoryInfo.first}%",
                        subtitle = "${memoryInfo.second} MB free",
                        icon = Icons.Default.Memory,
                        color = getMemoryColor(memoryInfo.first),
                        modifier = Modifier.weight(1f),
                        progress = memoryInfo.first / 100f,
                        isOptimized = hasBeenQuickOptimized
                    )

                    AnimatedMetricCard(
                        title = "CPU",
                        value = if (hasBeenQuickOptimized) "Excellent" else "Normal",
                        subtitle = if (hasBeenQuickOptimized) "Boosted" else "Optimized",
                        icon = Icons.Default.Speed,
                        color = if (hasBeenQuickOptimized) NeonGreen else ElectricBlue,
                        modifier = Modifier.weight(1f),
                        progress = if (hasBeenQuickOptimized) 0.9f else 0.6f,
                        isOptimized = hasBeenQuickOptimized
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Optimization Tools",
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                EnhancedActionCard(
                    title = "Storage Cleanup",
                    description = "Clean photos, cache, and junk files",
                    icon = Icons.Default.CleaningServices,
                    color = NeonPink,
                    onClick = onNavigateToPhotoCleanup,
                    badge = "New"
                )

                Spacer(modifier = Modifier.height(12.dp))

                EnhancedActionCard(
                    title = "Battery Saver",
                    description = "Extend battery life with smart optimization",
                    icon = Icons.Default.BatteryChargingFull,
                    color = ElectricBlue,
                    onClick = onNavigateToBatterySaver,
                    badge = if (batteryLevel < 30) "Recommended" else if (batterySaverState.isEnabled) "Active" else null
                )

                Spacer(modifier = Modifier.height(12.dp))

                EnhancedActionCard(
                    title = "Gaming Mode",
                    description = "Boost performance for gaming",
                    icon = Icons.Default.SportsEsports,
                    color = VibrantPurple,
                    onClick = onNavigateToGamingMode,
                    badge = if (gamingModeState.isEnabled) "Active" else null
                )

                Spacer(modifier = Modifier.height(24.dp))

                // System Information Card with Gen Z styling
                SystemInfoCard(context = context)
            }
        }
    }
}

@Composable
fun AnimatedRocketOptimizeCard(
    isOptimizing: Boolean,
    isOptimized: Boolean,
    canOptimize: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isOptimizing -> SunsetOrange
            isOptimized -> NeonGreen
            !canOptimize -> Color.Gray
            else -> NeonGreen
        },
        animationSpec = tween(500)
    )

    val scale by animateFloatAsState(
        targetValue = if (isOptimizing) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // Rocket animation states
    val rocketOffset by animateFloatAsState(
        targetValue = if (isOptimizing) -50f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val rocketRotation by animateFloatAsState(
        targetValue = if (isOptimizing) -15f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val rocketShake by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isOptimizing) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(enabled = canOptimize && !isOptimizing && !isOptimized) { onClick() },
        shape = MaterialTheme.shapes.medium,
        elevation = 8.dp,
        backgroundColor = backgroundColor
    ) {
        Box {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = rocketOffset.dp, y = rocketShake.dp)
                ) {
                    when {
                        isOptimizing -> {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rocketRotation)
                            )
                        }
                        isOptimized -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        !canOptimize -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isOptimizing -> "Launching Optimization..."
                            isOptimized -> "Optimization Complete"
                            !canOptimize -> "Quick Optimize (Cooldown)"
                            else -> "Quick Optimize"
                        },
                        style = MaterialTheme.typography.h3,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when {
                            isOptimizing -> "Rocket boosting device performance & CPU"
                            isOptimized -> "Power saver enabled, CPU boosted & performance enhanced"
                            !canOptimize -> "Please wait 30 seconds before next use"
                            else -> "Optimize phone speed, enable power saver & CPU boost (+25%)"
                        },
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                if (canOptimize && !isOptimizing && !isOptimized) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedDeviceScoreCard(
    score: Int,
    isOptimizing: Boolean,
    hasBeenOptimized: Boolean = false,
    isBatterySaverOn: Boolean = false,
    isGamingModeOn: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 2000)
    )
    
    val scoreColor = when {
        score >= 80 -> NeonGreen
        score >= 60 -> SunsetOrange
        else -> ErrorRed
    }

    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = if (isOptimizing) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(if (isOptimizing) pulseAnimation else 1f),
        shape = MaterialTheme.shapes.large,
        elevation = 12.dp,
        backgroundColor = CardBackground.copy(alpha = 0.9f)
    ) {
        Box {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scoreColor.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 300f
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isOptimizing -> "Optimizing Device..."
                            hasBeenOptimized -> "Device Score (Optimized)"
                            else -> "Device Score"
                        },
                        style = MaterialTheme.typography.h3,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (hasBeenOptimized) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Quick Optimized",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isBatterySaverOn) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Battery Saver Active",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isGamingModeOn) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "Gaming Mode Active",
                                tint = VibrantPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isOptimizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = scoreColor
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = animatedScore / 100f,
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = scoreColor
                        )
                    }
                    
                    if (isOptimizing) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = scoreColor
                        )
                    } else {
                        Text(
                            text = "$animatedScore",
                            style = MaterialTheme.typography.h1.copy(fontSize = 36.sp),
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when {
                        isOptimizing -> "Please wait..."
                        hasBeenOptimized -> "Performance enhanced with quick optimize!"
                        else -> getScoreDescription(score)
                    },
                    style = MaterialTheme.typography.body1,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Show active modes
                if (isBatterySaverOn || isGamingModeOn) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val activeModes = mutableListOf<String>()
                    if (isBatterySaverOn) activeModes.add("Battery Saver (+20%)")
                    if (isGamingModeOn) activeModes.add("Gaming Mode (+30%)")
                    
                    Text(
                        text = "Active: ${activeModes.joinToString(", ")}",
                        style = MaterialTheme.typography.caption,
                        color = SuccessGreen,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    isOptimized: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500)
    )

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = 6.dp,
        backgroundColor = if (isOptimized) 
            CardBackground.copy(alpha = 0.9f) 
        else 
            CardBackground.copy(alpha = 0.7f)
    ) {
        Box {
            // Gradient background
            if (isOptimized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 100f
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = color.copy(alpha = 0.3f)
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    if (isOptimized) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Improved",
                            tint = NeonGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = 16.dp, y = (-16).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.h3,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EnhancedActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    badge: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = 6.dp,
        backgroundColor = CardBackground.copy(alpha = 0.9f)
    ) {
        Box {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.3f),
                                    color.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.h3.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
            
            badge?.let {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    backgroundColor = when (it) {
                        "Active" -> SuccessGreen
                        "Recommended" -> WarningOrange
                        else -> ErrorRed
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SystemInfoCard(context: Context) {
    val systemInfo = remember { getSystemInfo(context) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = 6.dp,
        backgroundColor = CardBackground.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Information",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            systemInfo.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun calculateDeviceScore(
    storageUsage: Int, 
    batteryLevel: Int, 
    memoryUsage: Int, 
    isQuickOptimized: Boolean,
    isBatterySaverOn: Boolean,
    isGamingModeOn: Boolean
): Int {
    // Base score calculation (max 50)
    val storageScore = (100 - storageUsage) * 0.2  // 20% weight
    val batteryScore = batteryLevel * 0.15          // 15% weight  
    val memoryScore = (100 - memoryUsage) * 0.15   // 15% weight
    val baseScore = (storageScore + batteryScore + memoryScore).roundToInt()
    
    var finalScore = baseScore
    
    // Quick optimize bonus: +25%
    if (isQuickOptimized) {
        finalScore = (finalScore * 1.25).roundToInt()
    }
    
    // Battery saver bonus: +20%
    if (isBatterySaverOn) {
        finalScore = (finalScore * 1.20).roundToInt()
    }
    
    // Gaming mode bonus: +30%
    if (isGamingModeOn) {
        finalScore = (finalScore * 1.30).roundToInt()
    }
    
    // Ensure score is between 91-99 when all modes are active
    if (isQuickOptimized && isBatterySaverOn && isGamingModeOn) {
        finalScore = finalScore.coerceIn(91, 99)
    } else {
        // Cap at 99 maximum
        finalScore = finalScore.coerceAtMost(99)
    }
    
    return finalScore
}

private fun getScoreDescription(score: Int): String {
    return when {
        score >= 80 -> "Excellent performance"
        score >= 60 -> "Good performance"
        score >= 40 -> "Fair performance"
        else -> "Needs optimization"
    }
}

private fun getStorageColor(usage: Int): Color {
    return when {
        usage > 80 -> ErrorRed
        usage > 60 -> SunsetOrange
        else -> NeonGreen
    }
}

private fun getBatteryColor(level: Int): Color {
    return when {
        level < 20 -> ErrorRed
        level < 50 -> SunsetOrange
        else -> NeonGreen
    }
}

private fun getMemoryColor(usage: Int): Color {
    return when {
        usage > 80 -> ErrorRed
        usage > 60 -> SunsetOrange
        else -> NeonGreen
    }
}

private fun getBatteryIcon(level: Int): ImageVector {
    return when {
        level < 20 -> Icons.Default.BatteryAlert
        level < 50 -> Icons.Default.Battery3Bar
        level < 80 -> Icons.Default.Battery5Bar
        else -> Icons.Default.BatteryFull
    }
}

private fun getBatteryStatus(level: Int): String {
    return when {
        level < 20 -> "Critical"
        level < 50 -> "Low"
        level < 80 -> "Good"
        else -> "Excellent"
    }
}

private fun getStorageInfo(context: Context): Pair<Int, String> {
    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
    val usedBytes = totalBytes - availableBytes
    val usedPercentage = ((usedBytes.toFloat() / totalBytes.toFloat()) * 100).roundToInt()
    val usedGB = String.format("%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0))
    return Pair(usedPercentage, usedGB)
}

private fun getBatteryLevel(context: Context): Int {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
}

private fun getMemoryInfo(context: Context): Pair<Int, String> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
    val usedPercentage = ((usedMemory.toFloat() / memoryInfo.totalMem.toFloat()) * 100).roundToInt()
    val availableMB = (memoryInfo.availMem / (1024 * 1024)).toString()
    return Pair(usedPercentage, availableMB)
}

private fun getSystemInfo(context: Context): List<Pair<String, String>> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    
    return listOf(
        "Device Model" to android.os.Build.MODEL,
        "Android Version" to android.os.Build.VERSION.RELEASE,
        "Total RAM" to "${memoryInfo.totalMem / (1024 * 1024 * 1024)} GB",
        "Available RAM" to "${memoryInfo.availMem / (1024 * 1024)} MB",
        "CPU Cores" to Runtime.getRuntime().availableProcessors().toString()
    )
}