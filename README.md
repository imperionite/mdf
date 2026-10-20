# MDF — Medicine and Prescription Tracker

mdf is an offline-first Android application designed for managing prescriptions, recording prescribed medicines, tracking partial and completed purchases, and reviewing purchase history and spending insights.

---

## Features

- **Prescription Management:** Create, view, edit, and delete prescriptions.
- **Dosage Tracking:** Record medicine details and dosage regimens.
- **Purchase Monitoring:** Track purchased and remaining quantities.
- **Price History:** Preserve historical purchase prices and review spending insights.
- **Local Storage:** Store data securely and locally using Room.
- **Demonstration Data:** Seed two fictional demonstration datasets for immediate evaluation.

---

## Technology Stack

- **UI & Architecture:** Kotlin, Jetpack Compose, Android Navigation Compose
- **Data & Concurrency:** Room, Kotlin Coroutines
- **Testing:** JUnit, Android Instrumentation tests, Regression tests

---

## Local Persistence via Room Rationale

While local data persistence was optional for this project, the decision was made to integrate Room directly into the core architecture starting in [Milestone 1](https://docs.google.com/document/d/159k5Q9VZ9QieaRSABpE8hMTOG7tINOPJh0Zd3LwD6xw/edit?usp=sharing). 

Setting up a proper database early allowed the developer to gain practical experience with modern Android data tools while ensuring the app had a realistic, offline-first design from day one. Rather than treating persistence as something to retrofit later, building it in from the start established a clean data layer and saved the project from a major refactor down the line.

---

## Development Environment

**Windows 11 is the primary operating system setup documented and validated for this project.** Other operating systems have not been formally validated.

### Requirements

- Android Studio
- Compatible JDK
- Android SDK components required by the project
- USB-debugging-enabled Android device _(Note: Android Studio Emulator can be used, but performance is not validated due to resource limitations of running heavy system virtualization)_

If `adb` is not recognized in your terminal, add the Android SDK's `platform-tools` directory to your Windows `PATH`, or invoke `adb.exe` using its full path.

## Command Reference

Run the following commands from the repository root using Android Studio's PowerShell terminal.

### Build & Testing Commands

| Purpose                                 | Command                                        |
| --------------------------------------- | ---------------------------------------------- |
| Build the debug APK                     | `.\gradlew.bat assembleDebug`                  |
| Build and install on a connected device | `.\gradlew.bat installDebug`                   |
| Run local unit tests                    | `.\gradlew.bat testDebugUnitTest`              |
| Run connected instrumentation tests     | `.\gradlew.bat :app:connectedDebugAndroidTest` |
| Clean generated build files             | `.\gradlew.bat clean`                          |
| List available Gradle tasks             | `.\gradlew.bat tasks`                          |
| Display detailed build errors           | `.\gradlew.bat assembleDebug --stacktrace`     |

### Device & App Control Commands

| Purpose                        | Command                                                    |
| ------------------------------ | ---------------------------------------------------------- |
| List connected Android devices | `adb devices`                                              |
| Restart the ADB server         | `adb kill-server` / `adb start-server`                     |
| Install an existing APK        | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Launch MDF                     | `adb shell monkey -p com.imperionite.mdf 1`                |
| Force-stop MDF                 | `adb shell am force-stop com.imperionite.mdf`              |
| View Android runtime errors    | `adb logcat -s AndroidRuntime:E`                           |
| View all Android logs          | `adb logcat`                                               |

### Data & Package Management Commands

| Purpose                 | Command                                          |
| ----------------------- | ------------------------------------------------ |
| Clear MDF's app data    | `adb shell pm clear com.imperionite.mdf`         |
| Uninstall MDF           | `adb uninstall com.imperionite.mdf`              |
| Check installed package | `adb shell pm list packages com.imperionite.mdf` |

---

## Testing

The project includes both unit tests and connected Android instrumentation/regression tests. Always run both test suites to verify your current checkout. _A successful build alone does not guarantee that instrumentation tests have passed._

---

## Demo Data

A fresh database is automatically seeded with two prescription scenarios: one featuring partial purchases and one with completed purchases.

Existing unmarked user data is not intentionally overwritten or reset. _Demo records are provided strictly for software evaluation and do not constitute medical advice._
