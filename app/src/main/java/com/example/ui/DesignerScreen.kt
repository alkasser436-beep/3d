package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CameraPreset
import com.example.model.SelectionTarget
import com.example.ui.components.AiRenderResultDialog
import com.example.ui.components.FurniturePanel
import com.example.ui.components.MaterialsPanel
import com.example.ui.components.ProjectsDialog
import com.example.ui.components.RoomPanel
import com.example.ui.components.SnapshotPreviewDialog
import com.example.ui.components.StudioExportPanel
import com.example.ui.components.Viewport3D
import com.example.ui.components.formatDimension
import com.example.viewmodel.AiRenderState
import com.example.viewmodel.DesignerUiState
import com.example.viewmodel.DesignerViewModel
import com.example.viewmodel.StudioTab
import kotlinx.coroutines.launch

@Composable
fun DesignerScreen(viewModel: DesignerViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingPngFallbackBytes by remember { mutableStateOf<ByteArray?>(null) }

  LaunchedEffect(Unit) {
    viewModel.messages.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  val exportJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) viewModel.exportDesignToUri(uri)
  }

  val importJsonLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) viewModel.importDesignFromUri(uri)
  }

  val savePngFallbackLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("image/png"),
  ) { uri ->
    val bytes = pendingPngFallbackBytes
    pendingPngFallbackBytes = null
    if (uri != null && bytes != null) {
      scope.launch {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        snackbarHostState.showSnackbar("PNG snapshot saved")
      }
    }
  }

  if (state.isFullscreenViewport) {
    BackHandler { viewModel.toggleFullscreenViewport() }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      StudioTopBar(
        state = state,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onToggleRealistic = viewModel::toggleRealisticView,
        onCaptureSnapshot = viewModel::requestSnapshot,
        onOpenProjects = { viewModel.setShowProjectsDialog(true) },
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
    ) {
      val isWide = maxWidth >= 680.dp

      if (isWide) {
        Row(modifier = Modifier.fillMaxSize()) {
          AnimatedVisibility(visible = !state.isFullscreenViewport) {
            Row(modifier = Modifier.fillMaxHeight()) {
              StudioControlPanel(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                  .width(340.dp)
                  .fillMaxHeight()
                  .navigationBarsPadding(),
              )
              VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
          }

          ViewportContainer(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
          )
        }
      } else {
        Column(modifier = Modifier.fillMaxSize()) {
          ViewportContainer(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
              .fillMaxWidth()
              .weight(if (state.isFullscreenViewport) 1f else 0.48f),
          )

          if (!state.isFullscreenViewport) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StudioControlPanel(
              state = state,
              viewModel = viewModel,
              modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f)
                .navigationBarsPadding(),
            )
          }
        }
      }
    }
  }

  val snapPng = state.snapshotPreviewPng
  if (snapPng != null) {
    SnapshotPreviewDialog(
      pngBytes = snapPng,
      onSaveToPhotos = {
        viewModel.saveSnapshotToPhotos { bytes ->
          pendingPngFallbackBytes = bytes
          savePngFallbackLauncher.launch("${state.design.name.replace(" ", "_")}.png")
        }
      },
      onShare = {
        scope.launch {
          val uri = viewModel.createShareUriForSnapshot() ?: return@launch
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share 3D Room Render"))
        }
      },
      onDismiss = viewModel::dismissSnapshotPreview,
    )
  }

  if (state.aiRenderState !is AiRenderState.Idle) {
    AiRenderResultDialog(
      state = state.aiRenderState,
      onCancelOrDismiss = viewModel::cancelOrDismissAiRender,
      onRetry = viewModel::retryAiRender,
      onSaveToPhotos = {
        viewModel.saveAiRenderToPhotos { bytes ->
          pendingPngFallbackBytes = bytes
          savePngFallbackLauncher.launch("${state.design.name.replace(" ", "_")}_AI.png")
        }
      },
      onShare = {
        scope.launch {
          val uri = viewModel.createShareUriForAiRender() ?: return@launch
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share Photorealistic AI Interior"))
        }
      },
    )
  }

  if (state.showProjectsDialog) {
    ProjectsDialog(
      currentName = state.design.name,
      savedDesigns = state.savedDesigns,
      onSaveCurrent = viewModel::saveCurrentDesignSlot,
      onSaveAsNew = viewModel::saveAsNewDesignSlot,
      onLoadDesign = viewModel::loadDesignSlot,
      onDeleteDesign = viewModel::deleteDesignSlot,
      onExportJson = {
        exportJsonLauncher.launch("${state.design.name.replace(" ", "_")}.json")
      },
      onImportJson = {
        importJsonLauncher.launch(arrayOf("application/json", "text/plain"))
      },
      onDismiss = { viewModel.setShowProjectsDialog(false) },
    )
  }
}

