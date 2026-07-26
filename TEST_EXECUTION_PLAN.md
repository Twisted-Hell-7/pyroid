# Pyroid - Test Execution Plan

## Overview

This document outlines the prioritized test execution plan for the Pyroid offline Python IDE for Android. Tests are organized by priority level and should be executed in the order specified.

---

## Priority 1: Critical Security & Sandbox Tests

These tests must pass before any release. They cover security-critical functionality.

### 1.1 Python Runtime & Sandbox (§1)

**Test File:** `SandboxSecurityTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testMemoryLimitEnforcement` | Verify 256MB memory cap enforcement | High |
| `testCpuTimeLimitEnforcement` | Verify 30s CPU time limit | High |
| `testWallTimeLimitEnforcement` | Verify 60s wall time limit | High |
| `testOutputCapEnforcement` | Verify 10MB output cap | High |
| `testForkBombBlocked` | Confirm fork bomb attempts are blocked | Critical |
| `testSubprocessBlocked` | Confirm subprocess import is blocked | Critical |
| `testCtypesBlocked` | Confirm ctypes access is blocked | Critical |
| `testDestructiveShellCallsBlocked` | Confirm rm -rf / is blocked | Critical |
| `testPathTraversalBlocked` | Confirm path traversal is blocked | High |
| `testNetworkAccessBlocked` | Confirm curl/wget/ssh are blocked | High |
| `testSensitivePathBlocked` | Confirm /etc, /var, /usr are blocked | High |
| `testEmptyCommandRejected` | Confirm empty commands are rejected | Medium |
| `testBlockedCommandsRejected` | Confirm mkfs, dd are blocked | High |

### 1.2 Security / Adversarial (§12)

**Test File:** `SecurityAdversarialTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testPathTraversalWithDotDot` | Test `../../../etc/passwd` attempts | Critical |
| `testPathTraversalWithEncodedDotDot` | Test URL-encoded traversal | High |
| `testPathTraversalWithNullByte` | Test null byte injection | High |
| `testSymlinkToSensitivePath` | Test symlink attacks | High |
| `testArchiveExtractionSanitization` | Test zip-slip prevention | High |
| `testArchiveExtractionPathValidation` | Validate extracted paths | High |
| `testNetworkAccessBlockedByDefault` | Verify offline-first claim | High |
| `testFileSystemWriteBlockedByDefault` | Verify sandbox constraints | High |
| `testMalformedInputHandling` | Test fuzzing resistance | Medium |
| `testSQLInjectionPrevention` | Test SQL injection prevention | Medium |
| `testResourceExhaustionPrevention` | Test DoS prevention | High |
| `testCredentialProtection` | Verify credentials aren't logged | High |
| `testBlockedModules` | Verify subprocess/os.system blocked | High |
| `testSensitivePathProtection` | Verify sensitive paths are blocked | High |
| `testAppSandboxProtection` | Verify app isolation | High |

### 1.3 Concurrency & Thread Safety (§10)

**Test File:** `ConcurrencyStressTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testConcurrentCrashPreventionAccess` | 100 threads accessing CrashPrevention | High |
| `testConcurrentSafeExecute` | 50 threads calling safeExecute | High |
| `testConcurrentSafeSuspendExecute` | 50 coroutines calling safeSuspendExecute | High |
| `testConcurrentErrorCounting` | 20 threads x 10 errors each | High |
| `testConcurrentCrashHistoryAccess` | Concurrent read/write to history | High |
| `testConcurrentClearAndRecord` | Concurrent clear and record | High |
| `testConcurrentExceptionRecovery` | 30 concurrent recovery operations | High |
| `testConcurrentCircuitBreaker` | Circuit breaker under load | High |
| `testConcurrentCrashPreventionState` | State consistency under load | High |
| `testConcurrentMemoryReport` | 20 concurrent memory reports | Medium |
| `testConcurrentWatchdogStartStop` | Concurrent watchdog operations | Medium |
| `testConcurrentSafeExecuteWithRetries` | Retry logic under concurrency | High |

---

## Priority 2: Core Functionality Tests

These tests cover essential editor and runtime functionality.

### 2.1 Code Editor - Undo/Redo (§2)

**Test File:** `EditorEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testUndoRedoStackCapAt1000` | Verify 1000-action stack cap | High |
| `testUndoRedo1000TimesBackToBack` | Stress test undo/redo | High |
| `testUndoRedoExactlyAt1000thAction` | Boundary test at 1000 | High |
| `testUndoRedoAt1001stAction` | Test eviction at 1001 | High |

