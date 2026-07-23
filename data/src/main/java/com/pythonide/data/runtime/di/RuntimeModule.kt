package com.pythonide.data.runtime.di

import android.content.Context
import com.pythonide.data.debugger.DebuggerEngine
import com.pythonide.data.debugger.DebuggerRepositoryImpl
import com.pythonide.data.runtime.CrashPrevention
import com.pythonide.data.runtime.ExceptionRecovery
import com.pythonide.data.runtime.MemoryOptimizer
import com.pythonide.data.runtime.SecureStorage
import com.pythonide.data.runtime.StartupOptimizer
import com.pythonide.data.runtime.core.PythonNativeBridge
import com.pythonide.data.runtime.engine.ExecutionEngine
import com.pythonide.data.runtime.manager.InterpreterManager
import com.pythonide.data.runtime.permission.PermissionManager
import com.pythonide.data.runtime.repository.PythonRuntimeRepositoryImpl
import com.pythonide.data.runtime.sandbox.SandboxExecutor
import com.pythonide.domain.repository.DebuggerRepository
import com.pythonide.domain.repository.PythonRuntimeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {

    @Provides
    @Singleton
    fun providePythonNativeBridge(
        @ApplicationContext context: Context
    ): PythonNativeBridge {
        return PythonNativeBridge(context)
    }

    @Provides
    @Singleton
    fun provideInterpreterManager(
        nativeBridge: PythonNativeBridge
    ): InterpreterManager {
        return InterpreterManager(nativeBridge)
    }

    @Provides
    @Singleton
    fun provideExecutionEngine(
        @ApplicationContext context: Context,
        interpreterManager: InterpreterManager
    ): ExecutionEngine {
        return ExecutionEngine(context, interpreterManager)
    }

    @Provides
    @Singleton
    fun provideDebuggerEngine(
        @ApplicationContext context: Context
    ): DebuggerEngine {
        return DebuggerEngine(context)
    }

    @Provides
    @Singleton
    fun provideStartupOptimizer(
        @ApplicationContext context: Context
    ): StartupOptimizer {
        return StartupOptimizer(context)
    }

    @Provides
    @Singleton
    fun provideMemoryOptimizer(
        @ApplicationContext context: Context
    ): MemoryOptimizer {
        return MemoryOptimizer(context)
    }

    @Provides
    @Singleton
    fun provideCrashPrevention(): CrashPrevention {
        return CrashPrevention()
    }

    @Provides
    @Singleton
    fun provideExceptionRecovery(): ExceptionRecovery {
        return ExceptionRecovery()
    }

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideSandboxExecutor(): SandboxExecutor {
        return SandboxExecutor()
    }

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager {
        return PermissionManager(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RuntimeRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPythonRuntimeRepository(
        impl: PythonRuntimeRepositoryImpl
    ): PythonRuntimeRepository

    @Binds
    @Singleton
    abstract fun bindDebuggerRepository(
        impl: DebuggerRepositoryImpl
    ): DebuggerRepository
}
