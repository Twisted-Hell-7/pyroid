package com.pythonide.data.intellisense.providers

import com.pythonide.data.intellisense.core.DiagnosticProvider
import com.pythonide.domain.model.intellisense.Diagnostic
import com.pythonide.domain.model.intellisense.DiagnosticRange
import com.pythonide.domain.model.intellisense.DiagnosticSeverity
import com.pythonide.domain.model.intellisense.QuickFix
import com.pythonide.domain.model.intellisense.QuickFixKind
import com.pythonide.domain.model.intellisense.TextEdit

class SyntaxValidator : DiagnosticProvider {
    
    override suspend fun getDiagnostics(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        
        diagnostics.addAll(validateSyntax(content))
        diagnostics.addAll(validateIndentation(content))
        diagnostics.addAll(validateParentheses(content))
        diagnostics.addAll(validateStrings(content))
        diagnostics.addAll(validateColons(content))
        diagnostics.addAll(validateImports(content))
        
        return diagnostics
    }
    
    private fun validateSyntax(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
            
            if (trimmed.startsWith("def ") && !trimmed.endsWith(":") && !trimmed.contains("(")) {
                diagnostics.add(Diagnostic(
                    id = "syntax_def_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                    severity = DiagnosticSeverity.ERROR,
                    code = "E0001",
                    source = "syntax",
                    message = "Function definition must end with colon",
                    quickFixes = listOf(QuickFix(
                        id = "fix_def_colon_${lineIndex}",
                        label = "Add colon",
                        kind = QuickFixKind.QUICK_FIX,
                        diagnostics = listOf("E0001"),
                        edit = TextEdit(
                            DiagnosticRange(lineIndex, line.length, lineIndex, line.length),
                            ":"
                        )
                    ))
                ))
            }
            
            if (trimmed.startsWith("class ") && !trimmed.endsWith(":") && !trimmed.contains("(")) {
                diagnostics.add(Diagnostic(
                    id = "syntax_class_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                    severity = DiagnosticSeverity.ERROR,
                    code = "E0002",
                    source = "syntax",
                    message = "Class definition must end with colon",
                    quickFixes = listOf(QuickFix(
                        id = "fix_class_colon_${lineIndex}",
                        label = "Add colon",
                        kind = QuickFixKind.QUICK_FIX,
                        diagnostics = listOf("E0002"),
                        edit = TextEdit(
                            DiagnosticRange(lineIndex, line.length, lineIndex, line.length),
                            ":"
                        )
                    ))
                ))
            }
            
            if ((trimmed.startsWith("if ") || trimmed.startsWith("elif ") || 
                 trimmed.startsWith("else") || trimmed.startsWith("for ") || 
                 trimmed.startsWith("while ") || trimmed.startsWith("with ") ||
                 trimmed.startsWith("try") || trimmed.startsWith("except") ||
                 trimmed.startsWith("finally")) && 
                !trimmed.endsWith(":") && !trimmed.endsWith(":\\").trimEnd().endsWith(":"  )) {
                if (!trimmed.endsWith(":") && !trimmed.matches(Regex(".*:\\\\$"))) {
                    diagnostics.add(Diagnostic(
                        id = "syntax_block_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                        severity = DiagnosticSeverity.ERROR,
                        code = "E0003",
                        source = "syntax",
                        message = "Block statement must end with colon",
                        quickFixes = listOf(QuickFix(
                            id = "fix_block_colon_${lineIndex}",
                            label = "Add colon",
                            kind = QuickFixKind.QUICK_FIX,
                            diagnostics = listOf("E0003"),
                            edit = TextEdit(
                                DiagnosticRange(lineIndex, line.length, lineIndex, line.length),
                                ":"
                            )
                        ))
                    ))
                }
            }
        }
        
