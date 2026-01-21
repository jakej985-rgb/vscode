package com.codepocket.local.workspace

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Handles mirroring files between SAF (Storage Access Framework) 
 * and the app's sandbox directory.
 * 
 * Flow:
 * 1. User picks folder via SAF → mirror to sandbox
 * 2. User edits files in sandbox via VS Code
 * 3. User clicks "Sync" → export changes back to original
 */
class WorkspaceMirror(private val context: Context) {
    
    companion object {
        private const val TAG = "WorkspaceMirror"
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024L // 50 MB per file limit
    }
    
    private val repository = WorkspaceRepository(context)
    
    /**
     * Progress callback for mirror operations.
     */
    interface ProgressCallback {
        fun onProgress(current: Int, total: Int, currentFile: String)
        fun onComplete(workspace: WorkspaceInfo)
        fun onError(error: String)
    }
    
    /**
     * Mirror a SAF folder to the app sandbox.
     * 
     * @param sourceUri SAF URI of the source folder
     * @param callback Progress callback
     * @return WorkspaceInfo for the new workspace
     */
    suspend fun mirrorToSandbox(
        sourceUri: Uri,
        callback: ProgressCallback? = null
    ): WorkspaceInfo? = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromTreeUri(context, sourceUri)
            if (documentFile == null || !documentFile.isDirectory) {
                callback?.onError("Invalid folder selected")
                return@withContext null
            }
            
            val folderName = documentFile.name ?: "Untitled Workspace"
            val workspaceId = repository.generateWorkspaceId()
            val sandboxDir = repository.getWorkspaceSandboxDir(workspaceId)
            
            Log.i(TAG, "Mirroring '$folderName' to ${sandboxDir.absolutePath}")
            
            // Count files first
            val allFiles = collectFiles(documentFile)
            val totalFiles = allFiles.size
            Log.d(TAG, "Found $totalFiles files to copy")
            
            // Create sandbox directory
            sandboxDir.mkdirs()
            
            // Copy files
            var copiedCount = 0
            for ((docFile, relativePath) in allFiles) {
                val targetFile = File(sandboxDir, relativePath)
                targetFile.parentFile?.mkdirs()
                
                callback?.onProgress(copiedCount + 1, totalFiles, relativePath)
                
                if (docFile.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    copyDocumentToFile(docFile, targetFile)
                }
                
                copiedCount++
            }
            
