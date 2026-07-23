package com.pythonide.app.screens.packages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mediation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Package
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.domain.model.packageManager.BackupInfo
import com.pythonide.domain.model.packageManager.InstallLogEntry
import com.pythonide.domain.model.packageManager.InstallProgress
import com.pythonide.domain.model.packageManager.InstallStatus
import com.pythonide.domain.model.packageManager.InstallTask
import com.pythonide.domain.model.packageManager.OfflinePackage
import com.pythonide.domain.model.packageManager.PackageCategory
import com.pythonide.domain.model.packageManager.PackageInfo
import com.pythonide.domain.model.packageManager.PackageStatsData
import com.pythonide.domain.model.packageManager.PackageUpdateInfo
import com.pythonide.domain.model.packageManager.SortOrder
import com.pythonide.domain.model.packageManager.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PackageManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: PackageManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val installedPackages by viewModel.installedPackages.collectAsState()
    val installQueue by viewModel.installQueue.collectAsState()
    val installLogs by viewModel.installLogs.collectAsState()
    val offlinePackages by viewModel.offlinePackages.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val packageStats by viewModel.packageStats.collectAsState()
    val error by viewModel.error.collectAsState()
    val isGlobalInstalling by viewModel.isGlobalInstalling.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showBatchInstallDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedPackagesForBatch by remember { mutableStateOf(setOf<String>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Package Manager",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { showBackupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = "Backup"
                            )
                        }
                        IconButton(onClick = { showRestoreDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore"
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        badge = null,
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    BottomNavButton(
                        icon = Icons.Default.CheckCircle,
                        label = "Installed",
                        badge = installedPackages.size.toString(),
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    BottomNavButton(
                        icon = Icons.Default.InstallMobile,
                        label = "Queue",
                        badge = if (installQueue.isNotEmpty()) installQueue.size.toString() else null,
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    BottomNavButton(
                        icon = Icons.Default.Description,
                        label = "Logs",
                        badge = null,
                        isSelected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && selectedPackagesForBatch.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBatchInstallDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.GetApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Install ${selectedPackagesForBatch.size} packages"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Search") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Installed") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Queue") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Logs") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Offline") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                PackageStats(stats = packageStats)

                when (selectedTab) {
                    0 -> PackageSearchTab(
                        searchResults = searchResults,
                        installedPackages = installedPackages,
                        selectedPackages = selectedPackagesForBatch,
                        onSearch = { viewModel.searchPackages(it) },
                        onPackageClick = { viewModel.showPackageDetails(it) },
                        onInstallClick = { viewModel.showInstallDialog(it) },
                        onUninstallClick = { viewModel.uninstallPackage(it) },
                        onToggleSelection = { packageName ->
                            selectedPackagesForBatch = if (packageName in selectedPackagesForBatch) {
                                selectedPackagesForBatch - packageName
                            } else {
                                selectedPackagesForBatch + packageName
                            }
                        },
                        onSelectAll = { packages ->
                            selectedPackagesForBatch = packages.map { it.name }.toSet()
                        },
                        onClearSelection = { selectedPackagesForBatch = emptySet() },
                        isLoading = uiState.isLoading
                    )
                    1 -> InstalledPackagesTab(
                        installedPackages = installedPackages,
                        onPackageClick = { viewModel.showPackageDetails(it) },
                        onUninstallClick = { viewModel.uninstallPackage(it) },
                        onUpdateClick = { viewModel.updatePackage(it) },
                        isLoading = uiState.isLoading
                    )
                    2 -> InstallQueueTab(
                        installQueue = installQueue,
                        onCancelTask = { viewModel.cancelInstallTask(it) },
                        onClearCompleted = { viewModel.clearCompletedTasks() },
                        onPauseTask = { viewModel.pauseInstallTask(it) },
                        onResumeTask = { viewModel.resumeInstallTask(it) }
                    )
                    3 -> InstallLogsTab(
                        installLogs = installLogs,
                        onClearLogs = { viewModel.clearLogs() }
                    )
                    4 -> OfflinePackagesTab(
                        offlinePackages = offlinePackages,
                        onInstallOffline = { viewModel.showOfflineInstallDialog(it) },
                        onScanLocal = { viewModel.scanLocalPackages() },
                        onImportFile = { viewModel.importPackageFile() },
                        onDeletePackage = { viewModel.deleteOfflinePackage(it) }
                    )
                }
            }

            ProgressOverlay(visible = isGlobalInstalling)

            error?.let { errorMsg ->
                ErrorDialog(
                    message = errorMsg,
                    onDismiss = { viewModel.clearError() },
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refreshAll()
                    }
                )
            }
        }
    }

    uiState.selectedPackage?.let { packageInfo ->
        PackageDetailDialog(
            packageInfo = packageInfo,
            isInstalled = installedPackages.any { it.name == packageInfo.name },
            onDismiss = { viewModel.hidePackageDetails() },
            onInstall = {
                viewModel.showInstallDialog(packageInfo)
                viewModel.hidePackageDetails()
            },
            onUninstall = {
                viewModel.uninstallPackage(packageInfo.name)
                viewModel.hidePackageDetails()
            }
        )
    }

    uiState.installDialogPackage?.let { packageInfo ->
        InstallDialog(
            packageInfo = packageInfo,
            onDismiss = { viewModel.hideInstallDialog() },
            onInstall = { version, options ->
                viewModel.installPackage(packageInfo.name, version, options)
                viewModel.hideInstallDialog()
            }
        )
    }

    if (showBatchInstallDialog) {
        BatchInstallDialog(
            packages = selectedPackagesForBatch.toList(),
            onDismiss = { showBatchInstallDialog = false },
            onConfirm = { versions, options ->
                viewModel.batchInstall(selectedPackagesForBatch.toList(), versions, options)
                selectedPackagesForBatch = emptySet()
                showBatchInstallDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onConfirm = { name, includeInstalled, includeOffline ->
                viewModel.createBackup(name, includeInstalled, includeOffline)
                showBackupDialog = false
            }
        )
    }

    if (showRestoreDialog) {
        RestoreDialog(
            backups = backups,
            onDismiss = { showRestoreDialog = false },
            onRestore = { backup ->
                viewModel.restoreBackup(backup)
                showRestoreDialog = false
            }
        )
    }

    uiState.offlineInstallPackage?.let { offlinePackage ->
        OfflineInstallDialog(
            offlinePackage = offlinePackage,
            onDismiss = { viewModel.hideOfflineInstallDialog() },
            onInstall = { options ->
                viewModel.installOfflinePackage(offlinePackage, options)
                viewModel.hideOfflineInstallDialog()
            }
        )
    }

    uiState.compatibilityInfo?.let { info ->
        CompatibilityDialog(
            packageName = uiState.compatibilityPackageName ?: "",
            info = info,
            onDismiss = { viewModel.hideCompatibilityInfo() }
        )
    }
}

