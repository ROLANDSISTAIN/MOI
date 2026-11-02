package com.roland.moi.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Les photos sont copiées dans l'espace privé de MOI (invisibles dans la galerie). */
object Photos {
    fun dir(c: Context) = File(c.filesDir, "photos").apply { mkdirs() }

    fun file(c: Context, name: String) = File(dir(c), name)

    /** Copie et réduit l'image choisie. Renvoie le nom du fichier, ou "" en cas d'échec. */
    fun import(c: Context, uri: Uri): String = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2000) sample *= 2
        val bmp = c.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return ""
        val scale = 1600f / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val name = "p_" + System.currentTimeMillis() + ".jpg"
        file(c, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }.getOrDefault("")

    fun load(c: Context, name: String, maxSize: Int = 1200): Bitmap? {
        if (name.isBlank()) return null
        val f = file(c, name)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSize * 2) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun delete(c: Context, name: String) {
        if (name.isNotBlank()) file(c, name).delete()
    }
}