        return diagnostics
    }
    
    private fun validateIndentation(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        var expectedIndent = 0
        var inBlock = false
        
        lines.forEachIndexed { lineIndex, line ->
            if (line.isBlank()) return@forEachIndexed
            
            val indent = line.length - line.trimStart().length
            val trimmed = line.trim()
            
            if (trimmed.endsWith(":")) {
                inBlock = true
                expectedIndent = indent + 4
            } else if (inBlock && indent < expectedIndent && trimmed.isNotEmpty()) {
                if (indent % 4 != 0) {
                    diagnostics.add(Diagnostic(
                        id = "indent_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, indent),
                        severity = DiagnosticSeverity.WARNING,
                        code = "W0001",
                        source = "indentation",
                        message = "Unexpected indentation (expected ${expectedIndent} spaces)",
                        quickFixes = listOf(QuickFix(
                            id = "fix_indent_${lineIndex}",
                            label = "Fix indentation",
                            kind = QuickFixKind.QUICK_FIX,
                            diagnostics = listOf("W0001"),
                            edit = TextEdit(
                                DiagnosticRange(lineIndex, 0, lineIndex, indent),
                                " ".repeat(expectedIndent)
                            )
                        ))
                    ))
                }
            }
            
            if (trimmed.isNotEmpty() && indent < expectedIndent) {
                inBlock = false
                expectedIndent = indent
            }
        }
        
        return diagnostics
    }
    
    private fun validateParentheses(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val stack = ArrayDeque<Pair<Char, Int>>()
        var inString = false
        var stringChar = ' '
        var i = 0
        
        while (i < content.length) {
            val c = content[i]
            
            when {
                i + 2 < content.length && content.substring(i, i + 3) in listOf("\"\"\"", "'''") -> {
                    inString = !inString
                    stringChar = content[i]
                    i += 3
                    continue
                }
                !inString && (c == '\'' || c == '"') -> {
                    inString = true
                    stringChar = c
                }
                inString && c == stringChar && (i == 0 || content[i - 1] != '\\') -> {
                    inString = false
                }
                !inString -> {
                    when (c) {
                        '(', '[', '{' -> stack.addLast(c to i)
                        ')', ']', '}' -> {
                            val expected = when (c) {
                                ')' -> '('
                                ']' -> '['
                                '}' -> '{'
                                else -> ' '
                            }
                            
                            if (stack.isEmpty() || stack.last().first != expected) {
                                val line = content.substring(0, i).lines().size - 1
                                diagnostics.add(Diagnostic(
                                    id = "paren_${i}",
                                    range = DiagnosticRange(line, 0, line, 0),
                                    severity = DiagnosticSeverity.ERROR,
                                    code = "E0004",
                                    source = "syntax",
                                    message = "Unmatched '${c}'"
                                ))
                            } else {
                                stack.removeLast()
                            }
                        }
                    }
                }
            }
            i++
        }
        
        while (stack.isNotEmpty()) {
            val (char, pos) = stack.removeLast()
            val line = content.substring(0, pos).lines().size - 1
            diagnostics.add(Diagnostic(
                id = "paren_unclosed_${pos}",
                range = DiagnosticRange(line, 0, line, 0),
                severity = DiagnosticSeverity.ERROR,
                code = "E0005",
                source = "syntax",
                message = "Unclosed '${char}'"
            ))
        }
        
        return diagnostics
    }
    
    private fun validateStrings(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        var i = 0
        
        while (i < content.length) {
            val c = content[i]
            
            if (c == '\'' || c == '"') {
                val start = i
                val line = content.substring(0, i).lines().size - 1
                
                if (i + 2 < content.length && content.substring(i, i + 3).count { it == c } == 3) {
                    i += 3
                    val tripleQuote = c.toString().repeat(3)
                    while (i < content.length - 2) {
                        if (content.substring(i, i + 3) == tripleQuote) {
                            i += 3
                            break
                        }
                        i++
                    }
                    if (i >= content.length - 2 && !content.substring(i).contains(tripleQuote)) {
                        diagnostics.add(Diagnostic(
                            id = "string_unclosed_${start}",
                            range = DiagnosticRange(line, 0, line, 0),
                            severity = DiagnosticSeverity.ERROR,
                            code = "E0006",
                            source = "syntax",
                            message = "Unterminated triple-quoted string"
                        ))
                    }
                } else {
                    i++
                    while (i < content.length) {
                        if (content[i] == c && content[i - 1] != '\\') {
                            i++
                            break
                        }
                        if (content[i] == '\n') {
                            diagnostics.add(Diagnostic(
                                id = "string_newline_${start}",
                                range = DiagnosticRange(line, 0, line, 0),
                                severity = DiagnosticSeverity.ERROR,
                                code = "E0007",
                                source = "syntax",
                                message = "EOL while scanning string literal"
                            ))
                            break
                        }
                        i++
                    }
                }
            } else {
                i++
            }
        }
        
        return diagnostics
    }
    
    private fun validateColons(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.contains("= ") && !trimmed.startsWith("#") && 
                !trimmed.startsWith("def ") && !trimmed.startsWith("class ")) {
                val parts = trimmed.split("=")
                if (parts.size >= 2) {
                    val left = parts[0].trim()
                    if (left.contains(" ") && !left.contains("(")) {
                        diagnostics.add(Diagnostic(
                            id = "assign_${lineIndex}",
                            range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                            severity = DiagnosticSeverity.WARNING,
                            code = "W0002",
                            source = "lint",
                            message = "Multiple assignment targets"
                        ))
                    }
                }
            }
        }
        
        return diagnostics
    }
    
    private fun validateImports(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        val imports = mutableSetOf<String>()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.startsWith("import ")) {
                val module = trimmed.removePrefix("import ").split(" ")[0].split(",")[0].trim()
                if (module in imports) {
                    diagnostics.add(Diagnostic(
                        id = "import_dup_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                        severity = DiagnosticSeverity.WARNING,
                        code = "W0003",
                        source = "lint",
                        message = "Duplicate import of '${module}'"
                    ))
                }
                imports.add(module)
            }
        }
        
        return diagnostics
    }
}

