---
description: Run Workflow Medic (Android/Gradle Health Checks)
---
# Workflow Medic - CodePocket Local

Runs standard health checks for the Android project (vscode-android) using the bundled Android Studio JDK.

1. Lint Analysis
   - run_command: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; cd vscode-android; .\gradlew.bat lintDebug

2. Run Unit Tests
   - run_command: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; cd vscode-android; .\gradlew.bat testDebugUnitTest

3. Verify Build (Debug)
   - run_command: $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; cd vscode-android; .\gradlew.bat assembleDebug
