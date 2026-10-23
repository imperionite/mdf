# MDF — Medicine and Prescription Tracker

MDF is an offline-first Android application for managing prescriptions, recording prescribed medicines, tracking partial and completed purchases, and reviewing purchase history and spending insights.

---

## Features

- **Prescription Management:** Create, view, edit, and delete prescriptions.
- **Dosage Tracking:** Record medicine details and dosage regimens.
- **Purchase Monitoring:** Track purchased and remaining quantities.
- **Price History:** Preserve historical purchase prices and review spending insights.
- **Local Storage:** Store application data locally using Room.
- **Insight Report:** View a lightweight summary of application data through static visualizations and simple calculations.
- **Demonstration Data:** Seed two fictional prescription scenarios for immediate evaluation.

---

## Technology Stack

- **UI & Architecture:** Kotlin, Jetpack Compose, Android Navigation Compose
- **Data & Concurrency:** Room, Kotlin Coroutines
- **Testing:** JUnit, Android Instrumentation Tests, Regression Tests

---

## Getting Started

### 1. Clone the repository

```powershell
git clone https://github.com/imperionite/mdf.git
cd mdf
```

### 2. Open the Project

1. Install and open Android Studio.
2. Select **Open** and choose the cloned `mdf` project directory.
3. Allow Gradle synchronization to complete.
4. Connect an Android device with USB debugging enabled, or use an Android Emulator if your system supports it.

### 3. Build and Run

From the repository root, open Android Studio's PowerShell terminal and run:

```powershell
.\gradlew.bat assembleDebug
```

To install the debug build on a connected device:

```powershell
.\gradlew.bat installDebug
```

See the [Command Reference](#command-reference) for additional build, testing, and device-management commands.

---

## Local Persistence via Room Rationale

While local data persistence was optional for this project, Room was integrated into the core architecture starting in [Milestone 1](https://docs.google.com/document/d/159k5Q9VZ9QieaRSABpE8hMTOG7tINOPJh0Zd3LwD6xw/edit?usp=sharing).

Setting up a database early provided practical experience with modern Android data tools while establishing a realistic offline-first design. Integrating persistence from the beginning also created a dedicated data layer and avoided the need for a major refactor later in development.

---

## Development Environment

**Windows 11 is the primary operating system setup documented and validated for this project.** Other operating systems have not been formally validated.

### Requirements

- Android Studio
- Compatible JDK
- Android SDK components required by the project
- USB-debugging-enabled Android device

**Note:** An Android Studio Emulator may also be used, but its performance has not been validated due to resource limitations associated with system virtualization.

If `adb` is not recognized in your terminal, add the Android SDK's `platform-tools` directory to your Windows `PATH`, or invoke `adb.exe` using its full path.

---

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

The project includes local unit tests and connected Android instrumentation and regression tests.

Run both test suites to verify your current checkout:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

A successful build alone does not guarantee that instrumentation tests have passed. Ensure an appropriate Android device is connected and available before running connected tests.

---

## Seed Data for Demo

A fresh database is automatically seeded with two fictional prescription scenarios: one featuring partial purchases and another featuring completed purchases.

Existing unmarked user data is not intentionally overwritten or reset. Demo records are provided strictly for software evaluation and do not constitute medical advice.

---

## Changelog

### MS1 — Baseline

- Established the four core prescription and purchase use cases.
- Introduced Room for local data persistence.

See the [v1.0.0 baseline release](https://github.com/imperionite/mdf/releases/tag/v1.0.0).

### MS2 — Refinements and Supporting Features

- **Navigation:** Improved screen navigation routes and back-stack handling.
- **Prescription Management:** Added clearer list summaries and detailed prescription views, with safeguards against invalid medicine removals and prescribed-quantity reductions.
- **Purchase History & Pricing:** Preserved historical purchase prices independently of current medicine prices.
- **Persistence:** Refined Room migrations and seed-data handling to support database changes while protecting existing records.
- **Insight Report:** Added lightweight data visualizations and calculations for a concise overview of application data.
- **User Feedback:** Improved loading and navigation feedback during application operations.

### Core Use Cases Preserved

The four core use cases established in MS1 remain the foundation of the application:

1. **Create Prescription Transaction**
2. **Record Purchase Batch**
3. **Review Purchase History**
4. **Update Medicine Price**

MS2 refines these use cases to improve usability, data integrity, and maintainability. The Insight Report is a supporting visualization, not a replacement for or modification of the four core use cases.

---

## Screen Snapshots

<table>
  <tr>
    <td align="center" width="50%">
      <strong>Home Screen</strong><br><br>
      <img src="https://drive.google.com/uc?id=1JahjMXTy7RKH1sE5S-jTrHTXe0BnR59h" width="200" alt="MDF Home Screen">
    </td>
    <td align="center" width="50%">
      <strong>Add Prescription</strong><br><br>
      <img src="https://drive.google.com/uc?id=1c3dt-L6q_ZfozsIByrjkOHbeO5yfoqjs" width="200" alt="MDF Add Prescription Screen">
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <strong>View Prescription Details</strong><br><br>
      <img src="https://drive.google.com/uc?id=1K6IkyJSy4-jNy5A5pO3XZGVw_0OoE6OO" width="200" alt="MDF Prescription Details Screen">
    </td>
    <td align="center" width="50%">
      <strong>Edit Prescription</strong><br><br>
      <img src="https://drive.google.com/uc?id=1-2ymaDKxFtLG48decdoed8BYaLVlBziD" width="200" alt="MDF Edit Prescription Screen">
    </td>
  </tr>
  <tr>
    <td align="center" width="50%">
      <strong>Update Batch Purchase</strong><br><br>
      <img src="https://drive.google.com/uc?id=1LoIrxjpq1NxxuC04qzzwYSHIY1hUCJxC" width="200" alt="MDF Update Batch Purchase Screen">
    </td>
    <td align="center" width="50%">
      <strong>Insight Report</strong><br><br>
      <img src="https://drive.google.com/uc?id=1J1-rL_LhD467sEW68iqtOvl_yIKWGYH2" width="200" alt="MDF Insight Report Screen">
    </td>
  </tr>
</table>
