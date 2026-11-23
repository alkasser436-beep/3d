package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.PhotorealisticRenderRepository
import com.example.data.DesignRepository
import com.example.data.DesignTemplates
import com.example.data.RoomTemplate
import com.example.model.CameraPreset
import com.example.model.FurnitureItem
import com.example.model.FurnitureKind
import com.example.model.LengthUnit
import com.example.model.LightingPreset
import com.example.model.MaterialFinish
import com.example.model.OpeningType
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.example.model.SavedDesignSummary
import com.example.model.SelectionTarget
import com.example.model.SurfaceMaterial
import com.example.model.WallId
import com.example.model.WallOpening
import com.google.firebase.ai.type.QuotaExceededException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "DesignerViewModel"

enum class StudioTab(val label: String) {
  ROOM("Room"),
  FURNITURE("Furniture"),
  MATERIALS("Materials"),
  RENDER("Render"),
}

sealed interface AiRenderState {
  data object Idle : AiRenderState
  data class Loading(val sourceBitmap: Bitmap?) : AiRenderState
  data class Success(
    val renderedBitmap: Bitmap,
    val sourceBitmap: Bitmap,
    val pngBytes: ByteArray,
  ) : AiRenderState
  data class Error(
    val message: String,
    val sourceBitmap: Bitmap?,
  ) : AiRenderState
}

data class DesignerUiState(
  val design: RoomDesign = DesignTemplates.defaultForRoomType(RoomType.KITCHEN),
  val selectedTarget: SelectionTarget = SelectionTarget.Walls,
  val activeTab: StudioTab = StudioTab.ROOM,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val savedDesigns: List<SavedDesignSummary> = emptyList(),
  val activeCameraPreset: CameraPreset = CameraPreset.PERSPECTIVE_ORBIT,
  val snapshotPreviewPng: ByteArray? = null,
  val isCapturingSnapshot: Boolean = false,
  val aiRenderState: AiRenderState = AiRenderState.Idle,
  val showProjectsDialog: Boolean = false,
  val isFullscreenViewport: Boolean = false,
)

class DesignerViewModel(application: Application) : AndroidViewModel(application) {

  private enum class SnapshotPurpose { EXPORT_PNG, AI_RENDER }

  private val repository = DesignRepository(application)
  private val aiRenderRepository = PhotorealisticRenderRepository()

  private val _uiState = MutableStateFlow(DesignerUiState())
  val uiState: StateFlow<DesignerUiState> = _uiState.asStateFlow()

  private val _cameraCommands = MutableSharedFlow<CameraPreset>(extraBufferCapacity = 4)
  val cameraCommands: SharedFlow<CameraPreset> = _cameraCommands.asSharedFlow()

  private val _snapshotRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
  val snapshotRequests: SharedFlow<Unit> = _snapshotRequests.asSharedFlow()

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val messages: SharedFlow<String> = _messages.asSharedFlow()

  private val undoStack = ArrayDeque<RoomDesign>()
  private val redoStack = ArrayDeque<RoomDesign>()
  private var lastCoalesceKey: String? = null
  private var lastCoalesceTimeMs: Long = 0L
  private var autoSaveJob: Job? = null
  private var aiRenderJob: Job? = null
  private var pendingSnapshotPurpose: SnapshotPurpose = SnapshotPurpose.EXPORT_PNG

  init {
    viewModelScope.launch {
      val autoSaved = repository.loadAutoSave()
      val savedList = repository.listSavedDesigns()
      _uiState.update { state ->
        state.copy(
          design = autoSaved ?: state.design,
          savedDesigns = savedList,
        )
      }
    }
  }

  private fun commitDesignChange(
    coalesceKey: String? = null,
    transform: (RoomDesign) -> RoomDesign,
  ) {
    val current = _uiState.value.design
    val updated = transform(current)
    if (updated == current) return

    val now = System.currentTimeMillis()
    val shouldCoalesce = coalesceKey != null &&
      coalesceKey == lastCoalesceKey &&
      (now - lastCoalesceTimeMs) < 900L

    if (!shouldCoalesce) {
      if (undoStack.size >= 40) undoStack.removeFirst()
      undoStack.addLast(current)
      redoStack.clear()
    }
    lastCoalesceKey = coalesceKey
    lastCoalesceTimeMs = now

    _uiState.update {
      it.copy(
        design = updated,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
      )
    }
    scheduleAutoSave(updated)
  }

