package com.codepocket.local.workspace

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepocket.local.R
import com.codepocket.local.WebViewActivity
import com.codepocket.local.databinding.ActivityWorkspaceManagerBinding
import com.codepocket.local.databinding.ItemWorkspaceBinding
import com.codepocket.local.node.NodeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for managing workspaces.
 * 
 * Features:
 * - List all mirrored workspaces
 * - Add new workspace via SAF folder picker
 * - Open workspace in editor
 * - Sync changes back to original
 * - Delete workspace
 */
class WorkspaceManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkspaceManagerBinding
    private lateinit var repository: WorkspaceRepository
    private lateinit var mirror: WorkspaceMirror
    private lateinit var adapter: WorkspaceAdapter

    // SAF folder picker
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { selectedUri ->
            // Take persistent permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
            
            // Start mirroring
            mirrorWorkspace(selectedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkspaceManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WorkspaceRepository(this)
        mirror = WorkspaceMirror(this)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        
        loadWorkspaces()
    }

    override fun onResume() {
        super.onResume()
        refreshWorkspacesState()
    }

    private fun refreshWorkspacesState() {
        // First load what we have to show UI immediately
        loadWorkspaces()
        
        // Then check for background file changes (edits from Node.js)
        lifecycleScope.launch {
            val workspaces = repository.getAllWorkspaces()
            var anyChanged = false
            
            workspaces.forEach { ws ->
                val updated = mirror.checkUnsyncedChanges(ws)
                if (updated.lastEditedAt != ws.lastEditedAt) {
                    anyChanged = true
                }
            }
            
            // If we detected filesystem changes, reload the list to show "Unsynced" indicators
            if (anyChanged) {
                loadWorkspaces()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_workspaces)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = WorkspaceAdapter(
            onOpen = { workspace -> openWorkspace(workspace) },
            onSync = { workspace -> syncWorkspace(workspace) },
            onDelete = { workspace -> confirmDeleteWorkspace(workspace) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddWorkspace.setOnClickListener {
            openFolderPicker()
        }
    }

    private fun loadWorkspaces() {
        val workspaces = repository.getAllWorkspaces()
        adapter.submitList(workspaces)
        
        // Show/hide empty state
        if (workspaces.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun openFolderPicker() {
        folderPickerLauncher.launch(null)
    }

    private fun mirrorWorkspace(uri: android.net.Uri) {
        showProgress(true, getString(R.string.progress_mirroring))

        lifecycleScope.launch {
            val result = mirror.mirrorToSandbox(uri, object : WorkspaceMirror.ProgressCallback {
                override fun onProgress(current: Int, total: Int, currentFile: String) {
                    runOnUiThread {
                        binding.progressText.text = getString(
                            R.string.progress_copying_files,
                            current,
                            total
                        )
                    }
                }

                override fun onComplete(workspace: WorkspaceInfo) {
                    runOnUiThread {
                        showProgress(false)
                        Toast.makeText(
                            this@WorkspaceManagerActivity,
                            getString(R.string.workspace_added, workspace.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        loadWorkspaces()
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        showProgress(false)
                        Toast.makeText(
                            this@WorkspaceManagerActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
            
            if (result == null) {
                runOnUiThread { showProgress(false) }
            }
        }
    }

    private fun openWorkspace(workspace: WorkspaceInfo) {
        // Ensure server is running
        if (!NodeManager.isRunning()) {
            NodeManager.startNodeService(this)
        }
        
        // Pass workspace ID to the editor
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_WORKSPACE_ID, workspace.id)
        }
        startActivity(intent)
    }

    private fun syncWorkspace(workspace: WorkspaceInfo) {
        showProgress(true, getString(R.string.progress_syncing))

        lifecycleScope.launch {
            val result = mirror.exportToOriginal(workspace, object : WorkspaceMirror.ProgressCallback {
                override fun onProgress(current: Int, total: Int, currentFile: String) {
                    runOnUiThread {
                        binding.progressText.text = getString(
                            R.string.progress_syncing_files,
                            current,
                            total
                        )
                    }
                }

                override fun onComplete(updatedWorkspace: WorkspaceInfo) {
                    runOnUiThread {
                        showProgress(false)
                        Toast.makeText(
                            this@WorkspaceManagerActivity,
                            getString(R.string.sync_complete),
                            Toast.LENGTH_SHORT
                        ).show()
                        loadWorkspaces()
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        showProgress(false)
                        Toast.makeText(
                            this@WorkspaceManagerActivity,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
            
            if (result < 0) {
                runOnUiThread { showProgress(false) }
            }
        }
    }

    private fun confirmDeleteWorkspace(workspace: WorkspaceInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_workspace_title)
            .setMessage(getString(R.string.dialog_delete_workspace_message, workspace.name))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                repository.deleteWorkspaceWithFiles(workspace.id)
                loadWorkspaces()
                Toast.makeText(this, R.string.workspace_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showProgress(show: Boolean, message: String = "") {
        binding.progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressText.text = message
    }

    /**
     * Adapter for workspace list.
     */
    inner class WorkspaceAdapter(
        private val onOpen: (WorkspaceInfo) -> Unit,
        private val onSync: (WorkspaceInfo) -> Unit,
        private val onDelete: (WorkspaceInfo) -> Unit
    ) : RecyclerView.Adapter<WorkspaceAdapter.ViewHolder>() {

        private var workspaces = listOf<WorkspaceInfo>()

        fun submitList(list: List<WorkspaceInfo>) {
            workspaces = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemWorkspaceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(workspaces[position])
        }

        override fun getItemCount() = workspaces.size

        inner class ViewHolder(
            private val itemBinding: ItemWorkspaceBinding
        ) : RecyclerView.ViewHolder(itemBinding.root) {

            private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

            fun bind(workspace: WorkspaceInfo) {
                itemBinding.tvWorkspaceName.text = workspace.name
                itemBinding.tvFileCount.text = getString(
                    R.string.workspace_file_count,
                    workspace.fileCount
                )
                itemBinding.tvLastSynced.text = getString(
                    R.string.workspace_last_synced,
                    dateFormat.format(Date(workspace.lastSyncedAt))
                )

                // Show sync indicator if there are unsynced changes
                itemBinding.ivSyncIndicator.visibility = 
                    if (workspace.hasUnsyncedChanges) View.VISIBLE else View.GONE

                // Click handlers
                itemBinding.root.setOnClickListener { onOpen(workspace) }
                itemBinding.btnSync.setOnClickListener { onSync(workspace) }
                itemBinding.btnDelete.setOnClickListener { onDelete(workspace) }
            }
        }
    }
}
