package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CabinetBoxPreset
import com.example.data.LebanonCatalog
import com.example.model.FurnitureItem
import com.example.model.FurnitureKind
import com.example.model.RoomDesign
import com.example.model.SelectionTarget
import com.example.ui.i18n.LocalAppStrings
import kotlin.math.roundToInt

private val cabinetKinds = setOf(
  FurnitureKind.STRAIGHT_COUNTER,
  FurnitureKind.L_COUNTER,
  FurnitureKind.KITCHEN_ISLAND,
  FurnitureKind.UPPER_CABINET,
  FurnitureKind.TALL_PANTRY,
  FurnitureKind.WARDROBE,
  FurnitureKind.DRESSER,
  FurnitureKind.TV_CONSOLE,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CabinetBoxesPanel(
  design: RoomDesign,
  selectedTarget: SelectionTarget,
  onSelectCabinet: (String) -> Unit,
  onAddBoxPreset: (CabinetBoxPreset) -> Unit,
  onUpdateCabinet: (id: String, coalesceKey: String?, transform: (FurnitureItem) -> FurnitureItem) -> Unit,
  onDuplicateCabinet: (String) -> Unit,
  onDeleteCabinet: (String) -> Unit,
  onOpenMaterials: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val cabinets = design.furniture.filter { it.kind in cabinetKinds }
  val selectedBox = (selectedTarget as? SelectionTarget.Furniture)?.let { sel ->
    cabinets.firstOrNull { it.id == sel.id }
  } ?: cabinets.firstOrNull()

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionTitle(
      title = strings.cabinetsTitle,
      subtitle = strings.cabinetsSub,
    )

    if (cabinets.isEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = strings.noCabinetsEmptyMsg,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        cabinets.forEach { box ->
          val isSel = selectedBox?.id == box.id
          FilterChip(
            selected = isSel,
            onClick = { onSelectCabinet(box.id) },
            leadingIcon = {
              Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = {
              Text(
                text = "${box.label} (${formatDimension(box.widthMeters, design.unit)}${design.unit.suffix})",
                style = MaterialTheme.typography.labelMedium,
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("cab_box_${box.id}"),
          )
        }
      }
    }

    if (selectedBox != null) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(parseHexColorSafe(selectedBox.material.hexColor)),
              )
              Spacer(Modifier.width(8.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = selectedBox.label,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${selectedBox.style.label} • ${formatDimension(selectedBox.widthMeters, design.unit)} × ${formatDimension(selectedBox.depthMeters, design.unit)} × ${formatDimension(selectedBox.heightMeters, design.unit)} ${design.unit.suffix}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
            Row {
              IconButton(
                onClick = { onDuplicateCabinet(selectedBox.id) },
                modifier = Modifier.testTag("cab_duplicate_button"),
              ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate cabinet box")
              }
              IconButton(
                onClick = { onDeleteCabinet(selectedBox.id) },
                modifier = Modifier.testTag("cab_delete_button"),
              ) {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete cabinet box",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilledTonalButton(
              onClick = {
                onUpdateCabinet(selectedBox.id, null) {
                  it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f)
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("cab_rotate_90_button"),
            ) {
              Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.rotate90Btn)
            }
            OutlinedButton(
              onClick = {
                onSelectCabinet(selectedBox.id)
                onOpenMaterials()
              },
              modifier = Modifier
                .weight(1f)
                .testTag("cab_material_button"),
            ) {
              Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.openMaterialsBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          FurnitureStructureControls(
            item = selectedBox,
            onUpdateItem = { transform ->
              onUpdateCabinet(selectedBox.id, null, transform)
            },
          )

          DimensionSliderInput(
            label = strings.boxWidthLabel,
            valueMeters = selectedBox.widthMeters,
            rangeMeters = 0.30f..design.widthMeters,
            unit = design.unit,
            onValueMetersChange = { w ->
              onUpdateCabinet(selectedBox.id, "cw_${selectedBox.id}") { it.copy(widthMeters = w) }
            },
            testTagPrefix = "cab_width",
          )

          DimensionSliderInput(
            label = strings.boxDepthLabel,
            valueMeters = selectedBox.depthMeters,
            rangeMeters = 0.25f..design.lengthMeters,
            unit = design.unit,
            onValueMetersChange = { d ->
              onUpdateCabinet(selectedBox.id, "cd_${selectedBox.id}") { it.copy(depthMeters = d) }
            },
            testTagPrefix = "cab_depth",
          )

          DimensionSliderInput(
            label = strings.boxHeightLabel,
            valueMeters = selectedBox.heightMeters,
            rangeMeters = 0.25f..design.wallHeightMeters,
            unit = design.unit,
            onValueMetersChange = { h ->
              onUpdateCabinet(selectedBox.id, "ch_${selectedBox.id}") { it.copy(heightMeters = h) }
            },
            testTagPrefix = "cab_height",
          )

          DimensionSliderInput(
            label = strings.mountHeightLabel,
            valueMeters = selectedBox.yMeters,
            rangeMeters = 0f..(design.wallHeightMeters - 0.35f).coerceAtLeast(0.5f),
            unit = design.unit,
            onValueMetersChange = { y ->
              onUpdateCabinet(selectedBox.id, "cy_${selectedBox.id}") { it.copy(yMeters = y) }
            },
            testTagPrefix = "cab_elevation",
          )

          val halfW = (design.widthMeters * 0.5f - 0.15f).coerceAtLeast(0.5f)
          val halfL = (design.lengthMeters * 0.5f - 0.15f).coerceAtLeast(0.5f)

          Text(
            text = "${strings.posLeftRightLabel} (${formatDimension(selectedBox.xMeters, design.unit)}${design.unit.suffix})",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedBox.xMeters.coerceIn(-halfW, halfW),
            onValueChange = { x ->
              onUpdateCabinet(selectedBox.id, "cx_${selectedBox.id}") { it.copy(xMeters = x) }
            },
            valueRange = -halfW..halfW,
            modifier = Modifier.testTag("cab_pos_x_slider"),
          )

          Text(
            text = "${strings.posBackFrontLabel} (${formatDimension(selectedBox.zMeters, design.unit)}${design.unit.suffix})",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedBox.zMeters.coerceIn(-halfL, halfL),
            onValueChange = { z ->
              onUpdateCabinet(selectedBox.id, "cz_${selectedBox.id}") { it.copy(zMeters = z) }
            },
            valueRange = -halfL..halfL,
            modifier = Modifier.testTag("cab_pos_z_slider"),
          )

          Text(
            text = "${strings.rotationLabel} (${selectedBox.rotationDeg.roundToInt()}°)",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedBox.rotationDeg.coerceIn(0f, 360f),
            onValueChange = { rot ->
              onUpdateCabinet(selectedBox.id, "cr_${selectedBox.id}") { it.copy(rotationDeg = rot) }
            },
            valueRange = 0f..360f,
            modifier = Modifier.testTag("cab_rotation_slider"),
          )
        }
      }
    }

    SectionTitle(
      title = strings.addStandardBoxTitle,
      subtitle = strings.addStandardBoxSub,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      LebanonCatalog.cabinetBoxPresets.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pair.forEach { preset ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { onAddBoxPreset(preset) }
                .testTag("cab_preset_${preset.id}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier
                      .size(30.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      Icons.Outlined.Straighten,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(17.dp),
                    )
                  }
                  Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add ${preset.title}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text = preset.title,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = preset.sizeNote,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
      }
    }

    Spacer(Modifier.height(12.dp))
  }
}
