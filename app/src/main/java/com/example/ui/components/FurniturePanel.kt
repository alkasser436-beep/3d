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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.example.model.CabinetStyle
import com.example.model.FurnitureCategory
import com.example.model.FurnitureItem
import com.example.model.FurnitureKind
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.example.model.SelectionTarget
import com.example.ui.i18n.LocalAppStrings
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
  val strings = LocalAppStrings.current
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
              Text(strings.rotate90Btn)
            }
            OutlinedButton(
              onClick = onOpenMaterialsForSelected,
              modifier = Modifier
                .weight(1f)
                .testTag("edit_item_material_button"),
            ) {
              Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.materialBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          FurnitureStructureControls(
            item = selectedItem,
            onUpdateItem = { transform ->
              onUpdateFurniture(selectedItem.id, null, transform)
            },
          )

          DimensionSliderInput(
            label = strings.boxWidthLabel,
            valueMeters = selectedItem.widthMeters,
            rangeMeters = 0.30f..design.widthMeters,
            unit = design.unit,
            onValueMetersChange = { w ->
              onUpdateFurniture(selectedItem.id, "w_${selectedItem.id}") { it.copy(widthMeters = w) }
            },
            testTagPrefix = "item_width",
          )
          DimensionSliderInput(
            label = strings.boxDepthLabel,
            valueMeters = selectedItem.depthMeters,
            rangeMeters = 0.25f..design.lengthMeters,
            unit = design.unit,
            onValueMetersChange = { d ->
              onUpdateFurniture(selectedItem.id, "d_${selectedItem.id}") { it.copy(depthMeters = d) }
            },
            testTagPrefix = "item_depth",
          )
          DimensionSliderInput(
            label = strings.boxHeightLabel,
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
            text = "${strings.posLeftRightLabel} (${formatDimension(selectedItem.xMeters, design.unit)}${design.unit.suffix})",
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
            text = "${strings.posBackFrontLabel} (${formatDimension(selectedItem.zMeters, design.unit)}${design.unit.suffix})",
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
              label = strings.mountHeightLabel,
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
            text = "${strings.rotationLabel} (${selectedItem.rotationDeg.roundToInt()}°)",
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
        title = "${strings.placedItemsTitle} (${design.furniture.size})",
        subtitle = strings.placedItemsSub,
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
      title = strings.addElementTitle,
      subtitle = strings.addElementSub,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = activeCategory == null,
        onClick = { activeCategory = null },
        label = { Text(strings.starwoodAllFilter) },
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
            val catTint = when (kind.category) {
              FurnitureCategory.KITCHEN -> MaterialTheme.colorScheme.primary
              FurnitureCategory.BEDROOM -> MaterialTheme.colorScheme.secondary
              FurnitureCategory.LIVING -> MaterialTheme.colorScheme.tertiary
            }
            val catContainer = when (kind.category) {
              FurnitureCategory.KITCHEN -> MaterialTheme.colorScheme.primaryContainer
              FurnitureCategory.BEDROOM -> MaterialTheme.colorScheme.secondaryContainer
              FurnitureCategory.LIVING -> MaterialTheme.colorScheme.tertiaryContainer
            }
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, catTint.copy(alpha = 0.24f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { onAddFurniture(kind) }
                .testTag("add_kind_${kind.name.lowercase()}"),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(catContainer.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = iconForKind(kind),
                      contentDescription = null,
                      tint = catTint,
                      modifier = Modifier.size(19.dp),
                    )
                  }
                  Box(
                    modifier = Modifier
                      .size(26.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Add,
                      contentDescription = "Add ${kind.displayName}",
                      tint = catTint,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FurnitureStructureControls(
  item: FurnitureItem,
  onUpdateItem: ((FurnitureItem) -> FurnitureItem) -> Unit,
) {
  val strings = LocalAppStrings.current
  val hasDoors = item.kind in setOf(
    FurnitureKind.STRAIGHT_COUNTER,
    FurnitureKind.L_COUNTER,
    FurnitureKind.KITCHEN_ISLAND,
    FurnitureKind.UPPER_CABINET,
    FurnitureKind.TALL_PANTRY,
    FurnitureKind.WARDROBE,
    FurnitureKind.TV_CONSOLE,
  )
  val hasDrawers = item.kind in setOf(
    FurnitureKind.STRAIGHT_COUNTER,
    FurnitureKind.L_COUNTER,
    FurnitureKind.KITCHEN_ISLAND,
    FurnitureKind.TALL_PANTRY,
    FurnitureKind.NIGHTSTAND,
    FurnitureKind.DRESSER,
  )
  val isCabinetOrStorage = hasDoors || hasDrawers
  val styleOptions: List<Pair<CabinetStyle, String>> = when (item.kind) {
    FurnitureKind.SOFA -> listOf(
      CabinetStyle.HANDLELESS_GOLA to "Standard",
      CabinetStyle.SHAKER_FRAME to "L-Chaise",
      CabinetStyle.OPEN_SHELVING to "Armless",
    )
    FurnitureKind.DINING_TABLE, FurnitureKind.COFFEE_TABLE -> listOf(
      CabinetStyle.HANDLELESS_GOLA to "Solid Top",
      CabinetStyle.GLASS_DISPLAY to "Glass Top",
      CabinetStyle.OPEN_SHELVING to "Pedestal",
    )
    else -> if (isCabinetOrStorage) {
      CabinetStyle.entries.map { it to it.label }
    } else emptyList()
  }

  if (styleOptions.isEmpty() && !hasDoors && !hasDrawers) return

  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

  if (styleOptions.isNotEmpty()) {
    Text(
      text = if (isCabinetOrStorage) strings.cabinetShapeStyleTitle else strings.shapeStyleTitle,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      styleOptions.forEach { (styleVal, label) ->
        val effectiveSelected = if (styleVal == CabinetStyle.OPEN_SHELVING && isCabinetOrStorage) {
          item.style == CabinetStyle.OPEN_SHELVING || (!item.showDoors && !item.showDrawers)
        } else {
          item.style == styleVal
        }
        FilterChip(
          selected = effectiveSelected,
          onClick = {
            onUpdateItem { cur ->
              if (styleVal == CabinetStyle.OPEN_SHELVING && isCabinetOrStorage) {
                cur.copy(style = styleVal, showDoors = false, showDrawers = false)
              } else {
                val restoreDoors = if (cur.style == CabinetStyle.OPEN_SHELVING && !cur.showDoors) true else cur.showDoors
                val restoreDrawers = if (cur.style == CabinetStyle.OPEN_SHELVING && !cur.showDrawers) true else cur.showDrawers
                cur.copy(style = styleVal, showDoors = restoreDoors, showDrawers = restoreDrawers)
              }
            }
          },
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          modifier = Modifier.testTag("style_${styleVal.name.lowercase()}"),
        )
      }
    }
  }

  if (hasDoors || hasDrawers) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      if (hasDoors) {
        val doorsEnabled = item.style != CabinetStyle.OPEN_SHELVING && item.showDoors
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.doorsShuttersLabel, style = MaterialTheme.typography.bodyMedium)
            Text(
              text = if (doorsEnabled) strings.doorsVisibleSub else strings.doorsRemovedSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = doorsEnabled,
            onCheckedChange = { checked ->
              onUpdateItem { cur ->
                val nextStyle = if (checked && cur.style == CabinetStyle.OPEN_SHELVING) {
                  cur.kind.defaultStyle.takeIf { it != CabinetStyle.OPEN_SHELVING } ?: CabinetStyle.HANDLELESS_GOLA
                } else if (!checked && !cur.showDrawers) {
                  CabinetStyle.OPEN_SHELVING
                } else cur.style
                cur.copy(showDoors = checked, style = nextStyle)
              }
            },
            modifier = Modifier.testTag("toggle_doors_switch"),
          )
        }
      }

      if (hasDrawers) {
        val drawersEnabled = item.style != CabinetStyle.OPEN_SHELVING && item.showDrawers
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.drawersLabel, style = MaterialTheme.typography.bodyMedium)
            Text(
              text = if (drawersEnabled) strings.drawersVisibleSub else strings.drawersRemovedSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = drawersEnabled,
            onCheckedChange = { checked ->
              onUpdateItem { cur ->
                val nextStyle = if (checked && cur.style == CabinetStyle.OPEN_SHELVING) {
                  cur.kind.defaultStyle.takeIf { it != CabinetStyle.OPEN_SHELVING } ?: CabinetStyle.HANDLELESS_GOLA
                } else if (!checked && (!hasDoors || !cur.showDoors)) {
                  CabinetStyle.OPEN_SHELVING
                } else cur.style
                cur.copy(showDrawers = checked, style = nextStyle)
              }
            },
            modifier = Modifier.testTag("toggle_drawers_switch"),
          )
        }
      }

      val frontsVisible = (hasDoors && item.showDoors && item.style != CabinetStyle.OPEN_SHELVING) ||
        (hasDrawers && item.showDrawers && item.style != CabinetStyle.OPEN_SHELVING)
      if (frontsVisible) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.metalHandlesLabel, style = MaterialTheme.typography.bodyMedium)
            Text(
              text = if (item.showHandles) strings.handlesSteelSub else strings.handlesNoneSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = item.showHandles,
            onCheckedChange = { checked ->
              onUpdateItem { cur -> cur.copy(showHandles = checked) }
            },
            modifier = Modifier.testTag("toggle_handles_switch"),
          )
        }
      }
    }
  }
}

