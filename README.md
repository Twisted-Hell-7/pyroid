<div align="center">

# Python IDE for Android

**A fully offline, feature-complete Python IDE for Android -- built entirely in Kotlin with Jetpack Compose.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12.01-4285F4)](https://developer.android.com/jetpack/compose)
[![Android](https://img.shields.io/badge/Android-26%20to%2035-3DDC84?logo=android)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

</div>

---

## What is this?

Python IDE for Android is a complete development environment that runs entirely on your Android device -- no internet connection required. It bundles a Python runtime, includes a real code editor with IntelliSense, a pdb-based debugger, a package manager, and a full terminal -- all packaged in a modern Material 3 interface.

## Features

### Code Editor

- **Python Syntax Highlighting** -- keywords, strings, numbers, comments, functions, classes, operators, decorators, builtins
- **6 Editor Themes** -- Default (VS Code), Monokai, Solarized Dark, Dracula, GitHub Dark, AMOLED
- **Smart Editing** -- auto bracket/quote pairing, matching bracket detection, smart indentation after `def`, `class`, `if`, `for`, `while`, `try`, `with`
- **Find & Replace** -- regex support, case-sensitive/insensitive, whole-word matching, match counting, replace-all
- **Multi-Tab Editor** -- create, close, reorder, duplicate tabs with split view support (horizontal/vertical)
- **Undo/Redo** -- 1000-action stack with Insert/Delete/Replace tracking
- **Large File Support** -- chunked loading (1000-line chunks) with virtual scrolling for files of any size
- **File Watcher** -- NIO WatchService monitors external file changes in real time

### IntelliSense & Linting

- **5 Completion Providers** -- keywords (35), builtins (70+), stdlib modules (200+), common functions (40+), code snippets (35+)
- **Context-Aware** -- suggestions adapt after dot, import, def, class; disabled inside strings and comments
- **Background Indexer** -- extracts classes, functions, variables, imports; builds symbol index for fast lookup
- **Syntax Validator** -- detects missing colons, indentation errors, unmatched brackets, unterminated strings, duplicate imports
- **Linter** -- unused variables/imports, missing docstrings, line length (>79), naming conventions (CamelCase/snake_case), common mistakes (bare except, `== True/False/None`)
- **Quick Fixes** -- automated fixes for most linting issues

### Debugger

- **pdb-Based Engine** -- generates a custom `bdb.Bdb` subclass with JSON event protocol over stdout/stdin
- **Breakpoints** -- add, remove, toggle, conditional breakpoints with hit count tracking
- **Stepping** -- continue, step into, step over, step out
- **Variable Inspection** -- inspect locals, expand compound types (list, dict, tuple, object), type inference
- **Watch Expressions** -- auto-evaluate on pause, error display
- **Call Stack** -- full frames with function names, filenames, line numbers, source lines
- **Runtime Info** -- memory usage (MB), thread count, active modules

### Terminal

- **Full ANSI Parser** -- 16-color + 256-color support, bold, italic, underline, dim, strikethrough
- **Multi-Terminal** -- create and manage multiple terminal sessions
- **Command History** -- navigate with up/down arrows
- **Configurable** -- timestamps, font size, max lines (10000), ANSI colors toggle

### Package Manager

- **PyPI Search** -- search packages with pagination
- **Install Queue** -- priority-based (LOW, NORMAL, HIGH, URGENT) with progress tracking
- **Package Operations** -- install, uninstall, upgrade, downgrade, reinstall, batch install
- **Offline Support** -- install from .whl, .tar.gz, .zip files
- **Backup & Restore** -- export/import requirements.txt with metadata
- **Dependency Analysis** -- dependency tree, reverse dependencies, conflict detection
- **ABI Compatibility** -- detects native binary requirements, suggests alternatives
- **Pip Cache** -- view cache info, purge cache

### File Manager

- **Full CRUD** -- create, read, update, delete files and directories
- **Storage Sources** -- internal, external, SAF, project, recent, favorites
- **View Modes** -- list, grid, compact with sorting (name, size, date, type, extension)
- **Clipboard** -- copy, cut, paste file operations
- **Search** -- case-insensitive file name filtering
- **Favorites & Recents** -- quick access to important files

### Project Manager

- **9 Project Templates** -- Blank, Console App, Web App, Data Science, Game, API, Automation, Testing, Custom
- **Project Sessions** -- persist open files, cursor positions, terminal history
- **Auto-Save** -- configurable interval, save on close/switch
- **Backup** -- project backup with size tracking
- **Metadata** -- Python version, interpreter path, author, license, dependencies, git repo

### Settings

- **Theme** -- light/dark/system, dynamic colors (Android 12+), AMOLED black
- **Font** -- size, family (Monospace/Sans Serif/Serif/Casual/Cursive), line height
- **Editor** -- line numbers, word wrap, highlight current line, auto/smart indent, tab size, bracket pair colorization, minimap, sticky scroll
- **Console** -- font size/family, timestamps, ANSI colors, max lines, auto-scroll
- **Backup** -- auto-backup interval, max backups, backup settings/projects/packages

## Architecture

```
python-ide/
  app/                          Presentation Layer
    screens/                    7 feature screens
    ui/theme/                   Material 3 theming
    navigation/                 Navigation graph + transitions
    di/                         Hilt modules
    viewmodel/                  ViewModels
  domain/                       Domain Layer
    model/                      Data classes + enums
    repository/                 Interface contracts
  data/                         Data Layer
    runtime/                    Python execution engine
    editor/                     Editor core + syntax highlighting
    intellisense/               Completion + diagnostics
    debugger/                   pdb-based debugger
    terminal/                   ANSI terminal parser
    local/                      Room DB + DataStore
    repository/                 Implementations
```

**Patterns**: Clean Architecture, MVVM, Repository Pattern, Circuit Breaker, Observer Pattern

**Concurrency**: Kotlin Coroutines + Flow, SupervisorJob, Mutex, AtomicBoolean, ConcurrentHashMap

**Thread Safety**: AtomicReference, ConcurrentLinkedQueue, synchronized blocks, structured concurrency

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.1.0 |
| Build | Gradle Kotlin DSL + AGP | 8.11.1 / 8.7.3 |
| UI | Jetpack Compose | BOM 2024.12.01 |
| Design | Material 3 | Dynamic Colors |
| Navigation | Navigation Compose | 2.8.5 |
| DI | Hilt | 2.53.1 |
| Database | Room | 2.6.1 |
| Preferences | DataStore | 1.1.1 |
| HTTP | OkHttp | 4.12.0 |
| Coroutines | Kotlinx Coroutines | 1.9.0 |
| Processing | KSP | 2.1.0-1.0.29 |
| Core | AndroidX Core KTX | 1.15.0 |
| Lifecycle | AndroidX Lifecycle | 2.8.7 |
| Testing | JUnit | 4.13.2 |

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 35

### Build

```bash
# Clone the repository
git clone https://github.com/dev-saswat-07/python-ide.git

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Build Commands

| Command | Description |
|---|---|
| `./gradlew assembleDebug` | Build debug APK |
| `./gradlew assembleRelease` | Build release APK |
| `./gradlew test` | Run unit tests |
| `./gradlew lint` | Run Android lint |
| `./gradlew :data:compileDebugKotlin` | Compile data module only |

## Supported Android Versions

| Property | Value |
|---|---|
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Dynamic Colors | API 31+ (Android 12+) |

## How It Works

**Python Runtime**: The app bundles a Python interpreter extracted from APK assets at first launch. Code is executed via `ProcessBuilder` with `python3 -c`, capturing stdout/stderr in real time.

**Debugger**: A custom `bdb.Bdb` subclass generates JSON events over stdout. The app sends commands (continue, step, next, return) via stdin. Breakpoints, variable inspection, and watch expressions are all handled through this protocol.

**Sandbox**: Code execution runs with resource limits (256MB memory, 30s CPU, 60s wall time, 10MB output) and blocks dangerous system commands.

**Memory Management**: The app monitors heap usage and triggers GC at 80% capacity. It detects low-RAM devices and adjusts behavior accordingly.

## License

This project is licensed under the MIT License -- see the [LICENSE](LICENSE) file for details.
