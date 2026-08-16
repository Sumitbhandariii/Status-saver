package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdMobManager
import com.example.ui.components.FullScreenMediaViewer
import com.example.ui.components.StatusVaultTopAppBar
import com.example.ui.dialogs.AdInterstitialDialog
import com.example.ui.dialogs.AdRewardedDialog
import com.example.ui.dialogs.PermissionGuideDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.StatusCleanerDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.StatusListScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.AppSurface
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.PrimaryDeepPurple
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryText
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.StatusVaultTheme
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.StatusTab
import com.example.ui.viewmodel.StatusVaultViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: StatusVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdMobManager.currentActivity = this
        enableEdgeToEdge()

        setContent {
            StatusVaultTheme {
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        onFinish = { viewModel.completeOnboarding() }
                    )
                } else {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AdMobManager.currentActivity = this
    }
}

@Composable
fun MainAppContent(viewModel: StatusVaultViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val currentTab by viewModel.currentTab.collectAsState()
    val statusTab by viewModel.statusTab.collectAsState()
    val savedFilter by viewModel.savedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val allStatuses by viewModel.allStatuses.collectAsState()
    val savedStatuses by viewModel.savedStatuses.collectAsState()
    val filteredSaved by viewModel.filteredSavedStatuses.collectAsState()
    val statusImages by viewModel.statusImages.collectAsState()
    val statusVideos by viewModel.statusVideos.collectAsState()
    val dashboardMetrics by viewModel.dashboardMetrics.collectAsState()

    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    val viewerMedia by viewModel.viewerMedia.collectAsState()
    val viewerList by viewModel.viewerList.collectAsState()
    val viewerIndex by viewModel.viewerIndex.collectAsState()

    val isCleanerDialogVisible by viewModel.isCleanerDialogVisible.collectAsState()
    val isSettingsDialogVisible by viewModel.isSettingsDialogVisible.collectAsState()
    val isPermissionGuideVisible by viewModel.isPermissionGuideVisible.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val cleanerStats by viewModel.cleanerStats.collectAsState()
    val adConfig by viewModel.adConfig.collectAsState()
    val adDialogVisible by AdMobManager.adDialogVisible.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Back button handling with 1-hour interval closing Interstitial Ad
    BackHandler(enabled = true) {
        when {
            viewerMedia != null -> viewModel.closeViewer()
            isMultiSelectMode -> viewModel.clearSelection()
            currentTab != NavigationTab.HOME -> viewModel.setTab(NavigationTab.HOME)
            else -> {
                // User is on Home tab and pressing back to exit the app
                AdMobManager.showAppExitInterstitial(activity) {
                    activity?.finish()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                StatusVaultTopAppBar(
                    title = "StatusVault",
                    subtitle = when (currentTab) {
                        NavigationTab.HOME -> "Status Saver & Gallery Vault"
                        NavigationTab.STATUSES -> if (statusTab == StatusTab.IMAGES) "WhatsApp Photos" else "WhatsApp Videos"
                        NavigationTab.SAVED -> "Saved Statuses (${savedStatuses.size})"
                    },
                    onRefresh = { viewModel.refreshStatuses() },
                    onOpenCleaner = { viewModel.openCleanerDialog() },
                    onOpenSettings = { viewModel.openSettingsDialog() },
                    isRefreshing = isRefreshing
                )
            },
            bottomBar = {
                StatusVaultBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) },
                    newCount = dashboardMetrics.newCount,
                    savedCount = dashboardMetrics.savedCount
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.navigationBarsPadding()
                )
            },
            containerColor = AppBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            metrics = dashboardMetrics,
                            recentStatuses = allStatuses,
                            onNavigateTab = { viewModel.setTab(it) },
                            onNavigateStatusTab = { viewModel.setStatusTab(it) },
                            onNavigateSavedFilter = { viewModel.setSavedFilter(it) },
                            onSaveAll = { viewModel.saveAllCurrentStatuses() },
                            onOpenCleaner = { viewModel.openCleanerDialog() },
                            onStatusClick = { viewModel.openViewer(it, allStatuses) },
                            onSaveStatus = { viewModel.saveSingleStatus(it) },
                            onFavoriteStatus = { viewModel.toggleFavorite(it) },
                            onShareStatus = { viewModel.shareStatus(context, it) }
                        )
                    }

                    NavigationTab.STATUSES -> {
                        StatusListScreen(
                            currentTab = statusTab,
                            onTabSelected = { viewModel.setStatusTab(it) },
                            images = statusImages,
                            videos = statusVideos,
                            isSelectionMode = isMultiSelectMode,
                            selectedIds = selectedIds,
                            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                            onToggleSelectId = { viewModel.toggleSelectId(it) },
                            onSelectAll = { viewModel.selectAll(it) },
                            onSaveSelected = { viewModel.saveSelected() },
                            onShareSelected = { viewModel.shareSelected(context) },
                            onStatusClick = { item, list -> viewModel.openViewer(item, list) },
                            onSaveStatus = { viewModel.saveSingleStatus(it) },
                            onFavoriteStatus = { viewModel.toggleFavorite(it) },
                            onShareStatus = { viewModel.shareStatus(context, it) },
                            onOpenPermissionGuide = { viewModel.openPermissionGuide() },
                            onRefresh = { viewModel.refreshStatuses() }
                        )
                    }

                    NavigationTab.SAVED -> {
                        SavedScreen(
                            savedFilter = savedFilter,
                            onFilterSelected = { viewModel.setSavedFilter(it) },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            items = filteredSaved,
                            isSelectionMode = isMultiSelectMode,
                            selectedIds = selectedIds,
                            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                            onToggleSelectId = { viewModel.toggleSelectId(it) },
                            onSelectAll = { viewModel.selectAll(it) },
                            onDeleteSelected = { viewModel.deleteSelectedSaved() },
                            onShareSelected = { viewModel.shareSelected(context) },
                            onStatusClick = { item, list -> viewModel.openViewer(item, list) },
                            onFavoriteStatus = { viewModel.toggleFavorite(it) },
                            onShareStatus = { viewModel.shareStatus(context, it) },
                            onNavigateToStatuses = { viewModel.setTab(NavigationTab.STATUSES) }
                        )
                    }
                }
            }
        }

        // Full Screen Viewer Modal Overlay
        AnimatedVisibility(
            visible = viewerMedia != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            viewerMedia?.let { currentMedia ->
                FullScreenMediaViewer(
                    status = currentMedia,
                    currentIndex = viewerIndex,
                    totalCount = viewerList.size,
                    onClose = { viewModel.closeViewer() },
                    onNavigate = { direction -> viewModel.navigateViewer(direction) },
                    onSave = { viewModel.saveSingleStatus(it) },
                    onShare = { viewModel.shareStatus(context, it) },
                    onFavorite = { viewModel.toggleFavorite(it) },
                    onDelete = if (currentMedia.isSaved) {
                        { viewModel.deleteSavedMedia(it) }
                    } else null
                )
            }
        }

        // Status Cleaner Dialog
        if (isCleanerDialogVisible) {
            StatusCleanerDialog(
                stats = cleanerStats,
                onDismiss = { viewModel.closeCleanerDialog() },
                onCleanCache = { viewModel.cleanCache() },
                onDeleteAllSaved = { viewModel.deleteAllSavedMedia() }
            )
        }

        // Settings Dialog (Production User Preferences)
        if (isSettingsDialogVisible) {
            SettingsDialog(
                onOpenPermissionGuide = { viewModel.openPermissionGuide() },
                onOpenCleaner = { viewModel.openCleanerDialog() },
                onDismiss = { viewModel.closeSettingsDialog() }
            )
        }

        // WhatsApp Folder & Permission Guide Dialog
        if (isPermissionGuideVisible) {
            PermissionGuideDialog(
                onFolderSelected = { uri -> viewModel.updateCustomTreeUri(uri) },
                onDismiss = { viewModel.closePermissionGuide() }
            )
        }

        // Ad Interstitial Modal (Fallback)
        if (adDialogVisible == "INTERSTITIAL") {
            AdInterstitialDialog(
                onDismiss = { AdMobManager.closeAd() }
            )
        }

        // Ad Rewarded Modal (Fallback)
        if (adDialogVisible?.startsWith("REWARDED") == true) {
            val reason = adDialogVisible?.substringAfter("REWARDED:") ?: ""
            AdRewardedDialog(
                reason = reason,
                onReward = {
                    AdMobManager.closeAd(reward = true)
                },
                onDismiss = { AdMobManager.closeAd(reward = false) }
            )
        }
    }
}