### 2.2 Code Editor - Unicode Handling (§2)

**Test File:** `EditorEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testUnicodeIdentifiers` | Japanese identifiers | Medium |
| `testEmojiInStrings` | Emoji in string literals | Medium |
| `testRTLTextHandling` | Right-to-left text | Medium |
| `testZeroWidthChars` | Zero-width characters | Low |

### 2.3 Code Editor - Bracket Matching (§2)

**Test File:** `BracketHandlerEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testAutoPairingOpeningBracket` | Auto-pair `(` | Medium |
| `testAutoPairingClosingBracketSkip` | Skip over `)` | Medium |
| `testAutoPairingQuote` | Auto-pair quotes | Medium |
| `testAutoPairingDisabled` | No auto-pair when disabled | Low |
| `testMatchingBracketSimple` | Simple bracket matching | Medium |
| `testMatchingBracketNested` | Nested bracket matching | Medium |
| `testIsBalancedSimple` | Balanced brackets check | Medium |
| `testIsBalancedUnbalanced` | Unbalanced brackets check | Medium |
| `testDeeplyNestedBrackets` | 500-level nesting | High |
| `testQuoteInsideString` | Quote inside string literal | Medium |
| `testQuoteInsideComment` | Quote inside comment | Medium |

### 2.4 Code Editor - Search (§2)

**Test File:** `SearchHandlerEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testSearchEmptyQuery` | Empty search query | Low |
| `testSearchNoMatches` | Search with no results | Low |
| `testSearchMultipleMatches` | Multiple match results | Low |
| `testSearchCaseSensitive` | Case-sensitive search | Low |
| `testSearchWholeWord` | Whole word search | Low |
| `testSearchRegex` | Regex search | Medium |
| `testSearchInvalidRegex` | Invalid regex handling | Medium |
| `testFindNext` | Find next match | Low |
| `testFindPrevious` | Find previous match | Low |
| `testReplace` | Single replace | Low |
| `testReplaceAll` | Replace all matches | Low |
| `testSearchWithCatastrophicBacktrackingPattern` | ReDoS prevention | High |

### 2.5 Debugger (§4)

**Test File:** `DebuggerProtocolTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testBreakpointCreation` | Create breakpoint | Low |
| `testBreakpointWithCondition` | Conditional breakpoint | Medium |
| `testStackFrameCreation` | Stack frame creation | Low |
| `testVariableCreation` | Variable creation | Low |
| `testVariableWithChildren` | Nested variables | Medium |
| `testWatchExpressionCreation` | Watch expression | Low |
| `testDebugStateEnumValues` | Debug states | Low |
| `testDebugEventBreakpointHit` | Breakpoint hit event | Medium |
| `testDebugEventStepComplete` | Step complete event | Medium |
| `testDebugEventExceptionRaised` | Exception event | Medium |
| `testMultipleBreakpoints` | 500 breakpoints | High |

---

## Priority 3: Edge Case & Boundary Tests

These tests cover edge cases and boundary conditions.

### 3.1 Editor Edge Cases (§2)

**Test File:** `EditorEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testExtremelyLongSingleLine` | 100K character line | Medium |
| `testCursorMovementOnLongLine` | Cursor on long line | Low |
| `testUnbalancedBrackets` | Unbalanced brackets | Low |
| `testAutoPairingInsideFString` | Auto-pair in f-string | Low |
| `testReplaceAllWithZeroMatches` | Replace with no matches | Low |
| `testSearchWithOverlappingMatches` | Overlapping matches | Low |
| `testLineLengthAtBoundary` | 79 vs 80 chars | Low |
| `testLineLengthWithMultiByteChars` | Unicode char counting | Medium |
| `testBOMPrefixedUTF8` | UTF-8 BOM handling | Low |
| `testMixedLineEndings` | CRLF + LF mixed | Low |
| `testZeroByteFile` | Empty file | Low |
| `testReadOnlyModeBlocksInsert` | Read-only mode | Low |
| `testReadOnlyModeBlocksDelete` | Read-only mode | Low |
| `testSelectAllEmptyContent` | Select all empty | Low |
| `testDeleteSelection` | Delete selection | Low |
| `testCursorMovementBeyondBounds` | Cursor out of bounds | Low |
| `testCursorMovementToNegative` | Negative cursor | Low |
| `testFontSizeMinBoundary` | Font size min (8) | Low |
| `testFontSizeMaxBoundary` | Font size max (72) | Low |
| `testFontSizeBelowMin` | Font size below min | Low |
| `testFontSizeAboveMax` | Font size above max | Low |

