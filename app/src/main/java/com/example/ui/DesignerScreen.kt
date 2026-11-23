package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SquareFoot
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.model.AppLanguage
import com.example.model.CameraPreset
import com.example.model.SelectionTarget
import com.example.ui.components.AiRenderResultDialog
import com.example.ui.components.AluminumHardwarePanel
import com.example.ui.components.CabinetBoxesPanel
import com.example.ui.components.FurniturePanel
import com.example.ui.components.GraniteStonePanel
import com.example.ui.components.MaterialsPanel
import com.example.ui.components.ProjectsDialog
import com.example.ui.components.RoomPanel
import com.example.ui.components.SinkPulloutPanel
import com.example.ui.components.SnapshotPreviewDialog
import com.example.ui.components.StudioExportPanel
import com.example.ui.components.Viewport3D
import com.example.ui.components.formatDimension
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor
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

  val appStrings = remember(state.appLanguage) { stringsFor(state.appLanguage) }
  val layoutDir = if (state.appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(
    LocalAppStrings provides appStrings,
    LocalLayoutDirection provides layoutDir,
  ) {
  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      StudioTopBar(
        state = state,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onSelectLanguage = { lang ->
          viewModel.setAppLanguage(lang)
          scope.launch { snackbarHostState.showSnackbar(stringsFor(lang).languageChangedMsg) }
        },
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
              .weight(if (state.isFullscreenViewport) 1f else 0.45f),
          )

          if (!state.isFullscreenViewport) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StudioControlPanel(
              state = state,
              viewModel = viewModel,
              modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
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
}

@Composable
private fun StudioTopBar(
  state: DesignerUiState,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onOpenProjects: () -> Unit,
) {
  val d = state.design
  val strings = LocalAppStrings.current
  var showLangMenu by remember { mutableStateOf(false) }
  val dimLabel = "${formatDimension(d.widthMeters, d.unit)}×${formatDimension(d.lengthMeters, d.unit)}${d.unit.suffix}"

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.22f),
            ),
          ),
        )
        .statusBarsPadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                  ),
                ),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.ViewInAr,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(19.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = d.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${strings.roomTypeLabel(d.roomType)} • $dimLabel",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
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

          Box {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              modifier = Modifier
                .clickable { showLangMenu = true }
                .testTag("language_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  Icons.Outlined.Language,
                  contentDescription = "Language",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = state.appLanguage.shortCode,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            DropdownMenu(
              expanded = showLangMenu,
              onDismissRequest = { showLangMenu = false },
            ) {
              AppLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = "${lang.nativeName} (${lang.shortCode})",
                      fontWeight = if (lang == state.appLanguage) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                  onClick = {
                    showLangMenu = false
                    onSelectLanguage(lang)
                  },
                  modifier = Modifier.testTag("lang_option_${lang.name.lowercase()}"),
                )
              }
            }
          }

          IconButton(
            onClick = onOpenProjects,
            modifier = Modifier.testTag("projects_top_button"),
          ) {
            Icon(
              Icons.Outlined.FolderOpen,
              contentDescription = stringResource(R.string.cd_projects),
              tint = MaterialTheme.colorScheme.primary,
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
  val strings = LocalAppStrings.current
  val selectedLabel = when (val t = state.selectedTarget) {
    SelectionTarget.Walls -> strings.wallsLabel
    SelectionTarget.Floor -> strings.floorLabel
    SelectionTarget.Ceiling -> strings.ceilingLabel
    SelectionTarget.AllCountertops -> strings.countertopsLabel
    is SelectionTarget.Furniture ->
      state.design.furniture.firstOrNull { it.id == t.id }?.label ?: strings.furnitureFallbackLabel
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

    // Selected element badge (top-start, 1-tap to customize material)
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
      shadowElevation = 3.dp,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(10.dp)
        .clickable { viewModel.setActiveTab(StudioTab.MATERIALS) },
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary),
        )
        Icon(
          Icons.Outlined.Palette,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = selectedLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // Realistic toggle + PNG Snapshot + Fullscreen pill cluster (top-end)
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      shadowElevation = 3.dp,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(10.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (state.design.realisticView) {
            MaterialTheme.colorScheme.tertiary
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
          },
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = viewModel::toggleRealisticView)
            .testTag("realistic_top_toggle"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              Icons.Outlined.AutoAwesome,
              contentDescription = stringResource(R.string.cd_realistic_view),
              tint = if (state.design.realisticView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (state.design.realisticView) strings.realisticChip else strings.studioChip,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (state.design.realisticView) Color.White else MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        IconButton(
          onClick = viewModel::requestSnapshot,
          enabled = !state.isCapturingSnapshot,
          modifier = Modifier
            .size(36.dp)
            .testTag("snapshot_top_button"),
        ) {
          Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = stringResource(R.string.cd_snapshot),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
        }

        IconButton(
          onClick = viewModel::toggleFullscreenViewport,
          modifier = Modifier
            .size(36.dp)
            .testTag("fullscreen_viewport_button"),
        ) {
          Icon(
            imageVector = if (state.isFullscreenViewport) {
              Icons.Outlined.CloseFullscreen
            } else {
              Icons.Outlined.OpenInFull
            },
            contentDescription = stringResource(R.string.cd_fullscreen_3d),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // Floating Camera Presets + AI Photo CTA bar (bottom-center)
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
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CameraPreset.entries.forEach { preset ->
          val active = state.activeCameraPreset == preset
          FilterChip(
            selected = active,
            onClick = { viewModel.setCameraPreset(preset) },
            label = { Text(strings.cameraPresetLabel(preset), style = MaterialTheme.typography.labelSmall) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("viewport_cam_${preset.name.lowercase()}"),
          )
        }
        val aiLoading = state.aiRenderState is AiRenderState.Loading
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = Color.Transparent,
          shadowElevation = 2.dp,
          modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.horizontalGradient(
                listOf(
                  MaterialTheme.colorScheme.primary,
                  MaterialTheme.colorScheme.tertiary,
                ),
              ),
              alpha = if (aiLoading) 0.55f else 1f,
            )
            .clickable(enabled = !aiLoading, onClick = viewModel::requestAiPhotorealisticRender)
            .testTag("ai_render_cta"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              Icons.Outlined.AutoAwesome,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = strings.realPhotoCta,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
            )
          }
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
  val strings = LocalAppStrings.current
  val tabListState = rememberLazyListState()
  LaunchedEffect(state.activeTab) {
    tabListState.animateScrollToItem(state.activeTab.ordinal)
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
      ) {
        Column {
          LazyRow(
            state = tabListState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            itemsIndexed(StudioTab.entries) { _, tab ->
              val icon = when (tab) {
                StudioTab.ROOM -> Icons.Outlined.SquareFoot
                StudioTab.CABINETS -> Icons.Outlined.Inventory2
                StudioTab.ALUMINUM -> Icons.Outlined.Hardware
                StudioTab.GRANITE -> Icons.Outlined.Layers
                StudioTab.SINK_PULLOUT -> Icons.Outlined.WaterDrop
                StudioTab.FURNITURE -> Icons.Outlined.Chair
                StudioTab.MATERIALS -> Icons.Outlined.Palette
                StudioTab.RENDER -> Icons.Outlined.Tune
              }
              val tabLabel = when (tab) {
                StudioTab.ROOM -> strings.tabRoom
                StudioTab.CABINETS -> strings.tabCabinets
                StudioTab.ALUMINUM -> strings.tabAluminum
                StudioTab.GRANITE -> strings.tabGranite
                StudioTab.SINK_PULLOUT -> strings.tabSinkPullout
                StudioTab.FURNITURE -> strings.tabFurniture
                StudioTab.MATERIALS -> strings.tabMaterials
                StudioTab.RENDER -> strings.tabRender
              }
              val isSelected = state.activeTab == tab
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                border = if (isSelected) null else BorderStroke(
                  1.dp,
                  MaterialTheme.colorScheme.outlineVariant,
                ),
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                  .height(38.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { viewModel.setActiveTab(tab) }
                  .testTag("tab_${tab.name.lowercase()}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = tabLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        StudioTab.CABINETS -> CabinetBoxesPanel(
          design = state.design,
          selectedTarget = state.selectedTarget,
          onSelectCabinet = { id -> viewModel.selectTarget(SelectionTarget.Furniture(id)) },
          onAddBoxPreset = viewModel::addCabinetBoxPreset,
          onUpdateCabinet = viewModel::updateFurniture,
          onDuplicateCabinet = viewModel::duplicateFurniture,
          onDeleteCabinet = viewModel::deleteFurniture,
          onOpenMaterials = { viewModel.setActiveTab(StudioTab.MATERIALS) },
          modifier = Modifier.weight(1f),
        )
        StudioTab.ALUMINUM -> AluminumHardwarePanel(
          specs = state.design.kitchenSpecs,
          onUpdateSpecs = viewModel::updateKitchenSpecs,
          onApplyAluminumGlassFrames = viewModel::applyGlassFramesToUpperCabinets,
          modifier = Modifier.weight(1f),
        )
        StudioTab.GRANITE -> GraniteStonePanel(
          design = state.design,
          onSelectSlab = viewModel::applyLebaneseGranite,
          onUpdateSpecs = viewModel::updateKitchenSpecs,
          modifier = Modifier.weight(1f),
        )
        StudioTab.SINK_PULLOUT -> SinkPulloutPanel(
          specs = state.design.kitchenSpecs,
          onUpdateSpecs = viewModel::updateKitchenSpecs,
          onToggleOrganizer = viewModel::toggleOrganizer,
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
          selectedMfcGrade = state.design.kitchenSpecs.mfcBoardGrade,
          onSelectMfcGrade = { grade ->
            viewModel.updateKitchenSpecs { it.copy(mfcBoardGrade = grade) }
          },
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
