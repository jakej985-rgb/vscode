package com.codepocket.local.workspace

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Repository for storing and retrieving workspace metadata.
 * Uses SharedPreferences with JSON serialization for simplicity.
 */
class WorkspaceRepository(context: Context) {
    
    companion object {
        private const val TAG = "WorkspaceRepository"
        private const val PREFS_NAME = "codepocket_workspaces"
        private const val KEY_WORKSPACES = "workspaces_json"
    }
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val sandboxRoot = File(context.filesDir, "workspaces")
    
    init {
        // Ensure workspace root directory exists
        sandboxRoot.mkdirs()
    }
    
    /**
     * Get all saved workspaces.
     */
    fun getAllWorkspaces(): List<WorkspaceInfo> {
        val json = prefs.getString(KEY_WORKSPACES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WorkspaceInfo>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing workspaces", e)
            emptyList()
        }
    }
    
    /**
     * Get a workspace by ID.
     */
    fun getWorkspace(id: String): WorkspaceInfo? {
        return getAllWorkspaces().find { it.id == id }
    }
    
    /**
     * Get a workspace by original URI.
     */
    fun getWorkspaceByUri(uri: String): WorkspaceInfo? {
        return getAllWorkspaces().find { it.originalUri == uri }
    }
    
    /**
     * Save a workspace (creates new or updates existing).
     */
    fun saveWorkspace(workspace: WorkspaceInfo) {
        val workspaces = getAllWorkspaces().toMutableList()
        val index = workspaces.indexOfFirst { it.id == workspace.id }
        
        if (index >= 0) {
            workspaces[index] = workspace
        } else {
            workspaces.add(workspace)
        }
        
        saveAll(workspaces)
        Log.d(TAG, "Saved workspace: ${workspace.name} (${workspace.id})")
    }
    
    /**
     * Delete a workspace (removes from list but doesn't delete files).
     */
    fun deleteWorkspace(id: String) {
        val workspaces = getAllWorkspaces().filterNot { it.id == id }
        saveAll(workspaces)
        Log.d(TAG, "Deleted workspace: $id")
    }
    
    /**
     * Delete a workspace and its sandbox files.
     */
    fun deleteWorkspaceWithFiles(id: String): Boolean {
        val workspace = getWorkspace(id) ?: return false
        
        // Delete sandbox directory
        val sandboxDir = File(workspace.sandboxPath)
        if (sandboxDir.exists()) {
            sandboxDir.deleteRecursively()
        }
        
        // Remove from list
        deleteWorkspace(id)
        Log.d(TAG, "Deleted workspace with files: ${workspace.name}")
        return true
    }
    
    /**
     * Get the sandbox root directory.
     */
    fun getSandboxRoot(): File = sandboxRoot
    
    /**
     * Generate a new workspace ID.
     */
    fun generateWorkspaceId(): String {
        return "ws_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Get the sandbox directory for a workspace.
     */
    fun getWorkspaceSandboxDir(workspaceId: String): File {
        return File(sandboxRoot, workspaceId)
    }
    
    private fun saveAll(workspaces: List<WorkspaceInfo>) {
        val json = gson.toJson(workspaces)
        prefs.edit().putString(KEY_WORKSPACES, json).apply()
    }
}
