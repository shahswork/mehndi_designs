package com.sashtech.mehndidesignsimple.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageUtils {

    private val httpClient by lazy { OkHttpClient() }

    suspend fun saveImageToGallery(
        context: Context,
        imageUrl: String,
        title: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(imageUrl).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val inputStream = response.body?.byteStream() ?: return@withContext false
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return@withContext false

            val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val filename = "Mehndi_${cleanTitle}_${System.currentTimeMillis()}.jpg"

            var fos: OutputStream? = null
            var success = false

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MehndiStudio")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }

                    val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (imageUri != null) {
                        fos = resolver.openOutputStream(imageUri)
                        if (fos != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                            success = true
                        }
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val appDir = File(imagesDir, "MehndiStudio")
                    if (!appDir.exists()) {
                        appDir.mkdirs()
                    }
                    val image = File(appDir, filename)
                    fos = FileOutputStream(image)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    success = true
                }
            } finally {
                bitmap.recycle()
            }

            fos?.flush()
            fos?.close()

            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Saved to Gallery (Pictures/MehndiStudio)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
            return@withContext success
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving: ${e.localizedMessage ?: "Unknown"}", Toast.LENGTH_SHORT).show()
            }
            return@withContext false
        }
    }

    fun shareDesign(context: Context, title: String, imageUrl: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mehndi Design: $title")
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out this gorgeous Mehndi design: '$title'\n\nView design: $imageUrl\n\nShared via Mehndi Design App"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Mehndi Design"))
    }
}
