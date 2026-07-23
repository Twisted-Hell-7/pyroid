package com.pythonide.data.repository

import com.pythonide.data.local.FileDao
import com.pythonide.data.local.FileEntity
import com.pythonide.domain.model.PythonFile
import com.pythonide.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val fileDao: FileDao
) : FileRepository {

    override fun getAllFiles(): Flow<List<PythonFile>> {
        return fileDao.getAllFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFileById(id: String): Flow<PythonFile?> {
        return fileDao.getFileById(id).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun createFile(name: String, content: String): PythonFile {
        val file = PythonFile(
            id = UUID.randomUUID().toString(),
            name = name,
            path = "/$name",
            content = content,
            lastModified = System.currentTimeMillis()
        )
        fileDao.insertFile(file.toEntity())
        return file
    }

    override suspend fun updateFile(file: PythonFile) {
        fileDao.updateFile(file.toEntity())
    }

    override suspend fun deleteFile(id: String) {
        fileDao.deleteFile(id)
    }

    override suspend fun getFileContent(id: String): String? {
        return fileDao.getFileContent(id)
    }

    override suspend fun saveFileContent(id: String, content: String) {
        fileDao.updateFileContent(id, content, System.currentTimeMillis())
    }

    private fun FileEntity.toDomain() = PythonFile(
        id = id,
        name = name,
        path = path,
        content = content,
        lastModified = lastModified
    )

    private fun PythonFile.toEntity() = FileEntity(
        id = id,
        name = name,
        path = path,
        content = content,
        lastModified = lastModified
    )
}
