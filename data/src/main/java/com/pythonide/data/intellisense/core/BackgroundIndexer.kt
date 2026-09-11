package com.pythonide.data.intellisense.core

import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.IndexEntry
import com.pythonide.domain.model.intellisense.IndexStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BackgroundIndexer {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    
    private var indexingJob: Job? = null
    
    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()
    
    private val _stats = MutableStateFlow(IndexStats(0, 0, 0, 0))
    val stats: StateFlow<IndexStats> = _stats.asStateFlow()
    
    private val index = mutableMapOf<String, MutableList<IndexEntry>>()
    private val fileIndex = mutableMapOf<String, List<IndexEntry>>()
    
    suspend fun indexFile(filePath: String, content: String) = mutex.withLock {
        val entries = extractEntries(filePath, content)
        val old = fileIndex[filePath] ?: emptyList()
        // Remove old entries for this file only
        old.forEach { e ->
            index[e.name]?.removeAll { it.filePath == filePath }
            if (index[e.name]?.isEmpty() == true) index.remove(e.name)
        }
        fileIndex[filePath] = entries
        entries.forEach { entry ->
            index.getOrPut(entry.name) { mutableListOf() }.add(entry)
        }
        
        updateStats()
    }
    
    suspend fun indexFiles(files: Map<String, String>) = mutex.withLock {
        _isIndexing.value = true
        val startTime = System.currentTimeMillis()
        
        try {
            index.clear()
            fileIndex.clear()
            
            files.forEach { (filePath, content) ->
                val entries = extractEntries(filePath, content)
                fileIndex[filePath] = entries
                entries.forEach { entry ->
                    index.getOrPut(entry.name) { mutableListOf() }.add(entry)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            _stats.value = IndexStats(
                totalFiles = files.size,
                totalEntries = index.size,
                lastUpdated = System.currentTimeMillis(),
                indexingTimeMs = duration
            )
        } finally {
            _isIndexing.value = false
        }
    }
    
    suspend fun removeFile(filePath: String) = mutex.withLock {
        val entries = fileIndex.remove(filePath) ?: return@withLock
        entries.forEach { entry ->
            index[entry.name]?.removeAll { it.filePath == filePath }
            if (index[entry.name]?.isEmpty() == true) index.remove(entry.name)
        }
        updateStats()
    }
    
    suspend fun getEntry(name: String): IndexEntry? {
        return index[name]?.firstOrNull()
    }

    suspend fun getEntries(name: String): List<IndexEntry> {
        return index[name]?.toList() ?: emptyList()
    }
    
    suspend fun searchEntries(query: String): List<IndexEntry> {
        return index.values.flatten().filter { entry ->
            entry.name.contains(query, ignoreCase = true)
        }
    }
    
    suspend fun getEntriesForFile(filePath: String): List<IndexEntry> {
        return fileIndex[filePath] ?: emptyList()
    }
    
    suspend fun getAllEntries(): List<IndexEntry> {
        return index.values.flatten().toList()
    }
    
    suspend fun clearIndex() = mutex.withLock {
        index.clear()
        fileIndex.clear()
        _stats.value = IndexStats(0, 0, 0, 0)
    }
    
    fun startIndexing(files: Map<String, String>) {
        indexingJob?.cancel()
        indexingJob = scope.launch {
            indexFiles(files)
        }
    }
    
    private fun extractEntries(filePath: String, content: String): List<IndexEntry> {
        val entries = mutableListOf<IndexEntry>()
        val lines = content.lines()
        var currentScope = ""
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
            
            val classMatch = Regex("^class\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\(.*\\))?\\s*:").find(trimmed)
            if (classMatch != null) {
                val className = classMatch.groupValues[1]
                val bases = classMatch.groupValues[2].removeSurrounding("(", ")")
                    .split(",").map { it.trim() }.filter { it.isNotEmpty() }
                
                entries.add(IndexEntry(
                    name = className,
                    kind = CompletionKind.CLASS,
                    filePath = filePath,
                    line = lineIndex,
                    signature = "class $className($bases)",
                    documentation = extractDocstring(lines, lineIndex),
                    scope = currentScope
                ))
                
                currentScope = className
                return@forEachIndexed
            }
            
            val funcMatch = Regex("^\\s*(async\\s+)?def\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\((.*?)\\)\\s*(->.*?)?:").find(line)
            if (funcMatch != null) {
                val isAsync = funcMatch.groupValues[1].isNotEmpty()
                val funcName = funcMatch.groupValues[2]
                val params = funcMatch.groupValues[3]
                val returnType = funcMatch.groupValues[4].removePrefix("->").trim()
                
                val paramList = params.split(",").map { param ->
                    val parts = param.trim().split(":")
                    parts[0].trim().removePrefix("*").removePrefix("**") to 
                        parts.getOrElse(1) { "" }.trim().removePrefix("=").trim()
                }.filter { it.first.isNotEmpty() }
                
                entries.add(IndexEntry(
                    name = funcName,
                    kind = if (isAsync) CompletionKind.METHOD else CompletionKind.FUNCTION,
                    filePath = filePath,
                    line = lineIndex,
                    signature = "def $funcName($params)",
                    documentation = extractDocstring(lines, lineIndex),
                    scope = currentScope
                ))
                
                return@forEachIndexed
            }
            
            val varMatch = Regex("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)").find(line)
            if (varMatch != null) {
                val varName = varMatch.groupValues[1]
                val varValue = varMatch.groupValues[2].trim()
                
                if (!varName.startsWith("_") || varName.startsWith("__")) {
                    entries.add(IndexEntry(
                        name = varName,
                        kind = CompletionKind.VARIABLE,
                        filePath = filePath,
                        line = lineIndex,
                        type = inferType(varValue),
                        scope = currentScope
                    ))
                }
                
                return@forEachIndexed
            }
            
            val importMatch = Regex("^from\\s+(\\S+)\\s+import\\s+(.+)").find(trimmed)
            if (importMatch != null) {
                val moduleName = importMatch.groupValues[1]
                val importedNames = importMatch.groupValues[2].split(",").map { it.trim() }
                
                importedNames.forEach { name ->
                    entries.add(IndexEntry(
                        name = name,
                        kind = CompletionKind.IMPORT,
                        filePath = filePath,
                        line = lineIndex,
                        scope = currentScope
                    ))
                }
                
                return@forEachIndexed
            }
            
            val simpleImportMatch = Regex("^import\\s+(\\S+)").find(trimmed)
            if (simpleImportMatch != null) {
                val moduleName = simpleImportMatch.groupValues[1]
                entries.add(IndexEntry(
                    name = moduleName,
                    kind = CompletionKind.MODULE,
                    filePath = filePath,
                    line = lineIndex,
                    scope = currentScope
                ))
                
                return@forEachIndexed
            }
            
            if (trimmed == "class " || trimmed.endsWith("):") || trimmed.endsWith(":")) {
                if (!trimmed.startsWith("def ") && !trimmed.startsWith("class ") &&
                    !trimmed.startsWith("if ") && !trimmed.startsWith("else") &&
                    !trimmed.startsWith("elif ") && !trimmed.startsWith("for ") &&
                    !trimmed.startsWith("while ") && !trimmed.startsWith("with ") &&
                    !trimmed.startsWith("try") && !trimmed.startsWith("except") &&
                    !trimmed.startsWith("finally")) {
                    currentScope = ""
                }
            }
        }
        
        return entries
    }
    
    private fun extractDocstring(lines: List<String>, functionLine: Int): String? {
        val nextLine = lines.getOrElse(functionLine + 1) { "" }.trim()
        
        if (nextLine.startsWith("\"\"\"") || nextLine.startsWith("'''")) {
            val quote = nextLine.substring(0, 3)
            val content = StringBuilder()
            var i = functionLine + 1
            
            while (i < lines.size) {
                val line = lines[i].trim()
                if (i == functionLine + 1) {
                    content.append(line.removePrefix(quote))
                } else {
                    if (line.contains(quote)) {
                        content.append(" ").append(line.removeSuffix(quote))
                        break
                    }
                    content.append(" ").append(line)
                }
                i++
            }
            
            return content.toString().trim().ifEmpty { null }
        }
        
        return null
    }
    
    private fun inferType(value: String): String {
        return when {
            value.startsWith("\"") || value.startsWith("'") -> "str"
            value == "True" || value == "False" -> "bool"
            value == "None" -> "None"
            value.matches(Regex("^-?\\d+$")) -> "int"
            value.matches(Regex("^-?\\d+\\.\\d+$")) -> "float"
            value.startsWith("[") -> "list"
            value.startsWith("{") -> "dict"
            value.startsWith("(") -> "tuple"
            value.startsWith("set(") -> "set"
            value.contains("()") -> value.removeSuffix("()")
            else -> "Any"
        }
    }
    
    private fun updateStats() {
        _stats.value = IndexStats(
            totalFiles = fileIndex.size,
            totalEntries = index.size,
            lastUpdated = System.currentTimeMillis(),
            indexingTimeMs = 0
        )
    }
    
    fun convertToCompletionItems(): List<CompletionItem> {
        return index.values.flatten().map { entry ->
            CompletionItem(
                id = "index_${entry.name}",
                label = entry.name,
                kind = entry.kind,
                detail = entry.signature,
                documentation = entry.documentation,
                returnType = entry.type,
                priority = when (entry.kind) {
                    CompletionKind.FUNCTION -> 85
                    CompletionKind.CLASS -> 80
                    CompletionKind.MODULE -> 75
                    CompletionKind.VARIABLE -> 70
                    CompletionKind.IMPORT -> 65
                    else -> 50
                },
                filterText = entry.name,
                sortOrder = when (entry.kind) {
                    CompletionKind.FUNCTION -> 1
                    CompletionKind.CLASS -> 2
                    CompletionKind.MODULE -> 3
                    CompletionKind.VARIABLE -> 4
                    else -> 5
                }
            )
        }
    }
}
