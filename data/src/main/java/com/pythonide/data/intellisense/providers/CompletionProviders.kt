package com.pythonide.data.intellisense.providers

import com.pythonide.data.intellisense.core.CompletionContext
import com.pythonide.data.intellisense.core.CompletionProvider
import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.ParameterInfo

class KeywordProvider : CompletionProvider {
    
    private val keywords = mapOf(
        "False" to "Boolean value False",
        "None" to "None type",
        "True" to "Boolean value True",
        "and" to "Logical AND",
        "as" to "Create alias for import",
        "assert" to "Assert condition is true",
        "async" to "Define asynchronous function",
        "await" to "Wait for asynchronous result",
        "break" to "Exit loop",
        "class" to "Define class",
        "continue" to "Skip to next iteration",
        "def" to "Define function",
        "del" to "Delete variable",
        "elif" to "Else if condition",
        "else" to "Else block",
        "except" to "Handle exception",
        "finally" to "Always execute block",
        "for" to "Iterate over sequence",
        "from" to "Import specific names",
        "global" to "Declare global variable",
        "if" to "Conditional statement",
        "import" to "Import module",
        "in" to "Membership test",
        "is" to "Identity test",
        "lambda" to "Create anonymous function",
        "nonlocal" to "Declare non-local variable",
        "not" to "Logical NOT",
        "or" to "Logical OR",
        "pass" to "No operation",
        "raise" to "Raise exception",
        "return" to "Return value from function",
        "try" to "Try block for exceptions",
        "while" to "While loop",
        "with" to "Context manager",
        "yield" to "Yield value from generator"
    )
    
    override suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem> {
        if (context.isInString || context.isAfterComment) return emptyList()
        if (context.isAfterDot || context.isAfterImport) return emptyList()
        
        return keywords.map { (keyword, doc) ->
            CompletionItem(
                id = "keyword_$keyword",
                label = keyword,
                kind = CompletionKind.KEYWORD,
                documentation = doc,
                priority = 100,
                filterText = keyword,
                sortOrder = 0
            )
        }
    }
}

class BuiltinProvider : CompletionProvider {
    
