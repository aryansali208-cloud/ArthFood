# ArthFood

**ArthFood** is an offline-first Android app for small food businesses to manage orders, billing, product stock, and vendor payments.

## Features

- **Orders and billing:** Create and edit customer orders, track order and payment status, and account for discounts, tax, delivery fees, and other charges.
- **Products and stock:** Maintain a product catalog with prices by weight and pack size. Stock tracking and low-stock thresholds are available.
- **Customers and vendors:** Keep customer details and order history, manage vendors, and record vendor payments.
- **Business reports:** Review profit summaries and vendor payables. Export an Excel workbook with Orders, Item Breakdown, Profit Summary, and Vendor Payables sheets.
- **Invoices and sharing:** Add business details and a logo, create invoice images, and share them from the app.
- **QR and backups:** Generate QR codes and export or restore app data using JSON backups.
- **Languages:** The app and invoices can each use English, Hindi, or Marathi.
- **WhatsApp order text:** Paste order text to create a parsed draft that can be reviewed and edited before saving.

## Tech stack

- Kotlin and Android
- Jetpack Compose with Material 3
- Room for local database storage and DataStore for preferences
- Kotlin Coroutines
- ZXing for QR code generation
- Gradle Kotlin DSL and a Gradle version catalog

The Android app's application ID is `com.arthfood.billing`. It supports Android API 24 and later, targets API 36, and is configured to compile with API 36.1.

## Getting started

### Requirements

- Android Studio with the Android SDK platform requested by the project (API 36.1)
- Gradle 9.3.1 available on your system
- A Java runtime compatible with the configured Android Gradle Plugin 9.1.1

The project sets Java source and target compatibility to 11; it does not specify an exact JDK version.

### Build and run

Clone the repository and build the debug app:

```bash
git clone https://github.com/aryansali208-cloud/ArthFood.git
cd ArthFood

gradle :app:assembleDebug
```

To install the debug app on a connected Android device or emulator:

```bash
gradle :app:installDebug
```

The repository contains Gradle wrapper configuration under `gradle/wrapper/`, but does not include the `gradlew` or `gradlew.bat` launch scripts. The commands above therefore use a local Gradle installation.

### Run tests

```bash
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

The connected-device test task requires an Android device or emulator.

### Configuration and release builds

The sample `.env.example` contains a commented Gemini API key placeholder. Gemini/Firebase AI dependencies are currently commented out in `app/build.gradle.kts`, so an API key is not needed for the active default build. If you enable that integration, keep real keys in local secrets and out of version control.

Release builds use a signing keystore. The build file reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`; it defaults the keystore path to `my-upload-key.jks` and uses the alias `upload`. These release credentials are not required for a debug build.

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── data/       # Room storage, models, and repository
│       │   │   ├── export/     # Invoice images, QR codes, JSON and Excel
│       │   │   ├── i18n/       # App and invoice strings
│       │   │   ├── licensing/
│       │   │   ├── ui/         # Compose screens, components, and theme
│       │   │   └── util/
│       │   └── res/            # Android resources
│       ├── test/               # Unit and Robolectric tests
│       └── androidTest/        # Instrumented Android tests
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── keygen.html                 # Browser-based license administration page
├── src/utils/license.ts       # License utility
└── public/assets/              # Project assets
```

## Notes

- The repository metadata mentions a Gemini API capability, but the current Android build has the Firebase AI dependency commented out. This README describes features confirmed by the active app code and configuration.
- The repository does not specify an exact Android Studio or JDK version.
