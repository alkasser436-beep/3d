package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AppLanguage
import com.example.model.RoomDesign
import com.example.model.SavedDesignSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesignRepository(private val context: Context) {

  private val designsDir: File by lazy {
    File(context.filesDir, "designs").apply { mkdirs() }
  }

  private val autoSaveFile: File by lazy {
    File(context.filesDir, "autosave_design.json")
  }

  private val prefs by lazy {
    context.getSharedPreferences("roomcraft_prefs", Context.MODE_PRIVATE)
  }

  fun loadAppLanguage(): AppLanguage {
    val saved = prefs.getString("app_language", AppLanguage.ENGLISH.name)
    return AppLanguage.entries.firstOrNull { it.name == saved } ?: AppLanguage.ENGLISH
  }

  fun saveAppLanguage(language: AppLanguage) {
    prefs.edit().putString("app_language", language.name).apply()
  }

  suspend fun loadAutoSave(): RoomDesign? = withContext(Dispatchers.IO) {
    if (!autoSaveFile.exists()) return@withContext null
    runCatching { DesignSerializer.designFromJsonString(autoSaveFile.readText()) }.getOrNull()
  }

  suspend fun saveAutoSave(design: RoomDesign) = withContext(Dispatchers.IO) {
    runCatching {
      autoSaveFile.writeText(DesignSerializer.designToJsonString(design))
    }
  }

  suspend fun listSavedDesigns(): List<SavedDesignSummary> = withContext(Dispatchers.IO) {
    designsDir.listFiles { f -> f.extension == "json" }
      ?.mapNotNull { file ->
        val d = DesignSerializer.designFromJsonString(file.readText()) ?: return@mapNotNull null
        SavedDesignSummary(
          id = d.id,
          name = d.name,
          roomType = d.roomType,
          dimensionsSummary = String.format(
            Locale.US,
            "%.1fm × %.1fm × %.1fm",
            d.widthMeters,
            d.lengthMeters,
            d.wallHeightMeters,
          ),
          updatedAtMillis = file.lastModified(),
        )
      }
      ?.sortedByDescending { it.updatedAtMillis }
      ?: emptyList()
  }

  suspend fun saveDesignSlot(design: RoomDesign) = withContext(Dispatchers.IO) {
    val safeId = design.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val target = File(designsDir, "$safeId.json")
    target.writeText(DesignSerializer.designToJsonString(design))
  }

  suspend fun loadDesignSlot(id: String): RoomDesign? = withContext(Dispatchers.IO) {
    val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val target = File(designsDir, "$safeId.json")
    if (!target.exists()) return@withContext null
    DesignSerializer.designFromJsonString(target.readText())
  }

  suspend fun deleteDesignSlot(id: String) = withContext(Dispatchers.IO) {
    val safeId = id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    File(designsDir, "$safeId.json").delete()
  }

  suspend fun exportDesignToUri(uri: Uri, design: RoomDesign): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(DesignSerializer.designToJsonString(design).toByteArray(Charsets.UTF_8))
      } ?: error("Cannot open output stream")
      true
    }.getOrDefault(false)
  }

  suspend fun importDesignFromUri(uri: Uri): RoomDesign? = withContext(Dispatchers.IO) {
    runCatching {
      val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: return@runCatching null
      DesignSerializer.designFromJsonString(text)
    }.getOrNull()
  }

  suspend fun savePngToPhotos(pngBytes: ByteArray, title: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val fileName = "RoomCraft_${title.replace(Regex("[^a-zA-Z0-9]"), "_")}_$timestamp.png"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RoomCraft3D")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
          ?: error("Failed to insert MediaStore entry")
        resolver.openOutputStream(uri)?.use { it.write(pngBytes) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
      } else {
        // On API < 29, fall back to app cache and return false so UI can trigger CreateDocument SAF picker
        false
      }
    }.getOrDefault(false)
  }

  suspend fun createShareableSnapshotUri(pngBytes: ByteArray): Uri? = withContext(Dispatchers.IO) {
    runCatching {
      val dir = File(context.cacheDir, "snapshots").apply { mkdirs() }
      val file = File(dir, "roomcraft_snapshot_${System.currentTimeMillis()}.png")
      file.writeBytes(pngBytes)
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
  }
}
