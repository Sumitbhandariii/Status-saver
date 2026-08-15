package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusItem
import com.example.ui.components.AdBannerView
import com.example.ui.components.MediaStatusCard
import com.example.ui.components.MetricCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryPurpleVariant
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SecondaryCyanDark
import com.example.ui.theme.SecondaryCyanLight
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.SavedFilter
import com.example.ui.viewmodel.StatusTab

@Composable
fun HomeScreen(
    metrics: DashboardMetrics,
    recentStatuses: List<StatusItem>,
    onNavigateTab: (NavigationTab) -> Unit,
    onNavigateStatusTab: (StatusTab) -> Unit,
    onNavigateSavedFilter: (SavedFilter) -> Unit,
    onSaveAll: () -> Unit,
    onOpenCleaner: () -> Unit,
    onStatusClick: (StatusItem) -> Unit,
    onSaveStatus: (StatusItem) -> Unit,
    onFavoriteStatus: (StatusItem) -> Unit,
    onShareStatus: (StatusItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PrimaryDeepPurple, PrimaryPurpleVariant, SecondaryCyanDark)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "AUTOMATIC STATUS SAVER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Save & Manage Statuses",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Instant 1-tap save for WhatsApp photos and videos directly to your device.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent Save All Button
                    Button(
                        onClick = onSaveAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("btn_home_save_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = PrimaryDeepPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save All (${metrics.totalAvailableCount})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = PrimaryDeepPurple,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Metrics Dashboard Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Status Overview",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "New Statuses",
                        count = metrics.newCount.toString(),
                        icon = Icons.Default.Download,
                        gradientColors = listOf(PrimaryDeepPurple, PrimaryPurpleVariant),
                        onClick = { onNavigateTab(NavigationTab.STATUSES) },
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Saved Statuses",
                        count = metrics.savedCount.toString(),
                        icon = Icons.Default.Bookmark,
                        gradientColors = listOf(AccentGreen, Color(0xFF059669)),
                        onClick = { onNavigateTab(NavigationTab.SAVED) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Images",
                        count = metrics.imagesCount.toString(),
                        icon = Icons.Default.Image,
                        gradientColors = listOf(SecondaryCyan, SecondaryCyanDark),
                        onClick = {
                            onNavigateStatusTab(StatusTab.IMAGES)
                            onNavigateTab(NavigationTab.STATUSES)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Videos",
                        count = metrics.videosCount.toString(),
                        icon = Icons.Default.Videocam,
                        gradientColors = listOf(AccentAmber, Color(0xFFE65100)),
                        onClick = {
                            onNavigateStatusTab(StatusTab.VIDEOS)
                            onNavigateTab(NavigationTab.STATUSES)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionTile(
                            icon = Icons.Default.Download,
                            label = "Save All",
                            bgColor = PrimaryPurpleLight,
                            tintColor = PrimaryDeepPurple,
                            onClick = onSaveAll,
                            testTag = "tile_save_all"
                        )
                    }

                    item {
                        QuickActionTile(
                            icon = Icons.Default.Image,
                            label = "Images",
                            bgColor = SecondaryCyanLight,
                            tintColor = SecondaryCyanDark,
                            onClick = {
                                onNavigateStatusTab(StatusTab.IMAGES)
                                onNavigateTab(NavigationTab.STATUSES)
                            },
                            testTag = "tile_view_images"
                        )
                    }

                    item {
                        QuickActionTile(
                            icon = Icons.Default.Videocam,
                            label = "Videos",
                            bgColor = Color(0xFFFFF3E0),
                            tintColor = Color(0xFFE65100),
                            onClick = {
                                onNavigateStatusTab(StatusTab.VIDEOS)
                                onNavigateTab(NavigationTab.STATUSES)
                            },
                            testTag = "tile_view_videos"
                        )
                    }

                    item {
                        QuickActionTile(
                            icon = Icons.Default.Star,
                            label = "Favorites",
                            bgColor = Color(0xFFFFF8E1),
                            tintColor = AccentAmber,
                            onClick = {
                                onNavigateSavedFilter(SavedFilter.FAVORITES)
                                onNavigateTab(NavigationTab.SAVED)
                            },
                            testTag = "tile_favorites"
                        )
                    }

                    item {
                        QuickActionTile(
                            icon = Icons.Default.CleaningServices,
                            label = "Cleaner",
                            bgColor = Color(0xFFFFEBEE),
                            tintColor = Color(0xFFD32F2F),
                            onClick = onOpenCleaner,
                            testTag = "tile_cleaner"
                        )
                    }
                }
            }
        }

        // AdMob Banner
        item {
            AdBannerView()
        }

        // Recent Statuses Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Statuses",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    )

                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = PrimaryDeepPurple,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable { onNavigateTab(NavigationTab.STATUSES) }
                            .padding(4.dp)
                            .testTag("btn_home_view_all_statuses")
                    )
                }

                if (recentStatuses.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentStatuses.take(8), key = { it.id }) { status ->
                            MediaStatusCard(
                                status = status,
                                isSelected = false,
                                isSelectionMode = false,
                                onCardClick = { onStatusClick(status) },
                                onLongClick = { onStatusClick(status) },
                                onSaveClick = { onSaveStatus(status) },
                                onFavoriteClick = { onFavoriteStatus(status) },
                                onShareClick = { onShareStatus(status) },
                                modifier = Modifier.width(170.dp)
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryPurpleLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = PrimaryDeepPurple
                                )
                            }
                            Column {
                                Text(
                                    text = "No recent statuses detected",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryText
                                    )
                                )
                                Text(
                                    text = "View WhatsApp statuses on your phone to see them here.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = SecondaryText
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .width(82.dp)
            .height(94.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryText,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