  private fun scheduleAutoSave(design: RoomDesign) {
    autoSaveJob?.cancel()
    autoSaveJob = viewModelScope.launch {
      delay(400L)
      repository.saveAutoSave(design)
    }
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val prev = undoStack.removeLast()
    redoStack.addLast(_uiState.value.design)
    lastCoalesceKey = null
    _uiState.update {
      it.copy(
        design = prev,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
      )
    }
    scheduleAutoSave(prev)
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val next = redoStack.removeLast()
    undoStack.addLast(_uiState.value.design)
    lastCoalesceKey = null
    _uiState.update {
      it.copy(
        design = next,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
      )
    }
    scheduleAutoSave(next)
  }

  fun setActiveTab(tab: StudioTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun toggleFullscreenViewport() {
    _uiState.update { it.copy(isFullscreenViewport = !it.isFullscreenViewport) }
  }

  fun setShowProjectsDialog(show: Boolean) {
    _uiState.update { it.copy(showProjectsDialog = show) }
  }

  fun setLengthUnit(unit: LengthUnit) {
    _uiState.update { it.copy(design = it.design.copy(unit = unit)) }
    scheduleAutoSave(_uiState.value.design)
  }

  fun setRoomType(roomType: RoomType) {
    commitDesignChange { it.copy(roomType = roomType) }
  }

  fun setRoomName(name: String) {
    commitDesignChange("room_name") { it.copy(name = name.take(40)) }
  }

  fun updateRoomDimensions(width: Float? = null, length: Float? = null, height: Float? = null) {
    commitDesignChange("room_dims") { d ->
      val newW = (width ?: d.widthMeters).coerceIn(2.0f, 12.0f)
      val newL = (length ?: d.lengthMeters).coerceIn(2.0f, 12.0f)
      val newH = (height ?: d.wallHeightMeters).coerceIn(2.2f, 4.5f)
      d.copy(
        widthMeters = newW,
        lengthMeters = newL,
        wallHeightMeters = newH,
        furniture = d.furniture.map { item ->
          item.copy(
            xMeters = item.xMeters.coerceIn(-newW * 0.5f + 0.15f, newW * 0.5f - 0.15f),
            zMeters = item.zMeters.coerceIn(-newL * 0.5f + 0.15f, newL * 0.5f - 0.15f),
          )
        },
      )
    }
  }

  fun applyTemplate(template: RoomTemplate) {
    commitDesignChange {
      template.builder().copy(
        id = "design_${UUID.randomUUID().toString().take(8)}",
        unit = it.unit,
        realisticView = it.realisticView,
        lightingPreset = it.lightingPreset,
      )
    }
    _uiState.update { it.copy(selectedTarget = SelectionTarget.Walls) }
    _cameraCommands.tryEmit(CameraPreset.PERSPECTIVE_ORBIT)
    _messages.tryEmit("Loaded ${template.title}")
  }

  fun addOpening(type: OpeningType, wall: WallId) {
    commitDesignChange { d ->
      val op = WallOpening(
        id = "op_${UUID.randomUUID().toString().take(6)}",
        type = type,
        wall = wall,
        offsetMeters = 0f,
        widthMeters = if (type == OpeningType.DOOR) 0.90f else 1.45f,
        heightMeters = if (type == OpeningType.DOOR) 2.10f else 1.25f,
        sillHeightMeters = if (type == OpeningType.DOOR) 0f else 0.90f,
      )
      d.copy(openings = d.openings + op)
    }
  }

  fun updateOpening(id: String, wall: WallId? = null, offsetMeters: Float? = null, widthMeters: Float? = null) {
    commitDesignChange("opening_$id") { d ->
      d.copy(
        openings = d.openings.map { op ->
          if (op.id != id) op
          else {
            val w = (widthMeters ?: op.widthMeters).coerceIn(0.55f, 2.8f)
            val maxHalf = ((wall ?: op.wall).let {
              if (it == WallId.NORTH || it == WallId.SOUTH) d.widthMeters else d.lengthMeters
            } * 0.5f - w * 0.5f - 0.12f).coerceAtLeast(0f)
            op.copy(
              wall = wall ?: op.wall,
              widthMeters = w,
              offsetMeters = (offsetMeters ?: op.offsetMeters).coerceIn(-maxHalf, maxHalf),
            )
          }
        },
      )
    }
  }

  fun removeOpening(id: String) {
    commitDesignChange { d ->
      d.copy(openings = d.openings.filterNot { it.id == id })
    }
  }

  fun addFurniture(kind: FurnitureKind) {
    val item = DesignTemplates.createFurnitureItem(kind, _uiState.value.design.furniture.size)
    commitDesignChange { d ->
      d.copy(furniture = d.furniture + item)
    }
    _uiState.update { it.copy(selectedTarget = SelectionTarget.Furniture(item.id)) }
  }

  fun updateFurniture(
    id: String,
    coalesceKey: String? = null,
    transform: (FurnitureItem) -> FurnitureItem,
  ) {
    commitDesignChange(coalesceKey) { d ->
      val halfW = d.widthMeters * 0.5f
      val halfL = d.lengthMeters * 0.5f
      d.copy(
        furniture = d.furniture.map { item ->
          if (item.id != id) item
          else {
            val next = transform(item)
            next.copy(
              widthMeters = next.widthMeters.coerceIn(0.25f, d.widthMeters),
              depthMeters = next.depthMeters.coerceIn(0.25f, d.lengthMeters),
              heightMeters = next.heightMeters.coerceIn(0.02f, d.wallHeightMeters),
              xMeters = next.xMeters.coerceIn(-halfW + 0.1f, halfW - 0.1f),
              zMeters = next.zMeters.coerceIn(-halfL + 0.1f, halfL - 0.1f),
              yMeters = next.yMeters.coerceIn(0f, (d.wallHeightMeters - 0.3f).coerceAtLeast(0f)),
              rotationDeg = ((next.rotationDeg % 360f) + 360f) % 360f,
            )
          }
        },
      )
    }
  }

  fun duplicateFurniture(id: String) {
    val existing = _uiState.value.design.furniture.firstOrNull { it.id == id } ?: return
    val copy = existing.copy(
      id = "item_${UUID.randomUUID().toString().take(8)}",
      label = "${existing.label} Copy",
      xMeters = (existing.xMeters + 0.35f).coerceAtMost(_uiState.value.design.widthMeters * 0.45f),
      zMeters = (existing.zMeters + 0.35f).coerceAtMost(_uiState.value.design.lengthMeters * 0.45f),
    )
    commitDesignChange { d -> d.copy(furniture = d.furniture + copy) }
    _uiState.update { it.copy(selectedTarget = SelectionTarget.Furniture(copy.id)) }
  }

  fun deleteFurniture(id: String) {
    commitDesignChange { d -> d.copy(furniture = d.furniture.filterNot { it.id == id }) }
    _uiState.update {
      if (it.selectedTarget == SelectionTarget.Furniture(id)) {
        it.copy(selectedTarget = SelectionTarget.Walls)
      } else it
    }
  }

  fun selectTarget(target: SelectionTarget) {
    _uiState.update { it.copy(selectedTarget = target) }
  }

  fun onElementSelectedFrom3D(type: String, id: String) {
    val target = when (type) {
      "WALLS" -> SelectionTarget.Walls
      "FLOOR" -> SelectionTarget.Floor
      "CEILING" -> SelectionTarget.Ceiling
      "COUNTERTOPS" -> SelectionTarget.AllCountertops
      "FURNITURE" -> SelectionTarget.Furniture(id)
      else -> return
    }
    _uiState.update { state ->
      val nextTab = if (target is SelectionTarget.Furniture && state.activeTab == StudioTab.ROOM) {
        StudioTab.FURNITURE
      } else state.activeTab
      state.copy(selectedTarget = target, activeTab = nextTab)
    }
  }

  fun applyMaterialToTarget(
    material: SurfaceMaterial,
    applyAsCountertopAccent: Boolean = false,
  ) {
    val target = _uiState.value.selectedTarget
    commitDesignChange("mat_${target}_${applyAsCountertopAccent}") { d ->
      when (target) {
        SelectionTarget.Walls -> d.copy(wallMaterial = material)
        SelectionTarget.Floor -> d.copy(floorMaterial = material)
        SelectionTarget.Ceiling -> d.copy(ceilingMaterial = material)
        SelectionTarget.AllCountertops -> d.copy(
          countertopMaterial = material,
          furniture = d.furniture.map { item ->
            if (item.secondaryMaterial != null) item.copy(secondaryMaterial = material) else item
          },
        )
        is SelectionTarget.Furniture -> d.copy(
          furniture = d.furniture.map { item ->
            if (item.id != target.id) item
            else if (applyAsCountertopAccent) item.copy(secondaryMaterial = material)
            else item.copy(material = material)
          },
        )
      }
    }
  }

  fun updateTargetFinishOnly(finish: MaterialFinish) {
    val currentMat = currentMaterialForTarget()
    applyMaterialToTarget(currentMat.copy(finish = finish))
  }

  fun updateTargetHexOnly(hex: String) {
    val normalized = normalizeHex(hex) ?: return
    val currentMat = currentMaterialForTarget()
    applyMaterialToTarget(currentMat.copy(hexColor = normalized))
  }

  fun currentMaterialForTarget(): SurfaceMaterial {
    val d = _uiState.value.design
    return when (val t = _uiState.value.selectedTarget) {
      SelectionTarget.Walls -> d.wallMaterial
      SelectionTarget.Floor -> d.floorMaterial
      SelectionTarget.Ceiling -> d.ceilingMaterial
      SelectionTarget.AllCountertops -> d.countertopMaterial
      is SelectionTarget.Furniture ->
        d.furniture.firstOrNull { it.id == t.id }?.material ?: d.wallMaterial
    }
  }

  fun toggleRealisticView() {
    commitDesignChange { it.copy(realisticView = !it.realisticView) }
  }

  fun convertToPhotorealistic() {
    commitDesignChange { it.copy(realisticView = true) }
    _uiState.update { it.copy(activeCameraPreset = CameraPreset.CORNER_EYE_LEVEL) }
    _cameraCommands.tryEmit(CameraPreset.CORNER_EYE_LEVEL)
    _messages.tryEmit("3D Realistic view active")
  }

  fun setLightingPreset(preset: LightingPreset) {
    commitDesignChange { it.copy(lightingPreset = preset, realisticView = true) }
  }

  fun setLightingParams(sunAzimuth: Float? = null, exposure: Float? = null) {
    commitDesignChange("lighting_params") { d ->
      d.copy(
        sunAzimuthDeg = sunAzimuth ?: d.sunAzimuthDeg,
        exposure = (exposure ?: d.exposure).coerceIn(0.6f, 1.8f),
      )
    }
  }

  fun setCameraPreset(preset: CameraPreset) {
    _uiState.update { it.copy(activeCameraPreset = preset) }
    _cameraCommands.tryEmit(preset)
  }

  fun requestSnapshot() {
    pendingSnapshotPurpose = SnapshotPurpose.EXPORT_PNG
    _uiState.update { it.copy(isCapturingSnapshot = true) }
    _snapshotRequests.tryEmit(Unit)
  }

  fun requestAiPhotorealisticRender() {
    if (_uiState.value.aiRenderState is AiRenderState.Loading) return
    aiRenderJob?.cancel()
    pendingSnapshotPurpose = SnapshotPurpose.AI_RENDER
    val turnedOnRealistic = !_uiState.value.design.realisticView
    if (turnedOnRealistic) {
      commitDesignChange { it.copy(realisticView = true) }
    }
    _uiState.update { it.copy(aiRenderState = AiRenderState.Loading(null)) }
    viewModelScope.launch {
      if (turnedOnRealistic) delay(160L)
      _snapshotRequests.tryEmit(Unit)
    }
  }

  fun onSnapshotCaptured(dataUrl: String) {
    viewModelScope.launch {
      val base64 = dataUrl.substringAfter("base64,", "")
      val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
      if (pendingSnapshotPurpose == SnapshotPurpose.AI_RENDER) {
        pendingSnapshotPurpose = SnapshotPurpose.EXPORT_PNG
        if (bytes == null) {
          _uiState.update {
            it.copy(
              aiRenderState = AiRenderState.Error("Failed to capture 3D viewport image.", null),
            )
          }
          return@launch
        }
        val sourceBmp = runCatching {
          aiRenderRepository.decodeAndPrepareInputBitmap(bytes)
        }.getOrElse { err ->
          Log.e(TAG, "Failed to decode viewport bitmap", err)
          _uiState.update {
            it.copy(
              aiRenderState = AiRenderState.Error(err.localizedMessage ?: "Snapshot decode failed.", null),
            )
          }
          return@launch
        }
        runAiPhotorealisticSynthesis(sourceBmp)
      } else {
        _uiState.update { it.copy(isCapturingSnapshot = false, snapshotPreviewPng = bytes) }
      }
    }
  }

  private fun runAiPhotorealisticSynthesis(sourceBmp: Bitmap) {
    aiRenderJob?.cancel()
    aiRenderJob = viewModelScope.launch {
      _uiState.update { it.copy(aiRenderState = AiRenderState.Loading(sourceBmp)) }
      try {
        val resultBmp = aiRenderRepository.renderPhotorealisticPhoto(sourceBmp, _uiState.value.design)
        val pngBytes = aiRenderRepository.bitmapToPngBytes(resultBmp)
        _uiState.update {
          it.copy(aiRenderState = AiRenderState.Success(resultBmp, sourceBmp, pngBytes))
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI photorealistic render timed out", e)
        _uiState.update {
          it.copy(
            aiRenderState = AiRenderState.Error(
              "AI render timed out after 60 seconds. Please try again.",
              sourceBmp,
            ),
          )
        }
      } catch (e: QuotaExceededException) {
        Log.e(TAG, "AI render quota exceeded", e)
        _uiState.update {
          it.copy(
            aiRenderState = AiRenderState.Error(
              e.localizedMessage ?: "Rate limit or billing quota exceeded for Gemini image generation.",
              sourceBmp,
            ),
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI photorealistic render failed", e)
        _uiState.update {
          it.copy(
            aiRenderState = AiRenderState.Error(
              e.localizedMessage ?: "AI photorealistic render failed.",
              sourceBmp,
            ),
          )
        }
      }
    }
  }

  fun retryAiRender() {
    val source = when (val s = _uiState.value.aiRenderState) {
      is AiRenderState.Success -> s.sourceBitmap
      is AiRenderState.Error -> s.sourceBitmap
      is AiRenderState.Loading -> s.sourceBitmap
      AiRenderState.Idle -> null
    }
    if (source != null) {
      runAiPhotorealisticSynthesis(source)
    } else {
      requestAiPhotorealisticRender()
    }
  }

  fun cancelOrDismissAiRender() {
    aiRenderJob?.cancel()
    aiRenderJob = null
    _uiState.update { it.copy(aiRenderState = AiRenderState.Idle) }
  }

  fun saveAiRenderToPhotos(onNeedSafFallback: (ByteArray) -> Unit) {
    val bytes = (_uiState.value.aiRenderState as? AiRenderState.Success)?.pngBytes ?: return
    val name = "${_uiState.value.design.name}_AI"
    viewModelScope.launch {
      val saved = repository.savePngToPhotos(bytes, name)
      if (saved) {
        _messages.tryEmit("AI Photo saved to Pictures/RoomCraft3D")
      } else {
        onNeedSafFallback(bytes)
      }
    }
  }

  suspend fun createShareUriForAiRender(): Uri? {
    val bytes = (_uiState.value.aiRenderState as? AiRenderState.Success)?.pngBytes ?: return null
    return repository.createShareableSnapshotUri(bytes)
  }

  fun dismissSnapshotPreview() {
    _uiState.update { it.copy(snapshotPreviewPng = null) }
  }

  fun saveSnapshotToPhotos(onNeedSafFallback: (ByteArray) -> Unit) {
    val bytes = _uiState.value.snapshotPreviewPng ?: return
    val name = _uiState.value.design.name
    viewModelScope.launch {
      val saved = repository.savePngToPhotos(bytes, name)
      if (saved) {
        _messages.tryEmit("Snapshot saved to Pictures/RoomCraft3D")
      } else {
        onNeedSafFallback(bytes)
      }
    }
  }

  suspend fun createShareUriForSnapshot(): Uri? {
    val bytes = _uiState.value.snapshotPreviewPng ?: return null
    return repository.createShareableSnapshotUri(bytes)
  }

  fun saveCurrentDesignSlot(customName: String) {
    viewModelScope.launch {
      val cleanName = customName.trim().ifEmpty { _uiState.value.design.name }
      val toSave = _uiState.value.design.copy(
        id = _uiState.value.design.id.ifEmpty { "design_${UUID.randomUUID().toString().take(8)}" },
        name = cleanName,
      )
      repository.saveDesignSlot(toSave)
      repository.saveAutoSave(toSave)
      val list = repository.listSavedDesigns()
      _uiState.update { it.copy(design = toSave, savedDesigns = list) }
      _messages.tryEmit("Saved \"$cleanName\"")
    }
  }

  fun saveAsNewDesignSlot(customName: String) {
    viewModelScope.launch {
      val cleanName = customName.trim().ifEmpty { "${_uiState.value.design.name} Copy" }
      val toSave = _uiState.value.design.copy(
        id = "design_${UUID.randomUUID().toString().take(8)}",
        name = cleanName,
      )
      repository.saveDesignSlot(toSave)
      repository.saveAutoSave(toSave)
      val list = repository.listSavedDesigns()
      _uiState.update { it.copy(design = toSave, savedDesigns = list) }
      _messages.tryEmit("Saved new project \"$cleanName\"")
    }
  }

  fun loadDesignSlot(id: String) {
    viewModelScope.launch {
      val loaded = repository.loadDesignSlot(id)
      if (loaded != null) {
        commitDesignChange { loaded }
        _uiState.update {
          it.copy(
            selectedTarget = SelectionTarget.Walls,
            showProjectsDialog = false,
          )
        }
        _cameraCommands.tryEmit(CameraPreset.PERSPECTIVE_ORBIT)
        _messages.tryEmit("Loaded \"${loaded.name}\"")
      }
    }
  }

  fun deleteDesignSlot(id: String) {
    viewModelScope.launch {
      repository.deleteDesignSlot(id)
      val list = repository.listSavedDesigns()
      _uiState.update { it.copy(savedDesigns = list) }
    }
  }

  fun exportDesignToUri(uri: Uri) {
    viewModelScope.launch {
      val ok = repository.exportDesignToUri(uri, _uiState.value.design)
      _messages.tryEmit(if (ok) "Design JSON exported" else "Failed to export JSON")
    }
  }

  fun importDesignFromUri(uri: Uri) {
    viewModelScope.launch {
      val imported = repository.importDesignFromUri(uri)
      if (imported != null) {
        commitDesignChange { imported }
        _uiState.update {
          it.copy(
            selectedTarget = SelectionTarget.Walls,
            showProjectsDialog = false,
          )
        }
        _cameraCommands.tryEmit(CameraPreset.PERSPECTIVE_ORBIT)
        _messages.tryEmit("Imported \"${imported.name}\"")
      } else {
        _messages.tryEmit("Invalid RoomCraft JSON file")
      }
    }
  }

  private fun normalizeHex(input: String): String? {
    val clean = input.trim().removePrefix("#").uppercase()
    if (!clean.matches(Regex("[0-9A-F]{6}"))) return null
    return "#$clean"
  }
}