### 3.2 Terminal Edge Cases (§5)

**Test File:** `TerminalEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testParseEmptyString` | Empty ANSI input | Low |
| `testParsePlainText` | Plain text | Low |
| `testParseBasicColorCodes` | 16-color codes | Low |
| `testParse256ColorCodes` | 256-color codes | Low |
| `testParseRgbColorCodes` | RGB color codes | Low |
| `testParseIncompleteEscapeSequence` | Truncated escape | Medium |
| `testParseInvalidColorCode` | Invalid color code | Medium |
| `testParseEmptyEscapeSequence` | Empty escape | Medium |
| `testStripAnsiPlainText` | Strip plain text | Low |
| `testStripAnsiColoredText` | Strip colored text | Low |
| `testHasAnsiPlainText` | Has ANSI plain | Low |
| `testHasAnsiColoredText` | Has ANSI colored | Low |
| `testParseExtremelyLongLine` | 500K char line | Medium |
| `testParseManyEscapeSequences` | 1000 escape sequences | Medium |
| `testNoAnsiStateLeakBetweenLines` | No state leak | High |

### 3.3 File Access Security (§12)

**Test File:** `SafeFileAccessSecurityTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testPathTraversalBlocked` | Path traversal | High |
| `testPathTraversalWriteBlocked` | Write traversal | High |
| `testPathTraversalDeleteBlocked` | Delete traversal | High |
| `testEtcPathBlocked` | /etc blocked | High |
| `testVarPathBlocked` | /var blocked | High |
| `testUsrPathBlocked` | /usr blocked | High |
| `testBinPathBlocked` | /bin blocked | High |
| `testSbinPathBlocked` | /sbin blocked | High |
| `testRootPathBlocked` | /root blocked | High |
| `testSystemPathBlocked` | /system blocked | High |
| `testProcPathBlocked` | /proc blocked | High |
| `testSysPathBlocked` | /sys blocked | High |
| `testDataDataPathBlocked` | /data/data blocked | High |
| `testFileTooLargeForRead` | File size limit | Medium |
| `testContentTooLargeForWrite` | Content size limit | Medium |
| `testHiddenFileBlockedByDefault` | Hidden files blocked | Low |
| `testCalculateChecksum` | Checksum calculation | Low |

### 3.4 Text Buffer Edge Cases (§2)

**Test File:** `OptimizedTextBufferEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testChunkedLoading` | Chunked file loading | Medium |
| `testChunkedLoadingLargeFile` | 10K line file | Medium |
| `testGetLine` | Get specific line | Low |
| `testGetLineOutOfBounds` | Out of bounds | Low |
| `testGetLines` | Get line range | Low |
| `testEditLine` | Edit line | Low |
| `testInsertLine` | Insert line | Low |
| `testDeleteLine` | Delete line | Low |
| `testSearch` | Search in buffer | Low |
| `testSearchCaseSensitive` | Case-sensitive search | Low |
| `testReplaceAll` | Replace all | Low |
| `testBoundaryAt999Lines` | 999 lines | Low |
| `testBoundaryAt1000Lines` | 1000 lines | Low |
| `testBoundaryAt1001Lines` | 1001 lines | Low |
| `testUnicodeContent` | Unicode content | Medium |
| `testEmojiContent` | Emoji content | Medium |
| `testMixedLineEndings` | Mixed line endings | Low |

### 3.5 IntelliSense Edge Cases (§3)

**Test File:** `IntelliSenseEdgeCaseTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testCompletionItemCreation` | Completion item | Low |
| `testDiagnosticCreation` | Diagnostic creation | Low |
| `testIndexEntryCreation` | Index entry | Low |
| `testLargeSymbolIndex` | 5000 symbols | Medium |
| `testSymbolIndexSearch` | Symbol search | Low |
| `testCircularImportDetection` | Circular imports | Medium |
| `testExtremelyLongIdentifier` | 1000 char identifier | Low |
| `testSyntaxValidatorMissingColon` | Missing colon | Low |
| `testSyntaxValidatorUnmatchedBracket` | Unmatched bracket | Low |
| `testSyntaxValidatorUnterminatedString` | Unterminated string | Low |
| `testQuickFixBatchProcessing` | 1000 quick fixes | Medium |
| `testUnicodeIdentifierCompletion` | Unicode identifiers | Low |

