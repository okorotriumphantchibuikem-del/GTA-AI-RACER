# GTA AI RACER — Android starter 0.1.0

This is the first native Android project milestone for a fully on-device GTA San Andreas racing assistant.

## Honest status
This is **not yet an automatic racing bot** and this source package is **not a compiled APK**. It is a starter app that establishes Android screen-capture permission and the accessibility-service foundation. The racing logic (road/checkpoint recognition, steering decisions, throttle/brake control and race testing) still needs to be implemented and calibrated against screenshots from the exact GTA Android edition.

## Build an APK
1. Install Android Studio on a computer (one-time build step).
2. Open this folder as an existing project.
3. Let Gradle sync and install Android SDK Platform 35 if prompted.
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Android Studio creates `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer/install that APK on the Redmi.

The installed app itself is designed to run locally on the Redmi; no laptop or cloud connection is intended during gameplay.

## First-run test
1. Open GTA AI RACER.
2. Tap **Enable game screen capture** and approve Android's screen-capture prompt.
3. Tap **Open accessibility settings**. Enable GTA AI RACER only if you want to test its future gesture-control capability. You can leave it off for this screen-capture-only milestone.
4. Open GTA San Andreas. Android displays an ongoing capture notification while capture is active.

## Important Android limitations
- Android requires explicit consent for screen capture and special accessibility permissions.
- Android/MIUI may stop background services to save battery; allow background activity if the capture service is interrupted.
- The standard GTA package name used by the launcher button may differ by edition/store. You can always open GTA manually.
- This project does not bypass GTA anti-cheat or modify game files.


## Racing input layer (new)
The project now includes `RaceInputController.kt`, an accessibility gesture layer based on the user's reported control scheme: swipe on the left side to steer and the lower-right pedal to accelerate. Steering is normalized to screen dimensions.

**Not yet autonomous:** The controller is only an input primitive. It is not yet connected to computer vision or a closed-loop driving policy, so do not treat it as a race-winning bot. The next required work is a frame-analysis loop, lane/road and checkpoint detection, steering calibration, safe speed control, and real-device race testing. Gesture coordinates must be calibrated on the actual phone because game HUD layouts and screen scaling vary.


## Build the APK without Android Studio (GitHub Actions)
This repository includes `.github/workflows/build-apk.yml`.
1. Create a GitHub repository named `GTA-AI-RACER` from github.com.
2. Upload the contents of this ZIP to the repository (extract the ZIP first).
3. Open the repository's **Actions** tab and run **Build GTA AI RACER APK** (or wait for the main-branch push build).
4. When the workflow succeeds, open its run, scroll to **Artifacts**, and download `GTA-AI-RACER-debug-apk`.
5. Extract the downloaded artifact and install `app-debug.apk` on Android. Approve installation from the browser/files app if Android asks.

This workflow compiles the Android starter. It does not magically complete the racing AI: computer vision, closed-loop driving decisions, calibrated input timing and real race validation remain future implementation milestones. The current controller is an experimental input primitive, not a race-winning autonomous agent.
