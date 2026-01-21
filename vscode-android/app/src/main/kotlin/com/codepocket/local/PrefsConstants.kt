package com.codepocket.local

/**
 * Shared preferences constants for the application.
 */
object PrefsConstants {
    const val PREF_NAME = "codepocket_prefs"
    const val KEY_SERVER_URL = "server_url"
    
    // Server configuration
    const val DEFAULT_PORT = 13337
    const val DEFAULT_HOST = "127.0.0.1"
    
    // Default to localhost server (Node.js serves the editor)
    const val DEFAULT_URL = "http://$DEFAULT_HOST:$DEFAULT_PORT/"
    
    // Fallback placeholder page for testing without server
    const val PLACEHOLDER_URL = "file:///android_asset/placeholder.html"
    
    // Health check endpoint
    const val HEALTH_CHECK_URL = "http://$DEFAULT_HOST:$DEFAULT_PORT/health"
}
