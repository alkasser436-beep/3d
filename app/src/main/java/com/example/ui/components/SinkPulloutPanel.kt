package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Countertops
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FaucetStyle
import com.example.model.KitchenSpecs
import com.example.model.LebaneseOrganizer
import com.example.model.SinkType
import com.example.ui.i18n.LocalAppStrings

@Composable
fun SinkPulloutPanel(
  specs: KitchenSpecs,
  onUpdateSpecs: ((KitchenSpecs) -> KitchenSpecs) -> Unit,
  onToggleOrganizer: (LebaneseOrganizer) -> Unit,
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
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text(
            text = strings.sinkHeaderTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = strings.sinkHeaderSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    SectionTitle(
      title = strings.sinkHeaderTitle,
      subtitle = strings.sinkHeaderSub,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      SinkType.entries.forEach { sink ->
        val selected = specs.sinkType == sink
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
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
            .clickable { onUpdateSpecs { it.copy(sinkType = sink) } }
            .testTag("sink_type_${sink.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  if (sink.isBlackGranite) Color(0xFF24262B) else Color(0xFFC8CDD4),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Outlined.Countertops,
                contentDescription = null,
                tint = if (sink.isBlackGranite) Color.White else Color(0xFF1E293B),
                modifier = Modifier.size(18.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(sink.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
              Text(
                text = sink.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (selected) {
              Icon(Icons.Outlined.Check, contentDescription = "Selected sink", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    }

    SectionTitle(
      title = strings.faucetHeaderTitle,
      subtitle = strings.faucetHeaderSub,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      FaucetStyle.entries.forEach { faucet ->
        val selected = specs.faucetStyle == faucet
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
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
            .clickable { onUpdateSpecs { it.copy(faucetStyle = faucet) } }
            .testTag("faucet_${faucet.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(if (faucet.isBlack) Color(0xFF22252A) else Color(0xFFC8CDD4)),
              )
              Text(faucet.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            }
            if (selected) {
              Icon(Icons.Outlined.Check, contentDescription = "Selected faucet", tint = MaterialTheme.colorScheme.secondary)
            }
          }
        }
      }
    }

    SectionTitle(
      title = "${strings.pulloutsHeaderTitle} (${specs.organizers.size})",
      subtitle = strings.pulloutsHeaderSub,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      LebaneseOrganizer.entries.forEach { org ->
        val checked = org in specs.organizers
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (checked) {
              MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.32f)
            } else {
              MaterialTheme.colorScheme.surface
            },
          ),
          border = BorderStroke(
            width = if (checked) 1.5.dp else 1.dp,
            color = if (checked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleOrganizer(org) }
            .testTag("organizer_${org.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = checked,
              onCheckedChange = { onToggleOrganizer(org) },
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(org.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
              Text(
                text = "${org.category} • Standard width: ${org.sizeSpec}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    Spacer(Modifier.height(12.dp))
  }
}
