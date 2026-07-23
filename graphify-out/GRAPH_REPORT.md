# Graph Report - python-ide  (2026-07-23)

## Corpus Check
- 115 files · ~85,587 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2470 nodes · 4474 edges · 117 communities (102 shown, 15 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 128 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Code Editor Core
- Bracket Handler
- Package Manager Impl
- Room Database
- Code Editor UI
- Editor ViewModel
- Package Management UI
- Package Manager ViewModel
- Project Repository Interface
- Settings Screen
- Project Repository Impl
- Completion UI
- Project ViewModel
- App Navigation
- Package Manager Interface
- Dependency Injection
- IntelliSense Engine
- Syntax Highlighting
- Python Runtime Engine
- Completion Providers
- File Manager
- Terminal UI
- Data Models
- File Repository
- Clipboard Handler
- Search Handler
- Indentation Handler
- Background Indexer
- File Manager ViewModel
- Python Bridge
- Execution Result
- Editor State
- Theme Configuration
- Template System
- Backup System
- Session Management
- Auto Save Config
- Project Stats
- File DAO
- Project DAO
- Project Session DAO
- Project Backup DAO
- Project Template DAO
- AutoSave Config DAO
- Terminal ViewModel
- REPL UI
- Home Screen
- Settings ViewModel
- Main ViewModel
- Home ViewModel
- App Theme
- Package Search
- Package Install Queue
- Package Backup
- Package Offline
- Package Dependencies
- Native Binary Detection
- Package Compatibility
- Package Logs
- Package Batch
- Package List
- Package Show
- FileDao
- TerminalScreen
- .navigateTo
- EditorState
- SearchHandler
- ProjectTemplateDao
- StorageSource
- FileAction
- TerminalModels.kt
- SettingsDataStore
- BracketHandler
- SettingsViewModel
- IndentationHandler
- DebugState
- FileOperationType
- AutoSaveConfigDao
- ProjectBackupDao
- ProjectSessionDao
- SettingsRepository
- SortBy
- PythonIDENavHost
- SplitMode
- Flow
- AnsiParser
- ViewMode
- OptimizedTextBuffer
- SettingsScreen
- FileRepository
- AGENTS.md
- EditorViewModel
- HomeScreen.kt
- .onCreate
- DispatcherModule
- DebugCommand
- PackageSortBy
- FontSettings
- .importSettings
- RuntimeModule
- FileOperationStatus
- ProjectSortBy
- ProjectSortOrder
- DiagnosticProvider
- NavigationDrawerTest

## God Nodes (most connected - your core abstractions)
1. `CodeEditorViewModel` - 75 edges
2. `PackageManagerViewModel` - 71 edges
3. `SettingsDataStore` - 67 edges
4. `SettingsRepository` - 66 edges
5. `ProjectRepositoryImpl` - 60 edges
6. `ProjectViewModel` - 59 edges
7. `SettingsViewModel` - 59 edges
8. `PackageManagerRepositoryImpl` - 59 edges
9. `PackageManagerRepository` - 55 edges
10. `DebuggerEngine` - 52 edges

## Surprising Connections (you probably didn't know these)
- `CodeEditorViewModel` --references--> `EditorConfig`  [EXTRACTED]
  app/src/main/java/com/pythonide/app/screens/editor/CodeEditorViewModel.kt → domain/src/main/java/com/pythonide/domain/model/editor/EditorState.kt
- `FileManagerViewModel` --references--> `FileManagerConfig`  [EXTRACTED]
  app/src/main/java/com/pythonide/app/screens/filemanager/FileManagerViewModel.kt → domain/src/main/java/com/pythonide/domain/model/filemanager/FileManagerModels.kt
- `PackageManagementScreen()` --calls--> `Tab`  [INFERRED]
  app/src/main/java/com/pythonide/app/screens/packages/PackageManagementScreen.kt → domain/src/main/java/com/pythonide/domain/model/editor/EditorState.kt
- `MetadataDialog()` --calls--> `ProjectMetadata`  [INFERRED]
  app/src/main/java/com/pythonide/app/screens/project/ProjectManagementScreen.kt → domain/src/main/java/com/pythonide/domain/model/project/ProjectModels.kt
- `DebuggerViewModel` --references--> `DebugState`  [EXTRACTED]
  app/src/main/java/com/pythonide/app/screens/debugger/DebuggerViewModel.kt → domain/src/main/java/com/pythonide/domain/model/debugger/DebuggerModels.kt

## Import Cycles
- None detected.

## Communities (117 total, 15 thin omitted)

### Community 0 - "Code Editor Core"
Cohesion: 0.09
Nodes (6): CodeEditorViewModel, AndroidViewModel, androidx, CursorPosition, Job, StateFlow

### Community 1 - "Bracket Handler"
Cohesion: 0.05
Nodes (12): BracketHandler, BracketResult, ClipboardHandler, EditorStateManager, CursorPosition, StateFlow, IndentationHandler, SearchHandler (+4 more)

### Community 2 - "Package Manager Impl"
Cohesion: 0.06
Nodes (25): Job, Result, StateFlow, PackageManagerRepositoryImpl, Critical, Job, StateFlow, T (+17 more)

### Community 3 - "Room Database"
Cohesion: 0.24
Nodes (3): Flow, ProjectDao, ProjectEntity

### Community 4 - "Code Editor UI"
Cohesion: 0.19
Nodes (21): CheckboxRow(), CodeContent(), CodeEditorScreen(), EditorContent(), EditorSettingsDialog(), GoToLineDialog(), androidx, com (+13 more)

### Community 5 - "Editor ViewModel"
Cohesion: 0.07
Nodes (17): EmptyState(), FileCard(), FileGrid(), formatDate(), HomeScreen(), androidx, Modifier, QuickActionChip() (+9 more)

### Community 6 - "Package Management UI"
Cohesion: 0.10
Nodes (46): BackupCard(), BackupDialog(), BackupRestoreView(), BatchInstallDialog(), BottomNavButton(), CompatibilityDialog(), DetailInfoRow(), EmptyState() (+38 more)

### Community 7 - "Package Manager ViewModel"
Cohesion: 0.04
Nodes (4): AndroidViewModel, Job, StateFlow, PackageManagerViewModel

### Community 8 - "Project Repository Interface"
Cohesion: 0.10
Nodes (5): ProjectBackup, Flow, Project, Result, ProjectRepository

### Community 9 - "Settings Screen"
Cohesion: 0.14
Nodes (7): StateFlow, ViewModel, MainViewModel, ThemeMode, DARK, LIGHT, SYSTEM

### Community 10 - "Project Repository Impl"
Cohesion: 0.16
Nodes (5): Flow, ProjectRepositoryImpl, ProjectMetadata, ProjectSession, ProjectTemplate

### Community 11 - "Completion UI"
Cohesion: 0.07
Nodes (34): CompletionItemRow(), CompletionPopup(), getColorForKind(), getDiagnosticColor(), getIconText(), Color, Modifier, CompletionKind (+26 more)

### Community 12 - "Project ViewModel"
Cohesion: 0.07
Nodes (5): AndroidViewModel, Job, Project, StateFlow, ProjectViewModel

### Community 13 - "App Navigation"
Cohesion: 0.07
Nodes (43): Editor, FileManager, Home, Modifier, Packages, Projects, PythonIDENavHost(), Screen (+35 more)

### Community 14 - "Package Manager Interface"
Cohesion: 0.10
Nodes (3): PackageBackup, Result, PackageManagerRepository

### Community 15 - "Dependency Injection"
Cohesion: 0.26
Nodes (4): DatabaseModule, Context, AppDatabase, RoomDatabase

### Community 16 - "IntelliSense Engine"
Cohesion: 0.11
Nodes (6): IntelliSenseEngine, Job, StateFlow, CompletionItem, IntelliSenseConfig, ParameterHints

### Community 17 - "Syntax Highlighting"
Cohesion: 0.11
Nodes (16): Color, PythonSyntaxHighlighter, SyntaxToken, TokenType, BUILTIN, CLASS_NAME, COMMENT, DECORATOR (+8 more)

### Community 18 - "Python Runtime Engine"
Cohesion: 0.13
Nodes (7): ExecutionEngine, ExecutionRequest, androidx, Job, SharedFlow, StateFlow, ExecutionResult

### Community 19 - "Completion Providers"
Cohesion: 0.17
Nodes (10): CompletionContext, CompletionProvider, BuiltinInfo, BuiltinProvider, FunctionInfo, FunctionProvider, KeywordProvider, ModuleProvider (+2 more)

### Community 20 - "File Manager"
Cohesion: 0.09
Nodes (8): AppSettings, BackupSettings, ConsoleSettings, EditorSettingsState, FontSettings, PackageSettings, ThemeSettings, TimeoutSettings

### Community 21 - "Terminal UI"
Cohesion: 0.11
Nodes (8): DebuggerEngine, Job, Result, RuntimeInfo, SharedFlow, StateFlow, StackFrame, Variable

### Community 22 - "Data Models"
Cohesion: 0.12
Nodes (4): CursorPosition, StateFlow, TabManager, Tab

### Community 23 - "File Repository"
Cohesion: 0.27
Nodes (6): Linter, SyntaxValidator, Diagnostic, DiagnosticRange, QuickFix, TextEdit

### Community 24 - "Clipboard Handler"
Cohesion: 0.30
Nodes (14): AutoSaveSettingsDialog(), BackupDialog(), EmptyProjectsState(), formatDate(), Project, MetadataDialog(), NewProjectDialog(), ProjectCard() (+6 more)

### Community 25 - "Search Handler"
Cohesion: 0.19
Nodes (5): BackgroundIndexer, Job, StateFlow, IndexEntry, IndexStats

### Community 26 - "Indentation Handler"
Cohesion: 0.18
Nodes (4): InterpreterManager, InterpreterSession, StateFlow, REPLHistoryEntry

### Community 27 - "Background Indexer"
Cohesion: 0.07
Nodes (27): InstallPriority, HIGH, LOW, NORMAL, URGENT, NativeBinaryInfo, OfflineFileType, DIRECTORY (+19 more)

### Community 29 - "File Manager ViewModel"
Cohesion: 0.13
Nodes (14): Command, DiagnosticTag, DEPRECATED, UNNECESSARY, ImportSuggestion, IntelliSenseState, QuickFixKind, ERROR (+6 more)

### Community 30 - "Python Bridge"
Cohesion: 0.23
Nodes (3): PythonNativeBridge, InterpreterConfig, PythonException

### Community 33 - "Theme Configuration"
Cohesion: 0.13
Nodes (3): RuntimeRepositoryModule, com, PythonRuntimeRepository

### Community 34 - "Template System"
Cohesion: 0.11
Nodes (8): FileManagerRepositoryImpl, ByteArray, FileOperation, Flow, Project, Result, FilePermissions, RecentFile

### Community 35 - "Backup System"
Cohesion: 0.14
Nodes (8): CoroutineExceptionHandler, CrashEvent, CrashPrevention, Job, StateFlow, T, ResourceState, WatchdogConfig

### Community 36 - "Session Management"
Cohesion: 0.20
Nodes (10): TemplateCategory, API, AUTOMATION, BLANK, CONSOLE_APP, CUSTOM, DATA_SCIENCE, GAME (+2 more)

### Community 37 - "Auto Save Config"
Cohesion: 0.07
Nodes (13): DebuggerViewModel, DebugPanel, CALL_STACK, ERRORS, LOGS, RUNTIME, VARIABLES, WARNINGS (+5 more)

### Community 38 - "Project Stats"
Cohesion: 0.22
Nodes (9): InstallTaskStatus, CANCELLED, COMPLETED, DOWNLOADING, FAILED, INSTALLING, PAUSED, QUEUED (+1 more)

### Community 39 - "File DAO"
Cohesion: 0.22
Nodes (9): TemplateIcon, API, CONSOLE, DATA, DEFAULT, GAME, GEAR, TEST (+1 more)

### Community 40 - "Project DAO"
Cohesion: 0.13
Nodes (3): BatchInstallRequest, InstallProgress, Flow

### Community 41 - "Project Session DAO"
Cohesion: 0.25
Nodes (7): InterpreterState, ERROR, IDLE, RESTARTING, RUNNING, STOPPED, WAITING_INPUT

### Community 42 - "Project Backup DAO"
Cohesion: 0.10
Nodes (6): FileManagerItem, FileManagerRepository, ByteArray, Flow, Project, Result

### Community 43 - "Project Template DAO"
Cohesion: 0.06
Nodes (5): android, FileManagerViewModel, AndroidViewModel, Project, StateFlow

### Community 44 - "AutoSave Config DAO"
Cohesion: 0.29
Nodes (7): PackageViewMode, BACKUP, INSTALLED, LOGS, OFFLINE, QUEUE, SEARCH

### Community 45 - "Terminal ViewModel"
Cohesion: 0.11
Nodes (5): DebuggerRepositoryImpl, Flow, Result, RuntimeInfo, LogEntry

### Community 46 - "REPL UI"
Cohesion: 0.08
Nodes (24): AppPermission, Checking, Denied, Granted, Idle, StateFlow, PermanentlyDenied, PermissionCategory (+16 more)

### Community 47 - "Home Screen"
Cohesion: 0.08
Nodes (3): CrashPreventionTest, ExceptionRecoveryTest, RuntimeException

### Community 48 - "Settings ViewModel"
Cohesion: 0.11
Nodes (15): AutoCloseable, Active, Cleanup, Error, Idle, Initializing, ByteArray, Job (+7 more)

### Community 49 - "Main ViewModel"
Cohesion: 0.08
Nodes (22): FileManagerConfig, FileManagerState, FileOperation, FileType, DIRECTORY, FILE, SYMLINK, UNKNOWN (+14 more)

### Community 50 - "Home ViewModel"
Cohesion: 0.17
Nodes (25): Breadcrumb(), CreateItemDialog(), CreateProjectDialog(), EmptyState(), FileCompactItem(), FileGridItem(), FileList(), FileListItem() (+17 more)

### Community 52 - "Package Search"
Cohesion: 0.15
Nodes (4): AndroidViewModel, StateFlow, TerminalViewModel, TerminalEntry

### Community 53 - "Package Install Queue"
Cohesion: 0.06
Nodes (20): CacheEntry, CacheManager, CacheStats, LruCache, ProjectInfo, SyntaxToken, SyntaxTokenType, BUILTIN (+12 more)

### Community 54 - "Package Backup"
Cohesion: 0.12
Nodes (5): WatchExpression, DebuggerRepository, Flow, Result, RuntimeInfo

### Community 55 - "Package Offline"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 56 - "Package Dependencies"
Cohesion: 0.05
Nodes (25): Cancelled(), Completed(), Error, Idle, StateFlow, ResourceLimits, ResourceUsage, Running (+17 more)

### Community 58 - "Package Compatibility"
Cohesion: 0.15
Nodes (17): BreakpointHit, DebugEvent, DebugSession, ErrorLogged, Evaluate, ExceptionRaised, ExecutionFinished, LogLevel (+9 more)

### Community 59 - "Package Logs"
Cohesion: 0.10
Nodes (15): ChangeType, CREATED, DELETED, MODIFIED, Error, FileChange, FileWatcher, Idle (+7 more)

### Community 61 - "Package Batch"
Cohesion: 0.11
Nodes (18): AnsiColor, BLACK, BLUE, BRIGHT_BLACK, BRIGHT_BLUE, BRIGHT_CYAN, BRIGHT_GREEN, BRIGHT_MAGENTA (+10 more)

### Community 66 - "Package List"
Cohesion: 0.12
Nodes (15): Architecture, Build, Debug Build, Features, Getting Started, Installation, License, Prerequisites (+7 more)

### Community 67 - "Package Show"
Cohesion: 0.17
Nodes (6): FontFamily, CASUAL, CURSIVE, MONOSPACE, SANS_SERIF, SERIF

### Community 70 - "FileDao"
Cohesion: 0.21
Nodes (3): FileDao, Flow, FileEntity

### Community 71 - "TerminalScreen"
Cohesion: 0.29
Nodes (10): AnsiText(), Modifier, TerminalEntryItem(), TerminalInput(), TerminalScreen(), TerminalSettingsDialog(), TerminalStatusBar(), TerminalTabBar() (+2 more)

### Community 72 - ".navigateTo"
Cohesion: 0.25
Nodes (4): Project, Result, T, ProjectStats

### Community 73 - "EditorState"
Cohesion: 0.22
Nodes (9): FileOperationType, COPY, CREATE_DIRECTORY, CREATE_FILE, DELETE, EXPORT, IMPORT, MOVE (+1 more)

### Community 74 - "SearchHandler"
Cohesion: 0.24
Nodes (3): Flow, PythonInterpreter, Flow

### Community 76 - "StorageSource"
Cohesion: 0.25
Nodes (7): StorageSource, EXTERNAL, FAVORITES, INTERNAL, PROJECT, RECENT, SAF

### Community 77 - "FileAction"
Cohesion: 0.18
Nodes (11): FileAction, COPY, CREATE, DELETE, EXPORT, IMPORT, MOVE, OPEN (+3 more)

### Community 78 - "TerminalModels.kt"
Cohesion: 0.20
Nodes (10): from256(), fromCode(), Terminal, TerminalEntryType, ERROR, INPUT, OUTPUT, SYSTEM (+2 more)

### Community 79 - "SettingsDataStore"
Cohesion: 0.04
Nodes (5): Keys, Flow, Result, T, SettingsDataStore

### Community 80 - "BracketHandler"
Cohesion: 0.29
Nodes (6): animatedComposable(), androidx, NavTransitions, EnterTransition, ExitTransition, NavType

### Community 81 - "SettingsViewModel"
Cohesion: 0.04
Nodes (3): StateFlow, ViewModel, SettingsViewModel

### Community 82 - "IndentationHandler"
Cohesion: 0.20
Nodes (10): Comparable, CursorInfo, CursorPosition, Delete, EditorAction, EditorConfig, EditorTabState, fromOffset() (+2 more)

### Community 83 - "DebugState"
Cohesion: 0.22
Nodes (8): DebugState, ERROR, IDLE, PAUSED, RUNNING, STARTING, STEPPING, STOPPED

### Community 84 - "FileOperationType"
Cohesion: 0.13
Nodes (6): ExceptionRecovery, T, RecoveryEntry, RetryConfig, RetryStats, RuntimeIntegrationTest

### Community 89 - "SettingsRepository"
Cohesion: 0.04
Nodes (3): Flow, Result, SettingsRepository

### Community 90 - "SortBy"
Cohesion: 0.33
Nodes (4): SplitMode, HORIZONTAL, NONE, VERTICAL

### Community 91 - "PythonIDENavHost"
Cohesion: 0.06
Nodes (20): AccessPolicy, Denied, Error, FileAccessEntry, FileInfo, FileOperation, CREATE, DELETE (+12 more)

### Community 92 - "SplitMode"
Cohesion: 0.21
Nodes (5): CursorPosition, SessionFile, TemplateFile, JSONArray, JSONObject

### Community 93 - "Flow"
Cohesion: 0.19
Nodes (10): Complete, Error, Idle, MemoryReport, StateFlow, Loading, MemoryReport, StartupMetrics (+2 more)

### Community 95 - "ViewMode"
Cohesion: 0.13
Nodes (14): AutoSaveConfig, CursorPosition, Project, ProjectState, SaveResult, FAILURE, NO_PROJECT, READ_ONLY (+6 more)

### Community 96 - "OptimizedTextBuffer"
Cohesion: 0.07
Nodes (3): StateFlow, OptimizedTextBuffer, OptimizedTextBufferTest

### Community 97 - "SettingsScreen"
Cohesion: 0.10
Nodes (8): MainActivity, PythonIDEColorScheme, PythonIDETheme(), EditorViewModelTest, StateFlow, TestEditorViewModel, Bundle, ComponentActivity

### Community 98 - "FileRepository"
Cohesion: 0.17
Nodes (15): About, BottomNavItem, Debugger, DrawerItem, Editor, Home, Navigate, NavigateUp (+7 more)

### Community 100 - "EditorViewModel"
Cohesion: 0.21
Nodes (3): StateFlow, SettingsViewModelTest, TestSettingsViewModel

### Community 101 - "HomeScreen.kt"
Cohesion: 0.20
Nodes (12): CircuitBreaker, CircuitBreakerOpenException, CircuitState, Closed, Failed, HalfOpen, Idle, StateFlow (+4 more)

### Community 103 - ".onCreate"
Cohesion: 0.21
Nodes (11): Active, CrashState, Critical, Inactive, MemoryReport, MemoryReport, Normal, Recovering (+3 more)

### Community 106 - "DebugCommand"
Cohesion: 0.29
Nodes (7): DebugCommand, CONTINUE, INSPECT, STEP_INTO, STEP_OUT, STEP_OVER, STOP

### Community 110 - "FontSettings"
Cohesion: 0.25
Nodes (8): CrashType, OUT_OF_MEMORY, RESOURCE_EXHAUSTION, STACK_OVERFLOW, THREAD_DEATH, UNCAUGHT_EXCEPTION, USER_ERROR, WATCHDOG_TIMEOUT

### Community 111 - ".importSettings"
Cohesion: 0.40
Nodes (5): Priority, HIGH, IMMEDIATE, LOW, NORMAL

### Community 114 - "FileOperationStatus"
Cohesion: 0.25
Nodes (7): FileOperationStatus, CANCELLED, COMPLETED, FAILED, IN_PROGRESS, PENDING, FileOperation

### Community 116 - "ProjectSortBy"
Cohesion: 0.29
Nodes (6): ProjectSortBy, CREATED, LAST_ACCESSED, LAST_SAVED, NAME, SIZE

### Community 117 - "ProjectSortOrder"
Cohesion: 0.67
Nodes (3): ProjectSortOrder, ASCENDING, DESCENDING

## Knowledge Gaps
- **296 isolated node(s):** `VARIABLES`, `WATCH`, `CALL_STACK`, `LOGS`, `RUNTIME` (+291 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PythonIDENavHost()` connect `App Navigation` to `SettingsScreen`, `Code Editor UI`, `Editor ViewModel`, `Package Management UI`, `TerminalScreen`, `Home ViewModel`, `Clipboard Handler`?**
  _High betweenness centrality (0.297) - this node is a cross-community bridge._
- **Why does `CodeEditorScreen()` connect `Code Editor UI` to `Code Editor Core`, `Completion UI`, `Auto Save Config`, `App Navigation`?**
  _High betweenness centrality (0.240) - this node is a cross-community bridge._
- **Why does `DebuggerEngine` connect `Terminal UI` to `Auto Save Config`, `Terminal ViewModel`, `RuntimeModule`, `DebugState`, `Package Backup`, `Package Compatibility`?**
  _High betweenness centrality (0.225) - this node is a cross-community bridge._
- **What connects `VARIABLES`, `WATCH`, `CALL_STACK` to the rest of the system?**
  _296 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Code Editor Core` be split into smaller, more focused modules?**
  _Cohesion score 0.08888888888888889 - nodes in this community are weakly interconnected._
- **Should `Bracket Handler` be split into smaller, more focused modules?**
  _Cohesion score 0.05267778753292362 - nodes in this community are weakly interconnected._
- **Should `Package Manager Impl` be split into smaller, more focused modules?**
  _Cohesion score 0.061128526645768025 - nodes in this community are weakly interconnected._