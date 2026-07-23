# Python IDE for Android

A fully offline Python IDE for Android devices, built with modern Android development practices.

## Features

- **Kotlin Only** - 100% Kotlin codebase, no Java
- **Jetpack Compose** - Modern declarative UI framework
- **Material Design 3** - Latest Material Design with Dynamic Colors support
- **Offline First** - Works completely offline, no internet required
- **Clean Architecture** - MVVM pattern with Repository pattern
- **Multi-Module** - Organized into feature modules

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Navigation Compose
- **Storage**: Room + DataStore
- **Build System**: Gradle Kotlin DSL with Version Catalog
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

## Project Structure

```
python-ide/
├── app/                          # Main application module
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/pythonide/app/
│           ├── PythonIDEApp.kt       # Application class
│           ├── MainActivity.kt       # Main activity
│           ├── di/                   # Hilt modules
│           ├── navigation/           # Navigation graph
│           ├── screens/              # UI screens
│           │   ├── home/            # Home screen
│           │   ├── editor/          # Code editor
│           │   └── settings/        # Settings screen
│           ├── ui/theme/            # Theme system
│           └── viewmodel/           # ViewModels
├── domain/                       # Domain layer
│   └── src/main/java/com/pythonide/domain/
│       ├── model/                 # Data models
│       └── repository/            # Repository interfaces
├── data/                         # Data layer
│   └── src/main/java/com/pythonide/data/
│       ├── local/                 # Local data sources
│       ├── repository/            # Repository implementations
│       └── di/                    # Data layer modules
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   └── wrapper/
├── build.gradle.kts              # Project-level build
├── settings.gradle.kts           # Module configuration
└── gradle.properties             # Gradle properties
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 35

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/python-ide-android.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build the project

4. Run on an emulator or physical device

## Build

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
./gradlew test
```

### Run Lint
```bash
./gradlew lint
```

## Architecture

This project follows Clean Architecture principles:

- **Domain Layer**: Contains business logic, models, and repository interfaces
- **Data Layer**: Contains repository implementations and data sources
- **Presentation Layer**: Contains UI, ViewModels, and navigation

## Theming

The app supports:
- **Light Theme**: Default light color scheme
- **Dark Theme**: Easy on the eyes dark mode
- **System Default**: Follows system theme settings
- **Dynamic Colors**: Material You colors on Android 12+

## License

This project is licensed under the MIT License - see the LICENSE file for details.
