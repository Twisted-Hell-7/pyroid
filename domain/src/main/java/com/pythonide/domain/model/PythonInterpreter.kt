package com.pythonide.domain.model

import java.util.UUID

data class PythonInterpreter(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Python",
    val state: InterpreterState = InterpreterState.IDLE,
    val pythonVersion: String = "3.14",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

enum class InterpreterState {
    IDLE,
    RUNNING,
    WAITING_INPUT,
    STOPPED,
    ERROR,
    RESTARTING
}