@Composable
fun PackageSearchTab(
    searchResults: List<PackageInfo>,
    installedPackages: List<PackageInfo>,
    selectedPackages: Set<String>,
    onSearch: (String) -> Unit,
    onPackageClick: (PackageInfo) -> Unit,
    onInstallClick: (PackageInfo) -> Unit,
    onUninstallClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: (List<PackageInfo>) -> Unit,
    onClearSelection: () -> Unit,
    isLoading: Boolean
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(SortOrder.NAME) }
    var filterCategory by rememberSaveable { mutableStateOf<PackageCategory?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        PackageSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { onSearch(searchQuery) },
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            filterCategory = filterCategory,
            onFilterChange = { filterCategory = it },
            showSortMenu = showSortMenu,
            onShowSortMenuChange = { showSortMenu = it },
            resultCount = searchResults.size
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${searchResults.size} packages found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                if (selectedPackages.isNotEmpty()) {
                    TextButton(onClick = onClearSelection) {
                        Text("Clear selection (${selectedPackages.size})")
                    }
                } else {
                    TextButton(onClick = { onSelectAll(searchResults) }) {
                        Text("Select all")
                    }
                }
            }
        }

        PackageSearchResults(
            searchResults = searchResults
                .filter { pkg ->
                    if (filterCategory != null) pkg.category == filterCategory else true
                }
                .sortedWith(
                    when (sortBy) {
                        SortOrder.NAME -> compareBy { it.name }
                        SortOrder.DOWNLOADS -> compareByDescending { it.downloads }
                        SortOrder.UPDATED -> compareByDescending { it.lastUpdated }
                        SortOrder.RATING -> compareByDescending { it.rating }
                        SortOrder.SIZE -> compareBy { it.size }
                    }
                ),
            installedPackages = installedPackages,
            selectedPackages = selectedPackages,
            onPackageClick = onPackageClick,
            onInstallClick = onInstallClick,
            onUninstallClick = onUninstallClick,
            onToggleSelection = onToggleSelection,
            isLoading = isLoading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    sortBy: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    filterCategory: PackageCategory?,
    onFilterChange: (PackageCategory?) -> Unit,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    resultCount: Int
) {
    var expandedCategories by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search packages...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                Row {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                    IconButton(onClick = { onShowSortMenuChange(!showSortMenu) }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    Box {
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { onShowSortMenuChange(false) }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name") },
                                onClick = {
                                    onSortChange(SortOrder.NAME)
                                    onShowSortMenuChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (sortBy == SortOrder.NAME) Icons.Default.CheckCircle else Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = if (sortBy == SortOrder.NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Downloads") },
                                onClick = {
                                    onSortChange(SortOrder.DOWNLOADS)
                                    onShowSortMenuChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (sortBy == SortOrder.DOWNLOADS) Icons.Default.CheckCircle else Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = if (sortBy == SortOrder.DOWNLOADS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Updated") },
                                onClick = {
                                    onSortChange(SortOrder.UPDATED)
                                    onShowSortMenuChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (sortBy == SortOrder.UPDATED) Icons.Default.CheckCircle else Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = if (sortBy == SortOrder.UPDATED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Rating") },
                                onClick = {
                                    onSortChange(SortOrder.RATING)
                                    onShowSortMenuChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (sortBy == SortOrder.RATING) Icons.Default.CheckCircle else Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = if (sortBy == SortOrder.RATING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size") },
                                onClick = {
                                    onSortChange(SortOrder.SIZE)
                                    onShowSortMenuChange(false)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (sortBy == SortOrder.SIZE) Icons.Default.CheckCircle else Icons.Default.Sort,
                                        contentDescription = null,
                                        tint = if (sortBy == SortOrder.SIZE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            )
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { onSearch() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Category:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("All") }
                    )
                }
                items(PackageCategory.entries.toList()) { category ->
                    FilterChip(
                        selected = filterCategory == category,
                        onClick = { onFilterChange(category) },
                        label = {
                            Text(
                                text = when (category) {
                                    PackageCategory.DATA_SCIENCE -> "Data Science"
                                    PackageCategory.MACHINE_LEARNING -> "ML"
                                    PackageCategory.WEB -> "Web"
                                    PackageCategory.UTILITIES -> "Utils"
                                    PackageCategory.DEVELOPMENT -> "Dev"
                                    PackageCategory.NETWORKING -> "Network"
                                    PackageCategory.DATABASE -> "DB"
                                    PackageCategory.TESTING -> "Test"
                                    PackageCategory.OTHER -> "Other"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageSearchResults(
    searchResults: List<PackageInfo>,
    installedPackages: List<PackageInfo>,
    selectedPackages: Set<String>,
    onPackageClick: (PackageInfo) -> Unit,
    onInstallClick: (PackageInfo) -> Unit,
    onUninstallClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (searchResults.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Search,
            title = "No packages found",
            subtitle = "Try a different search term or adjust filters"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(searchResults) { packageInfo ->
                val isInstalled = installedPackages.any { it.name == packageInfo.name }
                val isSelected = packageInfo.name in selectedPackages

                PackageCard(
                    packageInfo = packageInfo,
                    isInstalled = isInstalled,
                    isSelected = isSelected,
                    onClick = { onPackageClick(packageInfo) },
                    onInstallClick = { onInstallClick(packageInfo) },
                    onUninstallClick = { onUninstallClick(packageInfo.name) },
                    onToggleSelection = { onToggleSelection(packageInfo.name) },
                    showCheckbox = selectedPackages.isNotEmpty() || true
                )
            }
        }
    }
}

@Composable
fun InstalledPackagesTab(
    installedPackages: List<PackageInfo>,
    onPackageClick: (PackageInfo) -> Unit,
    onUninstallClick: (String) -> Unit,
    onUpdateClick: (PackageInfo) -> Unit,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (installedPackages.isEmpty()) {
        EmptyState(
            icon = Icons.Default.InstallMobile,
            title = "No packages installed",
            subtitle = "Browse and install packages from the Search tab"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(installedPackages) { packageInfo ->
                InstalledPackageCard(
                    packageInfo = packageInfo,
                    onClick = { onPackageClick(packageInfo) },
                    onUninstallClick = { onUninstallClick(packageInfo.name) },
                    onUpdateClick = { onUpdateClick(packageInfo) }
                )
            }
        }
    }
}

@Composable
fun InstallQueueTab(
    installQueue: List<InstallTask>,
    onCancelTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit
) {
    val completedTasks = installQueue.filter { it.status == InstallStatus.COMPLETED || it.status == InstallStatus.FAILED }
    val activeTasks = installQueue.filter { it.status != InstallStatus.COMPLETED && it.status != InstallStatus.FAILED }

    if (installQueue.isEmpty()) {
        EmptyState(
            icon = Icons.Default.InstallMobile,
            title = "No packages in queue",
            subtitle = "Add packages to install from the Search tab"
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (activeTasks.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active (${activeTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(activeTasks) { task ->
                    InstallQueueItem(
                        task = task,
                        onCancel = { onCancelTask(task.id) },
                        onPause = { onPauseTask(task.id) },
                        onResume = { onResumeTask(task.id) }
                    )
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Completed (${completedTasks.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onClearCompleted) {
                                Text("Clear all")
                            }
                        }
                    }
                }

                items(completedTasks) { task ->
                    InstallQueueItem(
                        task = task,
                        onCancel = { onCancelTask(task.id) },
                        onPause = { },
                        onResume = { }
                    )
                }
            }
        }
    }
}

@Composable
fun InstallLogsTab(
    installLogs: List<InstallLogEntry>,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(installLogs.size) {
        if (installLogs.isNotEmpty()) {
            listState.animateScrollToItem(installLogs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Install Logs (${installLogs.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (installLogs.isNotEmpty()) {
                TextButton(onClick = onClearLogs) {
                    Text("Clear logs")
                }
            }
        }

        InstallLogsView(
            logs = installLogs,
            listState = listState
        )
    }
}

@Composable
fun InstallLogsView(
    logs: List<InstallLogEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState()
) {
    if (logs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Description,
            title = "No logs yet",
            subtitle = "Install logs will appear here"
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { log ->
                LogEntryItem(log = log)
            }
        }
    }
}

@Composable
fun LogEntryItem(log: InstallLogEntry) {
    val timestamp = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }
    val color = when {
        log.isError -> MaterialTheme.colorScheme.error
        log.isWarning -> Color(0xFFFF9800)
        log.isSuccess -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = "[$timestamp] ${log.message}",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 4.dp)
    )
}

@Composable
fun OfflinePackagesTab(
    offlinePackages: List<OfflinePackage>,
    onInstallOffline: (OfflinePackage) -> Unit,
    onScanLocal: () -> Unit,
    onImportFile: () -> Unit,
    onDeletePackage: (OfflinePackage) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onScanLocal,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Local")
            }
            OutlinedButton(
                onClick = onImportFile,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.GetApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import File")
            }
        }

        if (offlinePackages.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = "No offline packages",
                subtitle = "Scan local directory or import .whl/.tar.gz files"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(offlinePackages) { pkg ->
                    OfflinePackageCard(
                        offlinePackage = pkg,
                        onInstall = { onInstallOffline(pkg) },
                        onDelete = { onDeletePackage(pkg) }
                    )
                }
            }
        }
    }
}

@Composable
fun BackupRestoreView(
    backups: List<BackupInfo>,
    onCreateBackup: () -> Unit,
    onRestore: (BackupInfo) -> Unit,
    onDeleteBackup: (BackupInfo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCreateBackup,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Backup")
            }
        }

        if (backups.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Backup,
                title = "No backups",
                subtitle = "Create a backup to save your packages"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(backups) { backup ->
                    BackupCard(
                        backup = backup,
                        onRestore = { onRestore(backup) },
                        onDelete = { onDeleteBackup(backup) }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    packageInfo: PackageInfo,
    isInstalled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInstallClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onToggleSelection: () -> Unit,
    showCheckbox: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = packageInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isInstalled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "Installed",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = packageInfo.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "v${packageInfo.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${packageInfo.downloads.toLocaleString()} downloads",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (packageInfo.size > 0) {
                        Text(
                            text = formatSize(packageInfo.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (packageInfo.rating > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (index < packageInfo.rating.toInt())
                                    Color(0xFFFFC107)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(packageInfo.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (isInstalled) {
                    IconButton(onClick = onUninstallClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Uninstall",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.size(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.GetApp,
                            contentDescription = "Install",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledPackageCard(
    packageInfo: PackageInfo,
    onClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = packageInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ) {
                        Text(
                            text = "v${packageInfo.version}",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = packageInfo.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (packageInfo.hasUpdate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Update available: v${packageInfo.latestVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row {
                if (packageInfo.hasUpdate) {
                    IconButton(onClick = onUpdateClick) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onUninstallClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Uninstall",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun InstallQueueItem(
    task: InstallTask,
    onCancel: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val progressColor = when (task.status) {
        InstallStatus.INSTALLING -> MaterialTheme.colorScheme.primary
        InstallStatus.DOWNLOADING -> MaterialTheme.colorScheme.tertiary
        InstallStatus.COMPLETED -> Color(0xFF4CAF50)
        InstallStatus.FAILED -> MaterialTheme.colorScheme.error
        InstallStatus.PAUSED -> Color(0xFFFF9800)
        InstallStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
        InstallStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.packageName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = task.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor
                    )
                }

                Row {
                    when (task.status) {
                        InstallStatus.INSTALLING, InstallStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        InstallStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(
                                    imageVector = Icons.Default.GetApp,
                                    contentDescription = "Resume",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> { }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${task.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.estimatedTimeRemaining > 0) {
                Text(
                    text = "ETA: ${formatDuration(task.estimatedTimeRemaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OfflinePackageCard(
    offlinePackage: OfflinePackage,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Package,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offlinePackage.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = offlinePackage.version,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = offlinePackage.filePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onInstall) {
                    Icon(
                        imageVector = Icons.Default.GetApp,
                        contentDescription = "Install",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BackupCard(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(backup.timestamp) {
        dateFormat.format(Date(backup.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Backup,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${backup.packageCount} packages - ${formatSize(backup.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun PackageDetailDialog(
    packageInfo: PackageInfo,
    isInstalled: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = packageInfo.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close"
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = packageInfo.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        DetailInfoRow(label = "Version", value = packageInfo.version)
                        DetailInfoRow(label = "Author", value = packageInfo.author)
                        DetailInfoRow(label = "License", value = packageInfo.license)
                        DetailInfoRow(label = "Home Page", value = packageInfo.homepage, isLink = true)
                        DetailInfoRow(label = "Downloads", value = packageInfo.downloads.toLocaleString())
                        DetailInfoRow(label = "Size", value = formatSize(packageInfo.size))
                        DetailInfoRow(label = "Requires", value = packageInfo.pythonVersion)
                    }

                    item {
                        if (packageInfo.description.isNotBlank()) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = packageInfo.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (packageInfo.dependencies.isNotEmpty()) {
                        item {
                            Text(
                                text = "Dependencies",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                packageInfo.dependencies.forEach { dep ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(dep, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }

                    if (packageInfo.versions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Available Versions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(packageInfo.versions.take(5)) { version ->
                            VersionItem(version = version)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    if (isInstalled) {
                        Button(
                            onClick = onUninstall,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Uninstall")
                        }
                    } else {
                        Button(
                            onClick = onInstall,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Install")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String, isLink: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isLink) TextDecoration.Underline else TextDecoration.None,
            modifier = if (isLink) Modifier.clickable { } else Modifier
        )
    }
}

@Composable
fun VersionItem(version: VersionInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "v${version.version}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (version.isLatest) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "Released: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(version.releaseDate))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (version.isLatest) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "Latest",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstallDialog(
    packageInfo: PackageInfo,
    onDismiss: () -> Unit,
    onInstall: (String, InstallOptions) -> Unit
) {
    var selectedVersion by remember { mutableStateOf(packageInfo.version) }
    var forceReinstall by remember { mutableStateOf(false) }
    var noDependencies by remember { mutableStateOf(false) }
    var upgradeDeps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Install ${packageInfo.name}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Select version and install options:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Version",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(packageInfo.versions.take(10)) { version ->
                        FilterChip(
                            selected = selectedVersion == version.version,
                            onClick = { selectedVersion = version.version },
                            label = {
                                Text(
                                    text = "v${version.version}",
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { forceReinstall = !forceReinstall }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = forceReinstall,
                        onCheckedChange = { forceReinstall = it }
                    )
                    Text(
                        text = "Force reinstall",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { noDependencies = !noDependencies }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = noDependencies,
                        onCheckedChange = { noDependencies = it }
                    )
                    Text(
                        text = "Skip dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { upgradeDeps = !upgradeDeps }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = upgradeDeps,
                        onCheckedChange = { upgradeDeps = it }
                    )
                    Text(
                        text = "Upgrade dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Package details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                DetailInfoRow(label = "Size", value = formatSize(packageInfo.size))
                DetailInfoRow(label = "Requires", value = packageInfo.pythonVersion)
                DetailInfoRow(
                    label = "Dependencies",
                    value = "${packageInfo.dependencies.size} packages"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = InstallOptions(
                        forceReinstall = forceReinstall,
                        noDependencies = noDependencies,
                        upgradeDependencies = upgradeDeps
                    )
                    onInstall(selectedVersion, options)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.GetApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BatchInstallDialog(
    packages: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>, InstallOptions) -> Unit
) {
    var installAllLatest by remember { mutableStateOf(true) }
    var forceReinstall by remember { mutableStateOf(false) }
    var noDependencies by remember { mutableStateOf(false) }
    var upgradeDeps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batch Install",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Installing ${packages.size} packages:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(packages) { packageName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Package,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { installAllLatest = !installAllLatest }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = installAllLatest,
                        onCheckedChange = { installAllLatest = it }
                    )
                    Text(
                        text = "Install latest versions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { forceReinstall = !forceReinstall }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = forceReinstall,
                        onCheckedChange = { forceReinstall = it }
                    )
                    Text(
                        text = "Force reinstall",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { noDependencies = !noDependencies }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = noDependencies,
                        onCheckedChange = { noDependencies = it }
                    )
                    Text(
                        text = "Skip dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { upgradeDeps = !upgradeDeps }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = upgradeDeps,
                        onCheckedChange = { upgradeDeps = it }
                    )
                    Text(
                        text = "Upgrade dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val versions = packages.associateWith { "latest" }
                    val options = InstallOptions(
                        forceReinstall = forceReinstall,
                        noDependencies = noDependencies,
                        upgradeDependencies = upgradeDeps
                    )
                    onConfirm(versions, options)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.GetApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit
) {
    var backupName by remember { mutableStateOf("") }
    var includeInstalled by remember { mutableStateOf(true) }
    var includeOffline by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Backup",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = backupName,
                    onValueChange = { backupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backup name") },
                    placeholder = {
                        Text(
                            text = "backup_${
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            }"
                        )
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Include in backup:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeInstalled = !includeInstalled }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = includeInstalled,
                        onCheckedChange = { includeInstalled = it }
                    )
                    Text(
                        text = "Installed packages list",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeOffline = !includeOffline }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = includeOffline,
                        onCheckedChange = { includeOffline = it }
                    )
                    Text(
                        text = "Offline package files",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = backupName.ifBlank {
                        "backup_${
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        }"
                    }
                    onConfirm(name, includeInstalled, includeOffline)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestoreDialog(
    backups: List<BackupInfo>,
    onDismiss: () -> Unit,
    onRestore: (BackupInfo) -> Unit
) {
    var selectedBackup by remember { mutableStateOf<BackupInfo?>(null) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restore Backup",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (backups.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Backup,
                        title = "No backups available",
                        subtitle = "Create a backup first"
                    )
                } else {
                    Text(
                        text = "Select a backup to restore:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backups) { backup ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBackup = backup },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedBackup == backup)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = backup.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dateFormat.format(Date(backup.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${backup.packageCount} packages - ${formatSize(backup.size)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selectedBackup == backup) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Warning: Restoring will replace your current package configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedBackup?.let { onRestore(it) }
                },
                enabled = selectedBackup != null
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompatibilityDialog(
    packageName: String,
    info: com.pythonide.domain.model.packageManager.CompatibilityInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compatibility Issue",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Package: $packageName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = info.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (info.hasNativeBinaries) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Native Binary Warning",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = info.binaryWarning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (info.alternatives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Suggested alternatives:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    info.alternatives.forEach { alt ->
                        Text(
                            text = "• $alt",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

@Composable
fun OfflineInstallDialog(
    offlinePackage: OfflinePackage,
    onDismiss: () -> Unit,
    onInstall: (InstallOptions) -> Unit
) {
    var forceReinstall by remember { mutableStateOf(false) }
    var noDependencies by remember { mutableStateOf(false) }
    var upgradeDeps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Install from File",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Package: ${offlinePackage.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version: ${offlinePackage.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "File: ${offlinePackage.fileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size: ${formatSize(offlinePackage.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Install options:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { forceReinstall = !forceReinstall }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = forceReinstall,
                        onCheckedChange = { forceReinstall = it }
                    )
                    Text(
                        text = "Force reinstall",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { noDependencies = !noDependencies }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = noDependencies,
                        onCheckedChange = { noDependencies = it }
                    )
                    Text(
                        text = "Skip dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { upgradeDeps = !upgradeDeps }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = upgradeDeps,
                        onCheckedChange = { upgradeDeps = it }
                    )
                    Text(
                        text = "Upgrade dependencies",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val options = InstallOptions(
                        forceReinstall = forceReinstall,
                        noDependencies = noDependencies,
                        upgradeDependencies = upgradeDeps
                    )
                    onInstall(options)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.InstallMobile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Error",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (onRetry != null) {
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun ProgressOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please wait while packages are being installed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun PackageStats(stats: PackageStatsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Installed",
                value = stats.installedCount.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            StatItem(
                label = "Updates",
                value = stats.updatesAvailable.toString(),
                icon = Icons.Default.Update,
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "Queue",
                value = stats.queuedCount.toString(),
                icon = Icons.Default.InstallMobile,
                color = Color(0xFFFF9800)
            )
            StatItem(
                label = "Size",
                value = formatSize(stats.totalSize),
                icon = Icons.Default.Inventory,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BottomNavButton(
    icon: ImageVector,
    label: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        BadgedBox(
            badge = {
                badge?.let {
                    Badge {
                        Text(
                            text = it,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class InstallOptions(
    val forceReinstall: Boolean = false,
    val noDependencies: Boolean = false,
    val upgradeDependencies: Boolean = false
)

private fun Long.toLocaleString(): String {
    return java.text.NumberFormat.getNumberInstance().format(this)
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
