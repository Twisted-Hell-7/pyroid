package com.pythonide.domain.model.intellisense

data class CompletionItem(
    val id: String,
    val label: String,
    val kind: CompletionKind,
    val detail: String? = null,
    val documentation: String? = null,
    val snippet: String? = null,
    val parameters: List<ParameterInfo>? = null,
    val returnType: String? = null,
    val deprecated: Boolean = false,
    val priority: Int = 0,
    val insertText: String = label,
    val filterText: String = label,
    val sortOrder: Int = 0
)

enum class CompletionKind {
    KEYWORD,
    BUILTIN,
    MODULE,
    FUNCTION,
    CLASS,
    VARIABLE,
    PARAMETER,
    PROPERTY,
    METHOD,
    SNIPPET,
    IMPORT,
    CONSTANT,
    ENUM,
    INTERFACE,
    STRUCT,
    FILE,
    FOLDER,
    COLOR,
    VALUE,
    OPERATOR,
    TYPE_PARAMETER
}

data class ParameterInfo(
    val name: String,
    val type: String? = null,
    val documentation: String? = null,
    val defaultValue: String? = null,
    val isOptional: Boolean = false
)

data class ParameterHints(
    val functionName: String,
    val parameters: List<ParameterInfo>,
    val activeParameter: Int = 0,
    val documentation: String? = null
)

data class Diagnostic(
    val id: String,
    val range: DiagnosticRange,
    val severity: DiagnosticSeverity,
    val code: String? = null,
    val source: String = "pylint",
    val message: String,
    val relatedInformation: List<RelatedInformation>? = null,
    val tags: List<DiagnosticTag>? = null,
    val quickFixes: List<QuickFix>? = null
)

data class DiagnosticRange(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
)

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
    HINT
}

enum class DiagnosticTag {
    UNNECESSARY,
    DEPRECATED
}

data class RelatedInformation(
    val location: DiagnosticRange,
    val message: String
)

data class QuickFix(
    val id: String,
    val label: String,
    val kind: QuickFixKind,
    val diagnostics: List<String>,
    val edit: TextEdit? = null,
    val command: Command? = null,
    val isPreferred: Boolean = false
)

enum class QuickFixKind {
    QUICK_FIX,
    REFACTOR,
    SOURCE,
    ERROR
}

data class TextEdit(
    val range: DiagnosticRange,
    val newText: String
)

data class Command(
    val id: String,
    val title: String,
    val arguments: List<Any>? = null
)

data class Snippet(
    val prefix: String,
    val body: String,
    val description: String,
    val scope: String = "python",
    val placeholders: List<SnippetPlaceholder> = emptyList()
)

data class SnippetPlaceholder(
    val index: Int,
    val placeholder: String,
    val defaultValue: String? = null
)

data class ImportSuggestion(
    val module: String,
    val fromModule: String? = null,
    val name: String,
    val alias: String? = null,
    val isUsed: Boolean = false,
    val line: Int = 0
)

data class IndexEntry(
    val name: String,
    val kind: CompletionKind,
    val filePath: String? = null,
    val line: Int = 0,
    val column: Int = 0,
    val signature: String? = null,
    val documentation: String? = null,
    val type: String? = null,
    val scope: String? = null,
    val isExported: Boolean = true
)

data class IndexStats(
    val totalFiles: Int,
    val totalEntries: Int,
    val lastUpdated: Long,
    val indexingTimeMs: Long
)

data class IntelliSenseConfig(
    val enableAutoComplete: Boolean = true,
    val enableParameterHints: Boolean = true,
    val enableDocumentation: Boolean = true,
    val enableSnippets: Boolean = true,
    val enableImportSuggestions: Boolean = true,
    val enableDiagnostics: Boolean = true,
    val enableLinting: Boolean = true,
    val enableQuickFixes: Boolean = true,
    val autoCompleteDelay: Long = 300,
    val maxCompletions: Int = 50,
    val enableBackgroundIndexing: Boolean = true
)

data class IntelliSenseState(
    val completions: List<CompletionItem> = emptyList(),
    val parameterHints: ParameterHints? = null,
    val diagnostics: List<Diagnostic> = emptyList(),
    val isCompletionsVisible: Boolean = false,
    val isParameterHintsVisible: Boolean = false,
    val selectedIndex: Int = 0,
    val triggerOffset: Int = 0,
    val triggerCharacter: String? = null
)
