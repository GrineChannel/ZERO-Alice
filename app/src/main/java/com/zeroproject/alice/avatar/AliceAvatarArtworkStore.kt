package com.zeroproject.alice.avatar

import android.content.Context
import android.net.Uri
import java.io.File

/** Stores the user-selected artwork privately, without broad storage permissions. */
class AliceAvatarArtworkStore(private val context: Context) {
    private val artworkFile: File get() = File(context.filesDir, "avatar/selected-artwork")
    fun existingFile(): File? = artworkFile.takeIf { it.isFile && it.length() > 0L }
    fun importFrom(uri: Uri) {
        val directory = artworkFile.parentFile ?: error("Avatar directory missing")
        if (!directory.exists() && !directory.mkdirs()) error("Cannot create avatar directory")
        val temporary = File(directory, "selected-artwork.tmp")
        try {
            context.contentResolver.openInputStream(uri).use { source ->
                requireNotNull(source) { "Cannot open selected image" }
                temporary.outputStream().use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 15L * 1024L * 1024L) { "Image exceeds 15 MB" }
                        target.write(buffer, 0, count)
                    }
                }
            }
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(temporary.absolutePath, options)
            require(options.outWidth in 1..8192 && options.outHeight in 1..8192) {
                "Unsupported image or image dimensions exceed 8192 pixels"
            }
            require(temporary.renameTo(artworkFile)) { "Cannot save image" }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }
    fun clear(): Boolean = !artworkFile.exists() || artworkFile.delete()
}
