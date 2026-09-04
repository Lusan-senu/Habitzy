# Habitzy

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Compose-1.7.x-brightgreen.svg)](httpsdeveloper.android.com/jsix/ui/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.x-orange.svg)](https://kotlinlang.org)

**Habitzy** is a modern, local-first Android habit-tracking application built with **Jetpack Compose** and **Material Design 3 Expressive**. Track your daily habits, view insightful analytics, and maintain your routine—all completely offline with no analytics or ads.

---

## 📱 Overview

Habitzy helps you build and maintain positive habits through a clean, expressive UI designed per Google's Material 3 Expressive guidelines. The app features:

- **Three habit types**: Binary (yes/no), Amount (numeric goals), and Checklist (sub‑steps)
- **Scheduling flexibility**: Daily, specific weekdays, times per week/month, every N days
- **Rich insights**: Streak tracking, completion rates (7/30/90/day/all‑time), consistency scores, heatmaps, and trend charts
- **Per‑habit customization**: Icons, colors, notes, reminders, vacation/pause mode, archiving
- **Local‑only data**: Room database + DataStore preferences, no network calls, no backend
- **Backup & import**: JSON export/import and CSV import support
- **Material 3 Expressive**: Dynamic color, expressive motion, elevated tonal elevation, and shape morphing

---

## 🛠️ Technology Stack

| Layer | Technology |
| --- | --- |
| **UI** | Jetpack Compose 1.7.x, Material Design 3 Expressive |
| **Architecture** | MVVM (ViewModel + Repository pattern) |
| **Dependency Injection** | Hilt 2.56 |
| **Local Database** | Room 2.7.0 |
| **Preferences** | Jetpack DataStore (Preferences) 1.1.x |
| **Networking** | (None — local‑only app) |
| **Charting** | Vico Compose M3 themed charts |
| **Image Loading** | Coil 3 |
| **Background Tasks** | WorkManager 2.10.0 |
| **JSON Serialization** | Kotlinx Serialization 1.7.x |
| **Date/Time** | kotlinx-datetime 0.6.x |
| **Reordering** | Compose Reorderable library |
| **Color Generation** | MaterialKolor (PaletteStyle.Expressive) |

---

## 📦 Features (Detailed)

### Habit Tracking

- **Binary habits** – One‑tap toggle for completed/undone
- **Amount habits** – Numeric progress toward a daily goal (e.g., 8 glasses water, 10 000 steps)
- **Checklist habits** – 2‑8 sub‑steps; day completed when all checked
- **Scheduling options**:
  - Every day
  - Specific days of the week (multi‑select)
  - X times per week (any N days)
  - X times per month
  - Every N days (rolling interval)
- **Per‑habit configuration**:
  - Name & optional description/note
  - Icon (bundled glyph or emoji picker)
  - Color from a curated M3 tonal palette (or custom picker)
  - Reminder(s) with editable message, per scheduled days
  - Priority / drag‑reorder sort order
  - Category / tag (free‑text, lightweight autocomplete)
  - Vacation/pause mode (date range; excluded from streaks)
  - Archive (soft‑delete: hidden but history retained, restorable)

### Logging & Back‑fill

- One‑tap toggle from habits list
- Tap‑and‑hold or stepper for amount progress
- Back‑fill any past day from Habit Detail
- Day notes (long‑press → text + optional single photo)
- Undo Snackbar after every toggle

### Insights Screen

- **Per‑habit metrics**: current streak, best streak ever, total completions, completion rate (7/30/90/all), consistency strength score
- **Activity grid**: GitHub‑style year/week/month heatmap
- **Streak‑over‑time line chart**
- **Global metrics**: overall completion rate, per‑habit ranking bars, "most consistent hour" histogram, best current streaks leaderboard, monthly heatmap, weekly completions trend (last 12 weeks)

### Settings

- **Account**: Profile name & photo (local only)
- **Appearance**: Theme mode (light/dark/system), dynamic color toggle, true‑black AMOLED option, accent seed color picker
- **Preferences**: Week start day, default reminder time, auto‑backup schedule
- **Data**: Manual JSON backup/restore, CSV import, scheduled local backups via WorkManager, reset all data
- **About**: Version info, licenses

### Navigation

- **Bottom navigation bar** (phones portrait) with 3 destinations: **Habits**, **Insights**, **Settings**
- **Profile** accessed via avatar chip in top app bar (not a 4th tab)
- **Habit Detail** and **Add/Edit Habit** pushed as routes
- **Adaptive scaffold**: NavigationBar → NavigationRail (landscape) → NavigationDrawer (tablets) via `NavigationSuiteScaffold`

---

## 📋 Prerequisites

- **Android Studio** (Ladybug or newer recommended)
- **JDK 17** (or JDK 21 for newer Compose versions)
- **Android SDK** API 35 (target) / API 26 (min)
- **Gradle** 8.2+ (included with Android Studio)
- A physical device or emulator running Android 8.0+ (API 26)

---

## 🏁 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Lusan-senu/Habitzy.git
cd Habitzy
```

### 2. Open in Android Studio

- Launch Android Studio → **Open** → select the `Habitzy` directory
- Gradle will sync automatically; accept any SDK/NDK prompts

### 3. Build the project

```bash
./gradlew assembleDebug
```

- On first build, Gradle will download dependencies (Compose BOM, Hilt, Room, etc.)
- Build succeeds when `BUILD SUCCESSFUL` appears

### 4. Run the app

- Connect a device via USB or start an emulator
- In Android Studio: **Run → app** (green play button)
- Or from CLI: `./gradlew installDebug && adb shell am start -n com.htj.habitzy/.MainActivity`

### 5. First‑run permissions

- The app will request **POST_NOTIFICATIONS** permission the first time you enable a reminder (Android 13+)
- Accept the permission to use reminder functionality

---

## 📁 Project Structure

```
Habitzy/
├── gradle/                  # Gradle wrapper scripts & configs
├── app/
│   ├── build.gradle.kts     # App‑level dependencies & plugins
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # App declaration, permissions, Hilt setup
│   │   │   ├── java/com/htj/habitzy/
│   │   │   │   ├── HabitzyApp.kt          # @HiltAndroidApp entry point
│   │   │   │   └── MainActivity.kt        # Single‑activity, hosts NavHost + theme
│   │   │   └── res/
│   │   │       ├── layout/                  # Compose setContent files
│   │   │       ├── drawable/                # Vector icons, app logo
│   │   │       └── values/                  # Strings, styles, themes
│   │   ├── java/com/htj/habitzy/data/     # Room entities, DAOs, Database
│   │   │   ├── repository/                # Repository implementations
│   │   │   └── backup/                    # JSON/CSV backup logic
│   │   ├── java/com/htj/habitzy/domain/   # Domain models (use‑cases agnostic of DB)
│   │   │   ├── model/                     # Habit, HabitLog, StreakInfo, InsightSummary
│   │   │   └── repository/                # Interfaces consumed by ViewModels
│   │   ├── java/com/htj/habitzy/ui/       # All Compose UI code
│   │   │   ├── theme/                     # Color.kt, Shape.kt, Typography.kt, Motion.kt, Elevation.kt, Theme.kt
│   │   │   ├── navigation/                # NavHost, Destinations, FloatingNavCluster, TopActionPill
│�   │   │   ├── components/                # Shared composables (HabitCard, StreakBadge, ProgressRing, etc.)
│   │   │   ├── habits/                    # HabitsScreen, HabitsViewModel, HabitsUiState
│   │   │   ├── habitdetail/               # HabitDetailScreen, ViewModel
│   │   │   ├── addedithabit/              # AddEditHabitScreen, ViewModel
│   │   │   ├── insights/                  # InsightsScreen, ViewModel
│   │   │   ├── settings/                  # SettingsScreen + sub‑screens (Account, Appearance, Data, About)
│   │   │   └── profile/                   # ProfileScreen, ViewModel
│   │   └── notifications/                 # NotificationScheduler, ReminderReceiver, BootCompletedReceiver
│   ├── src/androidTest/                   # Instrumented tests
│   └── src/test/                          # Unit tests
├── schemas/                               # Exported Room schema JSON files
├── guide.md                               # Phase‑by‑phase implementation guide (internal)
├── plan.md                                # Navigation shell & component plan (internal)
└── README.md  ←  *you are here*
```

---

## 🏛️ Architecture

Habitzy follows a **strict MVVM** layered architecture:

1. **UI Layer** (`ui/`)
   - Composables read `StateFlow<UiState>` from `@HiltViewModel`‑scoped ViewModels
   - Never talk directly to Room or DataStore
   - Send one‑shot user intents via ViewModel functions
   - Domain model types (`domain/model/`) cross the boundary; Room entities never leak

2. **ViewModel Layer** (`data/repository/` + `domain/usecase/`)
   - Hold UI state and expose it as Flows
   - Depend on **repository interfaces** from `domain/repository/`
   - Delegate streak/insight math to pure functions (`domain/usecase/`) — easily unit‑testable

3. **Repository Layer** (`data/repository/`)
   - Implement domain interfaces
   - Coordinate Room DAOs + DataStore + WorkManager
   - Map Room `Entity` → domain `Model` at the boundary

4. **Data Layer** (`data/local/`)
   - **Room**: Schema defined via entities/DAOs, accessed through `HabitzyDatabase`
   - **DataStore**: Typed wrappers for app preferences (`AppPreferences`, `ProfilePreferences`)
   - **Converters**: `LocalDate<->epochDay`, enum serializers, List<Int>↔String

5. **Domain Layer** (`domain/`)
   - Plain Kotlin data classes (`Habit`, `HabitLog`, `StreakInfo`, etc.)
   - Repository **interfaces** (contract for ViewModels)
   - **Use cases** (pure functions: `ComputeStreakUseCase`, `ComputeInsightsUseCase`, etc.)
   - No Android dependencies — fully testable with JUnit

---

## 🛠️ Available Scripts

| Script | Description |
| --- | --- |
| `./gradlew assembleDebug` | Build a debug APK (no signing config needed) |
| `./gradlew assembleRelease` | Build a release APK (requires `signingConfigs`) |
| `./gradlew :app:lintDebug` | Run Android Lint to catch UI/architecture issues |
| `./gradlew :app:testDebugUnitTest` | Run unit tests (JUnit + Kotlin test suites) |
| `./gradlew :app:connectedDebugAndroidTest` | Run composed UI tests on device/emulator |
| `./gradlew dependencies` | Print resolved dependency tree (useful for version bumps) |

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository and **clone** locally
2. Create a new branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes following the existing commit message convention
4. **Push** to your fork: `git push origin feature/amazing-feature`
5. Open a **Pull Request** against the `main` branch

**Development checklist:**
- Keep the build green: `assembleDebug + lintDebug` must pass
- Run existing unit tests: `testDebugUnitTest` green
- If adding a new screen or component, update the `ComponentsCatalogScreen` (temporary demo screen) to showcase it
- Follow the **Material 3 Expressive** design tokens defined in `ui/theme/` — never hard‑code hex colors or raw dp values in screen code
- Add content descriptions on all interactive icons for accessibility
- Ensure the floating nav cluster hides on detail/add/edit/settings screens (driven by back‑stack destination)

---

## 📄 License

```
MIT License

Copyright (c) 2026 Lusan-senu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📸 Screenshots

<div align="center">
  <img src="screenshots/habits_screen_light.png" alt="Habits screen (light theme)" width="250"/>
  <img src="screenshots/habits_screen_dark.png" alt="Habits screen (dark theme)" width="250"/>
  <img src="screenshots/insights_screen.png" alt="Insights screen" width="250"/>
  <img src="screensettings_screen.png" alt="Settings screen" width="250"/>
</div>

<div align="center">
  <img src="screenshots/habit_detail.png" alt="Habit Detail screen" width="250"/>
  <img src="screenshots/add_edit_habit.png" alt="Add/Edit Habit screen" width="250"/>
  <img src="screenshots/backup_import.png" alt="Backup/Import screen" width="250"/>
</div>

*(Add your own screenshots in `app/src/main/res/drawable` or `docs/` and reference them via relative paths above. Supported formats: PNG, WebP, JPEG.)*

---

## 🙏 Acknowledgments

- Inspiration from [InlitX/streak](https://github.com/InlitX/streak) and [shub39/Grit](https://github.com/shub39/Grit) — habit‑tracking patterns reimplemented from scratch
- Material Design 3 Expressive guidelines <https://m3.material.io/>
- MaterialKolor library for generative color schemes
- Vico for themed charting Compose components