    private val builtins = mapOf(
        "abs" to BuiltinInfo("Absolute value", listOf("x"), "The absolute value of a number"),
        "all" to BuiltinInfo("Check all true", listOf("iterable"), "True if all elements are true"),
        "any" to BuiltinInfo("Check any true", listOf("iterable"), "True if any element is true"),
        "bin" to BuiltinInfo("Binary representation", listOf("number"), "Binary string of integer"),
        "bool" to BuiltinInfo("Boolean type", listOf("value"), "Boolean value"),
        "bytearray" to BuiltinInfo("Mutable bytes", listOf("source", "encoding"), "Mutable sequence of bytes"),
        "bytes" to BuiltinInfo("Immutable bytes", listOf("source", "encoding"), "Immutable sequence of bytes"),
        "callable" to BuiltinInfo("Check callable", listOf("object"), "True if object is callable"),
        "chr" to BuiltinInfo("Character from code", listOf("i"), "Character from Unicode code point"),
        "classmethod" to BuiltinInfo("Class method", listOf("function"), "Create class method"),
        "compile" to BuiltinInfo("Compile source", listOf("source", "filename", "mode"), "Compile code to AST"),
        "complex" to BuiltinInfo("Complex number", listOf("real", "imag"), "Complex number type"),
        "delattr" to BuiltinInfo("Delete attribute", listOf("object", "name"), "Delete attribute from object"),
        "dict" to BuiltinInfo("Dictionary", listOf("iterable"), "Dictionary type"),
        "dir" to BuiltinInfo("Directory listing", listOf("object"), "List of attributes"),
        "divmod" to BuiltinInfo("Division modulo", listOf("a", "b"), "Tuple of (quotient, remainder)"),
        "enumerate" to BuiltinInfo("Enumerate", listOf("iterable", "start"), "Enumerate with index"),
        "eval" to BuiltinInfo("Evaluate expression", listOf("expression", "globals", "locals"), "Evaluate expression"),
        "exec" to BuiltinInfo("Execute code", listOf("object", "globals", "locals"), "Execute dynamically created code"),
        "filter" to BuiltinInfo("Filter", listOf("function", "iterable"), "Filter elements"),
        "float" to BuiltinInfo("Float type", listOf("x"), "Floating point number"),
        "format" to BuiltinInfo("Format value", listOf("value", "format_spec"), "Format value"),
        "frozenset" to BuiltinInfo("Frozen set", listOf("iterable"), "Immutable set"),
        "getattr" to BuiltinInfo("Get attribute", listOf("object", "name"), "Get attribute value"),
        "globals" to BuiltinInfo("Global dictionary", emptyList(), "Global symbol table"),
        "hasattr" to BuiltinInfo("Has attribute", listOf("object", "name"), "Check if attribute exists"),
        "hash" to BuiltinInfo("Hash value", listOf("object"), "Hash value of object"),
        "help" to BuiltinInfo("Help", listOf("object"), "Display help"),
        "hex" to BuiltinInfo("Hexadecimal", listOf("number"), "Hexadecimal string"),
        "id" to BuiltinInfo("Object identity", listOf("object"), "Unique identifier"),
        "input" to BuiltinInfo("User input", listOf("prompt"), "Read user input"),
        "int" to BuiltinInfo("Integer type", listOf("x", "base"), "Integer number"),
        "isinstance" to BuiltinInfo("Is instance", listOf("object", "classinfo"), "Check instance type"),
        "issubclass" to BuiltinInfo("Is subclass", listOf("class", "classinfo"), "Check subclass relationship"),
        "iter" to BuiltinInfo("Iterator", listOf("object"), "Create iterator"),
        "len" to BuiltinInfo("Length", listOf("s"), "Length of object"),
        "list" to BuiltinInfo("List", listOf("iterable"), "List type"),
        "locals" to BuiltinInfo("Local dictionary", emptyList(), "Local symbol table"),
        "map" to BuiltinInfo("Map", listOf("function", "iterable"), "Apply function to items"),
        "max" to BuiltinInfo("Maximum", listOf("iterable", "key"), "Largest item"),
        "memoryview" to BuiltinInfo("Memory view", listOf("object"), "Memory view object"),
        "min" to BuiltinInfo("Minimum", listOf("iterable", "key"), "Smallest item"),
        "next" to BuiltinInfo("Next", listOf("iterator", "default"), "Get next item"),
        "object" to BuiltinInfo("Base object", emptyList(), "Base class"),
        "oct" to BuiltinInfo("Octal", listOf("number"), "Octal string"),
        "open" to BuiltinInfo("Open file", listOf("file", "mode", "buffering"), "Open file"),
        "ord" to BuiltinInfo("Ordinal", listOf("c"), "Unicode code point"),
        "pow" to BuiltinInfo("Power", listOf("base", "exp", "mod"), "Power function"),
        "print" to BuiltinInfo("Print", listOf("sep", "end"), "Print to stdout"),
        "property" to BuiltinInfo("Property", listOf("fget", "fset", "fdel"), "Property decorator"),
        "range" to BuiltinInfo("Range", listOf("start", "stop", "step"), "Range sequence"),
        "repr" to BuiltinInfo("Representation", listOf("object"), "String representation"),
        "reversed" to BuiltinInfo("Reversed", listOf("seq"), "Reverse iterator"),
        "round" to BuiltinInfo("Round", listOf("number", "ndigits"), "Round number"),
        "set" to BuiltinInfo("Set", listOf("iterable"), "Set type"),
        "setattr" to BuiltinInfo("Set attribute", listOf("object", "name", "value"), "Set attribute"),
        "slice" to BuiltinInfo("Slice", listOf("start", "stop", "step"), "Slice object"),
        "sorted" to BuiltinInfo("Sorted", listOf("iterable", "key", "reverse"), "Sorted list"),
        "staticmethod" to BuiltinInfo("Static method", listOf("function"), "Create static method"),
        "str" to BuiltinInfo("String", listOf("object"), "String type"),
        "sum" to BuiltinInfo("Sum", listOf("iterable", "start"), "Sum of items"),
        "super" to BuiltinInfo("Super", emptyList(), "Parent class proxy"),
        "tuple" to BuiltinInfo("Tuple", listOf("iterable"), "Tuple type"),
        "type" to BuiltinInfo("Type", listOf("object"), "Type of object"),
        "vars" to BuiltinInfo("Variables", listOf("object"), "Object __dict__"),
        "zip" to BuiltinInfo("Zip", listOf("iterable"), "Zip iterables"),
        "__import__" to BuiltinInfo("Import", listOf("name"), "Import module")
    )
    
