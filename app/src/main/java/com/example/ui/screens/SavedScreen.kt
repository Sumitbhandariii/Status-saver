package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StatusItem
import com.example.ui.components.AdBannerView
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MediaStatusCard
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppSurface
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.SavedFilter

@Composable
fun SavedScreen(
    savedFilter: SavedFilter,
    onFilterSelected: (SavedFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    items: List<StatusItem>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelectionMode: () -> Unit,
    onToggleSelectId: (String) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onStatusClick: (StatusItem, List<StatusItem>) -> Unit,
    onFavoriteStatus: (StatusItem) -> Unit,
    onShareStatus: (StatusItem) -> Unit,
    onNavigateToStatuses: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Search Bar
        Surface(
            color = AppSurface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search saved statuses...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryText)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PrimaryDeepPurple
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = SecondaryText
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDeepPurple,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = Color(0xFFF9FAFB),
                        unfocusedContainerColor = Color(0xFFF9FAFB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_saved")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = savedFilter == SavedFilter.ALL,
                        onClick = { onFilterSelected(SavedFilter.ALL) },
                        label = { Text("All Media") },
                        leadingIcon = {
                            Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurpleLight,
                            selectedLabelColor = PrimaryDeepPurple
                        ),
                        modifier = Modifier.testTag("filter_all")
                    )

                    FilterChip(
                        selected = savedFilter == SavedFilter.IMAGES,
                        onClick = { onFilterSelected(SavedFilter.IMAGES) },
                        label = { Text("Images") },
                        leadingIcon = {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurpleLight,
                            selectedLabelColor = PrimaryDeepPurple
                        ),
                        modifier = Modifier.testTag("filter_images")
                    )

                    FilterChip(
                        selected = savedFilter == SavedFilter.VIDEOS,
                        onClick = { onFilterSelected(SavedFilter.VIDEOS) },
                        label = { Text("Videos") },
                        leadingIcon = {
                            Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurpleLight,
                            selectedLabelColor = PrimaryDeepPurple
                        ),
                        modifier = Modifier.testTag("filter_videos")
                    )

                    FilterChip(
                        selected = savedFilter == SavedFilter.FAVORITES,
                        onClick = { onFilterSelected(SavedFilter.FAVORITES) },
                        label = { Text("Favorites ⭐") },
                        leadingIcon = {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurpleLight,
                            selectedLabelColor = PrimaryDeepPurple
                        ),
                        modifier = Modifier.testTag("filter_favorites")
                    )
                }
            }
        }

        // Persistent AdMob Banner on Saved Screen
        AdBannerView()

        // Multi-select Toolbar
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFFFFEBEE),
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
                            modifier = Modifier.testTag("btn_saved_cancel_selection")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = AccentRed
                            )
                        }

                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentRed,
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
                                val allIds = items.map { it.id }
                                onSelectAll(allIds)
                            },
                            modifier = Modifier.testTag("btn_saved_select_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all",
                                tint = AccentRed
                            )
                        }

                        IconButton(
                            onClick = onShareSelected,
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.testTag("btn_saved_share_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share selected",
                                tint = if (selectedIds.isNotEmpty()) PrimaryDeepPurple else Color.Gray
                            )
                        }

                        Button(
                            onClick = onDeleteSelected,
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_saved_delete_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
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
        if (!isSelectionMode && items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} Saved Items",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                )

                IconButton(
                    onClick = onToggleSelectionMode,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = "Select",
                        tint = PrimaryDeepPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Grid of Saved Media
        if (items.isNotEmpty()) {
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

                items(items, key = { it.id }) { status ->
                    MediaStatusCard(
                        status = status,
                        isSelected = selectedIds.contains(status.id),
                        isSelectionMode = isSelectionMode,
                        onCardClick = { onStatusClick(status, items) },
                        onLongClick = { onToggleSelectId(status.id) },
                        onSaveClick = { /* Already saved */ },
                        onFavoriteClick = { onFavoriteStatus(status) },
                        onShareClick = { onShareStatus(status) }
                    )
                }
            }
        } else {
            EmptyStateView(
                icon = Icons.Default.Bookmark,
                title = if (searchQuery.isNotEmpty()) "No Matching Statuses" else "No Saved Statuses Yet",
                description = if (searchQuery.isNotEmpty()) "Try searching for a different name or clear the search query." else "Saved WhatsApp photos and videos will stay permanently in your vault right here.",
                actionButtonText = if (searchQuery.isEmpty()) "Browse Statuses" else "Clear Search",
                onActionClick = {
                    if (searchQuery.isNotEmpty()) onSearchQueryChange("") else onNavigateToStatuses()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
