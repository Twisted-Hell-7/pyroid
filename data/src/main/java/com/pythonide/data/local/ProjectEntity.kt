package com.pythonide.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val rootPath: String,
    val description: String,
    val templateId: String?,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val lastSavedAt: Long,
    val isFavorite: Boolean,
    val fileCount: Int,
    val totalSize: Long,
    val tags: String,
    val pythonVersion: String,
    val interpreterPath: String,
    val author: String,
    val version: String,
    val license: String,
    val dependencies: String,
    val gitRepository: String?,
    val customProperties: String
)

@Entity(tableName = "project_sessions")
data class ProjectSessionEntity(
    @PrimaryKey
    val projectId: String,
    val openFiles: String,
    val activeFileId: String?,
    val cursorPositions: String,
    val scrollPositions: String,
    val splitLayout: String,
    val terminalHistory: String,
    val lastSavedAt: Long,
    val createdAt: Long
)

@Entity(tableName = "project_backups")
data class ProjectBackupEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val projectName: String,
    val backupPath: String,
    val createdAt: Long,
    val size: Long,
    val fileCount: Int,
    val description: String
)

@Entity(tableName = "project_templates")
data class ProjectTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val icon: String,
    val files: String,
    val directories: String,
    val gitignoreContent: String?,
    val requirementsContent: String?,
    val isBuiltIn: Boolean
)

@Entity(tableName = "auto_save_config")
data class AutoSaveConfigEntity(
    @PrimaryKey
    val id: String = "default",
    val enabled: Boolean,
    val intervalMs: Long,
    val saveOnClose: Boolean,
    val saveOnSwitch: Boolean,
    val maxAutoSaves: Int
)