    override suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem> {
        if (context.isInString || context.isAfterComment) return emptyList()
        if (context.isAfterDot) return emptyList()
        
        return builtins.map { (name, info) ->
            CompletionItem(
                id = "builtin_$name",
                label = name,
                kind = CompletionKind.BUILTIN,
                detail = info.signature,
                documentation = info.documentation,
                parameters = info.parameters.map { param ->
                    ParameterInfo(name = param)
                },
                priority = 90,
                filterText = name,
                sortOrder = 1
            )
        }
    }
    
    private data class BuiltinInfo(
        val documentation: String,
        val parameters: List<String>,
        val description: String
    )
}

class ModuleProvider : CompletionProvider {
    
    private val stdlibModules = listOf(
        "abc", "aifc", "argparse", "array", "ast", "asynchat", "asyncio", "asyncore",
        "atexit", "audioop", "base64", "bdb", "binascii", "binhex", "bisect",
        "builtins", "bz2", "calendar", "cgi", "cgitb", "chunk", "cmath", "cmd",
        "code", "codecs", "codeop", "collections", "colorsys", "compileall", "concurrent",
        "configparser", "contextlib", "contextvars", "copy", "copyreg", "cProfile",
        "crypt", "csv", "ctypes", "curses", "dataclasses", "datetime", "dbm", "decimal",
        "difflib", "dis", "distutils", "doctest", "email", "encodings", "enum",
        "errno", "faulthandler", "fcntl", "filecmp", "fileinput", "fnmatch", "fractions",
        "ftplib", "functools", "gc", "getopt", "getpass", "gettext", "glob", "graphlib",
        "grp", "gzip", "hashlib", "heapq", "hmac", "html", "http", "idlelib",
        "imaplib", "imghdr", "imp", "importlib", "inspect", "io", "ipaddress", "itertools",
        "json", "keyword", "lib2to3", "linecache", "locale", "logging", "lzma",
        "mailbox", "mailcap", "marshal", "math", "mimetypes", "mmap", "modulefinder",
        "multiprocessing", "netrc", "nis", "nntplib", "numbers", "operator", "optparse",
        "os", "ossaudiodev", "pathlib", "pdb", "pickle", "pickletools", "pipes",
        "pkgutil", "platform", "plistlib", "poplib", "posix", "posixpath", "pprint",
        "profile", "pstats", "pty", "pwd", "py_compile", "pyclbr", "pydoc",
        "queue", "quopri", "random", "re", "readline", "reprlib", "resource",
        "rlcompleter", "runpy", "sched", "secrets", "select", "selectors", "shelve",
        "shlex", "shutil", "signal", "site", "smtpd", "smtplib", "sndhdr",
        "socket", "socketserver", "sqlite3", "ssl", "stat", "statistics", "string",
        "stringprep", "struct", "subprocess", "sunau", "symtable", "sys", "sysconfig",
        "syslog", "tabnanny", "tarfile", "telnetlib", "tempfile", "termios", "test",
        "textwrap", "threading", "time", "timeit", "tkinter", "token", "tokenize",
        "tomllib", "trace", "traceback", "tracemalloc", "tty", "turtle", "types",
        "typing", "unicodedata", "unittest", "urllib", "uu", "uuid", "venv",
        "warnings", "wave", "weakref", "webbrowser", "winreg", "winsound", "wsgiref",
        "xdrlib", "xml", "xmlrpc", "zipapp", "zipfile", "zipimport", "zlib"
    )
    