class Linter : DiagnosticProvider {
    
    override suspend fun getDiagnostics(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        
        diagnostics.addAll(checkUnusedVariables(content))
        diagnostics.addAll(checkUnusedImports(content))
        diagnostics.addAll(checkMissingDocstrings(content))
        diagnostics.addAll(checkLineLength(content))
        diagnostics.addAll(checkNamingConventions(content))
        diagnostics.addAll(checkCommonMistakes(content))
        
        return diagnostics
    }
    
    private fun checkUnusedVariables(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        val defined = mutableMapOf<String, Int>()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            val match = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=").find(trimmed)
            if (match != null) {
                val varName = match.groupValues[1]
                if (varName !in listOf("self", "cls")) {
                    defined[varName] = lineIndex
                }
            }
        }
        
        defined.forEach { (varName, lineIndex) ->
            val usageCount = content.split(varName).size - 1
            if (usageCount == 1) {
                diagnostics.add(Diagnostic(
                    id = "unused_${varName}_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, content.lines()[lineIndex].length),
                    severity = DiagnosticSeverity.WARNING,
                    code = "W0010",
                    source = "lint",
                    message = "Variable '${varName}' is assigned but never used",
                    quickFixes = listOf(QuickFix(
                        id = "fix_unused_${varName}_${lineIndex}",
                        label = "Remove unused variable",
                        kind = QuickFixKind.QUICK_FIX,
                        diagnostics = listOf("W0010")
                    ))
                ))
            }
        }
        