@Composable
fun StatusVaultBottomNav(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    newCount: Int,
    savedCount: Int
) {
    Surface(
        color = AppSurface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        NavigationBar(
            containerColor = AppSurface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.height(68.dp)
        ) {
            // Tab 1: Home
            NavigationBarItem(
                selected = currentTab == NavigationTab.HOME,
                onClick = { onTabSelected(NavigationTab.HOME) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == NavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = "Home",
                        fontWeight = if (currentTab == NavigationTab.HOME) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepPurple,
                    selectedTextColor = PrimaryDeepPurple,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText,
                    indicatorColor = PrimaryPurpleLight
                ),
                modifier = Modifier.testTag("nav_home")
            )

            // Tab 2: Statuses
            NavigationBarItem(
                selected = currentTab == NavigationTab.STATUSES,
                onClick = { onTabSelected(NavigationTab.STATUSES) },
                icon = {
                    if (newCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = PrimaryDeepPurple,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (newCount > 99) "99+" else newCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.STATUSES) Icons.Filled.Download else Icons.Outlined.Download,
                                contentDescription = "Statuses",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.STATUSES) Icons.Filled.Download else Icons.Outlined.Download,
                            contentDescription = "Statuses",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = "Statuses",
                        fontWeight = if (currentTab == NavigationTab.STATUSES) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepPurple,
                    selectedTextColor = PrimaryDeepPurple,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText,
                    indicatorColor = PrimaryPurpleLight
                ),
                modifier = Modifier.testTag("nav_statuses")
            )

            // Tab 3: Saved
            NavigationBarItem(
                selected = currentTab == NavigationTab.SAVED,
                onClick = { onTabSelected(NavigationTab.SAVED) },
                icon = {
                    if (savedCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = AccentGreen,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = savedCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == NavigationTab.SAVED) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Saved",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (currentTab == NavigationTab.SAVED) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Saved",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = "Saved",
                        fontWeight = if (currentTab == NavigationTab.SAVED) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryDeepPurple,
                    selectedTextColor = PrimaryDeepPurple,
                    unselectedIconColor = SecondaryText,
                    unselectedTextColor = SecondaryText,
                    indicatorColor = PrimaryPurpleLight
                ),
                modifier = Modifier.testTag("nav_saved")
            )
        }
    }
}
