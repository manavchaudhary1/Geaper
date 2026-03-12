package com.manav.geaper.data.prefs

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.net.toUri

/**
 * Converts a SAF tree URI (content://...) to a real filesystem path string.
 *
 * Android 10+ blocks direct writes to arbitrary external paths via File API
 * unless MANAGE_EXTERNAL_STORAGE is granted. This function just resolves the
 * display path; actual write permission is handled via that permission.
 */
fun safUriToPath(context: Context, uriString: String): String {
    if (uriString.isBlank()) return defaultSavePath(context)
    if (uriString.startsWith("/")) return uriString

    return try {
        val uri = uriString.toUri()
        resolveDocumentUri(uri) ?: defaultSavePath(context)
    } catch (e: Exception) {
        defaultSavePath(context)
    }
}

/** Writable fallback that never requires special permissions. */
fun defaultSavePath(context: Context): String =
    (context.getExternalFilesDir(null) ?: context.filesDir).absolutePath

private fun resolveDocumentUri(uri: Uri): String? {
    val docUri = try {
        DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
    } catch (e: Exception) { return null }

    val docId = DocumentsContract.getDocumentId(docUri)

    return when {
        docId.startsWith("primary:") -> {
            val rel = docId.removePrefix("primary:")
            val base = Environment.getExternalStorageDirectory().absolutePath
            if (rel.isBlank()) base else "$base/$rel"
        }
        docId.contains(":") -> {
            // Secondary / SD card volume — resolve via /storage/<uuid>
            val uuid = docId.substringBefore(":")
            val rel  = docId.substringAfter(":")
            val root = "/storage/$uuid"
            if (rel.isBlank()) root else "$root/$rel"
        }
        else -> null
    }
}