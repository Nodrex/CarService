# 🚘 CarConnect

![Android](https://img.shields.io/badge/Android-35-3DDC84?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Compose-Material_3-4285F4?style=for-the-badge&logo=android)

A modern Android 15 (minSdk 35) automation application designed to streamline your driving experience. CarConnect detects a connection to a designated vehicle's Bluetooth (such as a Subaru Crosstrek) and automatically prepares your device for the commute. From waking the screen to launching navigation and media, it handles all the tedious steps seamlessly in the background.

## ✨ Core Features

- 🔗 **Reliable Background Execution:** Utilizes Android 15's `CompanionDeviceManager` to reliably detect Bluetooth connections without violating strict background launch restrictions.
- 🔓 **Automated Workflow:** Employs Full-Screen Intents and WakeLocks to wake the screen and securely bypass the lock screen (works best when paired with Android Smart Lock).
- 🗺️ **App Orchestration:** Automatically dispatches media play intents directly to Spotify and brings Waze to the foreground for immediate, hands-free navigation.
- 🔦 **Hardware Automation:** Runs a Foreground Service (`connectedDevice` type) that registers an accelerometer listener, enabling a highly responsive "Shake-to-Flashlight" toggle via the `CameraManager`.
- 🎨 **Modern UI/UX:** Built entirely with Jetpack Compose. Features a robust onboarding flow to guide users through Android 15's complex permissions, dynamic adaptive launcher icons, and a gorgeous, theme-aware splash screen.

## 🛠️ Tech Stack

- **Language:** 100% Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Concurrency:** Kotlin Coroutines & Flows
- **Local Storage:** Preferences DataStore
- **System APIs:** `CompanionDeviceManager`, `SensorManager`, `CameraManager`, `NotificationManager`, `PowerManager`

## 🚀 Future Improvements (Roadmap)

- **Dynamic Device Profiles:** Introduce configurable profiles to execute different automation routines depending on which specific Bluetooth device connects (e.g., launching different apps for the car vs. home audio).
- **Custom Media Integration:** Replace the hardcoded Spotify launch with a user-selectable media preference (e.g., Spotify, YouTube Music, Apple Music, Pocket Casts).
- **Selectable Navigation Provider:** Allow users to choose their preferred navigation app to launch on connection (e.g., Waze, Google Maps, Sygic) via a new settings UI.
- **Programmable Shake Gestures:** Expand the `SensorEventListener` logic to let users map the shake gesture to custom actions—such as toggling media play/pause, skipping tracks, or launching a voice assistant—instead of being locked to the camera flashlight.
