package com.codepocket.local.node

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codepocket.local.MainActivity
import com.codepocket.local.R
import java.io.File
import java.io.FileOutputStream

/**
 * Foreground service that manages the Node.js runtime lifecycle.
 * 
 * This service:
 * - Starts Node.js in a background thread
 * - Maintains a persistent notification while running
 * - Handles graceful shutdown when the service is stopped
 * - Extracts the Node.js project from assets on first run
 * 
 * Usage:
 * - Start: `startForegroundService(Intent(context, NodeService::class.java))`
 * - Stop: `stopService(Intent(context, NodeService::class.java))`
 */
class NodeService : Service() {

    companion object {
        private const val TAG = "NodeService"
        private const val NOTIFICATION_CHANNEL_ID = "codepocket_node_service"
        private const val NOTIFICATION_ID = 1001
        
        // Node.js project folder name in assets
        private const val NODE_PROJECT_FOLDER = "nodejs-project"
        
        // Entry point script
        private const val MAIN_SCRIPT = "main.js"
        
        // Flag to track if Node.js libraries are available
        private var nodeLibrariesLoaded = false
        
        /**
         * Check if the Node.js native libraries are available.
         */
        fun isNodeAvailable(): Boolean = nodeLibrariesLoaded
        
        /**
         * Attempt to load Node.js native libraries.
         * Call this once during app initialization.
         */
        fun initializeNodeLibraries(): Boolean {
            return try {
                // These libraries will be provided by nodejs-mobile-android
                System.loadLibrary("node")
                System.loadLibrary("nodejs-mobile")
                nodeLibrariesLoaded = true
                Log.i(TAG, "Node.js libraries loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Node.js libraries not available: ${e.message}")
                nodeLibrariesLoaded = false
                false
            }
        }
    }
    
    private var nodeThread: Thread? = null
    private var isNodeRunning = false
    private var projectPath: String? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NodeService onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NodeService onStartCommand")
        
        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."))
        
        // Extract Node project if needed and start Node
        Thread {
            try {
                // Extract Node.js project from assets
                projectPath = extractNodeProject()
                
                if (projectPath != null) {
                    updateNotification("Running")
                    startNode(projectPath!!)
                } else {
                    updateNotification("Failed to extract project")
                    Log.e(TAG, "Failed to extract Node.js project")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Node", e)
                updateNotification("Error: ${e.message}")
            }
        }.start()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "NodeService onDestroy")
        stopNode()
        super.onDestroy()
    }

    /**
     * Start the Node.js runtime with the main script.
     */
    private fun startNode(projectPath: String) {
        if (isNodeRunning) {
            Log.w(TAG, "Node is already running")
            return
        }
        
        // Generate auth token
        val token = generateAuthToken(projectPath)
        Log.i(TAG, "Generated auth token: $token")
        
        if (!nodeLibrariesLoaded) {
            Log.w(TAG, "Node libraries not loaded, running in mock mode")
            startMockNode(projectPath)
            return
        }
        
        val scriptPath = File(projectPath, MAIN_SCRIPT).absolutePath
        Log.i(TAG, "Starting Node.js with script: $scriptPath")
        
        nodeThread = Thread {
            try {
                isNodeRunning = true
                // This will be the actual Node.js Mobile call
                // startNodeWithArguments(arrayOf("node", scriptPath))
                
                // For now, log that we would start Node here
                Log.i(TAG, "Node.js would start here with: $scriptPath")
                
                // Keep thread alive (in real implementation, Node.js blocks)
                while (isNodeRunning && !Thread.currentThread().isInterrupted) {
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {
                Log.i(TAG, "Node thread interrupted")
            } catch (e: Exception) {
                Log.e(TAG, "Node execution error", e)
            } finally {
                isNodeRunning = false
                Log.i(TAG, "Node thread exited")
            }
        }
        nodeThread?.name = "NodeJS-Runtime"
        nodeThread?.start()
    }

    private fun generateAuthToken(projectPath: String): String {
        val token = java.util.UUID.randomUUID().toString()
        val tokenFile = File(projectPath, ".auth_token")
        tokenFile.writeText(token)
        return token
    }
    
    /**
     * Mock Node.js execution for testing when libraries aren't available.
     */
    private fun startMockNode(projectPath: String) {
        Log.i(TAG, "Starting mock Node.js for testing")
        
        nodeThread = Thread {
            val logFile = File(projectPath, "node.log")
            
            try {
                isNodeRunning = true
                
                fun log(message: String) {
                    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 
                        java.util.Locale.US).format(java.util.Date())
                    val line = "[$timestamp] $message\n"
                    logFile.appendText(line)
                    Log.d(TAG, "[MockNode] $message")
                }
                
                log("Node.js Mobile started (mock mode)")
                log("Project path: $projectPath")
                
                var heartbeatCount = 0
                while (isNodeRunning && !Thread.currentThread().isInterrupted) {
                    Thread.sleep(5000)
                    heartbeatCount++
                    log("Heartbeat #$heartbeatCount")
                }
                
                log("Received shutdown signal")
            } catch (e: InterruptedException) {
                Log.i(TAG, "Mock Node thread interrupted")
            } finally {
                isNodeRunning = false
            }
        }
        nodeThread?.name = "MockNodeJS"
        nodeThread?.start()
    }

    /**
     * Stop the Node.js runtime gracefully.
     */
    private fun stopNode() {
        Log.i(TAG, "Stopping Node.js")
        isNodeRunning = false
        
        nodeThread?.let { thread ->
            try {
                thread.interrupt()
                thread.join(5000) // Wait up to 5 seconds
                if (thread.isAlive) {
                    Log.w(TAG, "Node thread did not stop gracefully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Node thread", e)
            }
            Unit
        }
        nodeThread = null
    }

    /**
     * Extract the Node.js project from assets to internal storage.
     * Returns the path to the extracted project, or null on failure.
     */
    private fun extractNodeProject(): String? {
        val targetDir = File(filesDir, NODE_PROJECT_FOLDER)
        
        // Check if already extracted
        val mainScript = File(targetDir, MAIN_SCRIPT)
        if (mainScript.exists()) {
            Log.d(TAG, "Node project already extracted at: ${targetDir.absolutePath}")
            return targetDir.absolutePath
        }
        
        Log.i(TAG, "Extracting Node project to: ${targetDir.absolutePath}")
        
        return try {
            targetDir.mkdirs()
            
            // List all files in the Node project assets folder
            val assetFiles = assets.list(NODE_PROJECT_FOLDER) ?: emptyArray()
            
            for (fileName in assetFiles) {
                val assetPath = "$NODE_PROJECT_FOLDER/$fileName"
                val targetFile = File(targetDir, fileName)
                
                // Copy file from assets
                assets.open(assetPath).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Extracted: $fileName")
            }
            
            targetDir.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract Node project", e)
            null
        }
    }

    /**
     * Create the notification channel (required for Android 8.0+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Code Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when the code server is running"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Create the foreground service notification.
     */
    private fun createNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CodePocket Server")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_code_editor)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Update the notification with a new status message.
     */
    private fun updateNotification(status: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(status))
    }
}
