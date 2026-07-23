package com.pythonide.domain.model

data class PythonFile(
    val id: String,
    val name: String,
    val path: String,
    val content: String,
    val lastModified: Long,
    val isModified: Boolean = false
) {
    companion object {
        const val EXTENSION = ".py"
        const val DEFAULT_NAME = "untitled"
    }
}
