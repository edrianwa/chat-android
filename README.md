# Phoenix (SecureChat Android)

A privacy-focused messaging app disguised as a casual game launcher. Uses a passcode-based routing system where different PINs unlock different screens — either the real encrypted chat or a decoy game.

## Project Structure

```
securechat-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/securechat/phoenix/
│   │   │   ├── PhoenixApp.kt                    # Hilt Application class
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt                 # Hilt dependency injection module
│   │   │   ├── data/
│   │   │   │   ├── PasscodeRepository.kt        # Passcode data access interface
│   │   │   │   └── SecurePasscodeStore.kt       # Encrypted DataStore implementation
│   │   │   ├── navigation/
│   │   │   │   ├── Destinations.kt              # Navigation route definitions
│   │   │   │   ├── PhoenixNavHost.kt            # Main navigation graph
│   │   │   │   ├── PasscodeRouter.kt            # Passcode → destination routing
│   │   │   │   └── ScreenRegistry.kt            # Modular screen registration
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt              # Entry point activity
│   │   │       ├── theme/
│   │   │       │   └── PhoenixTheme.kt          # Material3 theming
│   │   │       └── screens/
│   │   │           ├── passcode/
│   │   │           │   ├── PasscodeScreen.kt    # PIN-pad entry UI
│   │   │           │   └── PasscodeViewModel.kt # Passcode logic & routing
│   │   │           ├── setup/
│   │   │           │   ├── SetupScreen.kt       # First-time setup UI
│   │   │           │   └── SetupViewModel.kt    # Setup logic & validation
│   │   │           ├── chat/
│   │   │           │   └── ChatScreen.kt        # Secure chat (placeholder)
│   │   │           └── decoy/
│   │   │               └── DecoyGameScreen.kt   # Decoy game screen
│   │   ├── res/
│   │   │   ├── drawable/                        # Icon assets (phoenix flame)
│   │   │   ├── mipmap-anydpi-v26/              # Adaptive icon
│   │   │   ├── values/                          # Strings, themes
│   │   │   └── xml/                             # Backup rules
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts                             # Root build config
├── settings.gradle.kts                          # Project settings
└── gradle.properties
```

## Architecture

### Tech Stack

- **Kotlin** with **Jetpack Compose** for UI
- **Hilt** for dependency injection
- **Navigation Compose** for screen routing
- **DataStore Preferences** with SHA-256 hashing for secure passcode storage
- **Coroutines + StateFlow** for reactive state management

### Key Design Decisions

**Passcode Router System**
- `PasscodeRouter` resolves entered passcodes to navigation routes
- `ScreenRegistry` allows modular registration of new destination screens
- Passcodes are SHA-256 hashed before storage — raw values never persisted

**Security**
- Passcodes stored as SHA-256 hashes in DataStore
- DataStore file excluded from cloud backup
- App appears as "Phoenix" with a game-like flame icon in launcher

**Modular Screens**
- New screens can be registered via `ScreenRegistry.registerScreen()`
- Each screen needs: unique ID, display name, navigation route
- System supports 2+ passcodes with independent routing

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Building

Open the `securechat-android/` directory in Android Studio and sync Gradle.

```bash
# Command-line build
./gradlew assembleDebug
```

### App Flow

1. **First Launch** → Setup screen (create chat + decoy PINs)
2. **Subsequent Launches** → Passcode screen (PIN pad)
3. **Correct Chat PIN** → Navigates to Secure Chat
4. **Correct Decoy PIN** → Navigates to Decoy Game
5. **Wrong PIN** → Error shake, clears input

## Branding

- **Launcher Name**: Phoenix
- **Icon**: Stylized flame/phoenix shape (game-like aesthetic)
- **Theme**: Orange/amber tones inspired by fire
