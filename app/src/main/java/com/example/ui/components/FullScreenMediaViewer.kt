package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.SecondaryCyan
import kotlinx.coroutines.delay

/**
 * Full-screen high-resolution status media preview composable.
 * Supports zoomable pinch/double-tap photo viewing, responsive video playback,
 * navigation between statuses, and prominent Save & Share action buttons.
 */
@Composable
fun FullScreenMediaViewer(
    status: StatusItem,
    currentIndex: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onNavigate: (Int) -> Unit,
    onSave: (StatusItem) -> Unit,
    onShare: (StatusItem) -> Unit,
    onFavorite: (StatusItem) -> Unit,
    onDelete: ((StatusItem) -> Unit)? = null
) {
    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Interactive High-Res Media Content
        if (status.mediaType == MediaType.IMAGE) {
            ZoomableImageView(
                status = status,
                onToggleControls = { showControls = !showControls },
                onSwipeLeft = { if (currentIndex < totalCount - 1) onNavigate(1) },
                onSwipeRight = { if (currentIndex > 0) onNavigate(-1) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FullScreenVideoPlayer(
                status = status,
                showControls = showControls,
                onToggleControls = { showControls = !showControls },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Gradient Scrim for readable controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button and title info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("btn_viewer_close")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Viewer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = status.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (status.mediaType == MediaType.IMAGE) PrimaryDeepPurple else SecondaryCyan,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (status.mediaType == MediaType.IMAGE) "HD IMAGE" else "HD VIDEO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val detailInfo = if (status.mediaType == MediaType.VIDEO) {
                                "${currentIndex + 1} of $totalCount • ${status.formattedSize} • ${status.formattedDuration}"
                            } else {
                                "${currentIndex + 1} of $totalCount • ${status.formattedSize}"
                            }
                            Text(
                                text = detailInfo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Top Right Actions (Favorite & Info)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onFavorite(status) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("btn_viewer_fav")
                        ) {
                            Icon(
                                imageVector = if (status.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (status.isFavorite) AccentAmber else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showInfoSheet = !showInfoSheet },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("btn_viewer_info")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Media Details",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Left / Right Navigation Chevrons
        if (showControls) {
            if (currentIndex > 0) {
                IconButton(
                    onClick = { onNavigate(-1) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("btn_viewer_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Media",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            if (currentIndex < totalCount - 1) {
                IconButton(
                    onClick = { onNavigate(1) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("btn_viewer_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Media",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Bottom Action Bar with Prominent Save and Share Buttons
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optional Info banner if toggled
                    if (showInfoSheet) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1E2E).copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Status Information",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Path: ${status.filePath ?: status.uriString}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Size: ${status.formattedSize} • Type: ${if (status.mediaType == MediaType.IMAGE) "JPEG / PNG" else "MP4 Video"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Main Action Row (Save & Share Primary Buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prominent SAVE Button
                        Button(
                            onClick = { onSave(status) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status.isSaved) AccentGreen else PrimaryDeepPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_viewer_save")
                        ) {
                            Icon(
                                imageVector = if (status.isSaved) Icons.Default.Check else Icons.Default.Download,
                                contentDescription = if (status.isSaved) "Saved" else "Save Status",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (status.isSaved) "Saved to Gallery" else "Save Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Prominent SHARE Button
                        Button(
                            onClick = { onShare(status) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_viewer_share")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Status",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Delete button if already saved
                        if (status.isSaved && onDelete != null) {
                            IconButton(
                                onClick = { onDelete(status) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AccentRed.copy(alpha = 0.25f))
                                    .testTag("btn_viewer_delete")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Saved Media",
                                    tint = AccentRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zoomable High-Resolution Image Composable with double-tap zoom & pinch-to-zoom.
 */
@Composable
fun ZoomableImageView(
    status: StatusItem,
    onToggleControls: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(),
        label = "scale_anim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    if (newScale > 1f) {
                        scale = newScale
                        val maxOffsetX = (newScale - 1f) * 500f
                        val maxOffsetY = (newScale - 1f) * 800f
                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        // Gesture swipe for navigation when not zoomed in
                        if (pan.x < -40f && scale <= 1.05f) {
                            onSwipeLeft()
                        } else if (pan.x > 40f && scale <= 1.05f) {
                            onSwipeRight()
                        }
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (!status.filePath.isNullOrBlank()) status.filePath else status.uriString)
                .crossfade(true)
                .build(),
            contentDescription = status.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PrimaryDeepPurple,
                        modifier = Modifier.size(40.dp)
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Unable to load high-resolution image",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        )
    }
}

/**
 * Full-screen Video Player Composable with Playback Timeline & Audio Controls.
 */
@Composable
fun FullScreenVideoPlayer(
    status: StatusItem,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var isPlayerError by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(status.durationMs.coerceAtLeast(1000L)) }

    val context = LocalContext.current

    LaunchedEffect(isPlaying, isPlayerError) {
        while (isPlaying) {
            if (!isPlayerError && videoViewRef != null) {
                videoViewRef?.let {
                    currentPosition = it.currentPosition.toLong()
                    val dur = it.duration.toLong()
                    if (dur > 0) totalDuration = dur
                }
            } else {
                currentPosition += 250L
                if (currentPosition >= totalDuration) {
                    currentPosition = totalDuration
                    isPlaying = false
                    isCompleted = true
                }
            }
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggleControls() },
        contentAlignment = Alignment.Center
    ) {
        if (isPlayerError) {
            // Visual poster fallback for unsupported video formats
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (!status.filePath.isNullOrBlank()) status.filePath else status.uriString)
                    .crossfade(true)
                    .allowHardware(false)
                    .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                    .build(),
                contentDescription = status.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = if (!status.filePath.isNullOrBlank()) {
                            Uri.parse(status.filePath)
                        } else {
                            Uri.parse(status.uriString)
                        }
                        setOnErrorListener { _, _, _ ->
                            isPlayerError = true
                            isPlaying = true
                            true // Return true to indicate error handled and prevent system error dialog
                        }
                        try {
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                try {
                                    mp.isLooping = true
                                    if (mp.duration > 0) totalDuration = mp.duration.toLong()
                                    start()
                                    isPlaying = true
                                    isCompleted = false
                                } catch (_: Exception) {
                                    isPlayerError = true
                                }
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                isCompleted = true
                            }
                        } catch (_: Exception) {
                            isPlayerError = true
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Center Floating Play / Pause / Replay Button
        if (showControls) {
            IconButton(
                onClick = {
                    if (isPlayerError) {
                        if (isCompleted) {
                            currentPosition = 0L
                            isPlaying = true
                            isCompleted = false
                        } else {
                            isPlaying = !isPlaying
                        }
                    } else {
                        videoViewRef?.let {
                            if (isCompleted) {
                                it.seekTo(0)
                                it.start()
                                isPlaying = true
                                isCompleted = false
                            } else if (it.isPlaying) {
                                it.pause()
                                isPlaying = false
                            } else {
                                it.start()
                                isPlaying = true
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("btn_video_toggle_play")
            ) {
                Icon(
                    imageVector = when {
                        isCompleted -> Icons.Default.Replay
                        isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Video Progress Timeline Bar (Positioned above bottom action buttons)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMillis(currentPosition),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "HD 1080p",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SecondaryCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = formatMillis(totalDuration),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Slider(
                    value = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { frac ->
                        val targetMs = (frac * totalDuration).toInt()
                        if (!isPlayerError) {
                            videoViewRef?.seekTo(targetMs)
                        }
                        currentPosition = targetMs.toLong()
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SecondaryCyan,
                        activeTrackColor = SecondaryCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
