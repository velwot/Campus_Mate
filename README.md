# CampusMate

Minimal offline-first Android college organizer.

## Included
- Subject-wise note file uploads and subject notices/reminders
- Submission/deadline alert tab with due-date status
- Quick Ideas & Problems scratchpad
- Exam/event countdowns (Mid Exam, End Exam, practicals, etc.)
- ZIP backup import/export including text data and uploaded note files
- Local-only storage; no account or backend

## Build locally
Use Android Studio Quail 4 (2026.1.4) or a Gradle CLI environment with JDK 17. The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, compileSdk 36 and minSdk 24.

After Android SDK setup:
```bash
gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Build on GitHub
The repository includes `.github/workflows/build-apk.yml`. Push the project to a GitHub repository and run the `Build APK` workflow. It installs the Android SDK packages, builds the debug APK, and uploads the APK as a workflow artifact.
