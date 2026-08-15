package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import com.example.ui.components.AdBannerView
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaStatusCard
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppSurface
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryCyan
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.StatusTab

@Composable
fun StatusListScreen(
    currentTab: StatusTab,
    onTabSelected: (StatusTab) -> Unit,
    images: List<StatusItem>,
    videos: List<StatusItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelectionMode: () -> Unit,
    onToggleSelectId: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onSaveSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onStatusClick: (StatusItem, List<StatusItem>) -> Unit,
    onSaveStatus: (StatusItem) -> Unit,
    onFavoriteStatus: (StatusItem) -> Unit,
    onShareStatus: (StatusItem) -> Unit,
    onOpenPermissionGuide: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItems = if (currentTab == StatusTab.IMAGES) images else videos

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Tab Row (Images / Videos)
        Surface(
            color = AppSurface,
            shadowElevation = 1.dp
        ) {
            TabRow(
                selectedTabIndex = if (currentTab == StatusTab.IMAGES) 0 else 1,
                containerColor = AppSurface,
                contentColor = PrimaryDeepPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            tabPositions[if (currentTab == StatusTab.IMAGES) 0 else 1]
                        ),
                        color = PrimaryDeepPurple
                    )
                },
                divider = {
                    HorizontalDivider(color = BorderSubtle)
                }
            ) {
                Tab(
                    selected = currentTab == StatusTab.IMAGES,
                    onClick = { onTabSelected(StatusTab.IMAGES) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Images (${images.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (currentTab == StatusTab.IMAGES) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_images")
                )

                Tab(
                    selected = currentTab == StatusTab.VIDEOS,
                    onClick = { onTabSelected(StatusTab.VIDEOS) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Videos (${videos.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (currentTab == StatusTab.VIDEOS) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_videos")
                )
            }
        }

        // Multi-select Toolbar or Normal Bar
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = PrimaryPurpleLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleSelectionMode,
                            modifier = Modifier.testTag("btn_cancel_selection")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection",
                                tint = PrimaryDeepPurple
                            )
                        }

                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = PrimaryDeepPurple,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val allIds = currentItems.map { it.id }
                                onSelectAll(allIds)
                            },
                            modifier = Modifier.testTag("btn_select_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                                tint = PrimaryDeepPurple
                            )
                        }

                        IconButton(
                            onClick = onShareSelected,
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("btn_share_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share selected",
                                tint = if (selectedIds.isNotEmpty()) PrimaryDeepPurple else Color.Gray
                            )
                        }

                        Button(
                            onClick = onSaveSelected,
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDeepPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_save_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Save",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Subheader when not selecting
        if (!isSelectionMode && currentItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentItems.size} ${if (currentTab == StatusTab.IMAGES) "Photos" else "Videos"} Available",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                )

                TextButton(
                    onClick = onToggleSelectionMode,
                    testTag = "btn_enter_select_mode"
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = PrimaryDeepPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryDeepPurple,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Status Grid
        if (currentItems.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    AdBannerView()
                }

                items(currentItems, key = { it.id }) { status ->
                    MediaStatusCard(
                        status = status,
                        isSelected = selectedIds.contains(status.id),
                        isSelectionMode = isSelectionMode,
                        onCardClick = { onStatusClick(status, currentItems) },
                        onLongClick = { onToggleSelectId(status.id) },
                        onSaveClick = { onSaveStatus(status) },
                        onFavoriteClick = { onFavoriteStatus(status) },
                        onShareClick = { onShareStatus(status) }
                    )
                }
            }
        } else {
            // Empty State
            EmptyStateView(
                icon = if (currentTab == StatusTab.IMAGES) Icons.Default.Image else Icons.Default.Videocam,
                title = if (currentTab == StatusTab.IMAGES) "No Images Found" else "No Videos Found",
                description = "Open WhatsApp and view statuses from your contacts. They will automatically appear right here in StatusVault.",
                actionButtonText = "Folder Access Help",
                onActionClick = onOpenPermissionGuide,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TextButton(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
