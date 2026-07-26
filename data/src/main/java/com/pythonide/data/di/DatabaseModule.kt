package com.pythonide.data.di

import android.content.Context
import androidx.room.Room
import com.pythonide.data.local.AppDatabase
import com.pythonide.data.local.AutoSaveConfigDao
import com.pythonide.data.local.FileDao
import com.pythonide.data.local.ProjectBackupDao
import com.pythonide.data.local.ProjectDao
import com.pythonide.data.local.ProjectSessionDao
import com.pythonide.data.local.ProjectTemplateDao
import com.pythonide.data.local.SettingsDataStore
import com.pythonide.data.repository.FileManagerRepositoryImpl
import com.pythonide.data.repository.FileRepositoryImpl
import com.pythonide.data.repository.PackageManagerRepositoryImpl
import com.pythonide.data.repository.ProjectRepositoryImpl
import com.pythonide.domain.repository.FileManagerRepository
import com.pythonide.domain.repository.FileRepository
import com.pythonide.domain.repository.PackageManagerRepository
import com.pythonide.domain.repository.ProjectRepository
import com.pythonide.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "python_ide_database"
        )
            .build()
    }

    @Provides
    @Singleton
    fun provideFileDao(database: AppDatabase): FileDao {
        return database.fileDao()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideProjectSessionDao(database: AppDatabase): ProjectSessionDao {
        return database.projectSessionDao()
    }

    @Provides
    @Singleton
    fun provideProjectBackupDao(database: AppDatabase): ProjectBackupDao {
        return database.projectBackupDao()
    }

    @Provides
    @Singleton
    fun provideProjectTemplateDao(database: AppDatabase): ProjectTemplateDao {
        return database.projectTemplateDao()
    }

    @Provides
    @Singleton
    fun provideAutoSaveConfigDao(database: AppDatabase): AutoSaveConfigDao {
        return database.autoSaveConfigDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(
        impl: FileRepositoryImpl
    ): FileRepository

    @Binds
    @Singleton
    abstract fun bindFileManagerRepository(
        impl: FileManagerRepositoryImpl
    ): FileManagerRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsDataStore
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindPackageManagerRepository(
        impl: PackageManagerRepositoryImpl
    ): PackageManagerRepository
}
