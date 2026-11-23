package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CameraPreset
import com.example.model.LightingPreset
import com.example.model.RoomDesign
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StudioExportPanel(
  design: RoomDesign,
  activeCameraPreset: CameraPreset,
  isCapturingSnapshot: Boolean,
  isAiRendering: Boolean,
  onToggleRealisticView: () -> Unit,
  onConvertToPhotorealistic: () -> Unit,
  onAiPhotoRender: () -> Unit,
  onSelectLightingPreset: (LightingPreset) -> Unit,
  onLightingParamsChange: (sunAzimuth: Float?, exposure: Float?) -> Unit,
  onSelectCameraPreset: (CameraPreset) -> Unit,
  onCaptureSnapshot: () -> Unit,
  onOpenProjectsDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Button(
      onClick = onAiPhotoRender,
      enabled = !isAiRendering,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("ai_photo_render_panel_button"),
    ) {
      Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(if (isAiRendering) "Rendering Real Interior Photo..." else "AI Real Photo Render (DSLR Realism)")
    }

    FilledTonalButton(
      onClick = onConvertToPhotorealistic,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .testTag("transform_photorealistic_button"),
    ) {
      Icon(Icons.Outlined.CameraAlt, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Snap Corner Eye-Level + 3D Lights")
    }

    // Realistic View Toggle Card
    Card(
      colors = CardDefaults.cardColors(
        containerColor = if (design.realisticView) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
          MaterialTheme.colorScheme.surface
        },
      ),
      border = BorderStroke(
        width = if (design.realisticView) 1.5.dp else 1.dp,
        color = if (design.realisticView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
      ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggleRealisticView() },
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
              text = "Realistic View Mode",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "SSAO contact shadows, bump normals, reflections & bloom",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
          checked = design.realisticView,
          onCheckedChange = { onToggleRealisticView() },
          modifier = Modifier.testTag("realistic_mode_switch"),
        )
      }
    }

    // Lighting Presets
    SectionTitle(
      title = "Lighting Atmosphere",
      subtitle = "Selecting a preset enables Realistic View automatically",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      LightingPreset.entries.forEach { preset ->
        val selected = design.lightingPreset == preset && design.realisticView
        val icon = when (preset) {
          LightingPreset.DAYLIGHT -> Icons.Outlined.LightMode
          LightingPreset.GOLDEN_HOUR -> Icons.Outlined.WbTwilight
          LightingPreset.INDOOR_SPOTLIGHTS -> Icons.Outlined.Nightlight
        }
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
              MaterialTheme.colorScheme.surface
            },
          ),
          border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectLightingPreset(preset) }
            .testTag("lighting_${preset.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              icon,
              contentDescription = null,
              tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
              Text(preset.displayName, style = MaterialTheme.typography.labelLarge)
              Text(
                preset.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    // Fine Light Controls
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Sun Direction (${design.sunAzimuthDeg.roundToInt()}°)",
        style = MaterialTheme.typography.bodySmall,
      )
      Slider(
        value = design.sunAzimuthDeg,
        onValueChange = { onLightingParamsChange(it, null) },
        valueRange = 0f..360f,
        modifier = Modifier.testTag("sun_azimuth_slider"),
      )

      Text(
        text = String.format(Locale.US, "Exposure (%.2f)", design.exposure),
        style = MaterialTheme.typography.bodySmall,
      )
      Slider(
        value = design.exposure,
        onValueChange = { onLightingParamsChange(null, it) },
        valueRange = 0.65f..1.65f,
        modifier = Modifier.testTag("exposure_slider"),
      )
    }

    // Camera Presets
    SectionTitle(
      title = "Camera Views",
      subtitle = "Drag to orbit, pinch to zoom, two-finger drag to pan",
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CameraPreset.entries.forEach { preset ->
        val selected = activeCameraPreset == preset
        if (selected) {
          FilledTonalButton(
            onClick = { onSelectCameraPreset(preset) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("cam_${preset.name.lowercase()}"),
          ) {
            Icon(
              imageVector = when (preset) {
                CameraPreset.PERSPECTIVE_ORBIT -> Icons.Outlined.Cameraswitch
                CameraPreset.TOP_FLOORPLAN -> Icons.Outlined.ViewInAr
                CameraPreset.CORNER_EYE_LEVEL -> Icons.Outlined.CameraAlt
              },
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(preset.label, style = MaterialTheme.typography.labelSmall)
          }
        } else {
          OutlinedButton(
            onClick = { onSelectCameraPreset(preset) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("cam_${preset.name.lowercase()}"),
          ) {
            Text(preset.label, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Export & Projects
    SectionTitle(
      title = "Export & Save",
      subtitle = "High-res PNG snapshots & local JSON backups",
    )

    Button(
      onClick = onCaptureSnapshot,
      enabled = !isCapturingSnapshot,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("capture_snapshot_panel_button"),
    ) {
      Icon(Icons.Outlined.CameraAlt, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(if (isCapturingSnapshot) "Rendering 1920p PNG..." else "Export High-Res PNG Snapshot")
    }

    OutlinedButton(
      onClick = onOpenProjectsDialog,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("open_projects_panel_button"),
    ) {
      Icon(Icons.Outlined.FolderOpen, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text("Saved Projects & JSON Import/Export")
    }

    Spacer(Modifier.height(12.dp))
  }
}
