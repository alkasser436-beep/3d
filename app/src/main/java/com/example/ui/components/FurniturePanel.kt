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
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Countertops
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoorSliding
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Light
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Rectangle
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FurnitureCategory
import com.example.model.FurnitureItem
import com.example.model.FurnitureKind
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.example.model.SelectionTarget
import kotlin.math.roundToInt

private fun iconForKind(kind: FurnitureKind): ImageVector = when (kind) {
  FurnitureKind.STRAIGHT_COUNTER,
  FurnitureKind.L_COUNTER,
  FurnitureKind.KITCHEN_ISLAND -> Icons.Outlined.Countertops
  FurnitureKind.UPPER_CABINET -> Icons.Outlined.Inventory2
  FurnitureKind.TALL_PANTRY -> Icons.Outlined.Kitchen
  FurnitureKind.DINING_TABLE -> Icons.Outlined.TableRestaurant
  FurnitureKind.BED -> Icons.Outlined.Bed
  FurnitureKind.WARDROBE -> Icons.Outlined.DoorSliding
  FurnitureKind.NIGHTSTAND -> Icons.Outlined.Lightbulb
  FurnitureKind.DRESSER -> Icons.Outlined.Inventory2
  FurnitureKind.SOFA -> Icons.Outlined.Weekend
  FurnitureKind.COFFEE_TABLE -> Icons.Outlined.Chair
  FurnitureKind.TV_CONSOLE -> Icons.Outlined.Tv
  FurnitureKind.RUG -> Icons.Outlined.Rectangle
  FurnitureKind.FLOOR_LAMP -> Icons.Outlined.Light
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FurniturePanel(
  design: RoomDesign,
  selectedTarget: SelectionTarget,
  onSelectFurniture: (String) -> Unit,
  onAddFurniture: (FurnitureKind) -> Unit,
  onUpdateFurniture: (id: String, coalesceKey: String?, transform: (FurnitureItem) -> FurnitureItem) -> Unit,
  onDuplicateFurniture: (String) -> Unit,
  onDeleteFurniture: (String) -> Unit,
  onOpenMaterialsForSelected: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val defaultCategory = when (design.roomType) {
    RoomType.KITCHEN -> FurnitureCategory.KITCHEN
    RoomType.BEDROOM -> FurnitureCategory.BEDROOM
    RoomType.LIVING_ROOM -> FurnitureCategory.LIVING
  }
  var activeCategory by remember(design.roomType) { mutableStateOf<FurnitureCategory?>(defaultCategory) }

  val selectedItem = (selectedTarget as? SelectionTarget.Furniture)?.let { sel ->
    design.furniture.firstOrNull { it.id == sel.id }
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Active Selected Item Inspector
    if (selectedItem != null) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp),
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
                  .background(parseHexColorSafe(selectedItem.material.hexColor)),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = selectedItem.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row {
              IconButton(
                onClick = { onDuplicateFurniture(selectedItem.id) },
                modifier = Modifier.testTag("duplicate_item_button"),
              ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate item")
              }
              IconButton(
                onClick = { onDeleteFurniture(selectedItem.id) },
                modifier = Modifier.testTag("delete_item_button"),
              ) {
                Icon(
                  Icons.Outlined.DeleteOutline,
                  contentDescription = "Delete item",
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
                onUpdateFurniture(selectedItem.id, null) {
                  it.copy(rotationDeg = (it.rotationDeg + 90f) % 360f)
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("rotate_90_button"),
            ) {
              Icon(Icons.AutoMirrored.Outlined.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Rotate 90°")
            }
            OutlinedButton(
              onClick = onOpenMaterialsForSelected,
              modifier = Modifier
                .weight(1f)
                .testTag("edit_item_material_button"),
            ) {
              Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Material")
            }
          }

          DimensionSliderInput(
            label = "Width",
            valueMeters = selectedItem.widthMeters,
            rangeMeters = 0.30f..design.widthMeters,
            unit = design.unit,
            onValueMetersChange = { w ->
              onUpdateFurniture(selectedItem.id, "w_${selectedItem.id}") { it.copy(widthMeters = w) }
            },
            testTagPrefix = "item_width",
          )
          DimensionSliderInput(
            label = "Depth",
            valueMeters = selectedItem.depthMeters,
            rangeMeters = 0.25f..design.lengthMeters,
            unit = design.unit,
            onValueMetersChange = { d ->
              onUpdateFurniture(selectedItem.id, "d_${selectedItem.id}") { it.copy(depthMeters = d) }
            },
            testTagPrefix = "item_depth",
          )
          DimensionSliderInput(
            label = "Height",
            valueMeters = selectedItem.heightMeters,
            rangeMeters = 0.02f..design.wallHeightMeters,
            unit = design.unit,
            onValueMetersChange = { h ->
              onUpdateFurniture(selectedItem.id, "h_${selectedItem.id}") { it.copy(heightMeters = h) }
            },
            testTagPrefix = "item_height",
          )

          val halfW = (design.widthMeters * 0.5f - 0.15f).coerceAtLeast(0.5f)
          val halfL = (design.lengthMeters * 0.5f - 0.15f).coerceAtLeast(0.5f)

          Text(
            text = "Position X (${formatDimension(selectedItem.xMeters, design.unit)}${design.unit.suffix})",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedItem.xMeters.coerceIn(-halfW, halfW),
            onValueChange = { x ->
              onUpdateFurniture(selectedItem.id, "x_${selectedItem.id}") { it.copy(xMeters = x) }
            },
            valueRange = -halfW..halfW,
            modifier = Modifier.testTag("item_pos_x_slider"),
          )

          Text(
            text = "Position Z (${formatDimension(selectedItem.zMeters, design.unit)}${design.unit.suffix})",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedItem.zMeters.coerceIn(-halfL, halfL),
            onValueChange = { z ->
              onUpdateFurniture(selectedItem.id, "z_${selectedItem.id}") { it.copy(zMeters = z) }
            },
            valueRange = -halfL..halfL,
            modifier = Modifier.testTag("item_pos_z_slider"),
          )

          if (selectedItem.kind == FurnitureKind.UPPER_CABINET || selectedItem.yMeters > 0.01f) {
            DimensionSliderInput(
              label = "Mount Elevation (Y)",
              valueMeters = selectedItem.yMeters,
              rangeMeters = 0f..(design.wallHeightMeters - 0.4f).coerceAtLeast(0.5f),
              unit = design.unit,
              onValueMetersChange = { y ->
                onUpdateFurniture(selectedItem.id, "y_${selectedItem.id}") { it.copy(yMeters = y) }
              },
              testTagPrefix = "item_elevation",
            )
          }

          Text(
            text = "Rotation (${selectedItem.rotationDeg.roundToInt()}°)",
            style = MaterialTheme.typography.bodySmall,
          )
          Slider(
            value = selectedItem.rotationDeg.coerceIn(0f, 360f),
            onValueChange = { rot ->
              onUpdateFurniture(selectedItem.id, "r_${selectedItem.id}") { it.copy(rotationDeg = rot) }
            },
            valueRange = 0f..360f,
            modifier = Modifier.testTag("item_rotation_slider"),
          )
        }
      }
    }

    // Scene Items Picker
    if (design.furniture.isNotEmpty()) {
      SectionTitle(
        title = "Placed in Room (${design.furniture.size})",
        subtitle = "Tap any item in 3D or below to edit size, position & finish",
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        design.furniture.forEach { item ->
          val isSel = selectedItem?.id == item.id
          FilterChip(
            selected = isSel,
            onClick = { onSelectFurniture(item.id) },
            leadingIcon = {
              Icon(iconForKind(item.kind), contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.testTag("scene_item_${item.id}"),
          )
        }
      }
    }

    // Quick-Add Furniture & Cabinets Catalog
    SectionTitle(
      title = "Add Furniture & Cabinets",
      subtitle = "Tap to place into your room instantly",
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = activeCategory == null,
        onClick = { activeCategory = null },
        label = { Text("All") },
      )
      FurnitureCategory.entries.forEach { cat ->
        FilterChip(
          selected = activeCategory == cat,
          onClick = { activeCategory = cat },
          label = { Text(cat.label) },
        )
      }
    }

    val visibleKinds = FurnitureKind.entries.filter {
      activeCategory == null || it.category == activeCategory
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      visibleKinds.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pair.forEach { kind ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { onAddFurniture(kind) }
                .testTag("add_kind_${kind.name.lowercase()}"),
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
                  Icon(
                    imageVector = iconForKind(kind),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                  )
                  Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add ${kind.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text = kind.displayName,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${formatDimension(kind.defaultW, design.unit)}×${formatDimension(kind.defaultD, design.unit)}${design.unit.suffix}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          if (pair.size == 1) {
            Spacer(Modifier.weight(1f))
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}