    private val popularModules = mapOf(
        "os" to "Operating system interface",
        "sys" to "System-specific parameters",
        "json" to "JSON encoder and decoder",
        "re" to "Regular expressions",
        "math" to "Mathematical functions",
        "datetime" to "Date and time",
        "collections" to "Container datatypes",
        "itertools" to "Iterator building blocks",
        "functools" to "Higher-order functions",
        "pathlib" to "Object-oriented paths",
        "typing" to "Type hints",
        "logging" to "Logging facility",
        "unittest" to "Unit testing",
        "os.path" to "Common pathname manipulations",
        "shutil" to "High-level file operations",
        "subprocess" to "Subprocess management",
        "threading" to "Thread-based parallelism",
        "multiprocessing" to "Process-based parallelism",
        "socket" to "Low-level networking",
        "http" to "HTTP modules",
        "urllib" to "URL handling",
        "sqlite3" to "SQLite database",
        "csv" to "CSV file reading/writing",
        "xml" to "XML processing",
        "ast" to "Abstract Syntax Trees",
        "pickle" to "Object serialization",
        "hashlib" to "Secure hashes",
        "secrets" to "Secure random numbers",
        "random" to "Generate random numbers",
        "statistics" to "Mathematical statistics"
    )
    
    override suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem> {
        if (context.isInString || context.isAfterComment) return emptyList()
        
        if (context.isAfterImport || context.isAfterFrom) {
            return stdlibModules.map { module ->
                CompletionItem(
                    id = "module_$module",
                    label = module,
                    kind = CompletionKind.MODULE,
                    detail = popularModules.getOrDefault(module, "Standard library module"),
                    documentation = popularModules[module],
                    priority = 80,
                    filterText = module,
                    sortOrder = 2
                )
            }
        }
        
        if (context.isAfterDot) {
            return emptyList()
        }
        
        return stdlibModules.map { module ->
            CompletionItem(
                id = "module_$module",
                label = module,
                kind = CompletionKind.MODULE,
                detail = popularModules.getOrDefault(module, "Standard library module"),
                documentation = popularModules[module],
                priority = 80,
                filterText = module,
                sortOrder = 2
            )
        }
    }
}

class FunctionProvider : CompletionProvider {
    
