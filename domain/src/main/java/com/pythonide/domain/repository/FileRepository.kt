package com.pythonide.domain.repository

import com.pythonide.domain.model.PythonFile
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getAllFiles(): Flow<List<PythonFile>>
    fun getFileById(id: String): Flow<PythonFile?>
    suspend fun createFile(name: String, content: String = ""): PythonFile
    suspend fun updateFile(file: PythonFile)
    suspend fun deleteFile(id: String)
    suspend fun getFileContent(id: String): String?
    suspend fun saveFileContent(id: String, content: String)
}
