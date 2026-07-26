package com.pythonide.data.runtime.di

import android.content.Context
import com.pythonide.data.debugger.DebuggerRepositoryImpl
import com.pythonide.data.runtime.repository.PythonRuntimeRepositoryImpl
import com.pythonide.domain.repository.DebuggerRepository
import com.pythonide.domain.repository.PythonRuntimeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
