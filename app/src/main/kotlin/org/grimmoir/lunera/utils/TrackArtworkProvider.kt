package org.grimmoir.lunera.utils
import android.content.*
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
class TrackArtworkProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    @Synchronized override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw java.io.FileNotFoundException()
        val ctx = requireNotNull(context)
        val source = Uri.parse(uri.lastPathSegment ?: throw java.io.FileNotFoundException())
        if (source.scheme != "content" || source.authority != "media") throw java.io.FileNotFoundException()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        val directory = File(ctx.cacheDir, "track-art").apply { mkdirs() }
        val file = File(directory, "$hash.png")
        if (!file.exists()) {
            val retriever = MediaMetadataRetriever()
            val art = try { retriever.setDataSource(ctx, source); retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } } catch (_: Exception) { null } finally { retriever.release() }
            val bitmap = art ?: Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
                eraseColor(0xFF303039.toInt())
                val title = ctx.contentResolver.query(source, arrayOf(android.provider.MediaStore.Audio.Media.TITLE), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }.orEmpty()
                val initial = title.trim().takeIf { it.isNotEmpty() }?.let { String(Character.toChars(it.codePointAt(0))).uppercase(java.util.Locale.getDefault()) } ?: "♪"
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFE5DFFF.toInt(); textSize = 230f; textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                }
                android.graphics.Canvas(this).drawText(initial, 256f, 256f - (paint.ascent() + paint.descent()) / 2, paint)
            }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
