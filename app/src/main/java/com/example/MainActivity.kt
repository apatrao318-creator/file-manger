package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ThemeMode
import com.example.navigation.Screen
import com.example.presentation.components.ApkDetailsDialog
import com.example.presentation.components.DeleteConfirmationDialog
import com.example.presentation.components.FileDetailsDialog
import com.example.presentation.components.FileOperationProgressDialog
import com.example.presentation.components.ImagePreviewDialog
import com.example.presentation.components.NewFolderDialog
import com.example.presentation.components.RenameDialog
import com.example.presentation.components.StoragePermissionRationaleDialog
import com.example.presentation.components.TextViewerDialog
import com.example.presentation.components.ZipCreateDialog
import com.example.presentation.components.ZipViewerDialog
import com.example.presentation.screens.FavoritesScreen
import com.example.presentation.screens.FileBrowserScreen
import com.example.presentation.screens.HomeScreen
import com.example.presentation.screens.RecentScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.StorageAnalyzerScreen
import com.example.presentation.screens.TrashScreen
import com.example.presentation.viewmodel.FileManagerViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.FileUtils
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FileManagerViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                FileManagerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FileManagerApp(viewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Storage permission state
    fun checkHasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkHasPermission()) }
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Check permission on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkHasPermission()
                if (hasPermission) {
                    viewModel.refreshStorageStats()
                    viewModel.loadDirectory(viewModel.currentDirectory.value)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher for pre-Android 11
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = checkHasPermission()
        if (hasPermission) {
            viewModel.refreshStorageStats()
            viewModel.loadDirectory(viewModel.currentDirectory.value)
        }
    }

    fun proceedWithPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    fun requestStoragePermission() {
        showRationaleDialog = true
    }

    // User Message Snackbar
    val userMessage by viewModel.userMessage.collectAsState()
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Files,
        Screen.Analyzer,
        Screen.Favorites,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Storage permission banner if not yet granted
                AnimatedVisibility(
                    visible = !hasPermission,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Storage Access Required",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Grant file access to browse and manage device files",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { requestStoragePermission() },
                                modifier = Modifier.testTag("grant_permission_btn")
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }

                // NavHost
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            hasPermission = hasPermission,
                            onRequestPermission = { requestStoragePermission() },
                            onNavigateToBrowser = { targetFolder ->
                                if (targetFolder != null) {
                                    viewModel.navigateToPath(targetFolder)
                                }
                                navController.navigate(Screen.Files.route)
                            },
                            onNavigateToAnalyzer = { navController.navigate(Screen.Analyzer.route) },
                            onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                            onNavigateToRecent = { navController.navigate(Screen.Recent.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.Files.route) {
                        FileBrowserScreen(
                            viewModel = viewModel,
                            hasPermission = hasPermission,
                            onRequestPermission = { requestStoragePermission() },
                            onOpenSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    composable(Screen.Analyzer.route) {
                        StorageAnalyzerScreen(viewModel = viewModel)
                    }

                    composable(Screen.Favorites.route) {
                        FavoritesScreen(viewModel = viewModel)
                    }

                    composable(Screen.Recent.route) {
                        RecentScreen(viewModel = viewModel)
                    }

                    composable(Screen.Trash.route) {
                        TrashScreen(viewModel = viewModel)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Modal Dialogs triggered from ViewModel
    val showCreateFolder by viewModel.showCreateFolderDialog.collectAsState()
    if (showCreateFolder) {
        NewFolderDialog(
            onDismiss = { viewModel.dismissCreateFolder() },
            onConfirm = { folderName -> viewModel.createFolder(folderName) }
        )
    }

    val renameTarget by viewModel.renameTarget.collectAsState()
    renameTarget?.let { target ->
        RenameDialog(
            initialName = target.name,
            isDirectory = target.isDirectory,
            onDismiss = { viewModel.dismissRename() },
            onConfirm = { newName -> viewModel.renameFile(target, newName) }
        )
    }

    val deleteTargets by viewModel.deleteTargets.collectAsState()
    deleteTargets?.let { targets ->
        DeleteConfirmationDialog(
            itemCount = targets.size,
            firstItemName = targets.firstOrNull()?.name ?: "",
            onDismiss = { viewModel.dismissDelete() },
            onConfirm = { moveToTrash -> viewModel.deleteTargets(moveToTrash) }
        )
    }

    val detailTarget by viewModel.detailTarget.collectAsState()
    detailTarget?.let { target ->
        FileDetailsDialog(
            item = target,
            onDismiss = { viewModel.dismissDetails() },
            onOpen = {
                viewModel.dismissDetails()
                viewModel.onFileClicked(context, target)
            },
            onShare = {
                viewModel.dismissDetails()
                viewModel.shareFile(context, target.file)
            },
            onRename = {
                viewModel.dismissDetails()
                viewModel.showRename(target)
            },
            onDelete = {
                viewModel.dismissDetails()
                viewModel.promptDelete(listOf(target))
            }
        )
    }

    val imagePreviewTarget by viewModel.imagePreviewTarget.collectAsState()
    imagePreviewTarget?.let { target ->
        ImagePreviewDialog(
            item = target,
            onDismiss = { viewModel.dismissImagePreview() },
            onShare = { viewModel.shareFile(context, target.file) },
            onDelete = {
                viewModel.dismissImagePreview()
                viewModel.promptDelete(listOf(target))
            },
            onOpenExternal = {
                viewModel.openFileDirectly(context, target.file)
            }
        )
    }

    val textViewerTarget by viewModel.textViewerTarget.collectAsState()
    textViewerTarget?.let { (target, content) ->
        TextViewerDialog(
            item = target,
            content = content,
            onDismiss = { viewModel.dismissTextViewer() },
            onShare = { viewModel.shareFile(context, target.file) },
            onOpenExternal = {
                viewModel.openFileDirectly(context, target.file)
            }
        )
    }

    val zipViewerTarget by viewModel.zipViewerTarget.collectAsState()
    zipViewerTarget?.let { (target, entries) ->
        ZipViewerDialog(
            item = target,
            entries = entries,
            onDismiss = { viewModel.dismissZipViewer() },
            onExtract = {
                viewModel.extractZipArchive(target.file, null)
            }
        )
    }

    val showZipCreateDialog by viewModel.showZipCreateDialog.collectAsState()
    if (showZipCreateDialog) {
        val count = viewModel.selectedPaths.collectAsState().value.size
        ZipCreateDialog(
            itemCount = count,
            onDismiss = { viewModel.dismissZipCreateDialog() },
            onConfirm = { archiveName -> viewModel.createZipArchive(archiveName) }
        )
    }

    val apkInfoTarget by viewModel.apkInfoTarget.collectAsState()
    apkInfoTarget?.let { apkInfo ->
        ApkDetailsDialog(
            apkInfo = apkInfo,
            onDismiss = { viewModel.dismissApkInfo() },
            onInstall = {
                viewModel.dismissApkInfo()
                FileUtils.installApk(context, apkInfo.file).onFailure {
                    viewModel.showMessage(it.localizedMessage ?: "Failed to launch installer")
                }
            },
            onShare = {
                viewModel.dismissApkInfo()
                viewModel.shareFile(context, apkInfo.file)
            },
            onDelete = {
                viewModel.dismissApkInfo()
                viewModel.promptDelete(listOf(com.example.data.model.FileItem(apkInfo.file)))
            }
        )
    }

    val operationProgress by viewModel.operationProgress.collectAsState()
    operationProgress?.let { progress ->
        FileOperationProgressDialog(progress = progress)
    }

    if (showRationaleDialog) {
        StoragePermissionRationaleDialog(
            onConfirm = {
                showRationaleDialog = false
                proceedWithPermissionRequest()
            },
            onDismiss = {
                showRationaleDialog = false
            },
            onOpenPrivacyPolicy = {
                showRationaleDialog = false
                navController.navigate(Screen.Settings.route)
            }
        )
    }
}
