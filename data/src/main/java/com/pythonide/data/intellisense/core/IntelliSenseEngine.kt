package com.pythonide.data.intellisense.core

import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.Diagnostic
import com.pythonide.domain.model.intellisense.IntelliSenseConfig
import com.pythonide.domain.model.intellisense.IntelliSenseState
import com.pythonide.domain.model.intellisense.ParameterHints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IntelliSenseEngine {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    
    private val _state = MutableStateFlow(IntelliSenseState())
    val state: StateFlow<IntelliSenseState> = _state.asStateFlow()
    
    private val _config = MutableStateFlow(IntelliSenseConfig())
    val config: StateFlow<IntelliSenseConfig> = _config.asStateFlow()
    
    private var autoCompleteJob: Job? = null
    private var diagnosticsJob: Job? = null
    
    private val completionProviders = mutableListOf<CompletionProvider>()
    private val diagnosticProviders = mutableListOf<DiagnosticProvider>()
    
    private val completionCache = mutableMapOf<String, List<CompletionItem>>()
    private val symbolIndex = mutableMapOf<String, List<CompletionItem>>()
    
    fun registerCompletionProvider(provider: CompletionProvider) {
        completionProviders.add(provider)
    }
    
    fun registerDiagnosticProvider(provider: DiagnosticProvider) {
        diagnosticProviders.add(provider)
    }
    
    suspend fun triggerCompletion(
        content: String,
        cursorOffset: Int,
        triggerCharacter: String? = null
    ) = mutex.withLock {
        if (!_config.value.enableAutoComplete) return@withLock
        
        autoCompleteJob?.cancel()
        
        autoCompleteJob = scope.launch {
            delay(_config.value.autoCompleteDelay)
            
            val prefix = extractPrefix(content, cursorOffset)
            val context = extractContext(content, cursorOffset)
            
            val completions = mutableListOf<CompletionItem>()
            
            for (provider in completionProviders) {
                val providerCompletions = provider.getCompletions(
                    content = content,
                    cursorOffset = cursorOffset,
                    prefix = prefix,
                    context = context,
                    triggerCharacter = triggerCharacter
                )
                completions.addAll(providerCompletions)
            }
            
            val filtered = filterCompletions(completions, prefix)
                .sortedByDescending { it.priority }
                .take(_config.value.maxCompletions)
            
            _state.update {
                it.copy(
                    completions = filtered,
                    isCompletionsVisible = filtered.isNotEmpty(),
                    selectedIndex = 0,
                    triggerOffset = cursorOffset - prefix.length,
                    triggerCharacter = triggerCharacter
                )
            }
        }
    }
    
    suspend fun triggerParameterHints(
        content: String,
        cursorOffset: Int
    ) = mutex.withLock {
        if (!_config.value.enableParameterHints) return@withLock
        
        val hints = extractParameterHints(content, cursorOffset)
        
        _state.update {
            it.copy(
                parameterHints = hints,
                isParameterHintsVisible = hints != null
            )
        }
    }
    
    suspend fun updateDiagnostics(content: String) = mutex.withLock {
        if (!_config.value.enableDiagnostics) return@withLock
        
        diagnosticsJob?.cancel()
        
        diagnosticsJob = scope.launch {
            val diagnostics = mutableListOf<Diagnostic>()
            
            for (provider in diagnosticProviders) {
                val providerDiagnostics = provider.getDiagnostics(content)
                diagnostics.addAll(providerDiagnostics)
            }
            
            _state.update {
                it.copy(diagnostics = diagnostics)
            }
        }
    }
    
    fun hideCompletions() {
        _state.update {
            it.copy(
                isCompletionsVisible = false,
                completions = emptyList()
            )
        }
    }
    
    fun hideParameterHints() {
        _state.update {
            it.copy(
                isParameterHintsVisible = false,
                parameterHints = null
            )
        }
    }
    
    fun selectNextCompletion() {
        _state.update { state ->
            val nextIndex = (state.selectedIndex + 1).coerceAtMost(state.completions.size - 1)
            state.copy(selectedIndex = nextIndex)
        }
    }
    
    fun selectPreviousCompletion() {
        _state.update { state ->
            val prevIndex = (state.selectedIndex - 1).coerceAtLeast(0)
            state.copy(selectedIndex = prevIndex)
        }
    }
    
    fun getSelectedCompletion(): CompletionItem? {
        return _state.value.completions.getOrNull(_state.value.selectedIndex)
    }
    
    fun acceptCompletion(): CompletionItem? {
        val selected = getSelectedCompletion() ?: return null
        hideCompletions()
        return selected
    }
    
    fun updateConfig(config: IntelliSenseConfig) {
        _config.value = config
    }
    
    private fun extractPrefix(content: String, offset: Int): String {
        if (offset == 0) return ""
        
        var start = offset - 1
        while (start >= 0 && (content[start].isLetterOrDigit() || content[start] == '_')) {
            start--
        }
        
        return content.substring(start + 1, offset)
    }
    
    private fun extractContext(content: String, offset: Int): CompletionContext {
        val lines = content.substring(0, offset).lines()
        val currentLine = lines.lastOrNull() ?: ""
        val lineIndex = lines.size - 1
        
        val isAfterDot = currentLine.isNotEmpty() && currentLine.last() == '.'
        val isAfterImport = currentLine.trimStart().startsWith("import ") || 
                           currentLine.trimStart().startsWith("from ")
        val isAfterFrom = currentLine.trimStart().startsWith("from ")
        val isAfterDef = currentLine.trimStart().startsWith("def ")
        val isAfterClass = currentLine.trimStart().startsWith("class ")
        val isInString = isInString(content, offset)
        val isAfterComment = currentLine.contains('#')
        
        val depth = calculateScopeDepth(content, offset)
        
        return CompletionContext(
            line = lineIndex,
            column = currentLine.length,
            isAfterDot = isAfterDot,
            isAfterImport = isAfterImport,
            isAfterFrom = isAfterFrom,
            isAfterDef = isAfterDef,
            isAfterClass = isAfterClass,
            isInString = isInString,
            isAfterComment = isAfterComment,
            scopeDepth = depth,
            currentLine = currentLine
        )
    }
    
    private fun isInString(content: String, offset: Int): Boolean {
        var inSingleQuote = false
        var inDoubleQuote = false
        var inTripleSingle = false
        var inTripleDouble = false
        var i = 0
        
        while (i < offset && i < content.length) {
            val c = content[i]
            
            when {
                i + 2 < content.length && content.substring(i, i + 3) == "'''" -> {
                    inTripleSingle = !inTripleSingle
                    i += 3
                    continue
                }
                i + 2 < content.length && content.substring(i, i + 3) == "\"\"\"" -> {
                    inTripleDouble = !inTripleDouble
                    i += 3
                    continue
                }
                c == '\'' && !inDoubleQuote && !inTripleSingle && !inTripleDouble -> {
                    inSingleQuote = !inSingleQuote
                }
                c == '"' && !inSingleQuote && !inTripleSingle && !inTripleDouble -> {
                    inDoubleQuote = !inDoubleQuote
                }
                c == '\\' && (inSingleQuote || inDoubleQuote) -> {
                    i++
                }
            }
            i++
        }
        
        return inSingleQuote || inDoubleQuote || inTripleSingle || inTripleDouble
    }
    
    private fun calculateScopeDepth(content: String, offset: Int): Int {
        var depth = 0
        var i = 0
        
        while (i < offset && i < content.length) {
            val c = content[i]
            when (c) {
                ':' -> {
                    if (i + 1 < content.length && content[i + 1] == '\n') {
                        depth++
                    }
                }
                '\n' -> {
                    val lineStart = content.lastIndexOf('\n', i - 1) + 1
                    val line = content.substring(lineStart, i)
                    val trimmed = line.trimStart()
                    val indent = line.length - trimmed.length
                    val expectedIndent = depth * 4
                    if (indent < expectedIndent) {
                        depth = indent / 4
                    }
                }
            }
            i++
        }
        
        return depth
    }
    
    private fun filterCompletions(completions: List<CompletionItem>, prefix: String): List<CompletionItem> {
        if (prefix.isEmpty()) return completions
        
        return completions.filter { item ->
            item.filterText.startsWith(prefix, ignoreCase = true) ||
            item.label.startsWith(prefix, ignoreCase = true)
        }
    }
    
    private fun extractParameterHints(content: String, offset: Int): ParameterHints? {
        val beforeCursor = content.substring(0, offset)
        
        val lastOpenParen = beforeCursor.lastIndexOf('(')
        if (lastOpenParen == -1) return null
        
        val beforeParen = beforeCursor.substring(0, lastOpenParen)
        val functionName = extractFunctionName(beforeParen)
        
        if (functionName.isEmpty()) return null
        
        val activeParameter = countCommasBefore(beforeCursor, lastOpenParen)
        
        val functionItem = findFunctionInIndex(functionName)
        
        return ParameterHints(
            functionName = functionName,
            parameters = functionItem?.parameters ?: emptyList(),
            activeParameter = activeParameter,
            documentation = functionItem?.documentation
        )
    }
    
    private fun extractFunctionName(text: String): String {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return ""
        
        var end = trimmed.length
        while (end > 0 && (trimmed[end - 1].isLetterOrDigit() || trimmed[end - 1] == '_')) {
            end--
        }
        
        return trimmed.substring(end)
    }
    
    private fun countCommasBefore(text: String, position: Int): Int {
        var count = 0
        var depth = 0
        var i = 0
        
        while (i < position && i < text.length) {
            when (text[i]) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) count++
            }
            i++
        }
        
        return count
    }
    
    private fun findFunctionInIndex(name: String): CompletionItem? {
        return symbolIndex.values.flatten().find { 
            it.name == name && (it.kind == com.pythonide.domain.model.intellisense.CompletionKind.FUNCTION ||
                               it.kind == com.pythonide.domain.model.intellisense.CompletionKind.METHOD)
        }
    }
    
    fun addToIndex(entries: List<CompletionItem>) {
        entries.forEach { entry ->
            val key = entry.name.firstOrNull()?.lowercase() ?: "_"
            val existing = symbolIndex[key] ?: emptyList()
            symbolIndex[key] = existing + entry
        }
    }
    
    fun clearIndex() {
        symbolIndex.clear()
        completionCache.clear()
    }
    
    fun getIndexStats(): Pair<Int, Int> {
        val totalEntries = symbolIndex.values.sumOf { it.size }
        return Pair(symbolIndex.size, totalEntries)
    }
}

data class CompletionContext(
    val line: Int,
    val column: Int,
    val isAfterDot: Boolean,
    val isAfterImport: Boolean,
    val isAfterFrom: Boolean,
    val isAfterDef: Boolean,
    val isAfterClass: Boolean,
    val isInString: Boolean,
    val isAfterComment: Boolean,
    val scopeDepth: Int,
    val currentLine: String
)

interface CompletionProvider {
    suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem>
}

interface DiagnosticProvider {
    suspend fun getDiagnostics(content: String): List<Diagnostic>
}