@Composable
private fun StudioTopBar(
  state: DesignerUiState,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onToggleRealistic: () -> Unit,
  onCaptureSnapshot: () -> Unit,
  onOpenProjects: () -> Unit,
) {
  val d = state.design
  val dimLabel = "${formatDimension(d.widthMeters, d.unit)}×${formatDimension(d.lengthMeters, d.unit)}${d.unit.suffix}"

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 2.dp,
  ) {
    Column(modifier = Modifier.statusBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = d.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${d.roomType.displayName} • $dimLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          IconButton(
            onClick = onUndo,
            enabled = state.canUndo,
            modifier = Modifier.testTag("undo_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Outlined.Undo,
              contentDescription = stringResource(R.string.cd_undo),
            )
          }
          IconButton(
            onClick = onRedo,
            enabled = state.canRedo,
            modifier = Modifier.testTag("redo_button"),
          ) {
            Icon(
              Icons.AutoMirrored.Outlined.Redo,
              contentDescription = stringResource(R.string.cd_redo),
            )
          }

          FilterChip(
            selected = d.realisticView,
            onClick = onToggleRealistic,
            label = {
              Text(
                text = if (d.realisticView) "Realistic" else "Studio",
                style = MaterialTheme.typography.labelMedium,
              )
            },
            leadingIcon = {
              Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = stringResource(R.string.cd_realistic_view),
                modifier = Modifier.size(16.dp),
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .testTag("realistic_top_toggle"),
          )

          IconButton(
            onClick = onCaptureSnapshot,
            enabled = !state.isCapturingSnapshot,
            modifier = Modifier.testTag("snapshot_top_button"),
          ) {
            Icon(
              Icons.Outlined.CameraAlt,
              contentDescription = stringResource(R.string.cd_snapshot),
            )
          }

          IconButton(
            onClick = onOpenProjects,
            modifier = Modifier.testTag("projects_top_button"),
          ) {
            Icon(
              Icons.Outlined.FolderOpen,
              contentDescription = stringResource(R.string.cd_projects),
            )
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun ViewportContainer(
  state: DesignerUiState,
  viewModel: DesignerViewModel,
  modifier: Modifier = Modifier,
) {
  val selectedLabel = when (val t = state.selectedTarget) {
    SelectionTarget.Walls -> "Walls"
    SelectionTarget.Floor -> "Floor"
    SelectionTarget.Ceiling -> "Ceiling"
    SelectionTarget.AllCountertops -> "Countertops"
    is SelectionTarget.Furniture ->
      state.design.furniture.firstOrNull { it.id == t.id }?.label ?: "Furniture"
  }

  Box(modifier = modifier) {
    Viewport3D(
      design = state.design,
      selectedTarget = state.selectedTarget,
      cameraCommands = viewModel.cameraCommands,
      snapshotRequests = viewModel.snapshotRequests,
      onElementSelected = viewModel::onElementSelectedFrom3D,
      onSnapshotCaptured = viewModel::onSnapshotCaptured,
      modifier = Modifier.fillMaxSize(),
    )

    // Selected element badge (top-left)
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(10.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          Icons.Outlined.Palette,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = "Selected: $selectedLabel",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // Fullscreen toggle (top-right)
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(10.dp),
    ) {
      IconButton(
        onClick = viewModel::toggleFullscreenViewport,
        modifier = Modifier
          .size(40.dp)
          .testTag("fullscreen_viewport_button"),
      ) {
        Icon(
          imageVector = if (state.isFullscreenViewport) {
            Icons.Outlined.CloseFullscreen
          } else {
            Icons.Outlined.OpenInFull
          },
          contentDescription = stringResource(R.string.cd_fullscreen_3d),
          modifier = Modifier.size(18.dp),
        )
      }
    }

    // Floating Camera Presets + Convert to Realistic bar (bottom-center)
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      shadowElevation = 4.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 10.dp, start = 8.dp, end = 8.dp)
        .let { if (state.isFullscreenViewport) it.navigationBarsPadding() else it },
    ) {
      Row(
        modifier = Modifier
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CameraPreset.entries.forEach { preset ->
          val active = state.activeCameraPreset == preset
          FilterChip(
            selected = active,
            onClick = { viewModel.setCameraPreset(preset) },
            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("viewport_cam_${preset.name.lowercase()}"),
          )
        }
        Button(
          onClick = viewModel::requestAiPhotorealisticRender,
          enabled = state.aiRenderState !is AiRenderState.Loading,
          shape = RoundedCornerShape(18.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .height(34.dp)
            .testTag("ai_render_cta"),
        ) {
          Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
          )
          Spacer(Modifier.width(5.dp))
          Text("Real Photo", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

@Composable
private fun StudioControlPanel(
  state: DesignerUiState,
  viewModel: DesignerViewModel,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      TabRow(
        selectedTabIndex = state.activeTab.ordinal,
        containerColor = MaterialTheme.colorScheme.surface,
      ) {
        StudioTab.entries.forEach { tab ->
          val icon = when (tab) {
            StudioTab.ROOM -> Icons.Outlined.SquareFoot
            StudioTab.FURNITURE -> Icons.Outlined.Chair
            StudioTab.MATERIALS -> Icons.Outlined.Palette
            StudioTab.RENDER -> Icons.Outlined.Tune
          }
          Tab(
            selected = state.activeTab == tab,
            onClick = { viewModel.setActiveTab(tab) },
            text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
            icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
          )
        }
      }

      when (state.activeTab) {
        StudioTab.ROOM -> RoomPanel(
          design = state.design,
          onUnitChange = viewModel::setLengthUnit,
          onRoomTypeChange = viewModel::setRoomType,
          onDimensionsChange = viewModel::updateRoomDimensions,
          onAddOpening = viewModel::addOpening,
          onUpdateOpening = viewModel::updateOpening,
          onRemoveOpening = viewModel::removeOpening,
          onApplyTemplate = viewModel::applyTemplate,
          modifier = Modifier.weight(1f),
        )
        StudioTab.FURNITURE -> FurniturePanel(
          design = state.design,
          selectedTarget = state.selectedTarget,
          onSelectFurniture = { id -> viewModel.selectTarget(SelectionTarget.Furniture(id)) },
          onAddFurniture = viewModel::addFurniture,
          onUpdateFurniture = viewModel::updateFurniture,
          onDuplicateFurniture = viewModel::duplicateFurniture,
          onDeleteFurniture = viewModel::deleteFurniture,
          onOpenMaterialsForSelected = { viewModel.setActiveTab(StudioTab.MATERIALS) },
          modifier = Modifier.weight(1f),
        )
        StudioTab.MATERIALS -> MaterialsPanel(
          design = state.design,
          selectedTarget = state.selectedTarget,
          onSelectTarget = viewModel::selectTarget,
          onApplyMaterial = viewModel::applyMaterialToTarget,
          onUpdateFinishOnly = viewModel::updateTargetFinishOnly,
          onUpdateHexOnly = viewModel::updateTargetHexOnly,
          modifier = Modifier.weight(1f),
        )
        StudioTab.RENDER -> StudioExportPanel(
          design = state.design,
          activeCameraPreset = state.activeCameraPreset,
          isCapturingSnapshot = state.isCapturingSnapshot,
          isAiRendering = state.aiRenderState is AiRenderState.Loading,
          onToggleRealisticView = viewModel::toggleRealisticView,
          onConvertToPhotorealistic = viewModel::convertToPhotorealistic,
          onAiPhotoRender = viewModel::requestAiPhotorealisticRender,
          onSelectLightingPreset = viewModel::setLightingPreset,
          onLightingParamsChange = viewModel::setLightingParams,
          onSelectCameraPreset = viewModel::setCameraPreset,
          onCaptureSnapshot = viewModel::requestSnapshot,
          onOpenProjectsDialog = { viewModel.setShowProjectsDialog(true) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