    private val commonFunctions = listOf(
        FunctionInfo("print", "Print objects to the text stream", listOf("objects"), "None", "Built-in function"),
        FunctionInfo("input", "Read a string from standard input", listOf("prompt"), "str", "Built-in function"),
        FunctionInfo("len", "Return the number of items in a container", listOf("s"), "int", "Built-in function"),
        FunctionInfo("range", "Create a range object", listOf("start", "stop", "step"), "range", "Built-in function"),
        FunctionInfo("int", "Create an integer number", listOf("x", "base"), "int", "Built-in function"),
        FunctionInfo("float", "Create a floating point number", listOf("x"), "float", "Built-in function"),
        FunctionInfo("str", "Create a string object", listOf("object"), "str", "Built-in function"),
        FunctionInfo("list", "Create a list object", listOf("iterable"), "list", "Built-in function"),
        FunctionInfo("dict", "Create a dictionary object", listOf("iterable"), "dict", "Built-in function"),
        FunctionInfo("set", "Create a set object", listOf("iterable"), "set", "Built-in function"),
        FunctionInfo("tuple", "Create a tuple object", listOf("iterable"), "tuple", "Built-in function"),
        FunctionInfo("bool", "Create a boolean value", listOf("value"), "bool", "Built-in function"),
        FunctionInfo("type", "Return the type of an object", listOf("object"), "type", "Built-in function"),
        FunctionInfo("isinstance", "Check if object is an instance", listOf("object", "classinfo"), "bool", "Built-in function"),
        FunctionInfo("sorted", "Return a sorted list", listOf("iterable", "key", "reverse"), "list", "Built-in function"),
        FunctionInfo("reversed", "Return a reverse iterator", listOf("seq"), "reversed", "Built-in function"),
        FunctionInfo("enumerate", "Return an enumerate object", listOf("iterable", "start"), "enumerate", "Built-in function"),
        FunctionInfo("zip", "Combine iterables", listOf("iterable"), "zip", "Built-in function"),
        FunctionInfo("map", "Apply function to items", listOf("function", "iterable"), "map", "Built-in function"),
        FunctionInfo("filter", "Filter items", listOf("function", "iterable"), "filter", "Built-in function"),
        FunctionInfo("sum", "Sum the items", listOf("iterable", "start"), "int", "Built-in function"),
        FunctionInfo("min", "Return the smallest item", listOf("iterable", "key"), "Any", "Built-in function"),
        FunctionInfo("max", "Return the largest item", listOf("iterable", "key"), "Any", "Built-in function"),
        FunctionInfo("abs", "Return the absolute value", listOf("x"), "int", "Built-in function"),
        FunctionInfo("round", "Round a number", listOf("number", "ndigits"), "int", "Built-in function"),
        FunctionInfo("pow", "Return power", listOf("base", "exp", "mod"), "int", "Built-in function"),
        FunctionInfo("divmod", "Return quotient and remainder", listOf("a", "b"), "tuple", "Built-in function"),
        FunctionInfo("open", "Open a file", listOf("file", "mode", "buffering"), "io.TextIOWrapper", "Built-in function"),
        FunctionInfo("print", "Print to stdout", listOf("objects"), "None", "Built-in function"),
        FunctionInfo("input", "Read from stdin", listOf("prompt"), "str", "Built-in function"),
        FunctionInfo("help", "Display help", listOf("object"), "None", "Built-in function"),
        FunctionInfo("dir", "List attributes", listOf("object"), "list", "Built-in function"),
        FunctionInfo("vars", "Return __dict__", listOf("object"), "dict", "Built-in function"),
        FunctionInfo("globals", "Return global dictionary", emptyList(), "dict", "Built-in function"),
        FunctionInfo("locals", "Return local dictionary", emptyList(), "dict", "Built-in function"),
        FunctionInfo("id", "Return object identity", listOf("object"), "int", "Built-in function"),
        FunctionInfo("hash", "Return hash value", listOf("object"), "int", "Built-in function"),
        FunctionInfo("callable", "Check if callable", listOf("object"), "bool", "Built-in function"),
        FunctionInfo("chr", "Return character", listOf("i"), "str", "Built-in function"),
        FunctionInfo("ord", "Return ordinal", listOf("c"), "int", "Built-in function"),
        FunctionInfo("bin", "Return binary string", listOf("number"), "str", "Built-in function"),
        FunctionInfo("oct", "Return octal string", listOf("number"), "str", "Built-in function"),
        FunctionInfo("hex", "Return hex string", listOf("number"), "str", "Built-in function"),
        FunctionInfo("format", "Format value", listOf("value", "format_spec"), "str", "Built-in function"),
        FunctionInfo("repr", "Return string repr", listOf("object"), "str", "Built-in function"),
        FunctionInfo("ascii", "Return ASCII repr", listOf("object"), "str", "Built-in function"),
        FunctionInfo("exec", "Execute code", listOf("object"), "None", "Built-in function"),
        FunctionInfo("eval", "Evaluate expression", listOf("expression"), "Any", "Built-in function"),
        FunctionInfo("compile", "Compile source", listOf("source", "filename", "mode"), "code", "Built-in function")
    )
    
    override suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem> {
        if (context.isInString || context.isAfterComment) return emptyList()
        
        return commonFunctions.map { func ->
            CompletionItem(
                id = "func_${func.name}",
                label = func.name,
                kind = CompletionKind.FUNCTION,
                detail = func.signature,
                documentation = "${func.description}\n\nReturns: ${func.returns}",
                parameters = func.parameters.map { param ->
                    ParameterInfo(name = param)
                },
                returnType = func.returns,
                priority = 85,
                filterText = func.name,
                sortOrder = 1
            )
        }
    }
    
    private data class FunctionInfo(
        val name: String,
        val description: String,
        val parameters: List<String>,
        val returns: String,
        val category: String
    )
    
    private val FunctionInfo.signature: String
        get() = "$name(${parameters.joinToString(", ")})"
}
