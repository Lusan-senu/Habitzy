# Habitzy — Full Build Guide for OpenCode AI

> **Purpose of this file:** This is a self‑contained implementation plan for an Android habit‑tracking app called **Habitzy**. It is written to be fed to **OpenCode AI** (running inside Android Studio's terminal or as a CLI agent pointed at this project folder) phase‑by‑phase. Each phase below is scoped so you can paste it as a single instruction to OpenCode and get a compilable, testable result before moving to the next phase.
>
> **How to use this file with OpenCode:**
> 1. Create a new empty **Android Studio** project first (`Empty Activity`, Compose, Kotlin, min SDK 26, target/compile SDK 35). Commit it to git immediately (`git init && git add -A && git commit -m "init"`).
> 2. Put this file at the project root as `guide.md`.
> 3. Open OpenCode in the project root: `opencode`.
> 4. Feed OpenCode **one Phase at a time** (copy the Phase heading + its subsections into the prompt, or say *"Read guide.md and implement Phase 2 only, then stop and let me build/run"*). Do not ask it to do the whole file in one shot — Android projects compile far more reliably in small, verified increments.
> 5. After each phase: `./gradlew assembleDebug`, fix any compile errors with OpenCode, run on an emulator/device, **commit**, then move to the next phase.
> 6. Whenever OpenCode is unsure of an exact Compose Material 3 API name, tell it to check the installed `androidx.compose.material3` artifact sources (Android Studio → "Go to declaration") rather than guessing, since the M3 Expressive APIs are newer than most training data.
>
> This document is intentionally exhaustive. It repeats key constraints in multiple places on purpose so that a coding agent working phase‑by‑phase (with only partial context) does not drift from the spec.

---

## 0. Project Snapshot

| | |
|---|---|
| **App name** | Habitzy |
| **Package name** | `com.htj.habitzy` |
| **Platform** | Android (native), Kotlin, Jetpack Compose |
| **Min SDK** | 26 (Android 8.0) |
| **Target/Compile SDK** | 35 (Android 15) — bump to latest stable when you build |
| **Architecture** | MVVM + simple layered (UI → ViewModel → Repository → Room/DataStore) |
| **DI** | Hilt |
| **Local DB** | Room |
| **Preferences** | Jetpack DataStore (Proto or Preferences DataStore) |
| **Design system** | Material 3 **Expressive** — colors, shape, elevation, typography, motion, icons, strictly per https://m3.material.io/ |
| **Scope** | Habit tracking + insights only. **No** to‑do lists, no focus timer, no gamification/voxel island, no social/sync features, no accounts backend. Everything is local‑first, offline, single‑user. |
| **Inspiration (features only, not code/assets)** | [InlitX/streak](https://github.com/InlitX/streak) (schedules, activity grid, notes, vacation mode, backup/import, widgets, reminders w/ actions) and [shub39/Grit](https://github.com/shub39/Grit) (habit maps, dynamic color via MaterialKolor, Compose‑first structure) — reimplemented from scratch in Kotlin/Compose for Habitzy, not copied. |

### 0.1 Screens (exact scope — do not add extra top‑level screens)

```
Bottom navigation (3 destinations) + Profile entry point in the top bar:
├── Habits (home)              ← default start destination
│     └── Habit Detail          (pushed from a habit card)
│     └── Add / Edit Habit      (sheet or full screen, pushed from FAB or edit action)
├── Insights
├── Settings
│     ├── Account
│     ├── Appearance
│     ├── Preferences
│     ├── Data (backup / restore / import / export)
│     └── About
└── Profile                     (photo + name; opened from an avatar in the top app bar, not a 4th nav-bar tab)
```

**Navigation model:** Habits, Insights, Settings are the 3 items in the bottom `NavigationBar` (or `NavigationRail`/`NavigationSuiteScaffold` on larger widths — see §6.7). Profile is **not** a 4th bottom‑nav tab; it is reached by tapping the avatar/profile chip that lives in the top app bar of the Habits screen (this matches "one screen for profile" being a peer of Settings, not nested under it). Habit Detail and Add/Edit Habit are pushed routes, not nav‑bar destinations. Settings' five rows (Account, Appearance, Preferences, Data, About) are pushed sub‑screens from a single Settings list screen.

### 0.2 Habit tracking feature set (final scope, synthesized from Streak + Grit + "Everyday" style apps)

**Habit types**
- **Binary (Yes/No)** — one tap marks the day done/undone.
- **Amount / Quantity** — a numeric goal per day (e.g. "8 glasses", "10 000 steps") with a unit label; logging adds progress toward the goal, day is "done" when goal is met.
- **Checklist** — a habit broken into 2–8 sub‑steps; the day is "done" when all steps are checked.

**Scheduling**
- Every day
- Specific days of the week (multi‑select Mon–Sun)
- X times per week (any N days, order doesn't matter)
- X times per month
- Every N days (rolling interval from last completion)

**Per‑habit configuration**
- Name, optional short description/note
- Icon (from a bundled icon set) **or** a single emoji
- Color (from a curated M3 tonal palette, or a custom color via a color picker that regenerates a dynamic scheme)
- Reminder(s): one or more times of day, on the days the habit is scheduled, each with an editable short message
- Priority/sort order (manual drag‑reorder on the Habits screen)
- Category/group tag (optional, free‑text with autocomplete of previously used tags — lightweight, no forced taxonomy)
- Vacation/pause mode: a date range during which the habit does not count against streaks or completion rate
- Archive (soft delete: hidden from Habits screen and reminders, history retained, restorable from Settings → Data or an "Archived habits" row)

**Logging**
- One‑tap toggle for binary habits directly on the Habits screen list item
- Tap‑and‑hold or a stepper for amount habits to add progress without leaving the list
- Back‑fill: tapping any past day (in Habit Detail's calendar/grid) lets the user mark/unmark/adjust that day
- Day notes: long‑press a day in the detail screen to attach a short text note (and optionally a single photo) to that specific day/habit
- Undo affordance (Snackbar with "Undo") after every completion/uncompletion toggle

**Insight metrics (per‑habit, shown in Habit Detail)**
- Current streak, best streak ever
- Completion rate (last 7 / 30 / 90 days / all‑time, selectable)
- Total completions
- A "consistency strength" score (weighted recent‑vs‑old completions, see §11.4 for the exact formula)
- GitHub‑style activity grid (year view, scrollable), plus week and month views
- Streak‑over‑time line chart

**Insight metrics (global, shown in the Insights screen)**
- Overall completion rate across all active habits, today / this week / this month
- Per‑habit completion bars ranked best→worst for the selected period
- "When are you most consistent?" — a heatmap/histogram of completion hour‑of‑day across all logged habits
- Best current streaks leaderboard (top habits by current streak)
- Monthly heatmap (all habits combined, or filterable to one habit)
- Simple trend line: total completions per week over the last 12 weeks

**Data**
- Local‑only Room database; no network calls, no analytics, no ads SDK
- Manual export to a single JSON backup file (SAF file picker) and manual import/restore from a JSON file
- Optional scheduled local backups (daily/weekly) to a folder the user grants access to, using WorkManager
- CSV import mapping (habit name + date columns) as a lightweight universal fallback import path, inspired by Streak's "any CSV" import

---

## 1. Material 3 Expressive Design Foundations

**This section is the single source of truth for visual design.** Every screen spec later in this guide references these tokens by name. OpenCode must implement the actual Kotlin token files described here (§6) and then use *only* those token names in Composables — never hard‑coded hex colors or raw dp corner values inside screen code.

Follow https://m3.material.io/ heavily: this is not "Material 3 inspired," it is a strict implementation of the M3 Expressive spec's five pillars — **color, shape, typography, motion, elevation/containment** — plus its icon guidance.

### 1.1 Color system

M3 replaces fixed color names with **roles** generated algorithmically from one or more **seed colors** via the Material Color Utilities (HCT color space). Habitzy must:

1. Ship a **default seed color** for its brand (choose a vivid, high‑chroma color that reads as "energetic progress" — e.g. a warm coral/orange `#FF5A36` or a vivid teal `#00A896"; pick one and use it consistently in mocks and the default theme).
2. Support **dynamic color** from Android 12+ wallpaper (`dynamicColorScheme`) when the user opts in.
3. Support a **custom accent color** the user can pick per‑app‑theme in Appearance settings (separate from per‑habit colors), regenerating the whole scheme from that seed using the `MaterialKolor` library (`com.materialkolor:material-kolor`), the same approach used by Grit. MaterialKolor wraps Google's `material-color-utilities` and exposes `rememberDynamicColorScheme(seedColor, isDark, style = PaletteStyle.Expressive)` — **use `PaletteStyle.Expressive`**, which is the palette style tuned for M3 Expressive's higher-chroma, more playful tonal palettes (as opposed to `TonalSpot` which is calmer/Material You default).
4. Support **light, dark, and "follow system"** modes, plus a **true‑black AMOLED dark variant** (surface forced to pure `#000000` when enabled, for OLED battery savings — a toggle in Appearance, same idea as Streak's true‑black background option).

**Full color role table to implement** (this is the complete M3 role set — implement all of them in the `ColorScheme`, do not truncate):

| Group | Roles |
|---|---|
| Primary | `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant` |
| Secondary | `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `secondaryFixed`, `secondaryFixedDim`, `onSecondaryFixed`, `onSecondaryFixedVariant` |
| Tertiary | `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`, `tertiaryFixed`, `tertiaryFixedDim`, `onTertiaryFixed`, `onTertiaryFixedVariant` |
| Error | `error`, `onError`, `errorContainer`, `onErrorContainer` |
| Surface | `surface`, `onSurface`, `surfaceVariant` (deprecated but still referenced by some components — prefer `surfaceContainer*`), `onSurfaceVariant`, `surfaceDim`, `surfaceBright`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, `inverseSurface`, `inverseOnSurface`, `inversePrimary` |
| Outline | `outline`, `outlineVariant` |
| Misc | `scrim`, `shadow` |

**Usage rules (enforce in code review / lint comments OpenCode adds):**
- Never use `surface`/`background` for elevated cards that should visually separate from the page — use the **tone‑based surface containers** (`surfaceContainerLow` for the Habits list background if you want subtle depth, `surfaceContainer` for standard cards, `surfaceContainerHigh`/`surfaceContainerHighest` for elevated sheets and dialogs). This is the M3 replacement for opacity‑based elevation overlays.
- Always pair a color with its `on*` counterpart for text/icon contrast — never assume `onSurface` on a `primaryContainer` background.
- Reserve `primary`/`primaryContainer` for the single most important action per screen (FAB, the "mark done" affordance, the selected nav item). Use `secondary`/`tertiary` container roles for less prominent UI (chips, filter pills, category tags) — this is exactly the "expand the opportunity for color expression without competing with primary" guidance from the spec.
- Per‑habit accent colors (chosen by the user per habit) are painted as **small, contained accents** — the leading icon chip, the streak flame, the progress ring — never as a full‑card background tint stronger than the `*Container` role equivalent, so habit cards stay legible in both themes.

### 1.2 Shape system

M3's shape scale is a 10‑step corner‑radius system. Implement the **exact dp values** below as a `Shapes` token object (see §6.3), including the Expressive‑only "Increased" and "ExtraExtraLarge" steps used by larger/hero components:

| Token | Corner radius |
|---|---|
| `none` | 0dp |
| `extraSmall` | 4dp |
| `small` | 8dp |
| `medium` | 12dp |
| `large` | 16dp |
| `largeIncreased` | 20dp |
| `extraLarge` | 28dp |
| `extraLargeIncreased` | 32dp |
| `extraExtraLarge` | 48dp |
| `full` | stadium / fully rounded (use `CircleShape` or a huge percent radius) |

**Component→shape mapping to implement:**
- Buttons (filled/tonal/outlined), chips, small icon containers → `full` (pill) for expressive filled buttons, or `large`/`largeIncreased` for a slightly squarer expressive look — pick **`full` for FAB and primary CTA buttons**, `largeIncreased` for standard filled buttons, to create the "visual tension" the spec calls for between very round and moderately round shapes on the same screen.
- Habit list cards → `extraLarge` (28dp)
- Bottom sheets (Add/Edit Habit sheet) → `extraLargeIncreased` (32dp) top corners only
- Dialogs → `extraLarge`
- Small chips/tags/badges → `small` or `full`
- Text fields → `small` (M3 default) — do not over‑round input fields, it hurts legibility of the caret/underline affordance
- The circular progress ring around a habit's daily amount progress and the streak "flame" badge → `full`
- Use **shape morphing** (Compose `Shape` animation via `animateFloatAsState` interpolating between two `RoundedPolygon`s, or the `androidx.graphics.shapes` library) on: the FAB when it flips between "add" and "close" contexts, and on a habit card's checkbox as it toggles from unchecked (square‑ish rounded) to checked (circle) — this is a hero "shape morphing" moment referenced directly by the spec.

### 1.3 Typography

Implement the full **type scale** (5 categories × 3 sizes = 15 base styles), and additionally the **Expressive "Emphasized" variants** for the styles used in hero moments (screen titles, streak numbers, empty‑state headlines):

| Category | Small | Medium | Large |
|---|---|---|---|
| Display | 36sp / 44sp line‑height | 45sp / 52sp | 57sp / 64sp |
| Headline | 24sp / 32sp | 28sp / 36sp | 32sp / 40sp |
| Title | 14sp / 20sp (medium weight) | 16sp / 24sp (medium weight) | 22sp / 28sp |
| Body | 12sp / 16sp | 14sp / 20sp | 16sp / 24sp |
| Label | 11sp / 16sp (medium weight) | 12sp / 16sp (medium weight) | 14sp / 20sp (medium weight) |

- Use a **variable font** if available (e.g. bundle `Roboto Flex` in `res/font/`) so "Emphasized" styles can push `wght` and `GRAD` axes up for big streak numbers and headline moments without needing a second font file. If a variable font proves too fiddly to wire up, fall back to pairing a Regular + a heavier static weight (e.g. Roboto Regular + Roboto Bold/Black) for the emphasized variants — note this as a deliberate simplification in a code comment.
- Big streak counters (e.g. "🔥 42" on Habit Detail) use `displayMedium`/`displayLarge` **Emphasized**, not a one‑off custom `fontSize`.
- Sentence case everywhere in UI copy (button labels, titles, snackbars) — never Title Case or ALL CAPS, per M3 UX‑writing guidance. E.g. "Add habit", not "Add Habit" or "ADD HABIT".

### 1.4 Elevation & containment

M3 has mostly replaced shadow‑based elevation with **tonal (color) elevation** using the surface‑container roles from §1.1. Still implement the 6 elevation levels for shadow use on truly floating elements (FAB, dragged list item, modal bottom sheet, dialogs, snackbars, dropdown menus):

| Level | dp | Typical use |
|---|---|---|
| 0 | 0dp | Base page background |
| 1 | 1dp | Filled cards at rest |
| 2 | 3dp | Cards on hover/elevated list rows |
| 3 | 6dp | FAB at rest, dragged card |
| 4 | 8dp | Navigation drawer |
| 5 | 12dp | Modal bottom sheet, dialog |

Rule of thumb enforced across all screens: **raise a surface's elevation level (shadow) *and* step it one level up the surface‑container tonal scale together** — never one without the other — so light and dark themes both read the hierarchy correctly.

### 1.5 Motion

Implement M3 Expressive's **physics‑based spring motion**, not fixed‑duration easing curves, for all interactive transitions. Define one small `MotionScheme` object (see §6.5) with two named spring specs:

- **Expressive spring** (default scheme for Habitzy — bouncier, used for playful/rewarding moments): `spring(dampingRatio = 0.6f, stiffness = 380f)` for standard component motion (card taps, checkbox toggles, chip selection), and a slightly bouncier `spring(dampingRatio = 0.5f, stiffness = 300f)` reserved for **celebration moments only** (completing the last habit of the day, hitting a new best streak) — implemented via a short confetti/burst micro‑animation plus a scale‑bounce on the streak badge.
- **Standard spring** (calmer, used for navigation container transforms and anything that shouldn't feel "bouncy," e.g. opening Settings): `spring(dampingRatio = 1f, stiffness = 300f)` (critically damped, no overshoot).

Apply these consistently:
- Screen‑to‑screen navigation (Habits → Habit Detail): **container transform** — the tapped card should visually morph/expand into the detail screen's header area, not just cross‑fade. Use `SharedTransitionLayout`/`sharedBounds` (Compose Navigation's shared‑element APIs) with the standard spring.
- Bottom‑nav switching: **shared axis** (slight horizontal slide + fade) between Habits/Insights/Settings.
- FAB → Add/Edit sheet: FAB shape‑morphs into the sheet's drag handle area origin point (expressive spring).
- Checkbox / binary‑habit toggle: shape morph square→circle + a quick scale bounce (expressive spring) + haptic feedback (`HapticFeedbackType.Confirm` or a custom light `performHapticFeedback`).
- List reordering (drag‑to‑reorder habits): item lifts one elevation level, scales to 1.03×, with the expressive spring; other items slide out of the way with the standard spring.
- Loading states: skeleton shimmer, never a blocking spinner dialog, for the Habits list and Insights charts.

### 1.6 Icons

- Use **Material Symbols** (the current icon set that replaced Material Icons), specifically the **Rounded** style at default `wght=400, FILL=0, GRAD=0, opsz=24`, switching to `FILL=1` for the *selected* state of bottom‑nav icons and toggled/active icon buttons (this fill‑on‑select behavior is the standard M3 nav icon pattern).
- Bundle the small fixed set of icons Habitzy actually needs as vector drawables (`ImageVector`s) rather than pulling in the entire `androidx.compose.material:material-icons-extended` artifact (it's huge); the core icon set for the app is listed in §5.6 (habit category icon picker) plus nav icons: `home`/`list_alt` (Habits), `insights`/`bar_chart` (Insights), `settings`, `person`/`account_circle` (Profile), `add`, `check`, `edit`, `delete`, `archive`, `notifications`, `palette`, `backup`, `download`/`upload`, `info`.
- Habit icons: bundle ~40–60 simple rounded glyphs across common categories (health, fitness, mind, learning, finance, chores, social, creativity) **or** let the user pick any emoji instead (simpler to implement first — ship emoji picker in v1, add curated icon set as a fast‑follow if time allows).

---

## 2. Phase 1 — Project Setup & Dependencies

**Give this whole section to OpenCode as "Phase 1."** Goal: an empty app that builds, runs, shows a blank Compose screen themed with Habitzy's M3 Expressive color scheme, with all libraries wired and Hilt bootstrapped.

### 2.1 Gradle version catalog (`gradle/libs.versions.toml`)

Ask OpenCode to create/update the version catalog with (versions are a **starting point** — instruct OpenCode to resolve each to the actual latest stable release available at build time via Android Studio's suggestions, since this guide may be read months after being written):

```toml
[versions]
agp = "8.7.0"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2025.01.00"
navigationCompose = "2.8.5"
hilt = "2.56"
hiltNavigationCompose = "1.2.0"
room = "2.7.0"
datastore = "1.1.1"
materialKolor = "2.0.0"
coil = "3.0.4"
workManager = "2.10.0"
kotlinxSerializationJson = "1.7.3"
composeReorderable = "2.4.3"
vico = "2.1.1"
kotlinxDatetime = "0.6.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-ui = { module = "androidx.compose.ui:ui" }
androidx-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-material3 = { module = "androidx.compose.material3:material3" }
androidx-material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive" }
androidx-material3-adaptive-layout = { module = "androidx.compose.material3.adaptive:adaptive-layout" }
androidx-material3-adaptive-navigation = { module = "androidx.compose.material3.adaptive:adaptive-navigation" }
androidx-material3-adaptive-navigation-suite = { module = "androidx.compose.material3:material3-adaptive-navigation-suite" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
materialKolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }
hilt-work = { module = "androidx.hilt:hilt-work", version.ref = "hiltNavigationCompose" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
compose-reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "composeReorderable" }
vico-compose-m3 = { module = "com.patrykandpatrick.vico:compose-m3", version.ref = "vico" }
vico-core = { module = "com.patrykandpatrick.vico:core", version.ref = "vico" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

**Library purpose reference (so OpenCode knows why each is there):**

| Library | Why |
|---|---|
| Compose Material 3 (+ adaptive/navigation-suite) | The M3 Expressive component set; `material3-adaptive-navigation-suite` gives us the `NavigationSuiteScaffold` that auto‑switches bottom bar ↔ nav rail by window size class (§6.7) |
| Hilt + hilt-navigation-compose + hilt-work | Dependency injection for repositories/DB/DataStore into ViewModels and WorkManager workers |
| Room | Local database for habits, logs, notes |
| DataStore (Preferences) | App‑level settings: theme mode, dynamic color toggle, accent seed color, week‑start day, default reminder time, backup schedule |
| MaterialKolor | Generates a full M3 `ColorScheme` from any seed color at `PaletteStyle.Expressive`, used for both the app‑wide accent and per‑habit accent previews |
| Coil 3 | Loads the profile photo and any day‑note photos from local URIs |
| WorkManager + hilt-work | Scheduled local reminders and scheduled local backups |
| kotlinx.serialization | JSON backup/export/import format |
| kotlinx-datetime | Cross‑platform‑safe date/time handling for streak math, avoids `java.time` edge cases pre‑API 26 (not an issue here since minSdk 26 already has `java.time`, but kotlinx‑datetime keeps date math testable/pure) |
| Compose Reorderable (`sh.calvin.reorderable`) | Drag‑to‑reorder the Habits list |
| Vico | Compose‑native charting library (M3‑themed) for the Insights screen's line/bar/heatmap‑style charts |

### 2.2 App module `build.gradle.kts` essentials

Instruct OpenCode to:
- Apply plugins: `android-application`, `kotlin-android`, `kotlin-compose`, `kotlin-serialization`, `ksp`, `hilt-android`, `room`.
- Set `namespace = "com.htj.habitzy"`, `applicationId = "com.htj.habitzy"`, `minSdk = 26`, `targetSdk`/`compileSdk` = latest stable.
- `buildFeatures { compose = true }`.
- `room { schemaLocation("$projectDir/schemas") }` so Room exports JSON schemas for migration testing.
- Enable `vectorDrawables.useSupportLibrary = true`.
- Add a `debug` and `release` build type; for `release`, enable `isMinifyEnabled = true` with a starter `proguard-rules.pro` that keeps Room entities, Hilt‑generated classes, and kotlinx.serialization models (`-keep class com.htj.habitzy.data.** { *; }` plus the standard Room/Hilt/serialization consumer rules — OpenCode should pull the exact rules from each library's own consumer‑proguard file, not invent them).

### 2.3 `Application` class & Hilt bootstrap

```kotlin
@HiltAndroidApp
class HabitzyApp : Application()
```
Register it in `AndroidManifest.xml` (`android:name=".HabitzyApp"`), plus:
- `POST_NOTIFICATIONS` permission (Android 13+) for reminders.
- `RECEIVE_BOOT_COMPLETED` (to reschedule WorkManager/AlarmManager reminders after reboot).
- Request `POST_NOTIFICATIONS` at runtime the first time the user turns on any reminder (not on first app launch — respect the "ask in context" best practice).

### 2.4 Single‑activity setup

One `MainActivity : ComponentActivity` hosting a `HabitzyApp()` root Composable which sets up: the M3 theme (§6), a `SharedTransitionLayout` wrapper, and the `NavHost` (§7). Enable edge‑to‑edge (`enableEdgeToEdge()`) and handle system bar insets per screen using `WindowInsets` — this is required for a proper M3 Expressive look (content should draw behind translucent system bars where appropriate, e.g. behind the status bar on the Habits screen's large top app bar).

### 2.5 Definition of done for Phase 1
- App builds and installs.
- A single Composable screen shows "Habitzy" in `displayMedium` Emphasized style on a `surfaceContainerLowest` background, colored from the default seed via MaterialKolor, confirming the theme pipeline works end‑to‑end.
- Hilt compiles (an empty `@Module` is fine as a placeholder).
- Room compiles with zero entities yet, or a placeholder entity, just to confirm the KSP pipeline works.

---

## 3. Architecture Overview

```
com.htj.habitzy/
├── HabitzyApp.kt                     (Hilt Application)
├── MainActivity.kt
├── di/                               Hilt modules: DatabaseModule, DataStoreModule, RepositoryModule, WorkModule
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── HabitzyDatabase.kt
│   │   │   ├── entity/               HabitEntity, HabitLogEntity, HabitNoteEntity, ReminderEntity
│   │   │   ├── dao/                  HabitDao, HabitLogDao, HabitNoteDao, ReminderDao
│   │   │   └── converter/            Converters.kt (LocalDate<->Long, enums<->String, List<Int><->String)
│   │   └── datastore/                AppPreferences.kt, ProfilePreferences.kt
│   ├── repository/                   HabitRepositoryImpl, InsightsRepositoryImpl, SettingsRepositoryImpl, BackupRepositoryImpl, ProfileRepositoryImpl
│   └── backup/                       BackupModels.kt (serializable DTOs), BackupSerializer.kt, CsvImporter.kt
├── domain/
│   ├── model/                        Habit, HabitType, Schedule, HabitLog, StreakInfo, InsightSummary (plain Kotlin data classes, DB-agnostic)
│   ├── repository/                    interfaces consumed by ViewModels
│   └── usecase/                      GetHabitsForTodayUseCase, ToggleHabitCompletionUseCase, ComputeStreakUseCase, ComputeInsightsUseCase, ExportBackupUseCase, ImportBackupUseCase, ScheduleReminderUseCase (thin, single-purpose classes; optional but recommended for streak/insight math since it's reused across screens & is unit-testable in isolation)
├── ui/
│   ├── theme/                        Color.kt, Shape.kt, Type.kt, Motion.kt, Elevation.kt, Theme.kt
│   ├── navigation/                   HabitzyNavHost.kt, Destinations.kt, NavigationSuite.kt
│   ├── components/                   shared composables: HabitCard, ActivityGrid, StreakBadge, ProgressRing, SectionHeader, EmptyState, HabitzyTopBar, ConfirmDialog, IconOrEmojiPicker, ColorSwatchPicker
│   ├── habits/                       HabitsScreen.kt, HabitsViewModel.kt, HabitsUiState.kt
│   ├── habitdetail/                  HabitDetailScreen.kt, HabitDetailViewModel.kt
│   ├── addedithabit/                 AddEditHabitScreen.kt, AddEditHabitViewModel.kt
│   ├── insights/                     InsightsScreen.kt, InsightsViewModel.kt
│   ├── settings/                     SettingsScreen.kt (+ account/, appearance/, preferences/, data/, about/ subpackages, each with its own Screen+ViewModel)
│   └── profile/                      ProfileScreen.kt, ProfileViewModel.kt
├── notifications/                     NotificationScheduler.kt, ReminderReceiver.kt, ReminderActionReceiver.kt, BootCompletedReceiver.kt
└── widget/                            (optional stretch goal, §14)
```

**Layering rule for OpenCode:** Composables never touch Room/DataStore directly. Composables read `StateFlow<UiState>` from a `@HiltViewModel` and send one‑shot user intents as function calls on the ViewModel. ViewModels depend only on repository **interfaces** from `domain/repository/`, never on `data/local/*` types — this keeps the UI layer swappable and testable. Domain models (`domain/model/`) are the only types that cross from `data/` into `ui/`; Room `*Entity` classes never leak past the repository implementation boundary (map at the repository layer).

---

## 4. Phase 2 — Data Layer (Room, DataStore, Domain Models)

**Give this whole section to OpenCode as "Phase 2."** Goal: all entities, DAOs, the database class, DataStore preference wrappers, domain models, and repository implementations compile and have basic unit tests passing, with no UI yet.

### 4.1 Room entities

```kotlin
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val iconKey: String,              // e.g. "emoji:🏃" or "icon:fitness_run"
    val colorSeedArgb: Int,           // per-habit accent seed color
    val type: String,                 // "BINARY" | "AMOUNT" | "CHECKLIST"
    val amountGoal: Double? = null,   // only for AMOUNT
    val amountUnit: String? = null,   // e.g. "glasses", "km", "pages"
    val checklistItemsJson: String? = null, // JSON list of step labels, only for CHECKLIST
    val scheduleType: String,         // "DAILY" | "SPECIFIC_WEEKDAYS" | "TIMES_PER_WEEK" | "TIMES_PER_MONTH" | "EVERY_N_DAYS"
    val scheduleWeekdaysMask: Int = 0,   // bitmask Mon=1..Sun=64, used when SPECIFIC_WEEKDAYS
    val scheduleTimesTarget: Int = 0,    // used by TIMES_PER_WEEK / TIMES_PER_MONTH
    val scheduleEveryNDays: Int = 0,     // used by EVERY_N_DAYS
    val categoryTag: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpochDay: Long,
    val isArchived: Boolean = false,
    val vacationStartEpochDay: Long? = null,
    val vacationEndEpochDay: Long? = null,
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId"), Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,               // LocalDate.toEpochDay() — one row per habit per calendar day
    val isCompleted: Boolean,
    val amountValue: Double? = null,        // progress value for AMOUNT habits
    val checklistDoneMask: Int = 0,         // bitmask of completed checklist steps
    val completedAtEpochMillis: Long? = null, // for "most consistent hour" insight
)

@Entity(
    tableName = "habit_notes",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")]
)
data class HabitNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    val text: String,
    val photoUri: String? = null,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val hour: Int,
    val minute: Int,
    val message: String? = null,
    val isEnabled: Boolean = true,
)
```

**Notes for OpenCode:**
- Store dates as `epochDay` (`Long`, from `LocalDate.toEpochDay()`), never as formatted strings — this makes range queries, streak math, and timezone handling simple and consistent.
- The unique index on `(habitId, epochDay)` in `habit_logs` enforces "one log row per habit per day" at the DB layer; upserts (`@Insert(onConflict = OnConflictStrategy.REPLACE)`) implement the "tap to toggle" and "back‑fill a past day" behaviors with one DAO method.
- `checklistItemsJson`/`checklistDoneMask`: keep the checklist model simple — up to 32 steps addressable by an `Int` bitmask is more than enough (spec caps at 8 steps anyway), avoiding a whole extra child table for v1.

### 4.2 Type converters

```kotlin
class Converters {
    @TypeConverter fun fromEpochDay(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun toEpochDay(v: LocalDate?): Long? = v?.toEpochDay()
    // Habit type / schedule type enums stored as their .name() String; add @TypeConverter pairs as needed.
}
```

### 4.3 DAOs (representative signatures — implement the full CRUD surface, this is not exhaustive)

```kotlin
@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isArchived = 1 ORDER BY name ASC")
    fun observeArchivedHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeHabit(id: Long): Flow<HabitEntity?>

    @Insert suspend fun insert(habit: HabitEntity): Long
    @Update suspend fun update(habit: HabitEntity)
    @Query("UPDATE habits SET isArchived = :archived WHERE id = :id") suspend fun setArchived(id: Long, archived: Boolean)
    @Query("UPDATE habits SET sortOrder = :order WHERE id = :id") suspend fun updateSortOrder(id: Long, order: Int)
    @Delete suspend fun delete(habit: HabitEntity)
}

@Dao
interface HabitLogDao {
    @Upsert suspend fun upsert(log: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :day")
    suspend fun getLog(habitId: Long, day: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay BETWEEN :start AND :end ORDER BY epochDay ASC")
    fun observeLogsInRange(habitId: Long, start: Long, end: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE epochDay BETWEEN :start AND :end")
    fun observeAllLogsInRange(start: Long, end: Long): Flow<List<HabitLogEntity>>

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :day")
    suspend fun deleteLog(habitId: Long, day: Long)
}
```
Plus analogous `HabitNoteDao` and `ReminderDao`.

### 4.4 `HabitzyDatabase`

```kotlin
@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, HabitNoteEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HabitzyDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun habitNoteDao(): HabitNoteDao
    abstract fun reminderDao(): ReminderDao
}
```
Provide it via a Hilt `@Module @InstallIn(SingletonComponent::class) object DatabaseModule` with `Room.databaseBuilder(...).build()`. Because `version = 1` at first release, no migration is needed yet — but instruct OpenCode to **always** write an explicit `Migration` object for every schema change from v2 onward (never `fallbackToDestructiveMigration()` in a shipped app), and to keep the exported JSON schemas in `app/schemas/` committed to git so migration tests (§13) have something to diff against.

### 4.5 DataStore preferences

Use **Preferences DataStore** (simplest for a handful of scalar settings) with a small typed wrapper:

```kotlin
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val Context.dataStore by preferencesDataStore("habitzy_settings")
    private val ds = context.dataStore

    val themeMode: Flow<ThemeMode>        // SYSTEM | LIGHT | DARK
    val useDynamicColor: Flow<Boolean>
    val accentSeedColor: Flow<Int>
    val useTrueBlack: Flow<Boolean>
    val weekStartDay: Flow<DayOfWeek>     // MONDAY | SATURDAY | SUNDAY
    val defaultReminderHour: Flow<Int>
    val defaultReminderMinute: Flow<Int>
    val autoBackupEnabled: Flow<Boolean>
    val autoBackupFrequencyDays: Flow<Int>
    val appLockEnabled: Flow<Boolean>     // PIN/biometric gate, see §10.4
    suspend fun setThemeMode(mode: ThemeMode)
    // ... one setter per field, all suspend fun writing via ds.edit { }
}
```

A second tiny wrapper, `ProfilePreferences`, stores the local‑only profile name and photo URI (no account/auth system — see §10.1 on why "Account" in Settings is intentionally minimal).

### 4.6 Domain models (DB‑agnostic, what ViewModels actually work with)

```kotlin
data class Habit(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: HabitIcon,                 // sealed class: Emoji(String) | Bundled(key: String)
    val color: Color,
    val type: HabitType,                 // sealed class: Binary | Amount(goal, unit) | Checklist(steps: List<String>)
    val schedule: HabitSchedule,         // sealed class covering the 5 schedule kinds from §0.2
    val categoryTag: String?,
    val isArchived: Boolean,
    val vacationRange: ClosedRange<LocalDate>?,
    val sortOrder: Int,
)

data class HabitLog(val habitId: Long, val date: LocalDate, val isCompleted: Boolean, val amountValue: Double?, val checklistDoneMask: Int, val completedAt: Instant?)

data class StreakInfo(val currentStreak: Int, val bestStreak: Int, val totalCompletions: Int, val completionRate7d: Float, val completionRate30d: Float, val completionRate90d: Float, val completionRateAllTime: Float, val strengthScore: Float)

data class InsightSummary(val overallCompletionRateToday: Float, val overallCompletionRateWeek: Float, val overallCompletionRateMonth: Float, val perHabitRanking: List<HabitRankEntry>, val hourOfDayHistogram: IntArray /* size 24 */, val bestCurrentStreaks: List<HabitRankEntry>, val weeklyCompletionsTrend: List<Int> /* last 12 weeks */)
```

### 4.7 Repository interfaces (in `domain/repository/`) and impls (in `data/repository/`)

```kotlin
interface HabitRepository {
    fun observeActiveHabits(): Flow<List<Habit>>
    fun observeArchivedHabits(): Flow<List<Habit>>
    fun observeHabit(id: Long): Flow<Habit?>
    suspend fun createHabit(habit: Habit): Long
    suspend fun updateHabit(habit: Habit)
    suspend fun archiveHabit(id: Long, archived: Boolean)
    suspend fun deleteHabitPermanently(id: Long)
    suspend fun reorderHabits(orderedIds: List<Long>)
    suspend fun toggleCompletion(habitId: Long, date: LocalDate)
    suspend fun setAmountProgress(habitId: Long, date: LocalDate, value: Double)
    suspend fun setChecklistStep(habitId: Long, date: LocalDate, stepIndex: Int, done: Boolean)
    fun observeLogs(habitId: Long, range: ClosedRange<LocalDate>): Flow<List<HabitLog>>
    suspend fun setDayNote(habitId: Long, date: LocalDate, text: String, photoUri: String?)
}

interface InsightsRepository {
    fun observeStreakInfo(habitId: Long): Flow<StreakInfo>
    fun observeGlobalInsights(period: InsightPeriod): Flow<InsightSummary>
}

interface SettingsRepository { /* thin pass-through over AppPreferences, exposed as domain-friendly Flows/setters */ }
interface BackupRepository {
    suspend fun exportToUri(uri: Uri): Result<Unit>
    suspend fun importFromUri(uri: Uri, strategy: ImportStrategy): Result<ImportSummary>  // strategy: MERGE | REPLACE_ALL
    suspend fun importCsv(uri: Uri, columnMapping: CsvColumnMapping): Result<ImportSummary>
}
interface ProfileRepository { fun observeProfile(): Flow<Profile>; suspend fun setName(name: String); suspend fun setPhotoUri(uri: String?) }
```

Streak/insight math (`ComputeStreakUseCase`, `ComputeInsightsUseCase`) should be **pure functions over `List<HabitLog>` + `HabitSchedule`**, unit‑testable with no Android/Room dependency — put them under `domain/usecase/` and inject the repository only to fetch the raw logs, then hand off to the pure function. See §11.4 for the exact streak/strength algorithm to implement.

### 4.8 Definition of done for Phase 2
- All entities/DAOs/database compile; Room's annotation processor produces no warnings.
- A JUnit test inserts a few `HabitEntity`+`HabitLogEntity` rows via an in‑memory Room DB and asserts the unique‑index upsert behavior works (toggling a day twice returns to "not completed").
- `ComputeStreakUseCase` has unit tests for: a daily habit with a 5‑day streak broken by one gap; a "3 times a week" habit computing current streak correctly across week boundaries; an "every 3 days" habit's rolling interval logic.
- DataStore read/write round‑trips verified with a simple test.

---

## 5. Habit Icon Set Reference (for the icon/emoji picker, §1.6 / §9)

Give OpenCode this category→sample list when building the bundled icon picker grid (used in Add/Edit Habit, §9.3). Ship as `ImageVector`s named `Icons.Habitzy.<Name>` in `ui/theme/HabitzyIcons.kt`, all in Material Symbols Rounded style:

| Category | Sample icons |
|---|---|
| Health | water_drop, medication, favorite (heart), bedtime, air (breathing) |
| Fitness | directions_run, fitness_center, self_improvement (yoga), pool, sports_soccer |
| Mind | psychology, spa, headphones (meditation audio), draw (journaling), menu_book (reading) |
| Learning | school, language, code, piano, palette |
| Finance | savings, account_balance_wallet, receipt_long |
| Chores | cleaning_services, local_laundry_service, checklist, home |
| Social | groups, call, favorite_border (gratitude/relationships) |
| Creativity | brush, music_note, camera_alt, edit_note |
| General/fallback | star, flag, bolt, eco, timer |

If bundling ~40 vectors is too slow to hand‑author, ship the **emoji picker only for v1** (simpler: a `LazyVerticalGrid` over a curated emoji list, e.g. 60–80 common emoji across the same categories) and mark bundled icons as a fast‑follow — call this out explicitly to the user rather than silently skipping it.

---

## 6. Phase 3 — Design System Implementation

**Give this whole section to OpenCode as "Phase 3."** Goal: `ui/theme/*` fully implements §1, and a small `ComponentsCatalog` debug screen (temporary, deletable later) renders every shared component so you can visually confirm the theme before building real screens.

### 6.1 `Color.kt`

```kotlin
val HabitzyDefaultSeed = Color(0xFFFF5A36) // pick final brand seed; document the choice here

@Composable
fun habitzyColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    trueBlack: Boolean,
): ColorScheme {
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else {
        rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme, isAmoled = trueBlack, style = PaletteStyle.Expressive)
    }
    return base
}
```
(`rememberDynamicColorScheme` is MaterialKolor's API; check its actual current signature in the library source once added — the `isAmoled` param, if the installed version doesn't expose it, can be replaced by manually overriding `surface`/`surfaceContainer*` down to pure black post‑hoc when `trueBlack` is true.)

Also define named `Color` constants for per‑habit accent swatches (a curated palette of ~12 vivid, distinct, accessible‑contrast colors spanning the hue wheel) used in the habit color picker (§9.3) — each swatch, when picked, becomes that habit's `colorSeedArgb` and is *not* passed through the whole‑app dynamic scheme (habit colors are painted directly as accents on top of the app's neutral surfaces, not used to re‑theme the whole app).

### 6.2 `Type.kt`
Implement the full scale from §1.3 as a `Typography` instance, plus a second `EmphasizedTypography` object (or extra `TextStyle`s named `displayLargeEmphasized`, `headlineMediumEmphasized`, etc., stored in a `LocalEmphasizedTypography` composition local or just as top‑level constants) for the hero moments called out in §1.3.

### 6.3 `Shape.kt`
```kotlin
val HabitzyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
// Expressive-only extra tokens not part of the base `Shapes` class — expose as separate constants:
val ShapeLargeIncreased = RoundedCornerShape(20.dp)
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)
val ShapeFull = CircleShape
```

### 6.4 `Elevation.kt`
A small object mapping the 6 levels from §1.4 to both a `Dp` shadow elevation and the matching `ColorScheme` surface‑container role, e.g.:
```kotlin
enum class HabitzyElevation(val shadowDp: Dp) { Level0(0.dp), Level1(1.dp), Level2(3.dp), Level3(6.dp), Level4(8.dp), Level5(12.dp) }
@Composable fun HabitzyElevation.surfaceColor(scheme: ColorScheme): Color = when (this) {
    Level0 -> scheme.surface
    Level1 -> scheme.surfaceContainerLow
    Level2 -> scheme.surfaceContainer
    Level3 -> scheme.surfaceContainerHigh
    Level4, Level5 -> scheme.surfaceContainerHighest
}
```

### 6.5 `Motion.kt`
```kotlin
object HabitzyMotion {
    val expressiveSpring: SpringSpec<Float> = spring(dampingRatio = 0.6f, stiffness = 380f)
    val celebrationSpring: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 300f)
    val standardSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 300f)
}
```
Also define reusable `AnimatedContentTransitionScope` helpers: `sharedAxisX()`, `containerTransform()` used by the nav graph (§7), so individual screens don't hand‑roll transition specs inconsistently.

### 6.6 `Theme.kt`
```kotlin
@Composable
fun HabitzyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    seedColor: Color = HabitzyDefaultSeed,
    trueBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = habitzyColorScheme(seedColor, darkTheme, dynamicColor, trueBlack)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HabitzyTypography,
        shapes = HabitzyShapes,
        content = content,
    )
}
```
Read the four params (`darkTheme` resolved from `ThemeMode` + system, `dynamicColor`, `seedColor`, `trueBlack`) from `AppPreferences` at the call site in `MainActivity`/root Composable via `collectAsStateWithLifecycle()`, so changing Appearance settings recomposes the whole app's theme live.

### 6.7 Adaptive scaffold
Use `NavigationSuiteScaffold` (from `material3-adaptive-navigation-suite`) as the app's root scaffold so Habitzy automatically shows:
- A bottom `NavigationBar` on compact width (phones portrait) — the primary target form factor.
- A `NavigationRail` on medium width (phones landscape, small tablets/foldables unfolded).
- A permanent `NavigationDrawer` on expanded width (large tablets), optional stretch — acceptable to just keep the rail at expanded width for v1 and note the drawer as a fast‑follow.

### 6.8 Shared components (`ui/components/`)

Build these once, reuse everywhere — do not let screen code duplicate their logic:

- **`HabitzyTopBar`** — wraps `TopAppBar`/`LargeTopAppBar` with M3 Expressive's scroll‑collapse behavior (`TopAppBarScrollBehavior`), used with a large collapsing title on Habits/Insights and a standard `CenterAlignedTopAppBar` on pushed detail screens (with a back arrow).
- **`HabitCard`** — the Habits‑screen list row: leading icon/emoji chip (habit color as container background, `full` shape), name + schedule‑summary subtitle, trailing completion control (checkbox for Binary that shape‑morphs, a small stepper/progress ring for Amount, a fraction badge "2/4" for Checklist that opens a mini popover of the steps), swipe‑to‑reveal quick actions (edit / archive), long‑press to enter multi‑select/reorder mode. Card shape = `extraLarge`, elevation Level1 at rest → Level2 while being dragged.
- **`ActivityGrid`** — the GitHub‑style year/month/week completion grid; takes `List<HabitLog>` (or an aggregated `Map<LocalDate, Float>` intensity map for the global Insights view) and a `weekStartDay`; cell shape = `extraSmall`, color intensity interpolated across that habit's/global `primary` tonal steps (light tone = low completion, full `primary` = 100%).
- **`StreakBadge`** — small pill (`ShapeFull`) showing a flame icon + current streak count, using `celebrationSpring` bounce whenever the count increments.
- **`ProgressRing`** — circular determinate indicator for Amount‑habit daily progress, built on M3's expressive circular progress indicator wavy/blob style if available in the installed Material3 version, else a custom `Canvas` arc.
- **`SectionHeader`** — `titleMedium` label + optional trailing action text button, consistent spacing (16dp horizontal, 24dp top / 8dp bottom padding) for grouping content on Insights/Settings.
- **`EmptyState`** — centered illustration‑area (use a simple `Icon` at large size tinted `primaryContainer`/`onPrimaryContainer` if no custom illustration asset exists yet) + `titleMedium` message + optional CTA button; used for "no habits yet," "no data for this period," "no notes yet."
- **`ConfirmDialog`** — M3 `AlertDialog` wrapper for destructive actions (delete habit permanently, replace‑all import).
- **`IconOrEmojiPicker`** and **`ColorSwatchPicker`** — grids used inside Add/Edit Habit (§9.3).
- **`HabitzySnackbarHost`** — standard undo‑pattern snackbar host used app‑wide for completion‑toggle undo, delete undo, import/export result messages.

### 6.9 Definition of done for Phase 3
- A temporary `ComponentsCatalogScreen` (reachable via a debug‑only deep link or a long‑press on the app title) renders one instance of every component above, in both light and dark theme, confirming color roles, shapes, and motion look correct before real screens consume them.
- Toggling dark/light and true‑black in a temporary debug switch visibly restyles the catalog screen with no hard‑coded colors anywhere (grep the module for raw `Color(0x...)` outside `Color.kt`/`HabitzyIcons.kt` — there should be none).

---

## 7. Phase 4 — Navigation Graph

**Give this whole section to OpenCode as "Phase 4."** Goal: all routes exist (can be stub/placeholder screens with just a title Text at first), transitions per §1.5 are wired, deep structure matches §0.1 exactly.

### 7.1 Routes (`Destinations.kt`, using type‑safe Navigation Compose `@Serializable` routes)

```kotlin
@Serializable object HabitsRoute
@Serializable data class HabitDetailRoute(val habitId: Long)
@Serializable data class AddEditHabitRoute(val habitId: Long? = null) // null = "add" mode
@Serializable object InsightsRoute
@Serializable object SettingsRoute
@Serializable object AccountSettingsRoute
@Serializable object AppearanceSettingsRoute
@Serializable object PreferencesSettingsRoute
@Serializable object DataSettingsRoute
@Serializable object AboutSettingsRoute
@Serializable object ProfileRoute
```

### 7.2 Graph shape

- Root `NavHost` start destination = `HabitsRoute`.
- The 3 `NavigationSuiteScaffold` items map to `HabitsRoute`, `InsightsRoute`, `SettingsRoute` — tapping one of these when already at a **nested** route (e.g. deep in Settings→Data) should `popUpTo` that tab's root and restore state (standard bottom‑nav single‑top + restore‑state pattern), matching how Streak/Grit‑style apps keep each tab's back stack independent.
- `HabitDetailRoute` and `AddEditHabitRoute` are pushed on top of `HabitsRoute` (and also reachable from other places — e.g. tapping "edit" while already inside Habit Detail pushes `AddEditHabitRoute` on top of that).
- The 5 Settings sub‑screens are pushed on top of `SettingsRoute`.
- `ProfileRoute` is pushed from a profile‑avatar affordance placed in `HabitzyTopBar` on the Habits screen (per §0.1) — it is reachable from any tab's top bar for convenience, but always pushes onto the *current* tab's back stack rather than switching tabs, so back‑navigation feels local.

### 7.3 Transitions (apply per §1.5)

| Transition | Spec |
|---|---|
| Between the 3 bottom‑nav tabs | Shared‑axis X: exiting content slides out 30% width + fades out, entering content slides in from the opposite 30% + fades in; `standardSpring` |
| Habits list → Habit Detail | Container transform: the tapped `HabitCard`'s bounds (icon chip + name) morph into Habit Detail's header row using `sharedBounds`/`SharedTransitionLayout`; `standardSpring` |
| FAB → Add/Edit Habit sheet | FAB scales/morphs (shape morph circle→sheet‑handle‑area) as the sheet rises; `expressiveSpring` |
| Any push (Settings sub‑screens, Profile) | Simple slide‑in‑from‑end + fade, `standardSpring` (no need for container transform on purely textual settings rows) |
| Add/Edit Habit sheet dismiss on save success (only when the last habit of the day just got completed as a side effect — rare, but the completion celebration can also fire from Habit Detail) | trigger `celebrationSpring` bounce + brief confetti burst on the relevant `StreakBadge`, not on the navigation transition itself |

### 7.4 Definition of done for Phase 4
- Every route above exists and is reachable by navigating the real app (not just previews).
- Bottom‑nav tab switching preserves each tab's scroll position/back stack across switches.
- Back button behavior: from any pushed screen, back returns to the correct previous screen; from a bottom‑nav root tab, back exits the app only if that tab has no further parent (standard single‑activity behavior — do not fight the system back gesture).

---

## 8. Phase 5 — Habits Screen (Home)

**Give this whole section to OpenCode as "Phase 5."**

### 8.1 Layout

- `HabitzyTopBar` in **large, collapsing** mode: title "Habits" in `headlineMedium` when expanded, collapsing to `titleLarge` in the pinned bar on scroll. Leading: nothing (or app icon). Trailing: profile avatar chip (opens `ProfileRoute`) + an overflow/filter icon button (opens a bottom sheet with: show archived toggle, category filter chips, sort‑by menu — see §8.4).
- Below the top bar: a horizontally scrollable **date strip** for the current week (7 chips, today highlighted with `primary` container + `full` shape, other days `surfaceContainer`), letting the user jump to *any single day's* habit list without leaving the screen (tapping a past/future day filters the list below to habits scheduled that day, with logging enabled for past days only — future days show a read‑only preview). Default selection = today.
- Main content: `LazyColumn` of `HabitCard`s for habits scheduled on the selected day, grouped by `categoryTag` (`SectionHeader` per group, ungrouped habits under a plain "Habits" header) — grouping only renders if the user has actually set tags on at least one habit; otherwise flat list.
- Empty state: if the user has zero habits at all → `EmptyState` with a "Create your first habit" CTA. If they have habits but none scheduled for the selected day → a lighter inline message ("Nothing scheduled today ✅" or similar), not a full empty‑state takeover.
- `FloatingActionButton` (large FAB, `full` shape, `primary` container, "+" icon) bottom‑end, always visible, opens `AddEditHabitRoute(null)`.
- Drag‑to‑reorder: long‑press a `HabitCard` to enter reorder mode (all cards gain a drag handle, subtle scale‑down of non‑dragged items) using `sh.calvin.reorderable`; persist new `sortOrder` via `HabitRepository.reorderHabits` on drop.

### 8.2 Interactions

- **Single tap** on a card's body (not the trailing control) → navigate to `HabitDetailRoute(habitId)` with the container‑transform transition.
- **Tap the trailing control**:
  - Binary → toggles completion for the selected day immediately (optimistic UI update), shape‑morphs the check icon, haptic tick, shows an "Undo" snackbar for 4s.
  - Amount → tapping opens a small inline stepper popover (`-`, current value/goal, `+`) or a compact numeric input; each `+`/`-` tap updates `amountValue` and the `ProgressRing` animates; hitting the goal triggers the same completion feedback as Binary.
  - Checklist → tapping opens a small popover listing the steps with checkboxes; checking all steps marks the day complete.
- **Swipe card left** → reveal "Archive" quick action (surfaces `tertiaryContainer`).
- **Swipe card right** → reveal "Edit" quick action, pushes `AddEditHabitRoute(habitId)`.
- Completing **every** habit scheduled for "today" specifically (not other selected days) fires the celebration motion (confetti burst near the FAB or top bar) once per day, and only the first time that day's full completion is reached (guard with a small in‑memory/session flag plus a `lastCelebratedEpochDay` value in `AppPreferences` so it doesn't re‑fire on every recomposition or app relaunch that same day).

### 8.3 States

`HabitsUiState` should model at least: `isLoading`, `selectedDate`, `weekDates`, `groupedHabits: Map<String?, List<HabitWithTodayLog>>`, `showArchived`, `activeCategoryFilter`, `sortMode`, and a `snackbarMessage` one‑shot event (e.g. via a `Channel`/`SharedFlow` consumed once, not stuck in persisted state) for undo/toast‑style feedback.

### 8.4 Filter/sort sheet
A `ModalBottomSheet` (shape `extraLargeIncreased` top corners) with: a "Show archived habits" switch (navigates instead to a read‑only archived list, or simply appends them dimmed at the bottom — pick one and be consistent), category filter chips (multi‑select `FilterChip`s, `secondaryContainer` when selected), and a sort‑mode `SingleChoiceSegmentedButtonRow` (Manual order / Name / Created date / Current streak).

### 8.5 Definition of done for Phase 5
- Creating a habit in Add/Edit and returning to Habits shows it immediately (reactive `Flow` from Room, no manual refresh).
- Toggling completion persists correctly and survives process death (kill app from recents, reopen, state is correct).
- Reordering habits persists and survives app restart.
- Date‑strip navigation correctly filters by each habit's `HabitSchedule` (a "Mon/Wed/Fri" habit only appears on those weekdays; a "3x/week" habit appears every day but is understood as "still needs N more this week" — decide and document: simplest correct v1 behavior is to show "times per week/month" habits on **every** day until that period's target is met, then hide/gray them out for the rest of the period).

---

## 9. Phase 6 — Habit Detail Screen

**Give this whole section to OpenCode as "Phase 6."**

### 9.1 Layout (top → bottom, single scrollable column)

1. `CenterAlignedTopAppBar` with back arrow, habit name as title, overflow menu (Edit, Archive/Unarchive, Vacation mode, Delete permanently — delete requires `ConfirmDialog`).
2. **Header block**: large icon/emoji chip in the habit's color (shape `extraLarge`), habit name in `headlineSmall`, schedule summary subtitle in `bodyMedium` `onSurfaceVariant` (e.g. "Every Mon, Wed, Fri" or "3 times a week"), and a `StreakBadge` row showing current streak + best streak side by side.
3. **Today's action row**: the same completion control as the Habits‑screen card (Binary toggle / Amount stepper+ring / Checklist popover), sized larger here as the primary CTA of the screen.
4. **Segmented view selector**: `SingleChoiceSegmentedButtonRow` with "Week / Month / Year" to control the `ActivityGrid` below.
5. **`ActivityGrid`** for the selected range, tappable per‑day: tap toggles that day's completion (with a confirm step if it's more than, say, 30 days in the past, to avoid accidental bulk edits); long‑press opens the **day note** editor (text + optional single photo via Coil + a photo picker/camera intent).
6. **Stats grid** (a `LazyVerticalGrid`/`FlowRow` of small stat cards, `surfaceContainer`, shape `medium`): Completion rate (with a period selector chip: 7d/30d/90d/all), Total completions, Best streak, Current streak, Strength score (with a small info icon opening a tooltip/dialog explaining the formula in plain language, see §11.4).
7. **Streak‑over‑time chart**: a Vico line chart of streak length across the last N weeks/months.
8. **Notes list**: reverse‑chronological list of day notes for this habit (date, text, thumbnail if a photo is attached), tapping opens a full note detail/edit sheet.
9. If the habit is currently in vacation mode, show a persistent banner at the top of the content ("Paused until <date> · Edit") in a `tertiaryContainer` banner, and visually dim/skip those days in the `ActivityGrid` (neutral gray, not counted against streak).

### 9.2 Vacation mode & archive interactions
- "Vacation mode" in the overflow menu opens a small dialog with a date‑range picker (M3 `DateRangePicker`); while active, streak math and completion‑rate math (§11.4) must **exclude** the vacation range's days entirely, as if they don't exist on the calendar for that habit (they should not count as gaps that break a streak, nor as completions).
- "Archive" moves the habit off the Habits screen but retains all logs/notes; "Unarchive" (from Settings→Data→Archived habits list, or from this same overflow menu if navigated to directly) restores it to the active list. "Delete permanently" is the only truly destructive path and must go through `ConfirmDialog` with explicit copy naming the habit and warning that history is unrecoverable.

### 9.3 Definition of done for Phase 6
- Tapping any past day in any of Week/Month/Year views correctly toggles/back‑fills that day and immediately updates the streak/rate stats above (reactive recomputation, not a manual refresh button).
- Vacation‑mode days render distinctly in the grid and are excluded from streak/rate math (covered by a unit test on `ComputeStreakUseCase`).
- Day notes persist with their photo and show correctly after app restart.

---

## 10. Phase 7 — Add / Edit Habit Screen

**Give this whole section to OpenCode as "Phase 7."** Presented as a **full‑screen sheet** (`ModalBottomSheet` expanded to near‑full‑height, or a dedicated screen pushed with a bottom‑to‑top slide — either is acceptable; a full‑height sheet with a drag handle feels more "expressive" and is the preferred choice) so it doesn't feel like leaving the app's flow.

### 10.1 Fields, in order

1. **Name** (required, `OutlinedTextField`, autofocus on open when adding).
2. **Icon or emoji** — tapping opens `IconOrEmojiPicker` (§6.8) in a nested sheet/dialog; selected icon shown as a preview chip inline.
3. **Color** — `ColorSwatchPicker` row of the curated palette (§6.1) plus a "Custom…" swatch opening a full HSV/HCT color picker (reuse an approach like `skydoves/colorpicker-compose`'s pattern, or implement a simple custom `Canvas`‑based HCT picker — either is fine, just keep it accessible/contrast‑checked).
4. **Habit type** — `SingleChoiceSegmentedButtonRow`: Binary / Amount / Checklist. Selecting Amount reveals goal (number field) + unit (text field with common‑unit suggestions as `AssistChip`s: steps, glasses, minutes, pages, km). Selecting Checklist reveals a dynamic list of step text fields (add/remove rows, 2–8 steps, drag‑reorderable).
5. **Schedule** — `SingleChoiceSegmentedButtonRow` or a dropdown for the 5 schedule kinds (§0.2); each reveals its own sub‑control: weekday multi‑select chips (Mon–Sun) for Specific Weekdays; a number stepper "times per week/month" for those two modes; a number stepper "every N days" for that mode. Daily needs no sub‑control.
6. **Reminders** — a list of reminder rows (time + optional message + enabled switch), "+ Add reminder" button below, each time picked via M3 `TimePicker` dialog.
7. **Category tag** — free‑text field with an autocomplete dropdown of previously used tags (query `DISTINCT categoryTag FROM habits` via the DAO).
8. **Description/note** (optional, multi‑line `OutlinedTextField`).

### 10.2 Validation & save
- Disable the primary "Save"/"Create habit" button until: name is non‑blank, and (if Amount) goal > 0, and (if Checklist) at least 2 non‑blank steps, and (if Specific Weekdays) at least 1 day selected.
- On save: upsert the `Habit` via `HabitRepository`, schedule/cancel `WorkManager`/`AlarmManager` reminders to match the current reminder list (§12), pop the sheet, and if this was a brand‑new habit, land back on Habits with a brief "Habit created" snackbar; if editing, land back on whichever screen opened the sheet (Habits card‑edit swipe, or Habit Detail's overflow menu) with "Habit updated."
- Bottom bar of the sheet: a plain text "Cancel" button (left) and a filled "Save"/"Create habit" button (right, `full` shape, `primary`), both pinned above the keyboard.

### 10.3 Definition of done for Phase 7
- Creating a habit of each of the 3 types round‑trips correctly (visible on Habits screen, correct control renders, correct completion semantics).
- Editing an existing habit pre‑fills every field correctly, including reminders and checklist steps in their original order.
- Changing a habit's schedule type after it already has logs does not corrupt existing `HabitLogEntity` rows (schedule only affects *which future days it's shown/expected on* and streak math, not past log data).

---

## 11. Phase 8 — Insights Screen

**Give this whole section to OpenCode as "Phase 8."**

### 11.1 Layout (single scrollable column, `LargeTopAppBar` title "Insights")

1. **Period selector**: `SingleChoiceSegmentedButtonRow` — Today / This week / This month / All time — controls every section below.
2. **Overview card**: big `displayMedium` Emphasized percentage (overall completion rate for the period) + a short sub‑label ("of scheduled habits completed"), on a `primaryContainer` surface, shape `extraLarge`.
3. **Per‑habit ranking**: a `SectionHeader` "Habit performance" + a vertical list of horizontal bar rows (habit icon + name + a `LinearProgressIndicator`‑style bar in the habit's own color + a trailing percentage), sorted best→worst for the period. Cap the visible list at ~8 with a "Show all" expand affordance if there are more habits.
4. **Best current streaks**: `SectionHeader` "On a roll 🔥" + a horizontally scrollable row of `StreakBadge`‑style cards, top 5 habits by current streak.
5. **Consistency‑by‑hour histogram**: `SectionHeader` "When are you most consistent?" + a Vico bar chart, 24 bars (one per hour of day), height = count of completions logged around that hour across all habits and all time (or the selected period) — this directly answers "what time do I actually show up."
6. **Combined monthly heatmap**: `SectionHeader` "This month at a glance" + an `ActivityGrid` in "all habits combined" mode (cell intensity = fraction of that day's scheduled habits that were completed), with a habit filter chip row above it to narrow to a single habit if desired.
7. **12‑week trend**: `SectionHeader` "Momentum" + a Vico line chart of total completions per week for the last 12 weeks.
8. Empty state: if the user has fewer than, say, 3 days of any logged data yet, replace sections 3–7 with a friendly `EmptyState` ("Keep logging — insights unlock after a few days of data") rather than showing empty/misleading charts.

### 11.2 Data flow
`InsightsViewModel` observes `InsightsRepository.observeGlobalInsights(period)`, which internally combines `HabitRepository.observeActiveHabits()` with `HabitLogDao.observeAllLogsInRange(...)` and runs `ComputeInsightsUseCase` (pure function) on every emission — keep this **reactive**, not a manual "refresh" button, consistent with the rest of the app.

### 11.3 Performance note for OpenCode
For the "All time" period on a database that may have years of daily logs across many habits, avoid loading every single `HabitLogEntity` row into memory for every recomposition. Prefer SQL aggregation in the DAO (e.g. `GROUP BY strftime('%Y-%W', ...)`‑style queries adapted to `epochDay` math, or precomputed weekly/monthly rollups) for the trend chart and histogram specifically, falling back to in‑Kotlin aggregation only for ranges you've already bounded (e.g. "this month").

### 11.4 Streak, completion‑rate, and "strength score" formulas — implement exactly

Let `logs` = the sorted list of `HabitLog` for a habit, `schedule` = its `HabitSchedule`, and `today` = the current `LocalDate`. Vacation‑range days are removed from consideration entirely before any of the math below runs (as if the habit did not exist on those calendar days).

**Expected days** (`isExpectedDay(date, schedule)`):
- `Daily` → every day.
- `SpecificWeekdays(mask)` → only days whose weekday bit is set.
- `EveryNDays(n)` → a day is "expected" if it is exactly a multiple of `n` days after the habit's `createdAt` (or after the last completion — pick the simpler "rolling from last completion" model: the next expected day is `lastCompletionDate + n`, and there's always exactly one "currently due" day at a time, which is more forgiving and matches how these interval habits are usually implemented in apps like Streak).
- `TimesPerWeek(target)` / `TimesPerMonth(target)` → every day is *eligible*, but the habit is only "behind" once the days remaining in the period can no longer reach `target`; for streak purposes, treat each completed **period** (week or month) that hit its target as one streak unit, not each day.

**Current streak** (for Daily/SpecificWeekdays/EveryNDays): count consecutive expected days ending at `today` (or ending at the most recent expected day if today isn't yet resolved) where `isCompleted == true`, stopping at the first expected‑but‑not‑completed day. **Current streak** (for TimesPerWeek/TimesPerMonth): count consecutive periods, walking backward from the current period, where the completions‑in‑period ≥ target; the current (in‑progress) period only counts once its target is actually met.

**Best streak**: the maximum such run found anywhere in `logs` history, computed the same way.

**Completion rate (N days)**: `completedExpectedDays / totalExpectedDays` over the trailing N calendar days (excluding vacation days from the denominator).

**Strength score** (0–100, a single "how solid is this habit right now" number, inspired by Streak's per‑habit strength stat): a recency‑weighted completion rate over the trailing 60 expected days, where more recent days count more:

```
strength = 100 * Σ(i=0..59) [ completed(day_i) * weight(i) ] / Σ(i=0..59) [ weight(i) ]
where day_0 = most recent expected day, day_59 = the 60th-most-recent expected day
weight(i) = 0.5 ^ (i / 20)     // halves every 20 expected days back
```
This gives a habit that's been perfect for the last 3 weeks but shaky before that a high score, while an old streak that just broke this week drops quickly — which is the intuitive "strength" behavior. Clamp to `[0, 100]`; if fewer than 5 expected days exist yet, don't show a score (show "Not enough data").

**Hour‑of‑day histogram**: bucket every `HabitLogEntity.completedAtEpochMillis` (when present — older/back‑filled logs may lack it, exclude those from this specific chart only) into its local hour‑of‑day (0–23) across the selected period.

### 11.5 Definition of done for Phase 8
- All streak/rate/strength numbers shown in Insights and Habit Detail come from the **same** `domain/usecase` functions (no duplicate ad‑hoc math in either screen's ViewModel).
- Unit tests cover: TimesPerWeek streak counting across a period boundary; EveryNDays rolling‑interval streak; strength score monotonically decreasing as recent completions are removed one at a time in a test fixture.
- Charts render correctly with 0 data (empty state, not a crash), with 1 data point, and with a full year of data (performance acceptable, no jank on scroll).

---

## 12. Phase 9 — Settings (5 sub‑screens) & Profile

**Give this whole section to OpenCode as "Phase 9."**

### 12.1 Settings root screen
A simple `LazyColumn` of `ListItem`s (M3 `ListItem` composable, not a hand‑rolled row) grouped into "General" (Appearance, Preferences) and "Data & account" (Account, Data, About), each with a leading icon, a title, and an optional supporting‑text subtitle (e.g. Appearance's subtitle previews the current theme mode). Each row pushes its sub‑screen. Top bar: standard `CenterAlignedTopAppBar`, title "Settings", no back arrow needed if this is a bottom‑nav root, but the bar should still support the shared‑axis tab transition per §7.3.

### 12.2 Account (sub‑screen)
Habitzy is **local‑only with no server accounts** — be upfront about this rather than pretending there's a login system. This screen is intentionally minimal:
- A row showing the local profile summary (photo thumbnail + name), tapping it goes to `ProfileRoute` (the same screen reachable from the top‑bar avatar — one screen, two entry points).
- "App lock" toggle (PIN or biometric via `BiometricPrompt`) to gate opening the app — since there's no cloud account, this is the closest thing to "account security" and matches Streak's local PIN/fingerprint feature.
- If you later add an optional cloud sync (out of scope for v1 per the user's brief), this is where a "Sign in" row would eventually go — leave a comment noting the extension point, but do not build any networking/auth now.

### 12.3 Appearance (sub‑screen)
- **Theme mode**: `SingleChoiceSegmentedButtonRow` — System / Light / Dark.
- **Dynamic color** (Android 12+ only, hide/disable row below API 31): switch — "Use wallpaper colors."
- **Accent color** (only meaningful when dynamic color is off): tapping opens the same custom color picker used for per‑habit colors (§10.1), previews live via `HabitzyTheme`.
- **True black / AMOLED background**: switch, only meaningful in dark mode.
- **App icon**: a row of 2–3 alternate launcher icon swatches (adaptive icon variants) the user can pick, implemented via manifest `activity-alias` entries toggled at runtime (`PackageManager.setComponentEnabledSetting`) — a nice small "make it yours" touch mirrored from Streak's launcher‑icon feature; acceptable to ship with just 1 icon for v1 and mark this as a stretch goal if time is tight.
- Live preview: this screen itself should visibly restyle as the user changes these toggles (it's just consuming the same `HabitzyTheme`).

### 12.4 Preferences (sub‑screen)
- **Week starts on**: Monday / Saturday / Sunday (affects the Habits date strip and all `ActivityGrid`s).
- **Default reminder time**: the time pre‑filled when adding a new reminder in Add/Edit Habit.
- **Haptics**: on/off switch for the completion‑toggle haptic feedback.
- **Notification style** (optional): whether reminder notifications show the quick "Done"/"Snooze" action buttons (§12.7) or just open the app.

### 12.5 Data (sub‑screen)
- **Export backup**: button → SAF "create document" picker → writes a single JSON file (`BackupRepository.exportToUri`) containing every habit, log, note, and reminder (see schema below).
- **Import backup**: button → SAF "open document" picker → parses the JSON → shows a summary dialog ("Found 12 habits, 1,204 logs — Merge with existing data or Replace all?") → runs `BackupRepository.importFromUri` with the chosen `ImportStrategy`.
- **Import from CSV**: button → file picker → a small mapping screen letting the user pick which column is the habit name and which is the date (plus an optional "completed" boolean column, defaulting to "present row = completed") → `BackupRepository.importCsv`. This is the lightweight universal fallback (mirroring Streak's "any CSV with a habit and a date" import), separate from importing Habitzy's own named‑app format.
- **Automatic backups**: switch + frequency (Daily/Weekly) + a folder picker (SAF persistable URI permission), scheduled via a Hilt‑injected `WorkManager` periodic `BackupWorker`.
- **Archived habits**: a row opening a simple list of archived habits with "Unarchive" / "Delete permanently" actions per row.
- **Clear all data**: destructive, behind a `ConfirmDialog` requiring the user to type the app name or a confirmation phrase for extra safety, since this wipes the whole Room DB.

**Backup JSON shape** (`data/backup/BackupModels.kt`, `@Serializable`):
```kotlin
@Serializable
data class HabitzyBackup(
    val formatVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val habits: List<BackupHabit>,
    val logs: List<BackupLog>,
    val notes: List<BackupNote>,
    val reminders: List<BackupReminder>,
    val appPreferences: BackupPreferences? = null, // optional: include theme/settings in the backup too
)
```
Version the format from day one (`formatVersion`) so future imports can branch on older backups without guessing.

### 12.6 About (sub‑screen)
- App name + version (`BuildConfig.VERSION_NAME`), a short description.
- Links (open via `Intent.ACTION_VIEW`): source/GitHub link if the user open‑sources it, a "Rate this app" link (Play Store deep link, once published), a "Send feedback" `mailto:` intent.
- Open‑source licenses row → Android's built‑in `OssLicensesMenuActivity` (via the `oss-licenses-plugin`) or a simple in‑app list of the libraries from §2.1 with their license names, crediting **Loop Habit Tracker, Streak, and Grit as design/feature inspiration** (not code reuse) per this project's own attribution norms, since Habitzy's feature set was explicitly modeled on their public feature lists.

### 12.7 Notifications & reminders (implementation shared by Preferences + Add/Edit Habit)
- `NotificationScheduler` uses `AlarmManager.setExactAndAllowWhileIdle` (exact alarms need the `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` permission model on Android 12+; handle the "can't schedule exact alarms" fallback by using inexact `WorkManager` periodic work as a graceful degrade) for per‑habit reminder times, rescheduling on `BootCompletedReceiver` and whenever a reminder is added/edited/deleted.
- Each notification shows the habit name + its custom message (or a friendly default like "Time for {habit name}"), with two action buttons: **Done** (calls `HabitRepository.toggleCompletion` directly from a `BroadcastReceiver`, no need to open the app) and **Snooze** (reschedules a one‑off alarm +15/30/60 min, user‑configurable).
- One notification channel `"habit_reminders"`, importance `HIGH` (so it can heads‑up/sound), created at app startup, respecting user's system‑level channel overrides.

### 12.8 Profile screen
Minimal, single‑purpose: circular photo (tap to pick from gallery via `PhotoPicker`/`ActivityResultContracts.PickVisualMedia`, rendered with Coil), name (`OutlinedTextField`, saved on focus‑loss/debounce via `ProfileRepository.setName`), and nothing else — no bio, no stats duplication (those live in Insights). Top bar has a back arrow (it's always a pushed screen, never a nav‑bar root, per §0.1/§7.2).

### 12.9 Definition of done for Phase 9
- Every Appearance toggle visibly changes the whole app's theme and the change survives app restart (persisted via `AppPreferences`).
- Export → Import round‑trip on a fresh install reproduces every habit, log, note, and reminder exactly (write an instrumented test that exports, wipes the DB, imports, and diffs).
- A scheduled reminder actually fires (test via a near‑future time on an emulator) and its "Done" action correctly marks that day complete without opening the app.
- CSV import correctly maps a sample 3‑column CSV (habit, date, completed) into logs for a matching or newly created habit.

---

## 13. Phase 10 — Testing, Accessibility, and Polish

**Give this whole section to OpenCode as "Phase 10," after all screens exist.**

### 13.1 Automated testing matrix

| Layer | Tool | What to cover |
|---|---|---|
| Domain use cases | JUnit4/5, pure Kotlin | `ComputeStreakUseCase`, `ComputeInsightsUseCase`, all 5 schedule types, vacation‑range exclusion, strength score formula (§11.4) |
| Room DAOs | `Room.inMemoryDatabaseBuilder` + JUnit + `runTest` | Unique‑index upsert behavior, cascade delete (deleting a habit removes its logs/notes/reminders), range queries |
| Repositories | Fake/in‑memory implementations of the DAOs (or a real in‑memory Room DB) | Mapping Entity↔domain model correctness, especially enum/sealed‑class round‑trips (schedule type, habit type) |
| ViewModels | `Turbine` (or manual `StateFlow` collection) + fake repositories | `HabitsViewModel` filters by selected date correctly; `AddEditHabitViewModel` validation gates Save correctly; `InsightsViewModel` reacts to period changes |
| Compose UI | `createAndroidComposeRule`, semantics matchers | Habits screen renders N cards for N habits; tapping a Binary card's control toggles a content‑description/state change; Add/Edit form validation disables Save button |
| Backup round‑trip | Instrumented test | Export→wipe→import reproduces original data (per §12.9) |
| Migrations (once you ship v2+) | Room's `MigrationTestHelper` | Every migration tested against the exported schema JSON from §4.4 |

### 13.2 Accessibility checklist (M3 bakes this in, but verify explicitly)
- Every interactive element ≥ 48×48dp touch target, even when its visual icon is smaller (use `Modifier.minimumInteractiveComponentSize()` / padding).
- All color pairs (text/background, icon/background) meet WCAG contrast — verify custom per‑habit accent colors don't get placed as text-on-background without an `on*` pairing; run each curated swatch through a contrast check against both light and dark `surface` before shipping the palette.
- Every icon‑only button has a `contentDescription`; decorative icons use `contentDescription = null` explicitly (not omitted) so TalkBack doesn't stumble.
- `ActivityGrid` cells expose a semantics label per day ("Tuesday, March 4 — completed" / "not scheduled" / "vacation") for TalkBack, since the grid is otherwise a purely visual/color‑coded widget.
- Respect `LocalReduceMotion`/system "remove animations" accessibility setting — gate the celebration confetti and spring bounces behind a check, falling back to a simple instant state change or a shorter fade.
- Support system font‑scale up to at least 200% without clipping (test the Habits list and Habit Detail's stat‑card grid, which are the most layout‑dense screens).
- Dynamic text and RTL: use `start`/`end` padding (not `left`/`right`) throughout; test at least the Habits screen with a pseudo‑RTL locale.

### 13.3 Performance polish
- `LazyColumn`/`LazyVerticalGrid` `key = { it.id }` on every list to avoid recomposition thrashing on reorder/toggle.
- Baseline profile (`androidx.benchmark` / Macrobenchmark) generated for the Habits screen cold‑start path, since it's the app's most‑visited screen.
- Avoid recomposition of the whole Habits list on every single toggle — scope state reads (`derivedStateOf`, per‑item `remember`) so only the tapped `HabitCard` recomposes.
- Charts (Vico) should not recompute their full dataset on every keystroke of unrelated UI state — hoist chart data to a `remember(key)`'d model.

### 13.4 Definition of done for Phase 10
- All tests in §13.1 green in CI (`./gradlew test connectedCheck` locally at minimum).
- TalkBack manual pass on Habits, Habit Detail, Add/Edit Habit, Insights, Settings without any unlabeled controls.
- No visible layout breakage at 200% font scale on the two most content‑dense screens.

---

## 14. Stretch Goals (explicitly out of the v1 scope, do only after Phases 1–10 are done and stable)

Only pick these up if the user asks for them — they are **not** part of the core scope defined in §0, listed here so OpenCode doesn't accidentally build them prematurely or, conversely, refuse to consider them later:

- **Home‑screen widgets** (Glance for Compose): a "Today's habits" widget and a small stats widget, mirroring Streak's 4 widget types. Requires `androidx.glance:glance-appwidget` and its own theming pass since Glance doesn't share Compose Material3 components directly.
- **Shareable progress card**: render a habit's streak/activity‑grid as a shareable PNG (via `graphicsLayer`/`ImageBitmap` capture) sized for social sharing, similar to Streak's export‑as‑image feature.
- **Multiple design "moods"** (e.g. a Classic/Minimal/Expressive theme switcher à la Streak) — Habitzy's brief specifically asks for one strong Expressive identity, so treat alternate moods as a later differentiator, not a v1 requirement.
- **Tablet‑optimized list‑detail pane** using `ListDetailPaneScaffold` from `material3-adaptive` so large screens show Habits list + Habit Detail side‑by‑side instead of full‑screen push navigation.
- **Cloud backup/sync** — deliberately out of scope; the whole data model in §4 assumes single‑device local storage.

---

## 15. Suggested Phase Order & Example OpenCode Prompts

Feed these to OpenCode **one at a time**, in order, waiting for a successful build + your own manual smoke test after each before moving on.

1. *"Read guide.md sections 0–2. Set up the Habitzy project exactly as described in Phase 1: Gradle version catalog, application class, Hilt bootstrap, single activity with edge‑to‑edge, and a placeholder themed screen. Don't build any real feature screens yet."*
2. *"Read guide.md section 4 (Phase 2). Implement the full Room schema, DataStore preference wrappers, domain models, and repository interfaces/implementations exactly as specified, including the unit tests listed in the Definition of Done. No UI changes in this phase."*
3. *"Read guide.md sections 1 and 6 (Phase 3). Implement the complete M3 Expressive design system — Color.kt, Type.kt, Shape.kt, Elevation.kt, Motion.kt, Theme.kt — and the shared components in ui/components/ listed in §6.8. Add the temporary ComponentsCatalogScreen so I can visually verify every component in light and dark theme before we build real screens."*
4. *"Read guide.md section 7 (Phase 4). Wire up the full navigation graph with all routes from §0.1, using NavigationSuiteScaffold for the 3 bottom-nav tabs, type-safe routes, and the transition specs in §7.3. Screens can still be placeholders with just a title."*
5. *"Read guide.md section 8 (Phase 5). Build the real Habits screen per the full spec — date strip, grouped list, HabitCard interactions for all 3 habit types, swipe actions, drag-to-reorder, filter/sort sheet, empty states, and the daily-completion celebration. Wire it to the real repository from Phase 2."*
6. *"Read guide.md section 9 (Phase 6). Build the Habit Detail screen exactly per spec — header, today's action row, week/month/year ActivityGrid with tap-to-backfill and long-press day notes, stats grid, streak chart, notes list, vacation mode and archive/delete flows."*
7. *"Read guide.md section 10 (Phase 7). Build the Add/Edit Habit sheet with every field in §10.1 in order, full validation per §10.2, and correct pre-fill behavior when editing."*
8. *"Read guide.md section 11 (Phase 8). Build the Insights screen exactly per spec, implementing the streak/completion-rate/strength-score formulas in §11.4 as pure, unit-tested domain functions shared with Habit Detail — do not duplicate this math in the ViewModel."*
9. *"Read guide.md section 12 (Phase 9). Build all 5 Settings sub-screens and the Profile screen, plus the backup/import/CSV-import flows and the reminder scheduling system in §12.7."*
10. *"Read guide.md section 13 (Phase 10). Add the full testing matrix, do an accessibility pass, and apply the performance polish items listed."*

If OpenCode ever proposes deviating from a spec in this file (a different package structure, a different library, an extra screen), ask it to flag the deviation explicitly and explain why, rather than silently drifting — then decide whether to update this guide.md itself to reflect the accepted change, keeping the file authoritative for the rest of the build.

---

## 16. Spacing & Layout Grid Reference

Apply consistently across every screen (add as named `Dp` constants in `ui/theme/Spacing.kt`, never raw `.dp` literals scattered through screen code):

| Token | Value | Use |
|---|---|---|
| `SpaceXXS` | 2dp | Icon‑to‑badge micro gaps |
| `SpaceXS` | 4dp | Tight internal padding within a chip |
| `SpaceS` | 8dp | Between related inline elements (icon + label) |
| `SpaceM` | 12dp | Internal card padding (secondary axis) |
| `SpaceL` | 16dp | Standard screen‑edge margin; primary card padding |
| `SpaceXL` | 24dp | Between distinct sections on a screen |
| `SpaceXXL` | 32dp | Above/below a screen's hero header block |
| `SpaceXXXL` | 48dp | Empty‑state vertical centering padding |

Grid: base everything on an **8dp grid** with a **4dp** fine‑adjustment step where 8dp is too coarse (per the M3 Expressive layout guidance in §1). List item minimum height: 56dp (single line), 72dp (two‑line, which is what `HabitCard` typically is — icon + name + subtitle). Screen horizontal margins: 16dp on compact width, scaling to 24dp on medium width and using a centered max‑content‑width column (not full bleed) on expanded width.

---

## 17. ASCII Wireframes (for quick visual reference while building each screen)

These are structural sketches, not final visual design — use them to confirm layout order/hierarchy matches §8–§12 before writing pixel‑perfect Compose code.

### 17.1 Habits screen
```
┌─────────────────────────────────────┐
│  Habits                        (👤) │  <- large collapsing top bar, avatar opens Profile
│  [Mon][Tue][Wed*][Thu][Fri][Sat][Sun]│  <- date strip, *today/selected
├─────────────────────────────────────┤
│  HEALTH                             │  <- SectionHeader (category)
│  🏃 Run 5k            Mon/Wed/Fri  ○ │  <- HabitCard, binary control
│  💧 Drink water        Daily   ▓▓░ 6/8│ <- amount control w/ ring
├─────────────────────────────────────┤
│  MIND                                │
│  📖 Read              Daily     ☑    │
│  🧘 Meditate           3x/week  2/4→ │  <- checklist fraction, tap opens popover
├─────────────────────────────────────┤
│              (empty space)           │
│                                (＋)  │  <- FAB, bottom-end
└─────────────────────────────────────┘
```

### 17.2 Habit Detail screen
```
┌─────────────────────────────────────┐
│ ←  Run 5k                      ⋮    │
├─────────────────────────────────────┤
│        🏃  Run 5k                   │
│        Every Mon, Wed, Fri           │
│        🔥 12 current   🏆 30 best    │
├─────────────────────────────────────┤
│        [ Mark today done ]           │
├─────────────────────────────────────┤
│   [ Week | Month | Year ]            │
│   ▢▢▢▢▢▢▢  <- activity grid          │
│   ▢▢▢▢▢▢▢                            │
├─────────────────────────────────────┤
│  [Rate 87%] [Total 214] [Best 30]    │
│  [Current 12] [Strength 76]          │
├─────────────────────────────────────┤
│  Streak over time  ~/\_/‾\__/‾\~      │
├─────────────────────────────────────┤
│  Notes                               │
│  Mar 4 — "Felt great today"  📷      │
└─────────────────────────────────────┘
```

### 17.3 Add/Edit Habit sheet
```
┌─────────────────────────────────────┐
│  ═══  (drag handle)                  │
│  New habit                    Cancel │
├─────────────────────────────────────┤
│  Name: [______________________]      │
│  Icon: (🏃)  Color: (●)(●)(●)(●)(+)  │
│  Type: [Binary][Amount][Checklist]   │
│  Schedule: [Daily][Weekdays][3x/wk]..│
│    └ Mon Tue Wed Thu Fri Sat Sun     │
│  Reminders:  7:00 AM  "Let's go!" ✕  │
│              + Add reminder          │
│  Category: [health ▾]                │
│  Description: [__________________]   │
├─────────────────────────────────────┤
│                        [Save habit]  │
└─────────────────────────────────────┘
```

### 17.4 Insights screen
```
┌─────────────────────────────────────┐
│  Insights                            │
│  [Today][Week][Month][All time]      │
├─────────────────────────────────────┤
│         ┌───────────────┐            │
│         │     82%       │            │
│         │ of habits done│            │
│         └───────────────┘            │
├─────────────────────────────────────┤
│  Habit performance                   │
│  🏃 Run       ▓▓▓▓▓▓▓▓░░ 87%          │
│  💧 Water     ▓▓▓▓▓▓░░░░ 63%          │
├─────────────────────────────────────┤
│  On a roll 🔥                        │
│  [Run 12] [Read 9] [Meditate 4]      │
├─────────────────────────────────────┤
│  When are you most consistent?       │
│  ▁▁▂▃▅█▇▅▃▂▁▁▁▁▂▃▅▇█▅▃▂▁▁ (0-23h)   │
├─────────────────────────────────────┤
│  This month at a glance   [filter ▾] │
│  ▢▢▢▢▢▢▢ ... (heatmap)               │
├─────────────────────────────────────┤
│  Momentum  __/‾‾\_/‾\___/‾‾\__       │
└─────────────────────────────────────┘
```

### 17.5 Settings screen
```
┌─────────────────────────────────────┐
│  Settings                            │
├─────────────────────────────────────┤
│  GENERAL                             │
│  🎨 Appearance          Dark, dynamic│
│  ⚙  Preferences          Mon start   │
│                                       │
│  DATA & ACCOUNT                      │
│  👤 Account                          │
│  💾 Data                             │
│  ℹ  About                            │
└─────────────────────────────────────┘
```

### 17.6 Profile screen
```
┌─────────────────────────────────────┐
│ ←  Profile                           │
├─────────────────────────────────────┤
│              ( 📷 )                  │
│         Name: [___________]          │
└─────────────────────────────────────┘
```

---

## 18. Sample Composable Reference Implementations

These are **reference sketches** to anchor OpenCode's output style — not necessarily final/compiling code (verify exact current Compose Material3 API names against the installed library version, since some named APIs below, especially expressive‑only ones, are newer than most training data and may have shipped under a slightly different name/signature).

### 18.1 `HabitCard`

```kotlin
@Composable
fun HabitCard(
    habit: HabitWithTodayLog,
    onBodyClick: () -> Unit,
    onToggle: () -> Unit,
    onSwipeArchive: () -> Unit,
    onSwipeEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = HabitzyShapes.extraLarge
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = HabitzyElevation.Level1.shadowDp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onBodyClick)
                .padding(SpaceL),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitIconChip(icon = habit.icon, color = habit.color)
            Spacer(Modifier.width(SpaceM))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    habit.schedule.summaryText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(SpaceM))
            HabitCompletionControl(habit = habit, onToggle = onToggle)
        }
    }
}
```

### 18.2 `HabitIconChip` with shape morphing

```kotlin
@Composable
fun HabitIconChip(icon: HabitIcon, color: Color, isCompleted: Boolean = false) {
    val cornerFraction by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = HabitzyMotion.expressiveSpring,
        label = "chipShapeMorph",
    )
    val shape = remember(cornerFraction) {
        RoundedCornerShape(percent = (20 + cornerFraction * 30).roundToInt())
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        HabitIconOrEmoji(icon, tint = color)
    }
}
```

### 18.3 `ActivityGrid` (simplified structure)

```kotlin
@Composable
fun ActivityGrid(
    intensityByDate: Map<LocalDate, Float>, // 0f..1f completion intensity, or null-safe default
    range: ClosedRange<LocalDate>,
    weekStartDay: DayOfWeek,
    onDayClick: (LocalDate) -> Unit,
    onDayLongPress: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(range, weekStartDay) { chunkIntoWeeks(range, weekStartDay) }
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(SpaceXS)) {
        items(weeks, key = { it.first().toEpochDay() }) { week ->
            Column(verticalArrangement = Arrangement.spacedBy(SpaceXS)) {
                week.forEach { date ->
                    val intensity = intensityByDate[date] ?: 0f
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(HabitzyShapes.extraSmall)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f + intensity * 0.85f)
                            )
                            .combinedClickable(
                                onClick = { onDayClick(date) },
                                onLongClick = { onDayLongPress(date) },
                            )
                            .semantics {
                                contentDescription = "${date.dayOfWeek}, $date — ${(intensity * 100).roundToInt()}% complete"
                            },
                    )
                }
            }
        }
    }
}
```

### 18.4 `StreakBadge` with celebration bounce

```kotlin
@Composable
fun StreakBadge(streak: Int, justIncreased: Boolean, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(justIncreased) {
        if (justIncreased) {
            scale.animateTo(1.25f, HabitzyMotion.celebrationSpring)
            scale.animateTo(1f, HabitzyMotion.celebrationSpring)
        }
    }
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    ) {
        Row(Modifier.padding(horizontal = SpaceM, vertical = SpaceS), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Habitzy.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(SpaceXS))
            Text("$streak", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}
```

---

## 19. Common Pitfalls for an AI Coding Agent (read this before starting any phase)

- **Don't invent Compose Material3 API names.** The M3 Expressive components (expressive `Button` variants, wavy/blob progress indicators, floating/docked toolbars, the new `Shapes` extra tokens) are newer than a lot of training data. If an API doesn't resolve, check what's actually available in the installed `androidx.compose.material3` artifact (Android Studio "Go to declaration," or the library's decompiled sources) rather than guessing a plausible‑sounding name and shipping a compile error or a silently‑wrong fallback.
- **Don't hard‑code colors, corner radii, or spacing.** Every visual value in every screen composable should trace back to a named token from §6/§16. If you catch yourself writing `Color(0xFF...)`, `RoundedCornerShape(14.dp)`, or `.padding(13.dp)` inside a screen file, stop and add/reuse a token instead.
- **Don't let ViewModels depend on Room types.** `HabitEntity` must never appear in a `ViewModel` or `Composable`'s function signature — only `domain/model/Habit` and friends. Mapping happens once, at the repository boundary.
- **Don't recompute streak/insight math independently in more than one place.** Habit Detail and Insights must call the exact same `domain/usecase` functions from §11.4 — duplicating similar-but-subtly-different math is the most common way habit‑tracker apps end up with numbers that disagree between screens.
- **Don't treat `epochDay` and `Instant`/`epochMillis` interchangeably.** Log rows are keyed by calendar day (`epochDay`, timezone‑independent) for scheduling/streak purposes, but the optional `completedAtEpochMillis` (used only for the hour‑of‑day histogram) is a real timestamp — don't accidentally derive one from the other.
- **Don't skip the empty/loading/error states.** Every screen spec above calls out its empty state explicitly; a screen that only handles the "happy path with data" is not done.
- **Don't build features outside §0's scope** (to‑do lists, focus timers, gamified islands, social feeds, cloud sync) even if they'd be "easy to add while I'm in this file" — the brief is intentionally habit‑tracking‑and‑insights‑only. If something seems missing, check §14 (Stretch Goals) first, and otherwise ask before adding scope.
- **Don't fight the navigation back stack.** Use the type‑safe Navigation Compose APIs' built‑in `popUpTo`/`saveState`/`restoreState` for bottom‑nav tab switching (§7.2) instead of hand‑rolling back‑stack management — most "why did my back button do something weird" bugs in Compose apps come from bypassing these.
- **Don't ship a schema change without a `Migration`.** Even during early development it's worth practicing this discipline, since `fallbackToDestructiveMigration()` silently deletes the user's habit history the moment it's ever hit for real.
- **Do ask before assuming a library version.** Dependency versions in §2.1 are a snapshot; always let Android Studio/Gradle resolve to the actual current stable release rather than pinning to a possibly-stale number from this guide.

---

## 20. Glossary (for shared vocabulary across guide.md and OpenCode's own comments/commit messages)

| Term | Meaning in this project |
|---|---|
| **Habit** | A single trackable behavior the user defined (name, type, schedule, color/icon) |
| **Log** | One row recording whether a specific habit was completed on a specific calendar day |
| **Schedule** | The rule determining which days a habit is "expected" (Daily, Specific Weekdays, X/week, X/month, Every N days) |
| **Streak** | A consecutive run of expected days where the habit was completed, with no expected‑but‑missed day breaking it |
| **Strength score** | The recency‑weighted 0–100 health metric defined in §11.4, distinct from a raw streak count |
| **Vacation mode** | A date range during which a habit's expected days are suspended — no streak credit, no streak breakage |
| **Activity grid** | The GitHub‑style calendar‑heatmap component (`ActivityGrid`) used at week/month/year zoom levels |
| **Container transform** | The M3 motion pattern where a tapped element's bounds visually morph into the next screen's matching element |
| **Expressive spring / Standard spring** | The two named motion presets from §1.5/§6.5 — bouncy for rewarding interactions, critically‑damped for structural navigation |
| **Seed color** | The single source color from which an entire M3 `ColorScheme` is algorithmically derived (app‑wide accent, or per‑habit accent) |

---

## 21. Pre‑Release Checklist

Work through this once all 10 phases are functionally complete and before considering a first release build (internal testing track or a personal install):

- [ ] App icon (adaptive icon: foreground + background + monochrome layer for Android 13+ themed icons) finalized, matching the M3 Expressive brand seed color.
- [ ] `versionCode`/`versionName` set and a signing config in place (release keystore, **not** committed to git — document the keystore location and password‑manager entry instead).
- [ ] ProGuard/R8 rules verified by actually running a `release` build and smoke‑testing it (Room, Hilt, and kotlinx.serialization are the three most common things minification breaks silently).
- [ ] All strings extracted to `res/values/strings.xml` (no hard‑coded UI copy in Composables) so the app is translation‑ready even if you don't localize for v1.
- [ ] Privacy: since Habitzy has no network calls, no analytics SDK, and no ads SDK, write a one‑paragraph privacy policy saying exactly that (required by most distribution channels even for a "we collect nothing" app) and link it from About (§12.6).
- [ ] Manually test the full backup→wipe→restore flow on a real device, not just the emulator (SAF file‑picker behavior can differ).
- [ ] Manually test reminder notifications on a device with the app **force‑stopped and rebooted**, to confirm `BootCompletedReceiver` correctly reschedules everything.
- [ ] Run through §13.2's accessibility checklist one more time with TalkBack actually turned on.
- [ ] Confirm the app behaves correctly with **zero habits** (fresh install) all the way through Insights and Settings — this is the very first thing every new user sees.
- [ ] Battery/Doze testing: confirm reminders still fire reasonably reliably under Android's background restrictions on at least one OEM skin known for aggressive battery management, and document any manufacturer‑specific "allow background activity" guidance you end up needing to surface to users in the About/FAQ area.

---

## 22. Closing Notes

This guide deliberately over‑specifies token names, exact dp/sp values, and formulas so that an AI coding agent working across many separate sessions (with limited memory of earlier phases) has a single, stable source of truth to check itself against instead of re‑deriving decisions inconsistently each time. Treat `guide.md` as living documentation: whenever you and OpenCode deliberately deviate from something written here, **edit this file in the same commit** so it never drifts out of sync with the real codebase — a design guide nobody trusts anymore is worse than no design guide at all.

Good luck building Habitzy — a calm, expressive, fast, fully offline home for showing up every day.

---

## Appendix A — Reference `AndroidManifest.xml` Skeleton

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />

    <application
        android:name=".HabitzyApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:theme="@style/Theme.Habitzy.Splash">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Habitzy.Splash">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Alternate launcher icon (Appearance settings, §12.3), disabled by default -->
        <activity-alias
            android:name=".AltIconAlias"
            android:enabled="false"
            android:icon="@mipmap/ic_launcher_alt"
            android:targetActivity=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity-alias>

        <receiver android:name=".notifications.ReminderReceiver" android:exported="false" />
        <receiver android:name=".notifications.ReminderActionReceiver" android:exported="false" />
        <receiver android:name=".notifications.BootCompletedReceiver" android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```
Note `android:allowBackup="false"` — since Habitzy ships its own explicit export/import flow (§12.5) with a versioned JSON format, letting Android's opaque auto‑backup silently snapshot the Room DB in the background is more likely to cause confusing restore mismatches than to help; keep backups explicit and user‑initiated.

---

## Appendix B — Example Streak Unit Test (illustrates the exact behavior §11.4 must satisfy)

```kotlin
class ComputeStreakUseCaseTest {

    private val useCase = ComputeStreakUseCase()

    @Test
    fun `daily habit with one gap breaks current streak but keeps best streak`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.Daily
        val logs = (0..9).mapNotNull { offset ->
            val date = today.minusDays(offset.toLong())
            // Simulate: last 3 days completed, day 4 back missed, days 5-9 completed (an older 5-day streak)
            val completed = offset < 3 || offset in 4..8
            if (offset == 3) null else HabitLog(habitId = 1, date = date, isCompleted = completed, amountValue = null, checklistDoneMask = 0, completedAt = null)
        }

        val result = useCase(logs = logs, schedule = schedule, today = today, vacationRange = null)

        assertThat(result.currentStreak).isEqualTo(3)   // only the unbroken run ending today
        assertThat(result.bestStreak).isEqualTo(5)       // the older 5-day run, offsets 4..8
    }

    @Test
    fun `every-3-days habit only expects the rolling due date`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.EveryNDays(3)
        val logs = listOf(
            HabitLog(1, today.minusDays(6), true, null, 0, null),
            HabitLog(1, today.minusDays(3), true, null, 0, null),
            // day "today" not yet logged — should not break the streak since it's the currently-due day, not yet missed
        )

        val result = useCase(logs = logs, schedule = schedule, today = today, vacationRange = null)

        assertThat(result.currentStreak).isEqualTo(2)
    }

    @Test
    fun `vacation range excludes those days from both streak and rate math`() {
        val today = LocalDate.of(2026, 3, 10)
        val schedule = HabitSchedule.Daily
        val vacation = LocalDate.of(2026, 3, 5)..LocalDate.of(2026, 3, 7)
        val logs = (0..9).map { offset ->
            val date = today.minusDays(offset.toLong())
            HabitLog(1, date, isCompleted = date !in vacation, null, 0, null) // pretend vacation days were never logged
        }

        val result = useCase(logs = logs, schedule = schedule, today = today, vacationRange = vacation)

        // Because the vacation days are excluded entirely, the streak should read as unbroken across them
        assertThat(result.currentStreak).isEqualTo(10)
    }
}
```

---

## Appendix C — Example DataStore Preferences Full Implementation

```kotlin
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "habitzy_settings")

class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_SEED = intPreferencesKey("accent_seed_argb")
        val TRUE_BLACK = booleanPreferencesKey("true_black")
        val WEEK_START = stringPreferencesKey("week_start_day")
        val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY_DAYS = intPreferencesKey("auto_backup_frequency_days")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val LAST_CELEBRATED_EPOCH_DAY = longPreferencesKey("last_celebrated_epoch_day")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        ThemeMode.valueOf(it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    suspend fun setUseDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    val accentSeedColor: Flow<Int> = context.dataStore.data.map { it[Keys.ACCENT_SEED] ?: HabitzyDefaultSeed.toArgb() }
    suspend fun setAccentSeedColor(argb: Int) = edit { it[Keys.ACCENT_SEED] = argb }

    val useTrueBlack: Flow<Boolean> = context.dataStore.data.map { it[Keys.TRUE_BLACK] ?: false }
    suspend fun setUseTrueBlack(enabled: Boolean) = edit { it[Keys.TRUE_BLACK] = enabled }

    val weekStartDay: Flow<DayOfWeek> = context.dataStore.data.map {
        DayOfWeek.valueOf(it[Keys.WEEK_START] ?: DayOfWeek.MONDAY.name)
    }
    suspend fun setWeekStartDay(day: DayOfWeek) = edit { it[Keys.WEEK_START] = day.name }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS_ENABLED] ?: true }
    suspend fun setHapticsEnabled(enabled: Boolean) = edit { it[Keys.HAPTICS_ENABLED] = enabled }

    val lastCelebratedEpochDay: Flow<Long?> = context.dataStore.data.map { it[Keys.LAST_CELEBRATED_EPOCH_DAY] }
    suspend fun setLastCelebratedEpochDay(day: Long) = edit { it[Keys.LAST_CELEBRATED_EPOCH_DAY] = day }

    // ... default reminder time, auto-backup, app lock getters/setters follow the identical pattern

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
```

This is the pattern OpenCode should replicate for every remaining preference in §4.5's list — one `Flow` getter + one suspend setter per key, all backed by the same single `Keys` object and the same private `edit {}` helper, so Phase 2 stays consistent and easy to extend later (e.g. when Appearance's alternate‑launcher‑icon picker from §12.3 needs its own persisted key).

---

**End of guide.md.** Total scope: 10 build phases (§2–§13), 1 stretch‑goal list (§14), a suggested prompt sequence (§15), full design tokens (§1, §6, §16), full data schema (§4), full formulas (§11.4), and reference code/wireframes (§17–Appendix C) — everything OpenCode needs to build Habitzy end‑to‑end without re‑deriving product or design decisions on its own.