        return diagnostics
    }
    
    private fun checkUnusedImports(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        val imports = mutableMapOf<String, Int>()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.startsWith("from ") && trimmed.contains(" import ")) {
                val match = Regex("from\\s+\\S+\\s+import\\s+(.+)").find(trimmed)
                if (match != null) {
                    val importedNames = match.groupValues[1].split(",").map { it.trim().split(" ")[0] }
                    importedNames.forEach { name ->
                        if (name != "*") {
                            imports[name] = lineIndex
                        }
                    }
                }
            } else if (trimmed.startsWith("import ")) {
                val moduleName = trimmed.removePrefix("import ").split(" ")[0].split(",")[0].trim()
                imports[moduleName] = lineIndex
            }
        }
        
        imports.forEach { (name, lineIndex) ->
            val usageCount = content.split(name).size - 1
            if (usageCount == 1) {
                diagnostics.add(Diagnostic(
                    id = "unused_import_${name}_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, content.lines()[lineIndex].length),
                    severity = DiagnosticSeverity.WARNING,
                    code = "W0011",
                    source = "lint",
                    message = "Unused import '${name}'",
                    quickFixes = listOf(QuickFix(
                        id = "fix_unused_import_${name}_${lineIndex}",
                        label = "Remove unused import",
                        kind = QuickFixKind.QUICK_FIX,
                        diagnostics = listOf("W0011"),
                        edit = TextEdit(
                            DiagnosticRange(lineIndex, 0, lineIndex + 1, 0),
                            ""
                        )
                    ))
                ))
            }
        }
        
        return diagnostics
    }
    
    private fun checkMissingDocstrings(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed.startsWith("def ") || trimmed.startsWith("class ")) {
                val name = if (trimmed.startsWith("def ")) {
                    trimmed.removePrefix("def ").split("(")[0].trim()
                } else {
                    trimmed.removePrefix("class ").split("(")[0].split(":")[0].trim()
                }
                
                if (name.startsWith("_")) continue
                
                val nextLine = lines.getOrElse(lineIndex + 1) { "" }.trim()
                if (!nextLine.startsWith("\"\"\"") && !nextLine.startsWith("'''")) {
                    diagnostics.add(Diagnostic(
                        id = "docstring_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                        severity = DiagnosticSeverity.INFO,
                        code = "I0001",
                        source = "lint",
                        message = "Missing docstring for '${name}'"
                    ))
                }
            }
        }
        
        return diagnostics
    }
    
    private fun checkLineLength(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        val maxLength = 79
        
        lines.forEachIndexed { lineIndex, line ->
            if (line.length > maxLength && !line.trimStart().startsWith("#")) {
                diagnostics.add(Diagnostic(
                    id = "line_length_${lineIndex}",
                    range = DiagnosticRange(lineIndex, maxLength, lineIndex, line.length),
                    severity = DiagnosticSeverity.WARNING,
                    code = "W0012",
                    source = "lint",
                    message = "Line too long (${line.length} > ${maxLength} characters)"
                ))
            }
        }
        
        return diagnostics
    }
    
    private fun checkNamingConventions(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            val classMatch = Regex("^class\\s+([a-zA-Z_][a-zA-Z0-9_]*)").find(trimmed)
            if (classMatch != null) {
                val className = classMatch.groupValues[1]
                if (className[0].isLowerCase() && !className.startsWith("_")) {
                    diagnostics.add(Diagnostic(
                        id = "class_name_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                        severity = DiagnosticSeverity.WARNING,
                        code = "W0013",
                        source = "lint",
                        message = "Class name '${className}' should use CamelCase"
                    ))
                }
            }
            
            val defMatch = Regex("^\\s*def\\s+([a-zA-Z_][a-zA-Z0-9_]*)").find(line)
            if (defMatch != null) {
                val funcName = defMatch.groupValues[1]
                if (funcName[0].isUpperCase() && !funcName.startsWith("__")) {
                    diagnostics.add(Diagnostic(
                        id = "func_name_${lineIndex}",
                        range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                        severity = DiagnosticSeverity.WARNING,
                        code = "W0014",
                        source = "lint",
                        message = "Function name '${funcName}' should use snake_case"
                    ))
                }
            }
        }
        
        return diagnostics
    }
    
    private fun checkCommonMistakes(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lines = content.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmed = line.trim()
            
            if (trimmed == "except:") {
                diagnostics.add(Diagnostic(
                    id = "bare_except_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                    severity = DiagnosticSeverity.WARNING,
                    code = "W0015",
                    source = "lint",
                    message = "Bare except clause, consider specifying exception type"
                ))
            }
            
            if (trimmed.contains("== True") || trimmed.contains("== False") || 
                trimmed.contains("== None")) {
                diagnostics.add(Diagnostic(
                    id = "comparison_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                    severity = DiagnosticSeverity.WARNING,
                    code = "W0016",
                    source = "lint",
                    message = "Use 'is' for comparison with True/False/None"
                ))
            }
            
            if (trimmed == "except Exception:") {
                diagnostics.add(Diagnostic(
                    id = "broad_except_${lineIndex}",
                    range = DiagnosticRange(lineIndex, 0, lineIndex, line.length),
                    severity = DiagnosticSeverity.INFO,
                    code = "I0002",
                    source = "lint",
                    message = "Broad exception clause, consider specifying exception type"
                ))
            }
        }
        
        return diagnostics
    }
}
