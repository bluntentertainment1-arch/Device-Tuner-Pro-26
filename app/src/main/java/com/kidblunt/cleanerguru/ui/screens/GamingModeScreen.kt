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
import com.kidblunt.cleanerguru.manager.GamingModeManager
import kotlinx.coroutines.delay

@Composable
fun GamingModeScreen(
    onBack: () -> Unit
) {
    // Optimization states (NO persistence)
    var cpuOptimizationActive by remember { mutableStateOf(false) }
    var gamingModeActive by remember { mutableStateOf(false) }

    // Inactivity tracking
    var lastInteractionTime by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var inactivityHandled by remember { mutableStateOf(false) }

    fun registerInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        inactivityHandled = false
    }

    fun turnOffGamingOptimizations() {
        cpuOptimizationActive = false
        gamingModeActive = false
    }

    // Auto-off after 1 hour inactivity
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000) // check every minute
            val inactiveMinutes =
                (System.currentTimeMillis() - lastInteractionTime) / (1000 * 60)

            if (inactiveMinutes >= 60 && !inactivityHandled) {
                turnOffGamingOptimizations()
                inactivityHandled = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        /* ================= Header ================= */

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(id = R.string.gaming_mode),
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        /* ================= CPU Optimization ================= */

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.cpu_boost),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )

            Switch(
                checked = cpuOptimizationActive,
                onCheckedChange = { enabled ->
                    registerInteraction()
                    cpuOptimizationActive = enabled
                }
            )
        }

        Text(
            text = stringResource(id = R.string.cpu_optimization_desc),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        /* ================= Gaming Mode ================= */

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.gaming_mode),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )

            Switch(
                checked = gamingModeActive,
                onCheckedChange = { enabled ->
                    registerInteraction()
                    gamingModeActive = enabled
                }
            )
        }

        Text(
            text = stringResource(id = R.string.gaming_mode_desc),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        /* ================= Info ================= */

        Text(
            text = "Gaming optimizations automatically turn off after 1 hour of inactivity.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
