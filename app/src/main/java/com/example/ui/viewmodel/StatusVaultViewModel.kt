package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobConfig
import com.example.ads.AdMobManager
import com.example.data.model.MediaType
import com.example.data.model.StatusItem
import com.example.data.repository.CleanerStats
import com.example.data.repository.StatusRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
    HOME,
    STATUSES,
    SAVED
}

enum class StatusTab {
    IMAGES,
    VIDEOS
}

enum class SavedFilter {
    ALL,
    IMAGES,
    VIDEOS,
    FAVORITES
}

data class DashboardMetrics(
    val newCount: Int = 0,
    val savedCount: Int = 0,
    val imagesCount: Int = 0,
    val videosCount: Int = 0,
    val favoritesCount: Int = 0,
    val totalAvailableCount: Int = 0
)

class StatusVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatusRepository(application)

    // Current navigation state
    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _statusTab = MutableStateFlow(StatusTab.IMAGES)
    val statusTab: StateFlow<StatusTab> = _statusTab.asStateFlow()

    private val _savedFilter = MutableStateFlow(SavedFilter.ALL)
    val savedFilter: StateFlow<SavedFilter> = _savedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-select state
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    // Full screen viewer state
    private val _viewerMedia = MutableStateFlow<StatusItem?>(null)
    val viewerMedia: StateFlow<StatusItem?> = _viewerMedia.asStateFlow()

    private val _viewerList = MutableStateFlow<List<StatusItem>>(emptyList())
    val viewerList: StateFlow<List<StatusItem>> = _viewerList.asStateFlow()

    private val _viewerIndex = MutableStateFlow(0)
    val viewerIndex: StateFlow<Int> = _viewerIndex.asStateFlow()

    // UI Dialogs
    private val _isCleanerDialogVisible = MutableStateFlow(false)
    val isCleanerDialogVisible: StateFlow<Boolean> = _isCleanerDialogVisible.asStateFlow()

    private val _isSettingsDialogVisible = MutableStateFlow(false)
    val isSettingsDialogVisible: StateFlow<Boolean> = _isSettingsDialogVisible.asStateFlow()

    private val _isPermissionGuideVisible = MutableStateFlow(false)
    val isPermissionGuideVisible: StateFlow<Boolean> = _isPermissionGuideVisible.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(repository.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _cleanerStats = MutableStateFlow(CleanerStats())
    val cleanerStats: StateFlow<CleanerStats> = _cleanerStats.asStateFlow()

    // Event Messages (Snackbar/Toast)
    private val _eventMessage = MutableSharedFlow<String>()
    val eventMessage: SharedFlow<String> = _eventMessage.asSharedFlow()

    // Status Flows from Room
    val allStatuses = repository.allStatuses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedStatuses = repository.savedStatuses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteStatuses = repository.favoriteStatuses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered lists for Statuses screen
    val statusImages: StateFlow<List<StatusItem>> = allStatuses.combine(_currentTab) { list, _ ->
        list.filter { it.mediaType == MediaType.IMAGE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusVideos: StateFlow<List<StatusItem>> = allStatuses.combine(_currentTab) { list, _ ->
        list.filter { it.mediaType == MediaType.VIDEO }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered list for Saved screen
    val filteredSavedStatuses: StateFlow<List<StatusItem>> = combine(
        savedStatuses,
        _savedFilter,
        _searchQuery
    ) { list, filter, query ->
        val filtered = when (filter) {
            SavedFilter.ALL -> list
            SavedFilter.IMAGES -> list.filter { it.mediaType == MediaType.IMAGE }
            SavedFilter.VIDEOS -> list.filter { it.mediaType == MediaType.VIDEO }
            SavedFilter.FAVORITES -> list.filter { it.isFavorite }
        }
        if (query.isBlank()) {
            filtered
        } else {
            filtered.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allStatuses,
        savedStatuses,
        favoriteStatuses
    ) { all, saved, favs ->
        val newCount = all.count { it.isNew }
        val imagesCount = all.count { it.mediaType == MediaType.IMAGE }
        val videosCount = all.count { it.mediaType == MediaType.VIDEO }
        DashboardMetrics(
            newCount = newCount,
            savedCount = saved.size,
            imagesCount = imagesCount,
            videosCount = videosCount,
            favoritesCount = favs.size,
            totalAvailableCount = all.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val adConfig = AdMobManager.config

    init {
        AdMobManager.init(application)
        refreshStatuses()
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
        clearSelection()
        AdMobManager.checkAndShowInterstitial()
    }

    fun setStatusTab(tab: StatusTab) {
        _statusTab.value = tab
        clearSelection()
    }

    fun setSavedFilter(filter: SavedFilter) {
        _savedFilter.value = filter
        clearSelection()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun completeOnboarding() {
        repository.setOnboardingCompleted(true)
        _isOnboardingCompleted.value = true
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshStatuses()
            _isRefreshing.value = false
            if (result.isSuccess) {
                updateCleanerStats()
            } else {
                _eventMessage.emit("Unable to refresh: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun saveSingleStatus(status: StatusItem) {
        viewModelScope.launch {
            val result = repository.saveStatus(status)
            if (result.isSuccess) {
                _eventMessage.emit("Status saved successfully to gallery!")
                updateCleanerStats()
                AdMobManager.checkAndShowInterstitial()
            } else {
                _eventMessage.emit("Failed to save: ${result.exceptionOrNull()?.localizedMessage ?: "Storage error"}")
            }
        }
    }

    fun saveAllCurrentStatuses() {
        viewModelScope.launch {
            val currentList = allStatuses.value
            val unsaved = currentList.filter { !it.isSaved }
            if (unsaved.isEmpty()) {
                _eventMessage.emit("All statuses are already saved!")
                return@launch
            }

            val result = repository.saveAllStatuses(currentList)
            if (result.isSuccess) {
                _eventMessage.emit("Saved ${result.getOrDefault(0)} statuses to gallery!")
                updateCleanerStats()
                AdMobManager.checkAndShowInterstitial()
            } else {
                _eventMessage.emit("Save All failed: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun toggleFavorite(status: StatusItem) {
        viewModelScope.launch {
            repository.toggleFavorite(status)
            val msg = if (!status.isFavorite) "Added to Favorites ⭐" else "Removed from Favorites"
            _eventMessage.emit(msg)
        }
    }

    fun deleteSavedMedia(status: StatusItem) {
        viewModelScope.launch {
            val result = repository.deleteSavedStatus(status)
            if (result.isSuccess) {
                _eventMessage.emit("Saved status removed")
                updateCleanerStats()
                if (_viewerMedia.value?.id == status.id) {
                    closeViewer()
                }
            } else {
                _eventMessage.emit("Failed to delete saved status")
            }
        }
    }

    // Multi-select actions
    fun toggleSelectionMode() {
        val next = !_isMultiSelectMode.value
        _isMultiSelectMode.value = next
        if (!next) {
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelectId(id: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
        if (current.isEmpty()) {
            _isMultiSelectMode.value = false
        } else {
            _isMultiSelectMode.value = true
        }
    }

    fun selectAll(ids: List<String>) {
        _isMultiSelectMode.value = true
        _selectedIds.value = ids.toSet()
    }

    fun clearSelection() {
        _isMultiSelectMode.value = false
        _selectedIds.value = emptySet()
    }

    fun saveSelected() {
        viewModelScope.launch {
            val selected = _selectedIds.value
            val itemsToSave = allStatuses.value.filter { selected.contains(it.id) }
            val res = repository.saveAllStatuses(itemsToSave)
            clearSelection()
            if (res.isSuccess) {
                _eventMessage.emit("Saved ${res.getOrDefault(0)} selected items!")
                updateCleanerStats()
                AdMobManager.checkAndShowInterstitial()
            } else {
                _eventMessage.emit("Failed to save selected items")
            }
        }
    }

    fun deleteSelectedSaved() {
        viewModelScope.launch {
            val selected = _selectedIds.value
            val itemsToDelete = savedStatuses.value.filter { selected.contains(it.id) }
            val res = repository.deleteSelectedSaved(itemsToDelete)
            clearSelection()
            if (res.isSuccess) {
                _eventMessage.emit("Deleted ${res.getOrDefault(0)} saved files")
                updateCleanerStats()
            } else {
                _eventMessage.emit("Failed to delete selected files")
            }
        }
    }

    fun shareStatus(context: Context, status: StatusItem) {
        val intent = repository.shareMedia(status)
        if (intent != null) {
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                viewModelScope.launch { _eventMessage.emit("No compatible application found to share media") }
            }
        } else {
            viewModelScope.launch { _eventMessage.emit("Unable to prepare media for sharing") }
        }
    }

    fun shareSelected(context: Context) {
        val selected = _selectedIds.value
        val items = (if (_currentTab.value == NavigationTab.SAVED) savedStatuses.value else allStatuses.value)
            .filter { selected.contains(it.id) }

        if (items.isEmpty()) return

        val intent = repository.shareMultipleMedia(items)
        if (intent != null) {
            try {
                context.startActivity(intent)
                clearSelection()
            } catch (e: Exception) {
                viewModelScope.launch { _eventMessage.emit("No app found to share multiple items") }
            }
        } else {
            viewModelScope.launch { _eventMessage.emit("Unable to share selected media") }
        }
    }

    // Full screen viewer controls
    fun openViewer(item: StatusItem, list: List<StatusItem>) {
        val index = list.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        _viewerList.value = list
        _viewerIndex.value = index
        _viewerMedia.value = item
        viewModelScope.launch {
            repository.markAsViewed(item.id)
        }
    }

    fun closeViewer() {
        _viewerMedia.value = null
        _viewerList.value = emptyList()
        _viewerIndex.value = 0
    }

    fun navigateViewer(direction: Int) {
        val list = _viewerList.value
        if (list.isEmpty()) return
        val newIndex = (_viewerIndex.value + direction).coerceIn(0, list.size - 1)
        _viewerIndex.value = newIndex
        val item = list[newIndex]
        _viewerMedia.value = item
        viewModelScope.launch {
            repository.markAsViewed(item.id)
        }
    }

    // Cleaner actions
    fun openCleanerDialog() {
        updateCleanerStats()
        _isCleanerDialogVisible.value = true
    }

    fun closeCleanerDialog() {
        _isCleanerDialogVisible.value = false
    }

    fun deleteAllSavedMedia() {
        viewModelScope.launch {
            val res = repository.deleteAllSavedMedia()
            if (res.isSuccess) {
                _eventMessage.emit("All saved media cleaned up (${res.getOrDefault(0)} files removed)")
                updateCleanerStats()
            } else {
                _eventMessage.emit("Failed to clean saved media")
            }
            closeCleanerDialog()
        }
    }

    fun cleanCache() {
        viewModelScope.launch {
            val res = repository.cleanCache()
            if (res.isSuccess) {
                _eventMessage.emit("Freed ${_cleanerStats.value.formatBytes(res.getOrDefault(0L))} of cache space")
                updateCleanerStats()
            } else {
                _eventMessage.emit("Cache cleanup failed")
            }
        }
    }

    private fun updateCleanerStats() {
        viewModelScope.launch {
            val stats = repository.computeCleanerStats(savedStatuses.value)
            _cleanerStats.value = stats
        }
    }

    // Settings & Permissions
    fun openSettingsDialog() {
        _isSettingsDialogVisible.value = true
    }

    fun closeSettingsDialog() {
        _isSettingsDialogVisible.value = false
    }

    fun saveAdMobConfig(config: AdMobConfig) {
        AdMobManager.saveConfig(getApplication(), config)
        viewModelScope.launch { _eventMessage.emit("AdMob configuration updated successfully!") }
    }

    fun openPermissionGuide() {
        _isPermissionGuideVisible.value = true
    }

    fun closePermissionGuide() {
        _isPermissionGuideVisible.value = false
    }

    fun updateCustomTreeUri(uriString: String?) {
        repository.setCustomTreeUri(uriString)
        refreshStatuses()
    }
}
