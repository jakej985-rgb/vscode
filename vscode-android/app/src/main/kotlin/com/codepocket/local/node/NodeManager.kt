package com.codepocket.local.node

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.codepocket.local.PrefsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class to manage the Node.js service lifecycle.
 * 
 * Provides a simple API for starting/stopping the Node server
 * and querying its status.
 */
object NodeManager {
    
    private const val TAG = "NodeManager"
    
    private var isInitialized = false
    private var isServiceRunning = false
    
    /**
     * Initialize the Node.js environment.
     * Call this once during application startup.
     * 
     * @return true if Node.js libraries were loaded successfully
     */
    fun initialize(context: Context): Boolean {
        if (isInitialized) {
            return NodeService.isNodeAvailable()
        }
        
        Log.i(TAG, "Initializing Node.js environment")
        val loaded = NodeService.initializeNodeLibraries()
        isInitialized = true
        
        if (loaded) {
            Log.i(TAG, "Node.js libraries loaded - full functionality available")
        } else {
            Log.w(TAG, "Node.js libraries not available - running in mock mode")
        }
        
        return loaded
    }
    
    /**
     * Check if Node.js is available (native libraries loaded).
     */
    fun isNodeAvailable(): Boolean = NodeService.isNodeAvailable()
    
    /**
     * Check if the Node service is currently running.
     */
    fun isRunning(): Boolean = isServiceRunning
    
    /**
     * Start the Node.js service.
     */
    fun startNodeService(context: Context) {
        if (isServiceRunning) {
            Log.w(TAG, "Node service is already running")
            return
        }
        
        Log.i(TAG, "Starting Node service")
        val intent = Intent(context, NodeService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        isServiceRunning = true
    }
    
    /**
     * Stop the Node.js service.
     */
    fun stopNodeService(context: Context) {
        if (!isServiceRunning) {
            Log.w(TAG, "Node service is not running")
            return
        }
        
        Log.i(TAG, "Stopping Node service")
        val intent = Intent(context, NodeService::class.java)
        context.stopService(intent)
        
        isServiceRunning = false
    }
    
    /**
     * Toggle the Node.js service (start if stopped, stop if running).
     */
    fun toggleNodeService(context: Context) {
        if (isServiceRunning) {
            stopNodeService(context)
        } else {
            startNodeService(context)
        }
    }
    
    /**
     * Start the service and wait for it to be healthy.
     * 
     * @param context Application context
     * @param timeoutMs Maximum time to wait for server to be ready
     * @return true if server is ready, false if timeout
     */
    suspend fun startAndWaitForReady(context: Context, timeoutMs: Long = 10000): Boolean {
        if (!isServiceRunning) {
            startNodeService(context)
        }
        
        return waitForHealthy(timeoutMs)
    }
    
    /**
     * Check if the server is healthy by hitting the health endpoint.
     */
    suspend fun isServerHealthy(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(PrefsConstants.HEALTH_CHECK_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == 200
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed: ${e.message}")
            false
        }
    }
    
    /**
     * Wait for the server to become healthy.
     * 
     * @param timeoutMs Maximum time to wait
     * @param intervalMs Interval between health checks
     * @return true if server became healthy, false if timeout
     */
    suspend fun waitForHealthy(timeoutMs: Long = 10000, intervalMs: Long = 500): Boolean {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isServerHealthy()) {
                Log.i(TAG, "Server is healthy")
                return true
            }
            delay(intervalMs)
        }
        
        Log.w(TAG, "Server health check timed out after ${timeoutMs}ms")
        return false
    }
    
    /**
     * Get the server URL.
     */
    fun getServerUrl(): String = PrefsConstants.DEFAULT_URL
    
    /**
     * Get the path to the Node.js project directory.
     */
    fun getNodeProjectPath(context: Context): File {
        return File(context.filesDir, "nodejs-project")
    }
    
    /**
     * Get the Node.js log file path.
     */
    fun getNodeLogFile(context: Context): File {
        return File(getNodeProjectPath(context), "node.log")
    }
    
    /**
     * Read the last N lines from the Node log file.
     */
    fun readNodeLog(context: Context, lines: Int = 50): String {
        val logFile = getNodeLogFile(context)
        
        return if (logFile.exists()) {
            try {
                logFile.readLines().takeLast(lines).joinToString("\n")
            } catch (e: Exception) {
                Log.e(TAG, "Error reading node log", e)
                "Error reading log: ${e.message}"
            }
        } else {
            "No log file found"
        }
    }
    
    /**
     * Clear the Node log file.
     */
    fun clearNodeLog(context: Context): Boolean {
        val logFile = getNodeLogFile(context)
        return try {
            if (logFile.exists()) {
                logFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing node log", e)
            false
        }
    }
}
