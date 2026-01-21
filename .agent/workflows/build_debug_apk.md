---
description: Build Debug APK
---
# Build Debug APK

Manually triggers the build for the Debug APK.

1. Build Debug APK
   - run_command: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; cd vscode-android; .\gradlew.bat assembleDebug

2. Locate APK
   - run_command: echo "APK should be at: vscode-android/app/build/outputs/apk/debug/app-debug.apk"
