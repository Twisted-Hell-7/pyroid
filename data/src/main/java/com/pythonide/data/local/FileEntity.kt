package com.pythonide.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val path: String,
    val content: String,
    val lastModified: Long
)
