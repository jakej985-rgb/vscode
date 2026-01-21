package com.codepocket.local

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.codepocket.local.databinding.ActivityMainBinding
import com.codepocket.local.node.NodeManager
import com.codepocket.local.workspace.WorkspaceManagerActivity

/**
 * Main entry point for CodePocket Local.
 * 
 * This activity serves as the launcher and hub for:
 * - Starting/stopping the Node.js server
 * - Launching the WebView to display VS Code UI
 * - Accessing settings
 * - (Future) Managing workspaces
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Notification permission request
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start the service
            toggleServerAndUpdateUI()
        } else {
            Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Node.js environment
        initializeNode()
        
        setupUI()
    }

    private fun initializeNode() {
        val available = NodeManager.initialize(this)
        if (!available) {
            // Node.js libraries not available - will run in mock mode
            binding.tvNodeStatus.text = getString(R.string.node_status_mock_mode)
        }
    }

    private fun setupUI() {
        // Server toggle button
        binding.btnToggleServer.setOnClickListener {
            if (hasNotificationPermission()) {
                toggleServerAndUpdateUI()
            } else {
                requestNotificationPermission()
            }
        }

        // Launch WebView button
        binding.btnLaunchEditor.setOnClickListener {
            // Auto-start server if not running
            if (!NodeManager.isRunning()) {
                if (hasNotificationPermission()) {
                    NodeManager.startNodeService(this)
                }
            }
            startActivity(Intent(this, WebViewActivity::class.java))
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Workspaces button
        binding.btnWorkspaces.setOnClickListener {
            startActivity(Intent(this, WorkspaceManagerActivity::class.java))
        }

        // View logs button
        binding.btnViewLogs.setOnClickListener {
            showNodeLogs()
        }

        // Update UI state
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        updateServerUrlDisplay()
        updateServerStatus()
    }

    private fun updateServerUrlDisplay() {
        val prefs = getSharedPreferences(PrefsConstants.PREF_NAME, MODE_PRIVATE)
        val url = prefs.getString(PrefsConstants.KEY_SERVER_URL, PrefsConstants.DEFAULT_URL)
        binding.tvCurrentUrl.text = getString(R.string.current_url_format, url)
    }

    private fun updateServerStatus() {
        val isRunning = NodeManager.isRunning()
        val isAvailable = NodeManager.isNodeAvailable()

        binding.btnToggleServer.text = if (isRunning) {
            getString(R.string.btn_stop_server)
        } else {
            getString(R.string.btn_start_server)
        }

        binding.tvNodeStatus.text = when {
            isRunning && isAvailable -> getString(R.string.node_status_running)
            isRunning && !isAvailable -> getString(R.string.node_status_mock_running)
            !isRunning && isAvailable -> getString(R.string.node_status_stopped)
            else -> getString(R.string.node_status_mock_available)
        }

        // Update button colors
        if (isRunning) {
            binding.btnToggleServer.setBackgroundColor(getColor(R.color.error))
        } else {
            binding.btnToggleServer.setBackgroundColor(getColor(R.color.success))
        }
    }

    private fun toggleServerAndUpdateUI() {
        NodeManager.toggleNodeService(this)
        
        // Small delay to allow service to start/stop
        binding.root.postDelayed({
            updateServerStatus()
        }, 500)
    }

    private fun showNodeLogs() {
        val logs = NodeManager.readNodeLog(this, 100)
        
        // Show logs in a dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.dialog_logs_title)
            .setMessage(logs.ifBlank { getString(R.string.no_logs_available) })
            .setPositiveButton(R.string.btn_close, null)
            .setNeutralButton(R.string.btn_clear_logs) { _, _ ->
                NodeManager.clearNodeLog(this)
                Toast.makeText(this, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
