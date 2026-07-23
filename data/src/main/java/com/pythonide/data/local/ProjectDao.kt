package com.pythonide.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastAccessedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY lastAccessedAt DESC")
    fun getFavoriteProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY lastAccessedAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int = 10): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("UPDATE projects SET isFavorite = :isFavorite WHERE id = :projectId")
    suspend fun toggleFavorite(projectId: String, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}

@Dao
interface ProjectSessionDao {
    @Query("SELECT * FROM project_sessions WHERE projectId = :projectId")
    fun getSession(projectId: String): Flow<ProjectSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ProjectSessionEntity)

    @Update
    suspend fun updateSession(session: ProjectSessionEntity)

    @Delete
    suspend fun deleteSession(session: ProjectSessionEntity)

    @Query("DELETE FROM project_sessions WHERE projectId = :projectId")
    suspend fun deleteSessionByProjectId(projectId: String)
}

@Dao
interface ProjectBackupDao {
    @Query("SELECT * FROM project_backups WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getBackups(projectId: String): Flow<List<ProjectBackupEntity>>

    @Query("SELECT * FROM project_backups WHERE id = :backupId")
    fun getBackupById(backupId: String): Flow<ProjectBackupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: ProjectBackupEntity)

    @Delete
    suspend fun deleteBackup(backup: ProjectBackupEntity)

    @Query("DELETE FROM project_backups WHERE id = :backupId")
    suspend fun deleteBackupById(backupId: String)
}

@Dao
interface ProjectTemplateDao {
    @Query("SELECT * FROM project_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<ProjectTemplateEntity>>

    @Query("SELECT * FROM project_templates WHERE id = :templateId")
    fun getTemplateById(templateId: String): Flow<ProjectTemplateEntity?>

    @Query("SELECT * FROM project_templates WHERE category = :category")
    fun getTemplatesByCategory(category: String): Flow<List<ProjectTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ProjectTemplateEntity)

    @Update
    suspend fun updateTemplate(template: ProjectTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: ProjectTemplateEntity)

    @Query("DELETE FROM project_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    @Query("SELECT COUNT(*) FROM project_templates")
    suspend fun getTemplateCount(): Int
}

@Dao
interface AutoSaveConfigDao {
    @Query("SELECT * FROM auto_save_config WHERE id = 'default'")
    fun getConfig(): Flow<AutoSaveConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AutoSaveConfigEntity)

    @Update
    suspend fun updateConfig(config: AutoSaveConfigEntity)
}
