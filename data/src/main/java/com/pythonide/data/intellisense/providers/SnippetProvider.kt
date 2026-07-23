package com.pythonide.data.intellisense.providers

import com.pythonide.data.intellisense.core.CompletionContext
import com.pythonide.data.intellisense.core.CompletionProvider
import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.Snippet
import com.pythonide.domain.model.intellisense.SnippetPlaceholder

class SnippetProvider : CompletionProvider {
    
    private val snippets = listOf(
        Snippet(
            prefix = "def",
            body = "def ${"$"}1(${"$"}2):\n    ${"$"}0",
            description = "Define function",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "parameters"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "class",
            body = "class ${"$"}1:\n    def __init__(self${"$"}2):\n        ${"$"}0",
            description = "Define class",
            placeholders = listOf(
                SnippetPlaceholder(1, "ClassName"),
                SnippetPlaceholder(2, "args"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "if",
            body = "if ${"$"}1:\n    ${"$"}0",
            description = "If statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "condition"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "elif",
            body = "elif ${"$"}1:\n    ${"$"}0",
            description = "Elif statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "condition"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "else",
            body = "else:\n    ${"$"}0",
            description = "Else statement",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "for",
            body = "for ${"$"}1 in ${"$"}2:\n    ${"$"}0",
            description = "For loop",
            placeholders = listOf(
                SnippetPlaceholder(1, "item"),
                SnippetPlaceholder(2, "iterable"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "while",
            body = "while ${"$"}1:\n    ${"$"}0",
            description = "While loop",
            placeholders = listOf(
                SnippetPlaceholder(1, "condition"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "try",
            body = "try:\n    ${"$"}1\nexcept ${"$"}2 as ${"$"}3:\n    ${"$"}0",
            description = "Try-except block",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass"),
                SnippetPlaceholder(1, "code"),
                SnippetPlaceholder(2, "Exception"),
                SnippetPlaceholder(3, "e")
            )
        ),
        Snippet(
            prefix = "tryf",
            body = "try:\n    ${"$"}1\nexcept ${"$"}2 as ${"$"}3:\n    ${"$"}4\nfinally:\n    ${"$"}0",
            description = "Try-except-finally block",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass"),
                SnippetPlaceholder(1, "code"),
                SnippetPlaceholder(2, "Exception"),
                SnippetPlaceholder(3, "e"),
                SnippetPlaceholder(4, "handle")
            )
        ),
        Snippet(
            prefix = "with",
            body = "with ${"$"}1 as ${"$"}2:\n    ${"$"}0",
            description = "With statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "expression"),
                SnippetPlaceholder(2, "variable"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "withopen",
            body = "with open(${ "$"}1, '${"$"}2') as ${"$"}3:\n    ${"$"}0",
            description = "With open file",
            placeholders = listOf(
                SnippetPlaceholder(1, "filename"),
                SnippetPlaceholder(2, "mode"),
                SnippetPlaceholder(3, "f"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "lambda",
            body = "lambda ${"$"}1: ${"$"}0",
            description = "Lambda function",
            placeholders = listOf(
                SnippetPlaceholder(1, "args"),
                SnippetPlaceholder(0, "expression")
            )
        ),
        Snippet(
            prefix = "list",
            body = "[${"$"}1 for ${"$"}2 in ${"$"}3]",
            description = "List comprehension",
            placeholders = listOf(
                SnippetPlaceholder(1, "expression"),
                SnippetPlaceholder(2, "item"),
                SnippetPlaceholder(3, "iterable")
            )
        ),
        Snippet(
            prefix = "dict",
            body = "{${"$"}1: ${"$"}2 for ${"$"}3, ${"$"}4 in ${"$"}5}",
            description = "Dict comprehension",
            placeholders = listOf(
                SnippetPlaceholder(1, "key"),
                SnippetPlaceholder(2, "value"),
                SnippetPlaceholder(3, "k"),
                SnippetPlaceholder(4, "v"),
                SnippetPlaceholder(5, "iterable")
            )
        ),
        Snippet(
            prefix = "set",
            body = "{${"$"}1 for ${"$"}2 in ${"$"}3}",
            description = "Set comprehension",
            placeholders = listOf(
                SnippetPlaceholder(1, "expression"),
                SnippetPlaceholder(2, "item"),
                SnippetPlaceholder(3, "iterable")
            )
        ),
        Snippet(
            prefix = "main",
            body = "if __name__ == '__main__':\n    ${"$"}0",
            description = "Main guard",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "init",
            body = "def __init__(self${"$"}1):\n    ${"$"}0",
            description = "Init method",
            placeholders = listOf(
                SnippetPlaceholder(1, "args"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "self",
            body = "self.${"$"}1 = ${"$"}1",
            description = "Assign to self",
            placeholders = listOf(
                SnippetPlaceholder(1, "attribute")
            )
        ),
        Snippet(
            prefix = "property",
            body = "@property\ndef ${"$"}1(self):\n    return self._${"$"}1\n\n@${"$"}1.setter\ndef ${"$"}1(self, value):\n    self._${"$"}1 = value",
            description = "Property with getter and setter",
            placeholders = listOf(
                SnippetPlaceholder(1, "name")
            )
        ),
        Snippet(
            prefix = "staticmethod",
            body = "@staticmethod\ndef ${"$"}1(${"$"}2):\n    ${"$"}0",
            description = "Static method",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "args"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "classmethod",
            body = "@classmethod\ndef ${"$"}1(cls${"$"}2):\n    ${"$"}0",
            description = "Class method",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "args"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "test",
            body = "def test_${"$"}1(self):\n    ${"$"}0",
            description = "Test method",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "testsetup",
            body = "def setUp(self):\n    ${"$"}0",
            description = "Test setUp",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "testteardown",
            body = "def tearDown(self):\n    ${"$"}0",
            description = "Test tearDown",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "print",
            body = "print(${"$"}1)",
            description = "Print statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "value")
            )
        ),
        Snippet(
            prefix = "printerr",
            body = "print(${"$"}1, file=sys.stderr)",
            description = "Print to stderr",
            placeholders = listOf(
                SnippetPlaceholder(1, "value")
            )
        ),
        Snippet(
            prefix = "import",
            body = "import ${"$"}1",
            description = "Import statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "module")
            )
        ),
        Snippet(
            prefix = "fromimport",
            body = "from ${"$"}1 import ${"$"}2",
            description = "From import statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "module"),
                SnippetPlaceholder(2, "name")
            )
        ),
        Snippet(
            prefix = "fromimportas",
            body = "from ${"$"}1 import ${"$"}2 as ${"$"}3",
            description = "From import with alias",
            placeholders = listOf(
                SnippetPlaceholder(1, "module"),
                SnippetPlaceholder(2, "name"),
                SnippetPlaceholder(3, "alias")
            )
        ),
        Snippet(
            prefix = "ifname",
            body = "if __name__ == '__main__':\n    ${"$"}0",
            description = "If name is main",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "ifmain",
            body = "if __name__ == '__main__':\n    main()",
            description = "Call main function"
        ),
        Snippet(
            prefix = "maindef",
            body = "def main():\n    ${"$"}0\n\nif __name__ == '__main__':\n    main()",
            description = "Main function with guard",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "argparse",
            body = "import argparse\n\nparser = argparse.ArgumentParser()\nparser.add_argument('${"$"}1', type=${"$"}2)\nargs = parser.parse_args()\n${"$"}0",
            description = "Argparse setup",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "str"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "logging",
            body = "import logging\n\nlogger = logging.getLogger(__name__)\n${"$"}0",
            description = "Logging setup",
            placeholders = listOf(
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "dataclass",
            body = "from dataclasses import dataclass\n\n@dataclass\nclass ${"$"}1:\n    ${"$"}2: ${"$"}3\n    ${"$"}0",
            description = "Dataclass",
            placeholders = listOf(
                SnippetPlaceholder(1, "ClassName"),
                SnippetPlaceholder(2, "field"),
                SnippetPlaceholder(3, "type"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "enum",
            body = "from enum import Enum\n\nclass ${"$"}1(Enum):\n    ${"$"}2 = ${"$"}3\n    ${"$"}0",
            description = "Enum class",
            placeholders = listOf(
                SnippetPlaceholder(1, "EnumName"),
                SnippetPlaceholder(2, "VALUE"),
                SnippetPlaceholder(3, "1"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "contextmanager",
            body = "from contextlib import contextmanager\n\n@contextmanager\ndef ${"$"}1(${"$"}2):\n    try:\n        ${"$"}3\n        yield ${"$"}4\n    finally:\n        ${"$"}0",
            description = "Context manager",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "args"),
                SnippetPlaceholder(3, "setup"),
                SnippetPlaceholder(4, "value"),
                SnippetPlaceholder(0, "cleanup")
            )
        ),
        Snippet(
            prefix = "asyncdef",
            body = "async def ${"$"}1(${"$"}2):\n    ${"$"}0",
            description = "Async function",
            placeholders = listOf(
                SnippetPlaceholder(1, "name"),
                SnippetPlaceholder(2, "args"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "asyncfor",
            body = "async for ${"$"}1 in ${"$"}2:\n    ${"$"}0",
            description = "Async for loop",
            placeholders = listOf(
                SnippetPlaceholder(1, "item"),
                SnippetPlaceholder(2, "iterable"),
                SnippetPlaceholder(0, "pass")
            )
        ),
        Snippet(
            prefix = "asyncwith",
            body = "async with ${"$"}1 as ${"$"}2:\n    ${"$"}0",
            description = "Async with statement",
            placeholders = listOf(
                SnippetPlaceholder(1, "expression"),
                SnippetPlaceholder(2, "variable"),
                SnippetPlaceholder(0, "pass")
            )
        )
    )
    
    override suspend fun getCompletions(
        content: String,
        cursorOffset: Int,
        prefix: String,
        context: CompletionContext,
        triggerCharacter: String?
    ): List<CompletionItem> {
        if (context.isInString || context.isAfterComment) return emptyList()
        
        return snippets.map { snippet ->
            CompletionItem(
                id = "snippet_${snippet.prefix}",
                label = snippet.prefix,
                kind = CompletionKind.SNIPPET,
                detail = snippet.description,
                documentation = snippet.description,
                snippet = snippet.body,
                priority = 70,
                filterText = snippet.prefix,
                sortOrder = 3
            )
        }
    }
}
