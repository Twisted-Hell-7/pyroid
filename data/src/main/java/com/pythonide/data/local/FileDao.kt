package com.pythonide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    fun getFileById(id: String): Flow<FileEntity?>

    @Query("SELECT content FROM files WHERE id = :id")
    suspend fun getFileContent(id: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFile(id: String)

    @Query("UPDATE files SET content = :content, lastModified = :lastModified WHERE id = :id")
    suspend fun updateFileContent(id: String, content: String, lastModified: Long)
}
