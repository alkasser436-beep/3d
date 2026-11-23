package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LebaneseGraniteSlab
import com.example.data.LebanonCatalog
import com.example.model.KitchenSpecs
import com.example.model.RoomDesign
import com.example.ui.i18n.LocalAppStrings

@Composable
fun GraniteStonePanel(
  design: RoomDesign,
  onSelectSlab: (LebaneseGraniteSlab) -> Unit,
  onUpdateSpecs: ((KitchenSpecs) -> KitchenSpecs) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val specs = design.kitchenSpecs
  val activeMat = design.countertopMaterial

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(Icons.Outlined.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Column {
          Text(
            text = strings.graniteHeaderTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = strings.graniteHeaderSub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    SectionTitle(
      title = "${strings.slabThicknessLabel} & ${strings.slabFinishLabel}",
      subtitle = strings.slabThicknessSub,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf(2 to "2 cm", 3 to "3 cm", 4 to "4 cm").forEach { (cm, label) ->
        FilterChip(
          selected = specs.countertopThicknessCm == cm,
          onClick = { onUpdateSpecs { it.copy(countertopThicknessCm = cm) } },
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("granite_thickness_${cm}cm"),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = specs.countertopPolished,
        onClick = { onUpdateSpecs { it.copy(countertopPolished = true) } },
        label = { Text(strings.polishedFinish) },
        modifier = Modifier
          .weight(1f)
          .testTag("granite_polished"),
      )
      FilterChip(
        selected = !specs.countertopPolished,
        onClick = { onUpdateSpecs { it.copy(countertopPolished = false) } },
        label = { Text(strings.honedFinish) },
        modifier = Modifier
          .weight(1f)
          .testTag("granite_matte"),
      )
    }

    SectionTitle(
      title = strings.availableSlabsTitle,
      subtitle = "${strings.activeMaterialPrefix} ${activeMat.finish.label} (${activeMat.hexColor})",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      LebanonCatalog.graniteSlabs.forEach { slab ->
        val selected = activeMat.finish == slab.finish &&
          activeMat.hexColor.equals(slab.hexColor, ignoreCase = true)
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.surface
            },
          ),
          border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectSlab(slab) }
            .testTag("granite_slab_${slab.id}"),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(parseHexColorSafe(slab.hexColor))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              if (selected) {
                Icon(
                  Icons.Outlined.Check,
                  contentDescription = "Selected granite slab",
                  tint = if (isDarkHex(slab.hexColor)) Color.White else Color.Black,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = slab.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                ) {
                  Text(
                    text = slab.categoryTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              Spacer(Modifier.height(2.dp))
              Text(
                text = slab.arabicOriginNote,
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

private fun isDarkHex(hex: String): Boolean {
  val c = parseHexColorSafe(hex)
  val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
  return lum < 0.52f
}
