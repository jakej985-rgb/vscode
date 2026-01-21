package com.codepocket.local.workspace

import android.net.Uri

/**
 * Represents a workspace that has been mirrored to the app sandbox.
 * 
 * @property id Unique identifier for this workspace
 * @property name Display name (usually folder name)
 * @property originalUri SAF URI of the original folder
 * @property sandboxPath Path to the mirrored copy in app sandbox
 * @property fileCount Number of files in the workspace
 * @property lastSyncedAt Timestamp of last sync with original (millis)
 * @property lastEditedAt Timestamp of last edit in sandbox (millis)
 * @property createdAt Timestamp when workspace was created (millis)
 */
data class WorkspaceInfo(
    val id: String,
    val name: String,
    val originalUri: String,
    val sandboxPath: String,
    val fileCount: Int = 0,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val lastEditedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if there are unsaved changes (edited after last sync).
     */
    val hasUnsyncedChanges: Boolean
        get() = lastEditedAt > lastSyncedAt
    
    /**
     * Get the original URI as a Uri object.
     */
    fun getOriginalUri(): Uri = Uri.parse(originalUri)
    
    /**
     * Returns a copy with updated sync time.
     */
    fun markSynced(): WorkspaceInfo = copy(
        lastSyncedAt = System.currentTimeMillis()
    )
    
    /**
     * Returns a copy with updated edit time.
     */
    fun markEdited(): WorkspaceInfo = copy(
        lastEditedAt = System.currentTimeMillis()
    )
}
