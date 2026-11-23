package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Window
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AluminumFinish
import com.example.model.KitchenSpecs
import com.example.model.ProfileHandleType
import com.example.ui.i18n.LocalAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AluminumHardwarePanel(
  specs: KitchenSpecs,
  onUpdateSpecs: ((KitchenSpecs) -> KitchenSpecs) -> Unit,
  onApplyAluminumGlassFrames: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(Icons.Outlined.Hardware, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Column {
          Text(
            text = strings.aluminumHeaderTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = strings.aluminumHeaderSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    SectionTitle(
      title = strings.profileHandleTitle,
      subtitle = strings.profileHandleSub,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ProfileHandleType.entries.forEach { handle ->
        val selected = specs.handleType == handle
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surface
            },
          ),
          border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onUpdateSpecs { it.copy(handleType = handle) } }
            .testTag("handle_type_${handle.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  Icons.Outlined.LinearScale,
                  contentDescription = null,
                  tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
              Column {
                Text(
                  text = handle.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = handle.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (selected) {
              Icon(Icons.Outlined.Check, contentDescription = "Selected handle", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }

    SectionTitle(
      title = strings.aluminumColorTitle,
      subtitle = strings.aluminumColorSub,
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AluminumFinish.entries.forEach { fin ->
        val selected = specs.aluminumFinish == fin
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
          ),
          modifier = Modifier
            .clickable { onUpdateSpecs { it.copy(aluminumFinish = fin) } }
            .testTag("alum_finish_${fin.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(parseHexColorSafe(fin.hexColor))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Text(
              text = fin.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
          }
        }
      }
    }

    SectionTitle(
      title = strings.hardwareAccessoriesTitle,
      subtitle = strings.hardwareAccessoriesSub,
    )

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      shape = RoundedCornerShape(14.dp),
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
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.waterproofPlinthLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
              text = "10cm • ${specs.aluminumFinish.label}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = specs.aluminumPlinth,
            onCheckedChange = { c -> onUpdateSpecs { it.copy(aluminumPlinth = c) } },
            modifier = Modifier.testTag("alum_plinth_switch"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
              Text(strings.underCabLedLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
              Text(
                text = strings.underCabLedSub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = specs.underCabinetLed,
            onCheckedChange = { c -> onUpdateSpecs { it.copy(underCabinetLed = c) } },
            modifier = Modifier.testTag("alum_led_switch"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(strings.softCloseLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
              text = strings.softCloseSub,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = specs.softCloseHardware,
            onCheckedChange = { c -> onUpdateSpecs { it.copy(softCloseHardware = c) } },
            modifier = Modifier.testTag("soft_close_switch"),
          )
        }
      }
    }

    FilledTonalButton(
      onClick = onApplyAluminumGlassFrames,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("apply_alum_glass_frames_button"),
    ) {
      Icon(Icons.Outlined.Window, contentDescription = null)
      Spacer(Modifier.width(8.dp))
      Text(strings.applyGlassFramesBtn)
    }

    Spacer(Modifier.height(12.dp))
  }
}
