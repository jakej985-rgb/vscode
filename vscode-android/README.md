# CodePocket Local 📱🧰

**A fully local, offline-first IDE for Android.**

CodePocket Local runs a Node.js server embedded within an Android app to provide a powerful VS Code-like editing experience without relying on external servers or internet connection.

---

## ✨ Features

- **Full Editor**: Based on **Monaco Editor**, supporting syntax highlighting, minimap, and multi-tabs.
- **File System Access**: Open local folders via Android Storage Access Framework (SAF) with mirror sync.
- **Integrated Terminal**: Run shell commands (`ls`, `grep`, `cat`) directly within the IDE using `sh`.
- **Source Control**: Full **Git** support (Init, Status, Diff, Commit) powered by `isomorphic-git`.
- **Global Search**: Fast find-in-files with regex support.
- **Plugin API**: Extend functionality using JavaScript plugins.
- **Customizable**: Persistent settings for Theme, Font Size, and Word Wrap.
- **Extensions**: Browse and discover extensions via Open VSX (UI Integration).

---

## 🏗 Architecture

| Component | Technology | Description |
|-----------|------------|-------------|
| **Frontend** | WebView / Monaco | The editor UI running in Android System WebView. |
| **Backend** | Node.js Mobile | Embedded Node.js process serving the API and handling Filesystem operations. |
| **Bridge** | internal HTTP | Communication via `http://127.0.0.1:port` secured by Auth Tokens. |
| **Sync** | Kotlin / SAF | Native Android service syncing external SAF folders to the internal sandbox. |

---

## 🚀 Quick Start

### Installation

1. Download the latest `app-debug.apk` release.
2. Install on Android (Requires Android 8.0+).
3. Open **CodePocket**.

### Creating a Workspace

1. Tap **Workspaces**.
2. Tap **Add Workspace**.
3. Select a folder on your device.
4. Tap the Workspace name to open the editor.

### Using Plugins

Place `.js` files in your workspace's parent `plugins` folder (e.g., via `adb push` or file manager).
Plugins can register commands using the global `cp` API:

```javascript
// example-plugin.js
cp.registerCommand('hello', () => {
    cp.showNotification('Hello from Plugin!');
});
```

---

## 🛠 Building from Source

### Prerequisites
- JDK 17+
- Android SDK (API 34)

### Build Commands
```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

---

## 📚 Project Structure

```
vscode-android/
├── app/
│   ├── src/main/assets/nodejs-project/   # The Node.js Backend
│   │   ├── main.js                       # API Server
│   │   └── public/                       # Frontend Assets (Monaco, xterm)
│   └── src/main/java/                    # Kotlin Android Code
├── tasks/                                # Development Log
└── build.json                            # Project Metadata
```

---

## License

MIT License. Based on **Monaco Editor** and **Node.js Mobile**.
