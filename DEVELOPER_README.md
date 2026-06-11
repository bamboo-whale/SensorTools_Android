# SensorTools Developer Guide

This document is for team members and for project presentation to instructors.

## 1. Project Overview

SensorTools is an Android sensor toolkit built with Jetpack Compose. It provides:
- sensor browsing and grouping
- live sensor monitoring
- calibration for multiple sensors
- health scans with categorized results
- recording/export support

The current UX goal is:
- outer layer shows friendly names and categories
- detail layer shows technical names, vendor, and device parameters
- every long-running operation must have visible progress and error feedback

## 2. Architecture

The app follows a layered structure:

- Presentation layer
  - Compose screens
  - ViewModels
  - UI state collected through `StateFlow`

- Domain layer
  - sensor classification rules
  - health analysis logic
  - motion detection helpers

- Data layer
  - `SensorRepository` for sensor discovery and live reading
  - `CalibrationRepository` for calibration sampling and persistence
  - `PreferencesManager` for local settings and saved calibration data
  - `SensorRecordingService` for recording-related background work

The design keeps UI code thin and moves sensor access into repositories.

## 3. Code Structure

### App entry

- `app/src/main/java/com/sensortools/MainActivity.kt`
  - app host activity
  - sets up the Compose root

- `app/src/main/java/com/sensortools/ui/navigation/NavGraph.kt`
  - central navigation routes
  - screen switching and shared navigation arguments

### Presentation

- `ui/home`
  - `HomeScreen.kt`
  - `HomeViewModel.kt`
  - sensor list grouping by category

- `ui/detail`
  - `DetailScreen.kt`
  - `DetailViewModel.kt`
  - real-time sensor data visualization

- `ui/calibration`
  - `CalibrationScreen.kt`
  - `CalibrationViewModel.kt`
  - calibration states: idle, running, completed, failed/unavailable

- `ui/health`
  - `HealthScreen.kt`
  - `HealthViewModel.kt`
  - health scan dashboard and categorized results

- `ui/record`
  - recording UI and start/stop logic

- `ui/settings`
  - app preferences and behavior controls

- `ui/about`
  - project/app info

- `ui/components`
  - shared cards, status badges, charts, and sensor icon helpers

### Data

- `data/model`
  - `SensorInfo.kt`
  - `SensorData.kt`
  - `CalibrationData.kt`

- `data/repository`
  - `SensorRepository.kt`
  - `CalibrationRepository.kt`

- `data/local`
  - `PreferencesManager.kt`

- `data/service`
  - `SensorRecordingService.kt`

### Domain and utils

- `domain/HealthAnalyzer.kt`
  - health status evaluation

- `domain/SensorClassifier.kt`
  - sensor-specific classification rules

- `util/MotionDetector.kt`
  - motion state detection

- `util/ExportManager.kt`
  - file export support

## 4. UI / UX Rules

Current interface rules used in the latest changes:
- Home page uses friendly category names like `运动与姿态` and `环境感知`
- Category cards are collapsible to reduce visual noise
- Each category has an icon to improve scanability
- Detail page emphasizes the concrete model name and vendor information
- Health page uses dashboard-style cards, not a single-button layout
- Calibration page must not appear frozen when the result is incomplete

## 5. State Management Pattern

Most screens follow this pattern:
1. ViewModel loads or subscribes to sensor data.
2. UI collects `StateFlow`.
3. Compose recomposes when state changes.
4. Errors, timeout, and unavailable states are surfaced in the UI.

Why this matters:
- easier to debug
- clearer demo behavior
- safer than passing sensor state directly through UI callbacks

## 6. Sensor Flow Summary

### Home
- load all sensors from repository
- group them by category
- show friendly names first
- navigate to details on tap

### Detail
- initialize a specific sensor
- start live sampling
- display data, stats, and chart

### Calibration
- start calibration for accelerometer or gyroscope
- observe magnetometer calibration
- show progress immediately
- show completion or failure reason

### Health
- run health scan over available sensors
- preserve scan results
- display normal/abnormal/suspect/no-data categories

## 7. Local Testing

Recommended commands:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat
```

Manual verification checklist:
- app opens normally
- home screen categories expand/collapse correctly
- detail page shows the correct sensor model information
- calibration buttons change state immediately
- health page opens without crashing
- missing sensors show visible feedback instead of silent failure

## 8. Presentation Talking Points

When reporting to the teacher, you can describe the work like this:

1. We separated the app into data, domain, and UI layers.
2. We improved usability by replacing raw sensor names with friendly categories on the home screen.
3. We kept technical detail available in the detail page for advanced inspection.
4. We fixed blocking bugs in health and calibration flows by adding explicit state handling.
5. We redesigned the health page into a dashboard-style interface with denser information.
6. We made calibration states visible so incomplete work is not mistaken for a freeze.

## 9. Team Notes

- Keep UI text consistent with the friendly-name-first rule.
- Add new sensor categories in both the home grouping logic and the detail icon/title mapping.
- If repository behavior changes, update the corresponding ViewModel state messages so the UI stays informative.
- Avoid silent failures in any sensor-related flow. Prefer explicit `NO_DATA`, `UNAVAILABLE`, or `FAILED` states.