---

## Priority 4: Recovery & Resilience Tests

These tests cover error recovery and resilience.

### 4.1 Exception Recovery (§10)

**Test File:** `ExceptionRecoveryTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testExecuteWithRecoverySuccess` | Successful recovery | Low |
| `testExecuteWithRecoveryRetry` | Retry logic | Medium |
| `testExecuteWithRecoveryMaxRetriesExceeded` | Max retries | Medium |
| `testExecuteWithRecoveryNonRetryableException` | Non-retryable | Medium |
| `testRecoveryState` | Recovery state | Low |
| `testRetryStats` | Retry statistics | Low |
| `testRecoveryHistory` | Recovery history | Low |
| `testClearHistory` | Clear history | Low |
| `testSuccessRate` | Success rate | Low |
| `testMostFailingOperations` | Failing operations | Low |
| `testCircuitBreakerClosed` | Circuit breaker closed | Medium |
| `testCircuitBreakerOpens` | Circuit breaker opens | Medium |
| `testCircuitBreakerRejectsWhenOpen` | Reject when open | Medium |
| `testCircuitBreakerReset` | Reset circuit breaker | Low |
| `testCircuitBreakerSuccessResetsCount` | Success resets count | Low |
| `testCreateRetryableBlock` | Retryable block | Low |
| `testCreateFallbackBlockPrimary` | Fallback primary | Low |
| `testCreateFallbackBlockFallback` | Fallback fallback | Low |

### 4.2 Crash Prevention (§1)

**Test File:** `MemoryOptimizerTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testMemoryReport` | Memory report | Low |
| `testMemoryReportFormattedString` | Formatted report | Low |
| `testLowMemoryDetection` | Low memory detection | Medium |
| `testWatchdogStartStop` | Watchdog operations | Medium |
| `testWatchdogConfig` | Watchdog configuration | Low |
| `testCrashStateNormal` | Normal state | Low |
| `testCrashStateWarning` | Warning state | Medium |
| `testCrashStateCritical` | Critical state | High |
| `testShouldPreventExecution` | Prevent execution | High |
| `testCrashHistorySize` | History size cap | Low |
| `testGetRecentCrashes` | Recent crashes | Low |
| `testClearCrashHistory` | Clear history | Low |
| `testCreateErrorHandler` | Error handler | Low |
| `testSafeExecuteSuccess` | Safe execute success | Low |
| `testSafeExecuteFallback` | Safe execute fallback | Medium |
| `testSafeExecuteWithRetries` | Safe execute retries | Medium |
| `testCrashTypeOutOfMemory` | OOM classification | High |
| `testCrashTypeStackOverflow` | Stack overflow classification | High |
| `testCrashTypeThreadDeath` | Thread death classification | Medium |
| `testCrashTypeUncaughtException` | Uncaught exception | Low |
| `testCrashTypeWatchdogTimeout` | Watchdog timeout | Medium |

---

## Priority 5: Compatibility & Performance Tests

These tests cover compatibility and performance.

### 5.1 Compatibility Matrix (§13)

**Test File:** `CompatibilityMatrixTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testPythonVersionCompatibility` | Python 3.11 | Low |
| `testMinSdkCompatibility` | SDK 26 | Low |
| `testTargetSdkCompatibility` | SDK 35 | Low |
| `testArm64Compatibility` | ARM64 support | Medium |
| `testX86_64Compatibility` | x86_64 support | Medium |
| `testNativeWheelCompatibility` | Native wheel ABI | Medium |
| `testThemeEnumValues` | Theme enumeration | Low |
| `testThemeColors` | Theme colors | Low |
| `testAnsiColor16Colors` | 16 ANSI colors | Low |
| `testAnsiColor256Colors` | 256 ANSI colors | Low |
| `testDebugStateEnum` | Debug states | Low |
| `testCompletionKindEnum` | Completion kinds | Low |
| `testPythonFileExtensionRecognition` | .py extension | Low |
| `testPythonFileExtensionCaseInsensitive` | Case insensitive | Low |
| `testLineEndingHandling` | Line endings | Low |
| `testUTF8Encoding` | UTF-8 encoding | Low |

