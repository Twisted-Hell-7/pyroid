package com.pythonide.app.screens.project

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GitBranch
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.app.data.local.entity.Project
import com.pythonide.app.data.local.entity.ProjectBackup
import com.pythonide.app.data.local.entity.ProjectMetadata
import com.pythonide.app.data.model.AutoSaveConfig
import com.pythonide.app.data.model.ProjectSortBy
import com.pythonide.app.data.model.ProjectSortOrder
import com.pythonide.app.data.model.ProjectTemplate
import com.pythonide.app.data.model.TemplateCategory
import com.pythonide.app.data.model.TemplateIcon
import com.pythonide.app.viewmodel.ProjectViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagementScreen(
    onNavigateBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    viewModel: ProjectViewModel = hiltViewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showNewProjectDialog by viewModel.showNewProjectDialog.collectAsState()
    val showTemplateDialog by viewModel.showTemplateDialog.collectAsState()
    val showBackupDialog by viewModel.showBackupDialog.collectAsState()
    val showMetadataDialog by viewModel.showMetadataDialog.collectAsState()
    val showSaveAsDialog by viewModel.showSaveAsDialog.collectAsState()
    val showAutoSaveSettingsDialog by viewModel.showAutoSaveSettingsDialog.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val autoSaveConfig by viewModel.autoSaveConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Projects",
                        style = MaterialTheme.typography.headlineSmall
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
                    IconButton(onClick = { viewModel.setAutoSaveSettingsDialogVisible(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Auto-save settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setNewProjectDialogVisible(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Project"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            ProjectSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                sortBy = sortBy,
                onSortByChange = { viewModel.setSortBy(it) },
                sortOrder = sortOrder,
                onSortOrderChange = { viewModel.setSortOrder(it) },
                showFavoritesOnly = showFavoritesOnly,
                onToggleFavoritesOnly = { viewModel.toggleShowFavoritesOnly() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (projects.isEmpty()) {
                EmptyProjectsState()
            } else {
                ProjectList(
                    projects = projects,
                    onOpenProject = onOpenProject,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onCreateBackup = { project ->
                        viewModel.setSelectedProject(project)
                        viewModel.setBackupDialogVisible(true)
                    },
                    onEditMetadata = { project ->
                        viewModel.setSelectedProject(project)
                        viewModel.setMetadataDialogVisible(true)
                    },
                    onDeleteProject = { viewModel.deleteProject(it) }
                )
            }
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { viewModel.setNewProjectDialogVisible(false) },
            onCreateProject = { name, description, author, template, pythonVersion ->
                viewModel.createProject(name, description, author, template, pythonVersion)
            },
            templates = templates
        )
    }

    if (showTemplateDialog) {
        TemplateSelectionDialog(
            templates = templates,
            onDismiss = { viewModel.setTemplateDialogVisible(false) },
            onSelectTemplate = { viewModel.setSelectedTemplate(it) }
        )
    }

    if (showBackupDialog && selectedProject != null) {
        BackupDialog(
            project = selectedProject!!,
            onDismiss = { viewModel.setBackupDialogVisible(false) },
            onCreateBackup = { description ->
                viewModel.createBackup(selectedProject!!, description)
            }
        )
    }

    if (showMetadataDialog && selectedProject != null) {
        MetadataDialog(
            project = selectedProject!!,
            onDismiss = { viewModel.setMetadataDialogVisible(false) },
            onUpdateMetadata = { metadata ->
                viewModel.updateProjectMetadata(selectedProject!!, metadata)
            }
        )
    }

    if (showSaveAsDialog && selectedProject != null) {
        SaveAsDialog(
            project = selectedProject!!,
            onDismiss = { viewModel.setSaveAsDialogVisible(false) },
            onSaveAs = { newPath, newName ->
                viewModel.saveProjectAs(selectedProject!!, newPath, newName)
            }
        )
    }

    if (showAutoSaveSettingsDialog) {
        AutoSaveSettingsDialog(
            config = autoSaveConfig,
            onDismiss = { viewModel.setAutoSaveSettingsDialogVisible(false) },
            onUpdateConfig = { config ->
                viewModel.updateAutoSaveConfig(config)
            }
        )
    }
}

@Composable
fun ProjectSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortBy: ProjectSortBy,
    onSortByChange: (ProjectSortBy) -> Unit,
    sortOrder: ProjectSortOrder,
    onSortOrderChange: (ProjectSortOrder) -> Unit,
    showFavoritesOnly: Boolean,
    onToggleFavoritesOnly: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search projects...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                Row {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = if (sortOrder == ProjectSortOrder.ASCENDING)
                                Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Sort order"
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        ProjectSortBy.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = when (sortOption) {
                                            ProjectSortBy.NAME -> "Name"
                                            ProjectSortBy.DATE -> "Date"
                                            ProjectSortBy.SIZE -> "Size"
                                            ProjectSortBy.LAST_MODIFIED -> "Last Modified"
                                        }
                                    )
                                },
                                onClick = {
                                    onSortByChange(sortOption)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sortBy.name.lowercase().replaceFirstChar { it.uppercase() }}: ${
                    sortOrder.name.lowercase().replaceFirstChar { it.uppercase() }
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFavoritesOnly,
                label = { Text("Favorites") },
                leadingIcon = {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun ProjectList(
    projects: List<Project>,
    onOpenProject: (String) -> Unit,
    onToggleFavorite: (Project) -> Unit,
    onCreateBackup: (Project) -> Unit,
    onEditMetadata: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                onOpenProject = { onOpenProject(project.id) },
                onToggleFavorite = { onToggleFavorite(project) },
                onCreateBackup = { onCreateBackup(project) },
                onEditMetadata = { onEditMetadata(project) },
                onDeleteProject = { onDeleteProject(project) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(
    project: Project,
    onOpenProject: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCreateBackup: () -> Unit,
    onEditMetadata: () -> Unit,
    onDeleteProject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenProject() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (project.isFavorite) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = if (project.isFavorite) "Unfavorite"
                            else "Favorite",
                            tint = if (project.isFavorite) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!project.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDate(project.lastModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${project.fileCount} files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!project.tags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    project.tags.take(3).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    if (project.tags.size > 3) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "+${project.tags.size - 3}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCreateBackup) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Backup",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEditMetadata) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Edit metadata",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDeleteProject) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete project",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyProjectsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a new project to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (name: String, description: String, author: String, template: ProjectTemplate?, pythonVersion: String) -> Unit,
    templates: List<ProjectTemplate>
) {
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    var projectAuthor by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<ProjectTemplate?>(null) }
    var pythonVersion by remember { mutableStateOf("3.11") }
    var showTemplateGrid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Project",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = projectAuthor,
                    onValueChange = { projectAuthor = it },
                    label = { Text("Author (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pythonVersion,
                    onValueChange = { pythonVersion = it },
                    label = { Text("Python Version") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Template",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTemplateGrid = !showTemplateGrid },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedTemplate?.name ?: "Select template (optional)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedTemplate != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (showTemplateGrid) Icons.Default.ArrowUpward
                            else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showTemplateGrid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { template ->
                            TemplateCard(
                                template = template,
                                isSelected = selectedTemplate == template,
                                onClick = {
                                    selectedTemplate = if (selectedTemplate == template) null else template
                                    showTemplateGrid = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (projectName.isNotBlank()) {
                        onCreateProject(
                            projectName.trim(),
                            projectDescription.trim(),
                            projectAuthor.trim(),
                            selectedTemplate,
                            pythonVersion.trim()
                        )
                    }
                },
                enabled = projectName.isNotBlank()
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
fun TemplateSelectionDialog(
    templates: List<ProjectTemplate>,
    onDismiss: () -> Unit,
    onSelectTemplate: (ProjectTemplate) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }

    val filteredTemplates = if (selectedCategory != null) {
        templates.filter { it.category == selectedCategory }
    } else {
        templates
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Template",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                    TemplateCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = when (category) {
                                        TemplateCategory.WEB -> "Web"
                                        TemplateCategory.DATA_SCIENCE -> "Data Science"
                                        TemplateCategory.MACHINE_LEARNING -> "ML"
                                        TemplateCategory.API -> "API"
                                        TemplateCategory.AUTOMATION -> "Automation"
                                        TemplateCategory.UTILITY -> "Utility"
                                        TemplateCategory.GAME -> "Game"
                                        TemplateCategory.OTHER -> "Other"
                                    }
                                )
                            }
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTemplates) { template ->
                        TemplateCard(
                            template = template,
                            isSelected = false,
                            onClick = {
                                onSelectTemplate(template)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TemplateCard(
    template: ProjectTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (template.icon) {
                    TemplateIcon.GLOBE -> Icons.Default.Language
                    TemplateIcon.BRAIN -> Icons.Default.Layers
                    TemplateIcon.BOOK -> Icons.Default.LibraryBooks
                    TemplateIcon.CLOUD -> Icons.Default.CloudUpload
                    TemplateIcon.GEAR -> Icons.Default.Settings
                    TemplateIcon.CODE -> Icons.Default.Code
                    TemplateIcon.PLAY -> Icons.Default.Star
                    TemplateIcon.WRENCH -> Icons.Default.Description
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = template.language,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    }
}

@Composable
fun BackupDialog(
    project: Project,
    onDismiss: () -> Unit,
    onCreateBackup: (description: String) -> Unit
) {
    var backupDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Backup",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Create a backup of \"${project.name}\"",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = backupDescription,
                    onValueChange = { backupDescription = it },
                    label = { Text("Backup description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Backup will include all project files and settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreateBackup(backupDescription.trim())
                }
            ) {
                Text("Create Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataDialog(
    project: Project,
    onDismiss: () -> Unit,
    onUpdateMetadata: (ProjectMetadata) -> Unit
) {
    var pythonVersion by remember { mutableStateOf(project.metadata?.pythonVersion ?: "3.11") }
    var author by remember { mutableStateOf(project.metadata?.author ?: "") }
    var license by remember { mutableStateOf(project.metadata?.license ?: "MIT") }
    var dependencies by remember {
        mutableStateOf(project.metadata?.dependencies?.joinToString("\n") ?: "")
    }
    var gitRepo by remember { mutableStateOf(project.metadata?.gitRepository ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Project Metadata",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = pythonVersion,
                    onValueChange = { pythonVersion = it },
                    label = { Text("Python Version") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = license,
                    onValueChange = { license = it },
                    label = { Text("License") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dependencies,
                    onValueChange = { dependencies = it },
                    label = { Text("Dependencies (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gitRepo,
                    onValueChange = { gitRepo = it },
                    label = { Text("Git Repository URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GitBranch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val metadata = ProjectMetadata(
                        pythonVersion = pythonVersion.trim(),
                        author = author.trim().ifEmpty { null },
                        license = license.trim().ifEmpty { "MIT" },
                        dependencies = dependencies.lines().filter { it.isNotBlank() },
                        gitRepository = gitRepo.trim().ifEmpty { null }
                    )
                    onUpdateMetadata(metadata)
                }
            ) {
                Text("Save")
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
fun SaveAsDialog(
    project: Project,
    onDismiss: () -> Unit,
    onSaveAs: (newPath: String, newName: String) -> Unit
) {
    var newPath by remember { mutableStateOf(project.path) }
    var newName by remember { mutableStateOf("${project.name} (copy)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save As",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    text = "Save \"${project.name}\" as a new project",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Project Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPath,
                    onValueChange = { newPath = it },
                    label = { Text("Save Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank() && newPath.isNotBlank()) {
                        onSaveAs(newPath.trim(), newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newPath.isNotBlank()
            ) {
                Text("Save")
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
fun AutoSaveSettingsDialog(
    config: AutoSaveConfig,
    onDismiss: () -> Unit,
    onUpdateConfig: (AutoSaveConfig) -> Unit
) {
    var isEnabled by remember { mutableStateOf(config.isEnabled) }
    var intervalMinutes by remember { mutableFloatStateOf(config.intervalMinutes.toFloat()) }
    var saveOnPause by remember { mutableStateOf(config.saveOnPause) }
    var saveOnExit by remember { mutableStateOf(config.saveOnExit) }
    var maxBackups by remember { mutableStateOf(config.maxBackups.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Auto-save Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Auto-save",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Save Interval",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${intervalMinutes.toInt()} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Slider(
                        value = intervalMinutes,
                        onValueChange = { intervalMinutes = it },
                        valueRange = 1f..60f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "60 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Save on Pause",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = saveOnPause,
                            onCheckedChange = { saveOnPause = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Save on Exit",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Checkbox(
                            checked = saveOnExit,
                            onCheckedChange = { saveOnExit = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = maxBackups,
                        onValueChange = { maxBackups = it.filter { c -> c.isDigit() } },
                        label = { Text("Maximum Auto-backups") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        supportingText = {
                            Text("Keep up to ${maxBackups.ifEmpty { "0" }} auto-backups")
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newConfig = AutoSaveConfig(
                        isEnabled = isEnabled,
                        intervalMinutes = intervalMinutes.toInt(),
                        saveOnPause = saveOnPause,
                        saveOnExit = saveOnExit,
                        maxBackups = maxBackups.toIntOrNull() ?: config.maxBackups
                    )
                    onUpdateConfig(newConfig)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
