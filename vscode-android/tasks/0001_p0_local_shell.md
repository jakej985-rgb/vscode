# Task 0001 — P0 Local WebView Shell

**Status:** 🚧 In Progress  
**Priority:** P0 (Critical Path)

---

## Goal

Create an Android app that can load a configurable URL in a properly hardened WebView, with file chooser and download support stubbed in.

---

## Acceptance Criteria

- [ ] `./gradlew assembleDebug` completes successfully
- [ ] `./gradlew lint` passes with no errors
- [ ] WebView loads a configurable URL (default: placeholder page)
- [ ] WebChromeClient handles `onShowFileChooser` (stub OK)
- [ ] DownloadListener handles download requests (stub OK)
- [ ] Settings screen allows URL configuration
- [ ] Configuration persists via SharedPreferences
- [ ] App handles rotation without losing state
- [ ] App handles backgrounding gracefully

---

## Technical Approach

### 1. Project Setup

```
app/
├── build.gradle.kts
├── src/main/
│   ├── kotlin/com/codepocket/local/
│   │   ├── MainActivity.kt
│   │   ├── WebViewActivity.kt
│   │   ├── SettingsActivity.kt
│   │   └── util/
│   │       └── WebViewConfig.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   └── xml/
│   └── AndroidManifest.xml
└── proguard-rules.pro
```

### 2. WebView Configuration

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    allowFileAccess = true
    allowContentAccess = true
    databaseEnabled = true
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    useWideViewPort = true
    loadWithOverviewMode = true
    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    cacheMode = WebSettings.LOAD_DEFAULT
}

// Hardware acceleration
webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
```

### 3. File Chooser Support

```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this@WebViewActivity.filePathCallback = filePathCallback
        openFilePicker()
        return true
    }
}
```

### 4. Download Listener

```kotlin
webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
    // Stub: Log download request
    // Full implementation in later task
    Log.d("Download", "URL: $url, Type: $mimetype")
}
```

### 5. Settings Persistence

```kotlin
// SharedPreferences keys
const val PREF_NAME = "codepocket_prefs"
const val KEY_SERVER_URL = "server_url"
const val DEFAULT_URL = "file:///android_asset/placeholder.html"
```

---

## Validation

```bash
# Build
./gradlew assembleDebug

# Lint
./gradlew lint

# Install and test
./gradlew installDebug
adb shell am start -n com.codepocket.local/.MainActivity
```

### Manual Test Checklist

1. [ ] App launches without crash
2. [ ] WebView displays placeholder page
3. [ ] Settings button navigates to settings
4. [ ] URL can be changed in settings
5. [ ] New URL loads after returning
6. [ ] Rotate device — state preserved
7. [ ] Background app, return — state preserved
8. [ ] File chooser trigger logs (when tested with appropriate page)
9. [ ] Download trigger logs (when tested with appropriate page)

---

## Files to Create

| File | Purpose |
|------|---------|
| `build.gradle.kts` (project) | Root build config |
| `settings.gradle.kts` | Project settings |
| `app/build.gradle.kts` | App module config |
| `app/src/main/AndroidManifest.xml` | Permissions, activities |
| `app/src/main/kotlin/.../MainActivity.kt` | Entry point |
| `app/src/main/kotlin/.../WebViewActivity.kt` | WebView host |
| `app/src/main/kotlin/.../SettingsActivity.kt` | URL config |
| `app/src/main/res/layout/activity_main.xml` | Main layout |
| `app/src/main/res/layout/activity_webview.xml` | WebView layout |
| `app/src/main/res/layout/activity_settings.xml` | Settings layout |
| `app/src/main/res/values/strings.xml` | String resources |
| `app/src/main/res/values/themes.xml` | Theme definitions |
| `app/src/main/assets/placeholder.html` | Default page |

---

## Dependencies

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
}
```

---

## Notes

- WebView on Android does **not** implement file chooser by default — must override `WebChromeClient.onShowFileChooser`
- Downloads need explicit handling via `setDownloadListener`
- `127.0.0.1` / `localhost` are treated as secure origins (no HTTPS required)
- Activity recreation on rotation must preserve WebView state or restore URL
