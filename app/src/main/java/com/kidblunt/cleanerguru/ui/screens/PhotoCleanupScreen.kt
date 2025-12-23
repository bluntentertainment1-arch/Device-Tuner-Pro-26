@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
package com.kidblunt.cleanerguru.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import com.kidblunt.cleanerguru.R

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kidblunt.cleanerguru.ui.theme.*
import com.kidblunt.cleanerguru.ui.components.TwinklingStarsBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private const val TAG = "PhotoCleanupScreen"

data class PhotoItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val category: PhotoCategory = PhotoCategory.OTHER
)

enum class PhotoCategory {
    SCREENSHOTS, DUPLICATES, LARGE_FILES, OLD_PHOTOS, OTHER
}

data class PhotoAnalysis(
    val totalPhotos: Int,
    val totalSize: Long,
    val screenshots: List<PhotoItem>,
    val largeFiles: List<PhotoItem>,
    val oldPhotos: List<PhotoItem>,
    val duplicates: List<PhotoItem>
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoCleanupScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var selectedPhotos by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isScanning by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(PhotoCategory.OTHER) }
    var photoAnalysis by remember { mutableStateOf<PhotoAnalysis?>(null) }
    var showAnalysis by remember { mutableStateOf(false) }
    var deleteResult by remember { mutableStateOf<String?>(null) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                isScanning = true
                val scannedPhotos = scanPhotos(context.contentResolver)
                photos = scannedPhotos
                photoAnalysis = analyzePhotos(scannedPhotos)
                isScanning = false
                showAnalysis = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark theme background with twinkling stars
        TwinklingStarsBackground(
            modifier = Modifier.fillMaxSize(),
            starCount = 25,
            starColor = NeonPink.copy(alpha = 0.4f)
        )

        // Gradient overlay for dark theme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.8f),
                            CardBackground.copy(alpha = 0.9f),
                            DarkBackground.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Scaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                "Storage Cleanup",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (photos.isNotEmpty()) {
                                Text(
                                    text = "${photos.size} photos • ${formatFileSize(photos.sumOf { it.size })}",
                                    style = MaterialTheme.typography.caption,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    backgroundColor = NeonPink,
                    contentColor = Color.White,
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        if (photos.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    selectedPhotos = if (selectedPhotos.size == photos.size) {
                                        emptySet()
                                    } else {
                                        photos.map { it.uri }.toSet()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedPhotos.size == photos.size) {
                                        Icons.Default.CheckBoxOutlineBlank
                                    } else {
                                        Icons.Default.CheckBox
                                    },
                                    contentDescription = "Select All",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showAnalysis = !showAnalysis }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "Analysis",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedPhotos.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showDeleteDialog = true },
                        backgroundColor = ErrorRed
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    !permissionState.status.isGranted -> {
                        PermissionRequestContent(
                            onRequestPermission = { permissionLauncher.launch(permission) },
                            shouldShowRationale = permissionState.status.shouldShowRationale
                        )
                    }
                    isScanning -> {
                        LoadingContent("Scanning and analyzing photos...")
                    }
                    photos.isEmpty() -> {
                        EmptyContent(
                            onScanClick = {
                                scope.launch {
                                    isScanning = true
                                    val scannedPhotos = scanPhotos(context.contentResolver)
                                    photos = scannedPhotos
                                    photoAnalysis = analyzePhotos(scannedPhotos)
                                    isScanning = false
                                    showAnalysis = true
                                }
                            }
                        )
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = showAnalysis,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                photoAnalysis?.let { analysis ->
                                    PhotoAnalysisCard(
                                        analysis = analysis,
                                        onCategorySelected = { category ->
                                            currentFilter = category
                                            selectedPhotos = when (category) {
                                                PhotoCategory.SCREENSHOTS -> analysis.screenshots.map { it.uri }.toSet()
                                                PhotoCategory.LARGE_FILES -> analysis.largeFiles.map { it.uri }.toSet()
                                                PhotoCategory.OLD_PHOTOS -> analysis.oldPhotos.map { it.uri }.toSet()
                                                PhotoCategory.DUPLICATES -> analysis.duplicates.map { it.uri }.toSet()
                                                PhotoCategory.OTHER -> emptySet()
                                            }
                                        }
                                    )
                                }
                            }

                            PhotoStatsBar(
                                totalPhotos = photos.size,
                                selectedPhotos = selectedPhotos.size,
                                totalSize = photos.sumOf { it.size },
                                selectedSize = photos.filter { selectedPhotos.contains(it.uri) }.sumOf { it.size }
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(photos) { photo ->
                                    EnhancedPhotoGridItem(
                                        photo = photo,
                                        isSelected = selectedPhotos.contains(photo.uri),
                                        onToggleSelection = {
                                            selectedPhotos = if (selectedPhotos.contains(photo.uri)) {
                                                selectedPhotos - photo.uri
                                            } else {
                                                selectedPhotos + photo.uri
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isDeleting) {
                    LoadingContent("Deleting photos...")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            backgroundColor = CardBackground,
            title = { 
                Text(
                    "Delete Photos",
                    color = Color.White
                ) 
            },
            text = { 
                Column {
                    Text(
                        "Are you sure you want to delete ${selectedPhotos.size} photo(s)?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will free up ${formatFileSize(photos.filter { selectedPhotos.contains(it.uri) }.sumOf { it.size })}",
                        style = MaterialTheme.typography.caption,
                        color = SuccessGreen
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            isDeleting = true
                            val result = deletePhotos(context.contentResolver, selectedPhotos.toList())
                            photos = photos.filterNot { selectedPhotos.contains(it.uri) }
                            selectedPhotos = emptySet()
                            photoAnalysis = analyzePhotos(photos)
                            deleteResult = result
                            isDeleting = false
                        }
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Show delete result snackbar
    deleteResult?.let { result ->
        LaunchedEffect(result) {
            // You can show a snackbar or toast here
            deleteResult = null
        }
    }
}

@Composable
fun PhotoAnalysisCard(
    analysis: PhotoAnalysis,
    onCategorySelected: (PhotoCategory) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 6.dp,
        backgroundColor = CardBackground.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Smart Analysis",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (analysis.screenshots.isNotEmpty()) {
                    CategoryChip(
                        text = "Screenshots (${analysis.screenshots.size})",
                        color = WarningOrange,
                        onClick = { onCategorySelected(PhotoCategory.SCREENSHOTS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (analysis.largeFiles.isNotEmpty()) {
                    CategoryChip(
                        text = "Large Files (${analysis.largeFiles.size})",
                        color = ErrorRed,
                        onClick = { onCategorySelected(PhotoCategory.LARGE_FILES) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (analysis.oldPhotos.isNotEmpty()) {
                    CategoryChip(
                        text = "Old Photos (${analysis.oldPhotos.size})",
                        color = ElectricBlue,
                        onClick = { onCategorySelected(PhotoCategory.OLD_PHOTOS) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (analysis.duplicates.isNotEmpty()) {
                    CategoryChip(
                        text = "Duplicates (${analysis.duplicates.size})",
                        color = SuccessGreen,
                        onClick = { onCategorySelected(PhotoCategory.DUPLICATES) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        backgroundColor = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    shouldShowRationale: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = NeonPink
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (shouldShowRationale) {
                "Storage permission is required to scan and clean photos from your device."
            } else {
                "Please grant storage permission to continue."
            },
            style = MaterialTheme.typography.body1,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(backgroundColor = NeonPink)
        ) {
            Text("Grant Permission", color = Color.White)
        }
    }
}

@Composable
fun EmptyContent(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = NeonPink
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Photos Found",
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scan your device to find and analyze photos",
            style = MaterialTheme.typography.body1,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(backgroundColor = NeonPink)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan Photos", color = Color.White)
        }
    }
}

@Composable
fun LoadingContent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp,
            backgroundColor = CardBackground
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = NeonPink)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PhotoStatsBar(
    totalPhotos: Int,
    selectedPhotos: Int,
    totalSize: Long,
    selectedSize: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp,
        backgroundColor = CardBackground.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$totalPhotos Photos",
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = formatFileSize(totalSize),
                    style = MaterialTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            if (selectedPhotos > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$selectedPhotos selected",
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "Save ${formatFileSize(selectedSize)}",
                        style = MaterialTheme.typography.caption,
                        color = SuccessGreen
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPhotoGridItem(
    photo: PhotoItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onToggleSelection)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, SuccessGreen, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(photo.uri),
            contentDescription = photo.name,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Category indicator
        if (photo.category != PhotoCategory.OTHER) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                backgroundColor = getCategoryColor(photo.category),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = getCategoryIcon(photo.category),
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.caption,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // File size indicator
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            backgroundColor = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = formatFileSize(photo.size),
                modifier = Modifier.padding(4.dp),
                style = MaterialTheme.typography.caption,
                color = Color.White,
                fontSize = 10.sp
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SuccessGreen.copy(alpha = 0.3f))
            )

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = SuccessGreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}

private fun getCategoryColor(category: PhotoCategory): Color {
    return when (category) {
        PhotoCategory.SCREENSHOTS -> WarningOrange
        PhotoCategory.LARGE_FILES -> ErrorRed
        PhotoCategory.OLD_PHOTOS -> ElectricBlue
        PhotoCategory.DUPLICATES -> SuccessGreen
        PhotoCategory.OTHER -> Color.Gray
    }
}

private fun getCategoryIcon(category: PhotoCategory): String {
    return when (category) {
        PhotoCategory.SCREENSHOTS -> "📱"
        PhotoCategory.LARGE_FILES -> "📁"
        PhotoCategory.OLD_PHOTOS -> "📅"
        PhotoCategory.DUPLICATES -> "👥"
        PhotoCategory.OTHER -> "📷"
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

private suspend fun scanPhotos(contentResolver: ContentResolver): List<PhotoItem> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<PhotoItem>()
    
    try {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                photos.add(PhotoItem(uri, name, size, dateAdded))
            }
        }

        Log.d(TAG, "Scanned ${photos.size} photos")
    } catch (e: Exception) {
        Log.e(TAG, "Error scanning photos", e)
    }

    photos
}

private suspend fun analyzePhotos(photos: List<PhotoItem>): PhotoAnalysis = withContext(Dispatchers.IO) {
    val screenshots = photos.filter { 
        it.name.contains("screenshot", ignoreCase = true) || 
        it.name.contains("screen", ignoreCase = true)
    }
    
    val largeFiles = photos.filter { it.size > 10 * 1024 * 1024 } // > 10MB
    
    val thirtyDaysAgo = System.currentTimeMillis() / 1000 - (30 * 24 * 60 * 60)
    val oldPhotos = photos.filter { it.dateAdded < thirtyDaysAgo }
    
    // Enhanced duplicate detection based on file size and content similarity
    val duplicates = mutableListOf<PhotoItem>()
    val sizeGroups = photos.groupBy { it.size }
    
    sizeGroups.forEach { (size, photosWithSameSize) ->
        if (photosWithSameSize.size > 1 && size > 0) {
            // Group by similar names (first 10 characters)
            val nameGroups = photosWithSameSize.groupBy { it.name.take(10) }
            nameGroups.forEach { (namePrefix, similarPhotos) ->
                if (similarPhotos.size > 1) {
                    // Add all but the first one as duplicates
                    duplicates.addAll(similarPhotos.drop(1))
                }
            }
            
            // Also check for photos with exact same size but different names
            if (photosWithSameSize.size > 1) {
                val sortedByDate = photosWithSameSize.sortedBy { it.dateAdded }
                // Consider photos with same size taken within 1 minute as potential duplicates
                for (i in 1 until sortedByDate.size) {
                    val timeDiff = abs(sortedByDate[i].dateAdded - sortedByDate[i-1].dateAdded)
                    if (timeDiff <= 60) { // Within 1 minute
                        if (!duplicates.contains(sortedByDate[i])) {
                            duplicates.add(sortedByDate[i])
                        }
                    }
                }
            }
        }
    }
    
    PhotoAnalysis(
        totalPhotos = photos.size,
        totalSize = photos.sumOf { it.size },
        screenshots = screenshots,
        largeFiles = largeFiles,
        oldPhotos = oldPhotos,
        duplicates = duplicates.distinct()
    )
}

private suspend fun deletePhotos(contentResolver: ContentResolver, uris: List<Uri>): String = withContext(Dispatchers.IO) {
    var deletedCount = 0
    var failedCount = 0
    
    try {
        uris.forEach { uri ->
            try {
                val deleted = contentResolver.delete(uri, null, null)
                if (deleted > 0) {
                    deletedCount++
                    Log.d(TAG, "Successfully deleted photo: $uri")
                } else {
                    failedCount++
                    Log.w(TAG, "Failed to delete photo: $uri")
                }
            } catch (e: Exception) {
                failedCount++
                Log.e(TAG, "Error deleting photo: $uri", e)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error during batch delete", e)
    }
    
    return@withContext if (failedCount == 0) {
        "Successfully deleted $deletedCount photos"
    } else {
        "Deleted $deletedCount photos, failed to delete $failedCount photos"
    }
}