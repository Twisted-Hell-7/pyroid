package com.pythonide.app.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.PythonFile
import com.pythonide.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    val files: StateFlow<List<PythonFile>> = fileRepository.getAllFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewFile(name: String = "untitled.py") {
        viewModelScope.launch {
            fileRepository.createFile(name)
        }
    }

    fun deleteFile(id: String) {
        viewModelScope.launch {
            fileRepository.deleteFile(id)
        }
    }
}
