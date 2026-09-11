package com.pythonide.domain.model

data class ExecutionResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val code: String,
    val stdout: String = "",
    val stderr: String = "",
    val returnValue: Any? = null,
    val exitCode: Int = 0,
    val executionTimeMs: Long = 0,
    val exception: PythonException? = null
) {
    val isSuccessful: Boolean get() = exitCode == 0 && exception == null
}

data class PythonException(
    val type: String,
    val message: String,
    val traceback: String = "",
    val lineNumber: Int? = null,
    val fileName: String? = null
)

data class REPLHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val input: String,
    val output: String = "",
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class InterpreterConfig(
    val maxExecutionTimeMs: Long = 30_000L,
    val maxMemoryMb: Int = 256,
    val enableInput: Boolean = true,
    val enableImports: Boolean = true,
    val allowedModules: List<String> = emptyList(),
    val blockedModules: List<String> = listOf("os", "sys", "subprocess")
)
