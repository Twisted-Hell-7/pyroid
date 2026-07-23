package com.pythonide.app.screens.filemanager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.domain.model.filemanager.FileManagerItem
import com.pythonide.domain.model.filemanager.FileManagerState
import com.pythonide.domain.model.filemanager.FileType
import com.pythonide.domain.model.filemanager.Project
import com.pythonide.domain.model.filemanager.SortBy
import com.pythonide.domain.model.filemanager.StorageSource
import com.pythonide.domain.model.filemanager.ViewMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val config by viewModel.config.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showPropertiesDialog by viewModel.showPropertiesDialog.collectAsState()
    val showProjectDialog by viewModel.showProjectDialog.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val itemProperties by viewModel.itemProperties.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var createIsDirectory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (state.storageSource) {
                                StorageSource.RECENT -> "Recent Files"
                                StorageSource.FAVORITES -> "Favorites"
                                StorageSource.PROJECT -> "Projects"
                                else -> "File Manager"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.storageSource == StorageSource.INTERNAL || state.storageSource == StorageSource.EXTERNAL) {
                            Text(
                                text = state.currentPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (state.currentPath != "/") {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                },
                actions = {
                    if (state.selectedItems.isNotEmpty()) {
                        Text(
                            text = "${state.selectedItems.size} selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        IconButton(onClick = { viewModel.copySelected() }) {
                            Icon(Icons.Default.ContentCopy, "Copy")
                        }
                        IconButton(onClick = { viewModel.cutSelected() }) {
                            Icon(Icons.Default.ContentCut, "Cut")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44747))
                        }
                        IconButton(onClick = { viewModel.shareSelected() }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.CheckCircle, "Clear Selection")
                        }
                    } else {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, "Sort")
                        }
                        IconButton(onClick = { showViewMenu = true }) {
                            Icon(
                                if (state.viewMode == ViewMode.LIST) Icons.Default.ViewList else Icons.Default.GridView,
                                "View Mode"
                            )
                        }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.Folder, "More")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.showProjectDialog() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Code, "Projects")
                }
                SmallFloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder")
                }
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "New File")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Storage source tabs
            StorageSourceTabs(
                currentSource = state.storageSource,
                onSourceChange = { viewModel.setStorageSource(it) }
            )

            // Breadcrumb
            if (state.storageSource == StorageSource.INTERNAL || state.storageSource == StorageSource.EXTERNAL) {
                Breadcrumb(
                    path = state.breadcrumb,
                    onNavigate = { index ->
                        val path = "/" + state.breadcrumb.take(index + 1).joinToString("/")
                        viewModel.navigateTo(path)
                    }
                )
            }

            // Search bar
            AnimatedVisibility(
                visible = state.searchQuery.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { viewModel.setSearchQuery("") }
                )
            }

            // Content
            when (state.storageSource) {
                StorageSource.RECENT -> {
                    RecentFilesList(
                        files = state.recentFiles,
                        onItemClick = { onOpenFile(it.item.path) }
                    )
                }
                StorageSource.FAVORITES -> {
                    FileList(
                        items = state.favorites,
                        state = state,
                        onItemClick = { viewModel.navigateToItem(it) },
                        onItemLongClick = { viewModel.selectItem(it.id, true) },
                        onItemSelect = { viewModel.selectItem(it.id, true) },
                        onOpenFile = { onOpenFile(it.path) }
                    )
                }
                StorageSource.PROJECT -> {
                    ProjectList(
                        projects = state.projects,
                        onProjectClick = { viewModel.openProject(it) },
                        onProjectLongClick = { },
                        onDeleteProject = { viewModel.deleteProject(it) }
                    )
                }
                else -> {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (state.filteredItems.isEmpty()) {
                        EmptyState(
                            message = if (state.searchQuery.isNotEmpty()) {
                                "No files matching '${state.searchQuery}'"
                            } else {
                                "No files in this directory"
                            }
                        )
                    } else {
                        FileList(
                            items = state.filteredItems,
                            state = state,
                            onItemClick = { viewModel.navigateToItem(it) },
                            onItemLongClick = { viewModel.selectItem(it.id, true) },
                            onItemSelect = { viewModel.selectItem(it.id, true) },
                            onOpenFile = { onOpenFile(it.path) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateItemDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { name, isDirectory -> viewModel.createFile(name, isDirectory) }
        )
    }

    if (showRenameDialog && selectedItem != null) {
        RenameItemDialog(
            currentName = selectedItem?.name ?: "",
            onDismiss = { viewModel.hideRenameDialog() },
            onRename = { viewModel.renameItem(it) }
        )
    }

    if (showPropertiesDialog) {
        PropertiesDialog(
            properties = itemProperties,
            onDismiss = { viewModel.hidePropertiesDialog() }
        )
    }

    if (showProjectDialog) {
        CreateProjectDialog(
            onDismiss = { viewModel.hideProjectDialog() },
            onCreate = { name, description -> viewModel.createProject(name, description) }
        )
    }

    // Sort menu
    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Sort by Name") },
            onClick = { viewModel.setSortBy(SortBy.NAME); showSortMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Sort by Size") },
            onClick = { viewModel.setSortBy(SortBy.SIZE); showSortMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Sort by Date") },
            onClick = { viewModel.setSortBy(SortBy.DATE); showSortMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Sort by Type") },
            onClick = { viewModel.setSortBy(SortBy.TYPE); showSortMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Toggle Order") },
            onClick = { viewModel.toggleSortOrder(); showSortMenu = false }
        )
    }

    // View menu
    DropdownMenu(
        expanded = showViewMenu,
        onDismissRequest = { showViewMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("List View") },
            onClick = { viewModel.setViewMode(ViewMode.LIST); showViewMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Grid View") },
            onClick = { viewModel.setViewMode(ViewMode.GRID); showViewMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Compact View") },
            onClick = { viewModel.setViewMode(ViewMode.COMPACT); showViewMenu = false }
        )
    }

    // More menu
    DropdownMenu(
        expanded = showMoreMenu,
        onDismissRequest = { showMoreMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Toggle Hidden Files") },
            onClick = { viewModel.toggleHiddenFiles(); showMoreMenu = false }
        )
        DropdownMenuItem(
            text = { Text("Select All") },
            onClick = { viewModel.selectAll(); showMoreMenu = false }
        )
        if (state.clipboardItems.isNotEmpty()) {
            DropdownMenuItem(
                text = { Text("Paste (${state.clipboardItems.size})") },
                onClick = { viewModel.pasteItems(); showMoreMenu = false }
            )
        }
    }
}

@Composable
private fun StorageSourceTabs(
    currentSource: StorageSource,
    onSourceChange: (StorageSource) -> Unit
) {
    val sources = listOf(
        StorageSource.INTERNAL to "Files",
        StorageSource.RECENT to "Recent",
        StorageSource.FAVORITES to "Favorites",
        StorageSource.PROJECT to "Projects"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sources) { (source, label) ->
            val isSelected = currentSource == source
            Card(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSourceChange(source) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun Breadcrumb(
    path: List<String>,
    onNavigate: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(path) { index, segment ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigate(index) }
            ) {
                if (index > 0) {
                    Text(
                        text = " / ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = segment,
                    color = if (index == path.size - 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 12.sp,
                    fontWeight = if (index == path.size - 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search files...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
            },
            singleLine = true
        )
    }
}

@Composable
private fun FileList(
    items: List<FileManagerItem>,
    state: FileManagerState,
    onItemClick: (FileManagerItem) -> Unit,
    onItemLongClick: (FileManagerItem) -> Unit,
    onItemSelect: (FileManagerItem) -> Unit,
    onOpenFile: (FileManagerItem) -> Unit
) {
    when (state.viewMode) {
        ViewMode.LIST -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    FileListItem(
                        item = item,
                        isSelected = item.id in state.selectedItems,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onSelect = { onItemSelect(item) }
                    )
                }
            }
        }
        ViewMode.GRID -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items.chunked(3)) { row ->
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { item ->
                            FileGridItem(
                                item = item,
                                isSelected = item.id in state.selectedItems,
                                onClick = { onItemClick(item) },
                                onLongClick = { onItemLongClick(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty spaces
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        ViewMode.COMPACT -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    FileCompactItem(
                        item = item,
                        isSelected = item.id in state.selectedItems,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    item: FileManagerItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        Icon(
            imageVector = when (item.type) {
                FileType.DIRECTORY -> Icons.Default.Folder
                FileType.FILE -> getFileIcon(item.extension)
                else -> Icons.Default.Description
            },
            contentDescription = null,
            tint = when (item.type) {
                FileType.DIRECTORY -> Color(0xFF569CD6)
                FileType.FILE -> getFileColor(item.extension)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(40.dp)
                .padding(end = 12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (item.type == FileType.DIRECTORY) {
                    "Folder"
                } else {
                    formatFileSize(item.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatDate(item.lastModified),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileGridItem(
    item: FileManagerItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (item.type) {
                    FileType.DIRECTORY -> Icons.Default.Folder
                    FileType.FILE -> getFileIcon(item.extension)
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = when (item.type) {
                    FileType.DIRECTORY -> Color(0xFF569CD6)
                    FileType.FILE -> getFileColor(item.extension)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FileCompactItem(
    item: FileManagerItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (item.type) {
                FileType.DIRECTORY -> Icons.Default.Folder
                FileType.FILE -> getFileIcon(item.extension)
                else -> Icons.Default.Description
            },
            contentDescription = null,
            tint = when (item.type) {
                FileType.DIRECTORY -> Color(0xFF569CD6)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (item.type == FileType.FILE) {
            Text(
                text = formatFileSize(item.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentFilesList(
    files: List<com.pythonide.domain.model.filemanager.RecentFile>,
    onItemClick: (com.pythonide.domain.model.filemanager.RecentFile) -> Unit
) {
    if (files.isEmpty()) {
        EmptyState(message = "No recent files")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(files, key = { it.item.id }) { recent ->
            FileListItem(
                item = recent.item,
                isSelected = false,
                onClick = { onItemClick(recent) },
                onLongClick = { },
                onSelect = { }
            )
        }
    }
}

@Composable
private fun ProjectList(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onProjectLongClick: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit
) {
    if (projects.isEmpty()) {
        EmptyState(
            message = "No projects yet",
            subtitle = "Create a project to organize your files"
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) },
                onLongClick = { onProjectLongClick(project) },
                onDelete = { onDeleteProject(project) }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (project.description.isNotEmpty()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = project.rootPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Folder, "More")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    subtitle: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateItemDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDirectory by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isDirectory) "Create Folder" else "Create File")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDirectory,
                        onCheckedChange = { isDirectory = it }
                    )
                    Text("Create as folder")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, isDirectory) },
                enabled = name.isNotBlank()
            ) {
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
private fun RenameItemDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("New Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Rename")
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
private fun PropertiesDialog(
    properties: Map<String, Any>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column {
                properties.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Project") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank()
            ) {
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

private fun getFileIcon(extension: String?): ImageVector {
    return when (extension?.lowercase()) {
        "py", "pyw" -> Icons.Default.Code
        "kt", "java", "js", "ts", "swift" -> Icons.Default.Code
        "txt", "md", "log" -> Icons.Default.Description
        "json", "xml", "yaml", "yml" -> Icons.Default.Description
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> Icons.Default.Star
        "mp3", "wav", "ogg" -> Icons.Default.Star
        "mp4", "avi", "mkv" -> Icons.Default.Star
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Star
        "pdf" -> Icons.Default.Description
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.Description
        else -> Icons.Default.Description
    }
}

private fun getFileColor(extension: String?): Color {
    return when (extension?.lowercase()) {
        "py" -> Color(0xFF3572A5)
        "kt" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFED8B00)
        "js" -> Color(0xFFF7DF1E)
        "ts" -> Color(0xFF3178C6)
        "json" -> Color(0xFF292929)
        "xml" -> Color(0xFF0060AC)
        "md" -> Color(0xFF083FA1)
        "txt" -> Color(0xFF666666)
        else -> Color(0xFF666666)
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}