            // Create workspace info
            val workspace = WorkspaceInfo(
                id = workspaceId,
                name = folderName,
                originalUri = sourceUri.toString(),
                sandboxPath = sandboxDir.absolutePath,
                fileCount = totalFiles, 
                lastSyncedAt = System.currentTimeMillis(),
                lastEditedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            
            // Save to repository
            repository.saveWorkspace(workspace)
            
            Log.i(TAG, "Mirror complete: $copiedCount files copied")
            callback?.onComplete(workspace)
            
            workspace
        } catch (e: Exception) {
            Log.e(TAG, "Mirror failed", e)
            callback?.onError("Mirror failed: ${e.message}")
            null
        }
    }
    
    /**
     * Export changes from sandbox back to the original SAF location.
     * 
     * @param workspace The workspace to sync
     * @param callback Progress callback
     * @return Number of files synced, or -1 on error
     */
    suspend fun exportToOriginal(
        workspace: WorkspaceInfo,
        callback: ProgressCallback? = null
    ): Int = withContext(Dispatchers.IO) {
        try {
            val originalUri = Uri.parse(workspace.originalUri)
            val originalDoc = DocumentFile.fromTreeUri(context, originalUri)
            
            if (originalDoc == null || !originalDoc.isDirectory) {
                callback?.onError("Original folder no longer accessible")
                return@withContext -1
            }
            
            val sandboxDir = File(workspace.sandboxPath)
            if (!sandboxDir.exists()) {
                callback?.onError("Sandbox directory not found")
                return@withContext -1
            }
            
            // Find files modified since last sync
            val changedFiles = findChangedFiles(sandboxDir, workspace.lastSyncedAt)
            val totalFiles = changedFiles.size
            
            Log.i(TAG, "Exporting $totalFiles changed files")
            
            var syncedCount = 0
            for (file in changedFiles) {
                val relativePath = file.relativeTo(sandboxDir).path
                callback?.onProgress(syncedCount + 1, totalFiles, relativePath)
                
                copyFileToDocument(file, originalDoc, relativePath)
                syncedCount++
            }
            
            // Update workspace sync time
            val updatedWorkspace = workspace.markSynced()
            repository.saveWorkspace(updatedWorkspace)
            
            Log.i(TAG, "Export complete: $syncedCount files synced")
            callback?.onComplete(updatedWorkspace)
            
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            callback?.onError("Export failed: ${e.message}")
            -1
        }
    }
    
    /**
     * Refresh workspace by re-importing from original location.
     */
    suspend fun refreshFromOriginal(workspace: WorkspaceInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            val originalUri = Uri.parse(workspace.originalUri)
            val originalDoc = DocumentFile.fromTreeUri(context, originalUri)
            
            if (originalDoc == null || !originalDoc.isDirectory) {
                return@withContext false
            }
            
            val sandboxDir = File(workspace.sandboxPath)
            
            // Clear and re-mirror
            sandboxDir.deleteRecursively()
            sandboxDir.mkdirs()
            
            val allFiles = collectFiles(originalDoc)
            for ((docFile, relativePath) in allFiles) {
                val targetFile = File(sandboxDir, relativePath)
                targetFile.parentFile?.mkdirs()
                
                if (docFile.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    copyDocumentToFile(docFile, targetFile)
                }
            }
            
            // Update metadata
            val updatedWorkspace = workspace.copy(
                fileCount = allFiles.size,
                lastSyncedAt = System.currentTimeMillis()
            )
            repository.saveWorkspace(updatedWorkspace)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
            false
        }
    }
    
    /**
     * Collect all files from a DocumentFile tree.
     * Returns pairs of (DocumentFile, relativePath)
     */
    private fun collectFiles(
        root: DocumentFile,
        basePath: String = ""
    ): List<Pair<DocumentFile, String>> {
        val result = mutableListOf<Pair<DocumentFile, String>>()
        
        val files = root.listFiles()
        for (file in files) {
            val relativePath = if (basePath.isEmpty()) {
                file.name ?: continue
            } else {
                "$basePath/${file.name ?: continue}"
            }
            
            if (file.isDirectory) {
                result.add(file to relativePath)
                result.addAll(collectFiles(file, relativePath))
            } else {
                // Skip very large files
                if ((file.length() ?: 0) <= MAX_FILE_SIZE) {
                    result.add(file to relativePath)
                } else {
                    Log.w(TAG, "Skipping large file: $relativePath (${file.length()} bytes)")
                }
            }
        }
        
        return result
    }
    
    /**
     * Copy a DocumentFile to a regular File.
     */
    private fun copyDocumentToFile(source: DocumentFile, target: File) {
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }
    
    /**
     * Copy a regular File to a DocumentFile location.
     */
    private fun copyFileToDocument(
        source: File,
        targetRoot: DocumentFile,
        relativePath: String
    ) {
        // Navigate/create directory structure
        val pathParts = relativePath.split("/")
        var currentDoc = targetRoot
        
        for (i in 0 until pathParts.size - 1) {
            val dirName = pathParts[i]
            val existing = currentDoc.findFile(dirName)
            currentDoc = if (existing != null && existing.isDirectory) {
                existing
            } else {
                currentDoc.createDirectory(dirName) ?: return
            }
        }
        
        // Create or overwrite file
        val fileName = pathParts.last()
        val mimeType = getMimeType(fileName)
        
        val existingFile = currentDoc.findFile(fileName)
        val targetDoc = if (existingFile != null) {
            existingFile
        } else {
            currentDoc.createFile(mimeType, fileName) ?: return
        }
        
        context.contentResolver.openOutputStream(targetDoc.uri, "wt")?.use { output ->
            source.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }
    
    /**
     * Check if a workspace has unsynced changes by scanning the filesystem.
     * Updates the workspace's lastEditedAt if changes are found.
     */
    suspend fun checkUnsyncedChanges(workspace: WorkspaceInfo): WorkspaceInfo = withContext(Dispatchers.IO) {
        try {
            val sandboxDir = File(workspace.sandboxPath)
            if (!sandboxDir.exists()) return@withContext workspace
            
            val changedFiles = findChangedFiles(sandboxDir, workspace.lastSyncedAt)
            if (changedFiles.isNotEmpty()) {
                // If we found changes, ensure lastEditedAt is newer than lastSyncedAt
                // Using the most recent file modification time
                val mostRecentMod = changedFiles.maxOfOrNull { it.lastModified() } ?: System.currentTimeMillis()
                
                if (mostRecentMod > workspace.lastSyncedAt) {
                    val updated = workspace.copy(lastEditedAt = mostRecentMod)
                    repository.saveWorkspace(updated)
                    return@withContext updated
                }
            }
            workspace
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check changes", e)
            workspace
        }
    }

    /**
     * Find files modified after a given timestamp.
     */
    private fun findChangedFiles(dir: File, sinceTimestamp: Long): List<File> {
        val result = mutableListOf<File>()
        
        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.lastModified() > sinceTimestamp) {
                result.add(file)
            }
        }
        
        return result
    }
    
    /**
     * Get MIME type for a filename.
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "md" -> "text/markdown"
            "py" -> "text/x-python"
            "kt" -> "text/x-kotlin"
            "java" -> "text/x-java"
            "c", "cpp", "h" -> "text/x-c"
            "sh" -> "application/x-sh"
            "yaml", "yml" -> "text/yaml"
            else -> "application/octet-stream"
        }
    }
}
