# Qarz Daftari Mobile (Android Native)

Native Android application built with **Kotlin** and **Jetpack Compose (Material 3)**.

- **Package**: `com.qarzdaftari.aslbek`
- **Compile SDK**: 35 (Android 15)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35

## Implemented Systems

1. **Authentication System**:
   - Local persistent authentication powered by SQLite and `SessionManager`.
   - Email/Password user registration and login.
   - Quick Guest access mode (`Mehmon sifatida kirish`).
   - Persistent user sessions and secure logout.

2. **Debt Saving System**:
   - Interactive dialog to add and save debts.
   - Fields: Name, Debt direction (*Menga qarzdor* / *Men qarzdorman*), Amount (so'm), Initial payment status, Phone number, and Due date / notes.
   - Instant validation and reactive persistence.

3. **Debt Deleting System**:
   - Direct delete action on each debt record.
   - Safe confirmation prompt (`DeleteConfirmDialog`) preventing accidental removals.

4. **Debt Management & Financial Overview**:
   - Financial cards: Total lent, Total borrowed, Net balance, and Total record count.
   - Real-time search filter by debtor name or phone.
   - Status filters: All, Given (*Berganlarim*), Received (*Olganlarim*), Settled (*Yopilganlar*).
   - Partial or full payment recording with instant balance recalculation.

## Building with GitHub Actions

The repository includes an automated CI workflow at `.github/workflows/android-apk.yml`.
When changes are pushed to `mobile/**`, GitHub Actions automatically:
1. Sets up JDK 17 & Android SDK 35 build tools.
2. Compiles the Kotlin source using Gradle.
3. Assembles the debug APK (`app-debug.apk`).
4. Uploads the generated APK as an artifact (`qarzdaftari-debug-apk`).