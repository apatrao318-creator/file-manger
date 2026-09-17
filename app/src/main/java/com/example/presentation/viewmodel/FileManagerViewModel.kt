package com.example.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FavoriteEntity
import com.example.data.local.entity.RecentFileEntity
import com.example.data.local.entity.TrashItemEntity
import com.example.data.model.ApkInfo
import com.example.data.model.CategoryStat
import com.example.data.model.FileCategory
import com.example.data.model.FileItem
import com.example.data.model.SortOrder
import com.example.data.model.SortType
import com.example.data.model.StorageInfo
import com.example.data.model.ThemeMode
import com.example.data.model.ViewMode
import com.example.data.model.ZipEntryInfo
import com.example.data.repository.FileManagerRepository
import com.example.utils.FileUtils
import com.example.utils.StorageUtils
import com.example.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ClipboardMode { COPY, CUT }

data class OperationProgress(
    val title: String,
    val current: Int,
    val total: Int,
    val currentFile: String
)

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = FileManagerRepository(db)

    // Roots
    val storageRoot: File = StorageUtils.getPrimaryStorageRoot()

    // Current Directory & Nav History
    private val _currentDirectory = MutableStateFlow(storageRoot)
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val directoryStack = mutableListOf<File>()

    private val _currentItems = MutableStateFlow<List<FileItem>>(emptyList())
    val currentItems: StateFlow<List<FileItem>> = _currentItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Preferences
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _showHidden = MutableStateFlow(false)
    val showHidden: StateFlow<Boolean> = _showHidden.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Multi-Selection
    private val _selectedPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedPaths: StateFlow<Set<String>> = _selectedPaths.asStateFlow()

    val isSelectionMode: Boolean
        get() = _selectedPaths.value.isNotEmpty()

    // Clipboard
    private val _clipboardFiles = MutableStateFlow<List<File>>(emptyList())
    val clipboardFiles: StateFlow<List<File>> = _clipboardFiles.asStateFlow()

    private val _clipboardMode = MutableStateFlow<ClipboardMode?>(null)
    val clipboardMode: StateFlow<ClipboardMode?> = _clipboardMode.asStateFlow()

    // Storage & Category Stats
    private val _storageInfo = MutableStateFlow(StorageUtils.getStorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _categoryStats = MutableStateFlow<Map<FileCategory, CategoryStat>>(emptyMap())
    val categoryStats: StateFlow<Map<FileCategory, CategoryStat>> = _categoryStats.asStateFlow()

    private val _largestFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val largestFiles: StateFlow<List<FileItem>> = _largestFiles.asStateFlow()

    // Category Screen Filter
    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    private val _categoryFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val categoryFiles: StateFlow<List<FileItem>> = _categoryFiles.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Database flows
    val recentFiles: StateFlow<List<RecentFileEntity>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashItems: StateFlow<List<TrashItemEntity>> = repository.trashItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Modals / Dialogs
    private val _renameTarget = MutableStateFlow<FileItem?>(null)
    val renameTarget: StateFlow<FileItem?> = _renameTarget.asStateFlow()

    private val _showCreateFolderDialog = MutableStateFlow(false)
    val showCreateFolderDialog: StateFlow<Boolean> = _showCreateFolderDialog.asStateFlow()

    private val _deleteTargets = MutableStateFlow<List<FileItem>?>(null)
    val deleteTargets: StateFlow<List<FileItem>?> = _deleteTargets.asStateFlow()

    private val _detailTarget = MutableStateFlow<FileItem?>(null)
    val detailTarget: StateFlow<FileItem?> = _detailTarget.asStateFlow()

    private val _imagePreviewTarget = MutableStateFlow<FileItem?>(null)
    val imagePreviewTarget: StateFlow<FileItem?> = _imagePreviewTarget.asStateFlow()

    private val _textViewerTarget = MutableStateFlow<Pair<FileItem, String>?>(null)
    val textViewerTarget: StateFlow<Pair<FileItem, String>?> = _textViewerTarget.asStateFlow()

    private val _zipViewerTarget = MutableStateFlow<Pair<FileItem, List<ZipEntryInfo>>?>(null)
    val zipViewerTarget: StateFlow<Pair<FileItem, List<ZipEntryInfo>>?> = _zipViewerTarget.asStateFlow()

    private val _showZipCreateDialog = MutableStateFlow(false)
    val showZipCreateDialog: StateFlow<Boolean> = _showZipCreateDialog.asStateFlow()

    private val _apkInfoTarget = MutableStateFlow<ApkInfo?>(null)
    val apkInfoTarget: StateFlow<ApkInfo?> = _apkInfoTarget.asStateFlow()

    private val _operationProgress = MutableStateFlow<OperationProgress?>(null)
    val operationProgress: StateFlow<OperationProgress?> = _operationProgress.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        loadDirectory(_currentDirectory.value)
        refreshStorageStats()
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageInfo.value = StorageUtils.getStorageInfo()
            _categoryStats.value = StorageUtils.scanCategoryStats(getApplication())
            _largestFiles.value = StorageUtils.getLargestFiles(15)
        }
    }

    fun loadDirectory(directory: File) {
        viewModelScope.launch {
            _isLoading.value = true
            _currentDirectory.value = directory
            val files = repository.listFiles(
                directory = directory,
                showHidden = _showHidden.value,
                sortType = _sortType.value,
                sortOrder = _sortOrder.value
            )
            _currentItems.value = files
            _isLoading.value = false
        }
    }

    fun navigateTo(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        directoryStack.add(_currentDirectory.value)
        clearSelection()
        loadDirectory(directory)
    }

    fun navigateBack(): Boolean {
        if (directoryStack.isNotEmpty()) {
            val previous = directoryStack.removeAt(directoryStack.size - 1)
            clearSelection()
            loadDirectory(previous)
            return true
        }
        return false
    }

    fun navigateToRoot() {
        directoryStack.clear()
        clearSelection()
        loadDirectory(storageRoot)
    }

    fun navigateToPath(targetDir: File) {
        if (targetDir == _currentDirectory.value) return
        directoryStack.add(_currentDirectory.value)
        clearSelection()
        loadDirectory(targetDir)
    }

    // Quick Category click
    fun openCategory(category: FileCategory) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _isLoading.value = true
            _categoryFiles.value = StorageUtils.getFilesForCategory(category)
            _isLoading.value = false
        }
    }

    fun closeCategory() {
        _selectedCategory.value = null
        _categoryFiles.value = emptyList()
    }

    // View Mode, Sort, Settings
    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setSort(type: SortType, order: SortOrder) {
        _sortType.value = type
        _sortOrder.value = order
        loadDirectory(_currentDirectory.value)
    }

    fun toggleShowHidden() {
        _showHidden.value = !_showHidden.value
        loadDirectory(_currentDirectory.value)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    // Selection
    fun toggleSelection(path: String) {
        val current = _selectedPaths.value.toMutableSet()
        if (current.contains(path)) current.remove(path) else current.add(path)
        _selectedPaths.value = current
    }

    fun selectAll() {
        val allPaths = _currentItems.value.map { it.path }.toSet()
        _selectedPaths.value = allPaths
    }

    fun clearSelection() {
        _selectedPaths.value = emptySet()
    }

    fun getSelectedFiles(): List<File> {
        val set = _selectedPaths.value
        return _currentItems.value.filter { set.contains(it.path) }.map { it.file }
    }

    // Clipboard
    fun copySelected() {
        val files = getSelectedFiles()
        if (files.isNotEmpty()) {
            _clipboardFiles.value = files
            _clipboardMode.value = ClipboardMode.COPY
            clearSelection()
            showMessage("Copied ${files.size} items to clipboard")
        }
    }

    fun cutSelected() {
        val files = getSelectedFiles()
        if (files.isNotEmpty()) {
            _clipboardFiles.value = files
            _clipboardMode.value = ClipboardMode.CUT
            clearSelection()
            showMessage("Cut ${files.size} items to clipboard")
        }
    }

    fun pasteClipboard() {
        val files = _clipboardFiles.value
        val mode = _clipboardMode.value ?: return
        if (files.isEmpty()) return

        val targetDir = _currentDirectory.value
        viewModelScope.launch {
            _operationProgress.value = OperationProgress(
                title = if (mode == ClipboardMode.COPY) "Copying files..." else "Moving files...",
                current = 0,
                total = files.size,
                currentFile = ""
            )

            val result = if (mode == ClipboardMode.COPY) {
                repository.copyFiles(files, targetDir) { cur, tot, name ->
                    _operationProgress.value = OperationProgress("Copying files...", cur, tot, name)
                }
            } else {
                repository.moveFiles(files, targetDir) { cur, tot, name ->
                    _operationProgress.value = OperationProgress("Moving files...", cur, tot, name)
                }
            }

            _operationProgress.value = null
            _clipboardFiles.value = emptyList()
            _clipboardMode.value = null

            result.onSuccess { count ->
                showMessage("Successfully pasted $count items")
                loadDirectory(targetDir)
                refreshStorageStats()
            }.onFailure { e ->
                showMessage(e.localizedMessage ?: "Failed to paste items")
            }
        }
    }

    fun clearClipboard() {
        _clipboardFiles.value = emptyList()
        _clipboardMode.value = null
    }

    // File Actions
    fun onFileClicked(context: Context, item: FileItem) {
        if (isSelectionMode) {
            toggleSelection(item.path)
            return
        }

        if (item.isDirectory) {
            navigateTo(item.file)
            return
        }

        // Record in recent files
        viewModelScope.launch {
            repository.recordRecentFile(item.file)
        }

        // Specific file handlers:
        when {
            item.isImage -> {
                _imagePreviewTarget.value = item
            }
            item.isApk -> {
                val apkInfo = FileUtils.getApkInfo(context, item.file)
                if (apkInfo != null) {
                    _apkInfoTarget.value = apkInfo
                } else {
                    FileUtils.openFile(context, item.file).onFailure {
                        showMessage(it.localizedMessage ?: "Cannot open file")
                    }
                }
            }
            item.isZip -> {
                viewModelScope.launch {
                    val entries = ZipUtils.listZipEntries(item.file)
                    _zipViewerTarget.value = Pair(item, entries)
                }
            }
            item.isText -> {
                viewModelScope.launch {
                    try {
                        val content = withContext(Dispatchers.IO) {
                            item.file.bufferedReader().use { reader ->
                                val buffer = CharArray(16384)
                                val read = reader.read(buffer, 0, buffer.size)
                                if (read > 0) String(buffer, 0, read) else "(Empty file)"
                            }
                        }
                        _textViewerTarget.value = Pair(item, content)
                    } catch (e: Exception) {
                        FileUtils.openFile(context, item.file).onFailure {
                            showMessage(it.localizedMessage ?: "Cannot open text file")
                        }
                    }
                }
            }
            else -> {
                FileUtils.openFile(context, item.file).onFailure { err ->
                    showMessage(err.localizedMessage ?: "No app available to open this file")
                }
            }
        }
    }

    fun openFileDirectly(context: Context, file: File) {
        viewModelScope.launch { repository.recordRecentFile(file) }
        FileUtils.openFile(context, file).onFailure {
            showMessage(it.localizedMessage ?: "Unable to open file")
        }
    }

    fun shareFile(context: Context, file: File) {
        FileUtils.shareFile(context, file).onFailure {
            showMessage(it.localizedMessage ?: "Failed to share")
        }
    }

    fun shareSelected(context: Context) {
        val files = getSelectedFiles()
        FileUtils.shareMultipleFiles(context, files).onFailure {
            showMessage(it.localizedMessage ?: "Failed to share")
        }
        clearSelection()
    }

    // Toggle favorite
    fun toggleFavorite(item: FileItem) {
        viewModelScope.launch {
            if (item.isFavorite) {
                repository.removeFavorite(item.path)
                showMessage("Removed from Favorites")
            } else {
                repository.addFavorite(item.file)
                showMessage("Added to Favorites")
            }
            loadDirectory(_currentDirectory.value)
        }
    }

    // Create Folder
    fun showCreateFolder() {
        _showCreateFolderDialog.value = true
    }

    fun dismissCreateFolder() {
        _showCreateFolderDialog.value = false
    }

    fun createFolder(name: String) {
        _showCreateFolderDialog.value = false
        viewModelScope.launch {
            val result = repository.createFolder(_currentDirectory.value, name)
            result.onSuccess {
                showMessage("Folder created: $name")
                loadDirectory(_currentDirectory.value)
            }.onFailure {
                showMessage(it.localizedMessage ?: "Could not create folder")
            }
        }
    }

    // Rename
    fun showRename(item: FileItem) {
        _renameTarget.value = item
    }

    fun dismissRename() {
        _renameTarget.value = null
    }

    fun renameFile(item: FileItem, newName: String) {
        _renameTarget.value = null
        viewModelScope.launch {
            val result = repository.renameFile(item.file, newName)
            result.onSuccess {
                showMessage("Renamed to $newName")
                loadDirectory(_currentDirectory.value)
                refreshStorageStats()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Rename failed")
            }
        }
    }

    // Delete
    fun promptDelete(items: List<FileItem>) {
        if (items.isNotEmpty()) {
            _deleteTargets.value = items
        }
    }

    fun dismissDelete() {
        _deleteTargets.value = null
    }

    fun deleteTargets(moveToTrash: Boolean) {
        val targets = _deleteTargets.value ?: return
        _deleteTargets.value = null
        clearSelection()

        viewModelScope.launch {
            if (moveToTrash) {
                var trashCount = 0
                for (target in targets) {
                    val res = repository.moveToTrash(getApplication(), target.file)
                    if (res.isSuccess) trashCount++
                }
                showMessage("Moved $trashCount items to Recycle Bin")
            } else {
                val res = repository.deletePermanently(targets.map { it.file })
                res.onSuccess {
                    showMessage("Permanently deleted $it items")
                }.onFailure {
                    showMessage(it.localizedMessage ?: "Failed to delete")
                }
            }
            loadDirectory(_currentDirectory.value)
            refreshStorageStats()
        }
    }

    // Details & Modals
    fun showDetails(item: FileItem) {
        _detailTarget.value = item
    }

    fun dismissDetails() {
        _detailTarget.value = null
    }

    fun dismissImagePreview() {
        _imagePreviewTarget.value = null
    }

    fun dismissTextViewer() {
        _textViewerTarget.value = null
    }

    fun dismissZipViewer() {
        _zipViewerTarget.value = null
    }

    fun dismissApkInfo() {
        _apkInfoTarget.value = null
    }

    // ZIP creation
    fun showZipCreateDialog() {
        _showZipCreateDialog.value = true
    }

    fun dismissZipCreateDialog() {
        _showZipCreateDialog.value = false
    }

    fun createZipArchive(archiveName: String) {
        _showZipCreateDialog.value = false
        val sources = getSelectedFiles()
        if (sources.isEmpty()) return

        val validName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val destZip = FileUtils.getUniqueDestination(_currentDirectory.value, validName)

        viewModelScope.launch {
            _operationProgress.value = OperationProgress("Creating ZIP Archive...", 0, sources.size, "")
            val result = ZipUtils.createZip(sources, destZip) { cur, tot, name ->
                _operationProgress.value = OperationProgress("Creating ZIP Archive...", cur, tot, name)
            }
            _operationProgress.value = null
            clearSelection()

            result.onSuccess {
                showMessage("Archive created: ${it.name}")
                loadDirectory(_currentDirectory.value)
                refreshStorageStats()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to create archive")
            }
        }
    }

    // ZIP extraction
    fun extractZipArchive(zipFile: File, targetDirName: String?) {
        _zipViewerTarget.value = null
        val targetDir = if (!targetDirName.isNullOrBlank()) {
            File(_currentDirectory.value, targetDirName)
        } else {
            File(_currentDirectory.value, zipFile.nameWithoutExtension)
        }

        viewModelScope.launch {
            _operationProgress.value = OperationProgress("Extracting Archive...", 0, 0, zipFile.name)
            val result = ZipUtils.extractZip(zipFile, targetDir) { cur, tot, name ->
                _operationProgress.value = OperationProgress("Extracting Archive...", cur, tot, name)
            }
            _operationProgress.value = null

            result.onSuccess { count ->
                showMessage("Extracted $count files to ${targetDir.name}")
                loadDirectory(_currentDirectory.value)
                refreshStorageStats()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to extract archive")
            }
        }
    }

    // Trash operations
    fun restoreTrash(item: TrashItemEntity) {
        viewModelScope.launch {
            val res = repository.restoreTrashItem(item)
            res.onSuccess {
                showMessage("Restored ${it.name}")
                loadDirectory(_currentDirectory.value)
                refreshStorageStats()
            }.onFailure {
                showMessage(it.localizedMessage ?: "Failed to restore")
            }
        }
    }

    fun deleteTrashPermanently(item: TrashItemEntity) {
        viewModelScope.launch {
            repository.deleteTrashPermanently(item)
            showMessage("Permanently deleted ${item.name}")
            refreshStorageStats()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            showMessage("Recycle Bin emptied")
            refreshStorageStats()
        }
    }

    // Recent files actions
    fun removeRecent(path: String) {
        viewModelScope.launch {
            repository.removeRecentFile(path)
        }
    }

    fun clearAllRecent() {
        viewModelScope.launch {
            repository.clearRecentFiles()
            showMessage("Recent files cleared")
        }
    }

    // Search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            val results = repository.searchFiles(
                rootDir = storageRoot,
                query = query
            )
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