### 5.2 File Watcher (§2)

**Test File:** `FileWatcherTest.kt`

| Test | Description | Risk |
|------|-------------|------|
| `testFileWatcherCreation` | Create watcher | Low |
| `testFileChangeDataClass` | File change | Low |
| `testChangeTypeValues` | Change types | Low |
| `testFileWatcherStartStop` | Start/stop | Low |
| `testFileWatcherGetRecentChanges` | Recent changes | Low |
| `testFileWatcherClearChanges` | Clear changes | Low |
| `testLazyFileLoaderCreation` | Create loader | Low |
| `testLazyFileLoaderLoadFile` | Load file | Low |
| `testLazyFileLoaderLoadChunk` | Load chunk | Low |
| `testLazyFileLoaderGetTotalChunks` | Total chunks | Low |
| `testLazyFileLoaderGetLoadedChunks` | Loaded chunks | Low |
| `testLazyFileLoaderUnloadFile` | Unload file | Low |
| `testLazyFileLoaderClearAll` | Clear all | Low |
| `testLazyFileLoaderMemoryUsage` | Memory usage | Low |

---

## Test Execution Commands

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Class

```bash
./gradlew :data:test --tests "com.pythonide.data.runtime.SandboxSecurityTest"
```

### Run Tests by Priority

```bash
# Priority 1: Critical Security Tests
./gradlew :data:test --tests "com.pythonide.data.runtime.SandboxSecurityTest"
./gradlew :data:test --tests "com.pythonide.data.runtime.SecurityAdversarialTest"
./gradlew :data:test --tests "com.pythonide.data.runtime.ConcurrencyStressTest"

# Priority 2: Core Functionality Tests
./gradlew :data:test --tests "com.pythonide.data.editor.EditorEdgeCaseTest"
./gradlew :data:test --tests "com.pythonide.data.editor.BracketHandlerEdgeCaseTest"
./gradlew :data:test --tests "com.pythonide.data.editor.SearchHandlerEdgeCaseTest"
./gradlew :data:test --tests "com.pythonide.data.debugger.DebuggerProtocolTest"

# Priority 3: Edge Case Tests
./gradlew :data:test --tests "com.pythonide.data.terminal.TerminalEdgeCaseTest"
./gradlew :data:test --tests "com.pythonide.data.editor.SafeFileAccessSecurityTest"
./gradlew :data:test --tests "com.pythonide.data.editor.OptimizedTextBufferEdgeCaseTest"
./gradlew :data:test --tests "com.pythonide.data.intellisense.IntelliSenseEdgeCaseTest"

# Priority 4: Recovery Tests
./gradlew :data:test --tests "com.pythonide.data.runtime.ExceptionRecoveryTest"
./gradlew :data:test --tests "com.pythonide.data.runtime.MemoryOptimizerTest"

# Priority 5: Compatibility Tests
./gradlew :data:test --tests "com.pythonide.data.runtime.CompatibilityMatrixTest"
./gradlew :data:test --tests "com.pythonide.data.editor.FileWatcherTest"
```

---

## Test Coverage Summary

| Module | Test Files | Test Methods | Coverage Areas |
|--------|------------|--------------|----------------|
| data/src/test | 18 existing + 12 new = 30 | ~200+ | Runtime, Editor, Debugger, Terminal, IntelliSense |
| domain/src/test | 5 existing | ~50 | Domain models |
| app/src/test | 0 existing | 0 | **Needs ViewModel tests** |

---

## Notes

1. **MockK Dependency Added**: MockK and Turbine have been added to the version catalog and all modules for mocking and Flow testing.

2. **Test Infrastructure**: Existing tests use `UnconfinedTestDispatcher` with `Dispatchers.setMain()` for deterministic coroutine execution.

3. **Missing Test Areas** (requiring Android context):
   - ViewModel tests (app/src/test)
   - Compose UI tests (app/src/androidTest)
   - Room database tests (data/src/androidTest)
   - DataStore tests (data/src/androidTest)

4. **Performance Testing**: Consider adding:
   - Benchmark tests using AndroidX Benchmark
   - Memory leak detection with LeakCanary in debug builds
   - ANR detection in UI tests

5. **Security Testing**: The security tests focus on:
   - Path traversal prevention
   - Symlink attack prevention
   - Resource exhaustion prevention
   - Input validation
   - Sandboxed execution verification
