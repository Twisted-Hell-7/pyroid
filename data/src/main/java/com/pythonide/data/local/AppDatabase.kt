package com.pythonide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FileEntity::class,
        ProjectEntity::class,
        ProjectSessionEntity::class,
        ProjectBackupEntity::class,
        ProjectTemplateEntity::class,
        AutoSaveConfigEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectSessionDao(): ProjectSessionDao
    abstract fun projectBackupDao(): ProjectBackupDao
    abstract fun projectTemplateDao(): ProjectTemplateDao
    abstract fun autoSaveConfigDao(): AutoSaveConfigDao
}
