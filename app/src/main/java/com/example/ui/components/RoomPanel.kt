package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DesignTemplates
import com.example.data.RoomTemplate
import com.example.model.LengthUnit
import com.example.model.OpeningType
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.example.model.WallId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomPanel(
  design: RoomDesign,
  onUnitChange: (LengthUnit) -> Unit,
  onRoomTypeChange: (RoomType) -> Unit,
  onDimensionsChange: (width: Float?, length: Float?, height: Float?) -> Unit,
  onAddOpening: (OpeningType, WallId) -> Unit,
  onUpdateOpening: (id: String, wall: WallId?, offset: Float?, width: Float?) -> Unit,
  onRemoveOpening: (String) -> Unit,
  onApplyTemplate: (RoomTemplate) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Unit & Room Type
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Units",
        style = MaterialTheme.typography.titleSmall,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.testTag("unit_segmented")) {
        LengthUnit.entries.forEachIndexed { idx, unit ->
          SegmentedButton(
            selected = design.unit == unit,
            onClick = { onUnitChange(unit) },
            shape = SegmentedButtonDefaults.itemShape(idx, LengthUnit.entries.size),
            modifier = Modifier.testTag("unit_${unit.name.lowercase()}"),
          ) {
            Text(unit.suffix, style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Room Type", style = MaterialTheme.typography.titleSmall)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        RoomType.entries.forEach { type ->
          val selected = design.roomType == type
          val icon = when (type) {
            RoomType.KITCHEN -> Icons.Outlined.Kitchen
            RoomType.BEDROOM -> Icons.Outlined.Bed
            RoomType.LIVING_ROOM -> Icons.Outlined.Weekend
          }
          FilterChip(
            selected = selected,
            onClick = { onRoomTypeChange(type) },
            label = {
              Text(
                text = when (type) {
                  RoomType.KITCHEN -> "Kitchen"
                  RoomType.BEDROOM -> "Bedroom"
                  RoomType.LIVING_ROOM -> "Living"
                },
                style = MaterialTheme.typography.labelMedium,
              )
            },
            leadingIcon = {
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("room_type_${type.name.lowercase()}"),
          )
        }
      }
    }

    // Room Dimensions
    SectionTitle(
      title = "Room Dimensions",
      subtitle = "3D walls, floor & ceiling update live as you adjust",
    )

    DimensionSliderInput(
      label = "Width (Left ↔ Right)",
      valueMeters = design.widthMeters,
      rangeMeters = 2.0f..12.0f,
      unit = design.unit,
      onValueMetersChange = { onDimensionsChange(it, null, null) },
      testTagPrefix = "room_width",
    )

    DimensionSliderInput(
      label = "Length (Back ↔ Front)",
      valueMeters = design.lengthMeters,
      rangeMeters = 2.0f..12.0f,
      unit = design.unit,
      onValueMetersChange = { onDimensionsChange(null, it, null) },
      testTagPrefix = "room_length",
    )

    DimensionSliderInput(
      label = "Wall Height",
      valueMeters = design.wallHeightMeters,
      rangeMeters = 2.2f..4.5f,
      unit = design.unit,
      onValueMetersChange = { onDimensionsChange(null, null, it) },
      testTagPrefix = "room_height",
    )

    // Doors & Windows
    SectionTitle(
      title = "Doors & Windows",
      subtitle = "Real wall cutouts with natural daylight panes",
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedButton(
        onClick = { onAddOpening(OpeningType.WINDOW, WallId.NORTH) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("add_window_button"),
      ) {
        Icon(Icons.Outlined.Window, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("+ Window")
      }
      OutlinedButton(
        onClick = { onAddOpening(OpeningType.DOOR, WallId.EAST) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("add_door_button"),
      ) {
        Icon(Icons.Outlined.DoorFront, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("+ Door")
      }
    }

    design.openings.forEach { op ->
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (op.type == OpeningType.DOOR) Icons.Outlined.DoorFront else Icons.Outlined.Window,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "${op.type.label} (${formatDimension(op.widthMeters, design.unit)}${design.unit.suffix})",
                style = MaterialTheme.typography.labelLarge,
              )
            }
            IconButton(
              onClick = { onRemoveOpening(op.id) },
              modifier = Modifier
                .size(36.dp)
                .testTag("remove_opening_${op.id}"),
            ) {
              Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Remove ${op.type.label}",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }

          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WallId.entries.forEach { w ->
              FilterChip(
                selected = op.wall == w,
                onClick = { onUpdateOpening(op.id, w, null, null) },
                label = { Text(w.label.removeSuffix(" Wall"), style = MaterialTheme.typography.labelSmall) },
              )
            }
          }

          val span = if (op.wall == WallId.NORTH || op.wall == WallId.SOUTH) {
            design.widthMeters
          } else design.lengthMeters
          val maxOffset = ((span - op.widthMeters) * 0.5f - 0.15f).coerceAtLeast(0.1f)

          Text(
            text = "Position Along Wall",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Slider(
            value = op.offsetMeters.coerceIn(-maxOffset, maxOffset),
            onValueChange = { onUpdateOpening(op.id, null, it, null) },
            valueRange = -maxOffset..maxOffset,
          )
        }
      }
    }

    // Preset Room Templates
    SectionTitle(
      title = "Preset Room Templates",
      subtitle = "1-tap starter designs you can customize freely",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      DesignTemplates.templates.forEach { tpl ->
        val isMatch = tpl.roomType == design.roomType
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (isMatch) {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surface
            },
          ),
          border = BorderStroke(
            width = 1.dp,
            color = if (isMatch) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onApplyTemplate(tpl) }
            .testTag("template_${tpl.id}"),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tpl.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Spacer(Modifier.height(2.dp))
              Text(
                text = tpl.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
              onClick = { onApplyTemplate(tpl) },
              contentPadding = ButtonDefaults.TextButtonContentPadding,
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Load", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
    Spacer(Modifier.height(12.dp))
  }
}
