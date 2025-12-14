@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
package com.kidblunt.cleanerguru.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import com.kidblunt.cleanerguru.R

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import com.kidblunt.cleanerguru.ui.theme.PinkPrimary
import com.kidblunt.cleanerguru.ui.theme.PinkPrimaryLight
import com.kidblunt.cleanerguru.ui.theme.SoftSand
import com.kidblunt.cleanerguru.ui.components.TwinklingStarsBackground
import com.kidblunt.cleanerguru.ui.viewmodel.AuthState
import com.kidblunt.cleanerguru.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private const val TAG = "OnboardingScreen"

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    authViewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            title = "Clean Your Device",
            description = "Remove unwanted photos and free up storage space with intelligent scanning",
            icon = Icons.Default.CleaningServices
        ),
        OnboardingPage(
            title = "Optimize Battery",
            description = "Extend battery life with smart power management and background app control",
            icon = Icons.Default.BatteryChargingFull
        ),
        OnboardingPage(
            title = "Boost Performance",
            description = "Enhance gaming experience and overall device performance with one tap",
            icon = Icons.Default.Speed
        )
    )

    LaunchedEffect(Unit) {
        Log.d(TAG, "Checking login status")
        if (authViewModel.checkLoginStatus()) {
            Log.d(TAG, "User already logged in, navigating to dashboard")
            onNavigateToDashboard()
        } else {
            Log.d(TAG, "User not logged in, registering anonymous user")
            authViewModel.registerAnonymousUser()
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Log.d(TAG, "Authentication successful, navigating to dashboard")
                onNavigateToDashboard()
            }
            is AuthState.Error -> {
                Log.e(TAG, "Authentication error: ${state.message}")
            }
            else -> {
                Log.d(TAG, "Auth state: $state")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Twinkling stars background
        TwinklingStarsBackground(
            modifier = Modifier.fillMaxSize(),
            starCount = 25,
            starColor = PinkPrimary.copy(alpha = 0.4f)
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PinkPrimaryLight.copy(alpha = 0.3f), 
                            Color.White.copy(alpha = 0.8f), 
                            SoftSand.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            PagerIndicator(
                pagerState = pagerState,
                modifier = Modifier.padding(16.dp),
                activeColor = PinkPrimary,
                inactiveColor = PinkPrimary.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Back", color = PinkPrimary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            Log.d(TAG, "Get Started clicked")
                            if (authViewModel.checkLoginStatus()) {
                                onNavigateToDashboard()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = PinkPrimary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PinkPrimary)
            }
        }
    }
}

@Composable
fun PagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = activeColor.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(PinkPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PinkPrimary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.h1.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}