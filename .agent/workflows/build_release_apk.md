---
description: Build Release APK
---
# Build Release APK

Manually triggers the build for the Release APK.

1. Build Release APK
   - run_command: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; cd vscode-android; .\gradlew.bat assembleRelease

2. Locate APK
   - run_command: echo "APK should be at: vscode-android/app/build/outputs/apk/release/app-release.apk"
