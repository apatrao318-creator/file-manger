package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FileItem
import com.example.data.model.SortOrder
import com.example.data.model.SortType
import com.example.data.model.ViewMode
import com.example.presentation.components.FileItemGrid
import com.example.presentation.components.FileItemRow
import com.example.presentation.components.StoragePermissionExplanationScreen
import com.example.presentation.viewmodel.ClipboardMode
import com.example.presentation.viewmodel.FileManagerViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileManagerViewModel,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val currentItems by viewModel.currentItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showHidden by viewModel.showHidden.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val isSelectionMode = selectedPaths.isNotEmpty()

    val clipboardFiles by viewModel.clipboardFiles.collectAsState()
    val clipboardMode by viewModel.clipboardMode.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categoryFiles by viewModel.categoryFiles.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    var showSearchField by remember { mutableStateOf(true) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    // Custom back handler for directory navigation
    BackHandler(enabled = true) {
        if (isFabMenuExpanded) {
            isFabMenuExpanded = false
        } else if (searchQuery.isNotEmpty()) {
            viewModel.clearSearch()
        } else if (selectedCategory != null) {
            viewModel.closeCategory()
        } else if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            viewModel.navigateBack()
        }
    }

    // Which items to display:
    val displayItems: List<FileItem> = when {
        searchQuery.isNotBlank() -> searchResults
        selectedCategory != null -> categoryFiles
        else -> currentItems
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (isSelectionMode) {
                // Multi-select Action Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedPaths.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            modifier = Modifier.testTag("select_all_btn")
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(
                            onClick = { viewModel.copySelected() },
                            modifier = Modifier.testTag("copy_selected_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(
                            onClick = { viewModel.cutSelected() },
                            modifier = Modifier.testTag("cut_selected_btn")
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Move")
                        }
                        IconButton(
                            onClick = { viewModel.shareSelected(context) },
                            modifier = Modifier.testTag("share_selected_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(
                            onClick = { viewModel.showZipCreateDialog() },
                            modifier = Modifier.testTag("archive_selected_btn")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive ZIP")
                        }
                        IconButton(
                            onClick = { viewModel.promptDelete(viewModel.getSelectedFiles().map { FileItem(it) }) },
                            modifier = Modifier.testTag("delete_selected_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else {
                // Normal Browser Top Bar with Persistent Search Field
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                val placeholderText = when {
                                    selectedCategory != null -> "Filter ${selectedCategory!!.title}..."
                                    currentDirectory == viewModel.storageRoot -> "Filter internal storage..."
                                    else -> "Filter ${currentDirectory.name}..."
                                }
                                Text(
                                    text = placeholderText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearSearch() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("browser_search_input")
                        )
                    },
                    navigationIcon = {
                        if (selectedCategory != null) {
                            IconButton(onClick = { viewModel.closeCategory() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else if (currentDirectory != viewModel.storageRoot) {
                            IconButton(onClick = { viewModel.navigateBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up directory")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("action_view_mode")
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Toggle View Mode"
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("action_sort")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Name") },
                                    trailingIcon = { if (sortType == SortType.NAME) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        viewModel.setSort(SortType.NAME, sortOrder)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Size") },
                                    trailingIcon = { if (sortType == SortType.SIZE) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        viewModel.setSort(SortType.SIZE, sortOrder)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Date") },
                                    trailingIcon = { if (sortType == SortType.DATE) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        viewModel.setSort(SortType.DATE, sortOrder)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Type") },
                                    trailingIcon = { if (sortType == SortType.TYPE) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        viewModel.setSort(SortType.TYPE, sortOrder)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (sortOrder == SortOrder.ASCENDING) "Direction: Ascending" else "Direction: Descending")
                                    },
                                    trailingIcon = {
                                        Icon(
                                            if (sortOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            null
                                        )
                                    },
                                    onClick = {
                                        val newOrder = if (sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
                                        viewModel.setSort(sortType, newOrder)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New Folder") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.showCreateFolder()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showHidden) "Hide Hidden Files" else "Show Hidden Files") },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleShowHidden()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select All") },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.selectAll()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Paste button if items in clipboard
                if (clipboardFiles.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.pasteClipboard() },
                        icon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                        text = {
                            Text(
                                if (clipboardMode == ClipboardMode.COPY) "Paste (${clipboardFiles.size})" else "Move Here (${clipboardFiles.size})"
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp).testTag("paste_fab")
                    )
                }

                // Speed Dial / FAB Menu Sub-Items (Animated)
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Create New Folder Option
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                viewModel.showCreateFolder()
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder") },
                            text = { Text("New Folder") },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("fab_menu_new_folder")
                        )

                        // 2. Select Files for Multi-Delete Option
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                if (displayItems.isNotEmpty()) {
                                    viewModel.toggleSelection(displayItems.first().path)
                                }
                            },
                            icon = { Icon(Icons.Default.CheckBox, contentDescription = "Multi-Select") },
                            text = { Text("Select for Multi-Delete") },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("fab_menu_multi_delete")
                        )

                        // 3. Initiate ZIP Archive Option
                        ExtendedFloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                if (selectedPaths.isEmpty() && displayItems.isNotEmpty()) {
                                    viewModel.toggleSelection(displayItems.first().path)
                                }
                                viewModel.showZipCreateDialog()
                            },
                            icon = { Icon(Icons.Default.FolderZip, contentDescription = "ZIP Archive") },
                            text = { Text("ZIP Archive") },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("fab_menu_create_zip")
                        )
                    }
                }

                // Main Floating Action Button with smooth rotation animation
                if (!isSelectionMode && selectedCategory == null) {
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isFabMenuExpanded) 135f else 0f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "fab_rotation"
                    )

                    FloatingActionButton(
                        onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                        containerColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("main_action_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isFabMenuExpanded) "Close actions menu" else "Quick file actions",
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumbs Bar (only when browsing normal directories)
            if (selectedCategory == null && searchQuery.isBlank()) {
                val pathSegments = remember(currentDirectory, viewModel.storageRoot) {
                    val root = viewModel.storageRoot
                    val segments = mutableListOf<File>()
                    var curr: File? = currentDirectory
                    while (curr != null) {
                        segments.add(0, curr)
                        if (curr == root || curr.parentFile == null) break
                        curr = curr.parentFile
                    }
                    segments
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pathSegments.forEachIndexed { index, file ->
                            val isLast = index == pathSegments.size - 1
                            val label = if (file == viewModel.storageRoot) "Home" else file.name

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isLast) MaterialTheme.colorScheme.secondaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.navigateToPath(file) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLast) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Clipboard indicator strip if pending paste
            if (clipboardFiles.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${clipboardFiles.size} items ready to ${if (clipboardMode == ClipboardMode.COPY) "copy" else "move"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(
                            onClick = { viewModel.clearClipboard() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel clipboard",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Status bar showing item count and sort order
            if (hasPermission && !isLoading && !isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayItems.size} ${if (displayItems.size == 1) "item" else "items"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Sorted by ${sortType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (sortOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Body: Content Loading / Empty / List / Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!hasPermission) {
                    StoragePermissionExplanationScreen(
                        onRequestPermission = onRequestPermission,
                        onOpenPrivacyPolicy = onOpenSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoading || isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (displayItems.isEmpty()) {
                    // Empty folder state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "This folder is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(displayItems, key = { it.path }) { item ->
                                val isSelected = selectedPaths.contains(item.path)
                                FileItemRow(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = { viewModel.onFileClicked(context, item) },
                                    onLongClick = { viewModel.toggleSelection(item.path) },
                                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                                    onShowDetails = { viewModel.showDetails(item) },
                                    onRename = { viewModel.showRename(item) },
                                    onDelete = { viewModel.promptDelete(listOf(item)) },
                                    onShare = { viewModel.shareFile(context, item.file) }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayItems, key = { it.path }) { item ->
                                val isSelected = selectedPaths.contains(item.path)
                                FileItemGrid(
                                    item = item,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onClick = { viewModel.onFileClicked(context, item) },
                                    onLongClick = { viewModel.toggleSelection(item.path) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
