package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.DesignTemplates
import com.example.model.MaterialCategory
import com.example.model.MaterialFinish
import com.example.model.RoomDesign
import com.example.model.SelectionTarget
import com.example.model.SurfaceMaterial

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaterialsPanel(
  design: RoomDesign,
  selectedTarget: SelectionTarget,
  onSelectTarget: (SelectionTarget) -> Unit,
  onApplyMaterial: (SurfaceMaterial, applyAsSecondary: Boolean) -> Unit,
  onUpdateFinishOnly: (MaterialFinish) -> Unit,
  onUpdateHexOnly: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedFurniture = (selectedTarget as? SelectionTarget.Furniture)?.let { sel ->
    design.furniture.firstOrNull { it.id == sel.id }
  }
  var applyToSecondaryPart by remember(selectedFurniture?.id) { mutableStateOf(false) }

  val currentMaterial = when (selectedTarget) {
    SelectionTarget.Walls -> design.wallMaterial
    SelectionTarget.Floor -> design.floorMaterial
    SelectionTarget.Ceiling -> design.ceilingMaterial
    SelectionTarget.AllCountertops -> design.countertopMaterial
    is SelectionTarget.Furniture -> {
      if (applyToSecondaryPart && selectedFurniture?.secondaryMaterial != null) {
        selectedFurniture.secondaryMaterial
      } else {
        selectedFurniture?.material ?: design.wallMaterial
      }
    }
  }

  var activeCategory by remember(currentMaterial.finish.category) {
    mutableStateOf(currentMaterial.finish.category)
  }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SectionTitle(
      title = "Apply Material To",
      subtitle = "Tap in 3D or choose a surface below",
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val targets = buildList {
        add(SelectionTarget.Walls to "Walls")
        add(SelectionTarget.Floor to "Floor")
        add(SelectionTarget.Ceiling to "Ceiling")
        add(SelectionTarget.AllCountertops to "Countertops")
        if (selectedFurniture != null) {
          add(SelectionTarget.Furniture(selectedFurniture.id) to selectedFurniture.label)
        }
      }
      targets.forEach { (t, label) ->
        FilterChip(
          selected = selectedTarget == t,
          onClick = { onSelectTarget(t) },
          label = { Text(label, style = MaterialTheme.typography.labelMedium) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier.testTag("mat_target_${label.lowercase().replace(" ", "_")}"),
        )
      }
    }

    if (selectedFurniture != null && selectedFurniture.secondaryMaterial != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = !applyToSecondaryPart,
          onClick = { applyToSecondaryPart = false },
          label = { Text("Cabinets / Frame") },
          modifier = Modifier.weight(1f),
        )
        FilterChip(
          selected = applyToSecondaryPart,
          onClick = { applyToSecondaryPart = true },
          label = { Text("Top / Bedding") },
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Finish Category Tabs
    SectionTitle(
      title = "Real-World Finishes",
      subtitle = "Active: ${currentMaterial.finish.label} (${currentMaterial.hexColor})",
    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      MaterialCategory.entries.forEach { cat ->
        FilterChip(
          selected = activeCategory == cat,
          onClick = { activeCategory = cat },
          label = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
          modifier = Modifier.testTag("mat_cat_${cat.name.lowercase()}"),
        )
      }
    }

    // Paint Gloss Selector when Paint is active
    if (activeCategory == MaterialCategory.PAINT) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          MaterialFinish.MATTE_PAINT to "Matte",
          MaterialFinish.SATIN_PAINT to "Satin",
          MaterialFinish.GLOSSY_PAINT to "Glossy",
        ).forEach { (fin, label) ->
          FilterChip(
            selected = currentMaterial.finish == fin,
            onClick = { onUpdateFinishOnly(fin) },
            label = { Text(label) },
            modifier = Modifier
              .weight(1f)
              .testTag("paint_gloss_${label.lowercase()}"),
          )
        }
      }
    }

    // Curated Swatch Grid
    val swatches = DesignTemplates.swatchesByCategory[activeCategory].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      swatches.chunked(2).forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          row.forEach { sw ->
            val isSelected = currentMaterial.finish == sw.finish &&
              currentMaterial.hexColor.equals(sw.hexColor, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  onApplyMaterial(SurfaceMaterial(sw.finish, sw.hexColor), applyToSecondaryPart)
                }
                .testTag("swatch_${sw.name.lowercase().replace(" ", "_")}"),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(parseHexColorSafe(sw.hexColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  if (isSelected) {
                    Icon(
                      Icons.Outlined.Check,
                      contentDescription = "Selected swatch",
                      tint = if (isDarkColor(sw.hexColor)) Color.White else Color.Black,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = sw.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                  )
                  Text(
                    text = sw.hexColor,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
          if (row.size == 1) Spacer(Modifier.weight(1f))
        }
      }
    }

    // Custom Hex Code & Interactive HSV Picker
    SectionTitle(
      title = "Exact Hex Color Matcher",
      subtitle = "Enter any paint brand hex code or pick visually",
    )

    HexColorPickerBox(
      currentHex = currentMaterial.hexColor,
      onHexSelected = { hex ->
        if (applyToSecondaryPart && selectedFurniture != null) {
          onApplyMaterial(currentMaterial.copy(hexColor = hex), true)
        } else {
          onUpdateHexOnly(hex)
        }
      },
    )

    Spacer(Modifier.height(12.dp))
  }
}

@Composable
private fun HexColorPickerBox(
  currentHex: String,
  onHexSelected: (String) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var hexInput by remember(currentHex) { mutableStateOf(currentHex.uppercase()) }

  val initialHsv = remember(currentHex) {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(parseHexColorSafe(currentHex).toArgb(), hsv)
    hsv
  }
  var hue by remember { mutableFloatStateOf(initialHsv[0]) }
  var sat by remember { mutableFloatStateOf(initialHsv[1]) }
  var value by remember { mutableFloatStateOf(initialHsv[2]) }

  LaunchedEffect(currentHex) {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(parseHexColorSafe(currentHex).toArgb(), hsv)
    hue = hsv[0]
    sat = hsv[1]
    value = hsv[2]
    hexInput = currentHex.uppercase()
  }

  fun emitHsv(h: Float, s: Float, v: Float) {
    val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    val hex = String.format("#%06X", 0xFFFFFF and argb)
    hexInput = hex
    onHexSelected(hex)
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // 2D Saturation/Value Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(132.dp)
        .clip(RoundedCornerShape(14.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        .pointerInput(hue) {
          detectTapGestures { offset ->
            sat = (offset.x / size.width).coerceIn(0f, 1f)
            value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
            emitHsv(hue, sat, value)
          }
        }
        .pointerInput(hue) {
          detectDragGestures { change, _ ->
            change.consume()
            sat = (change.position.x / size.width).coerceIn(0f, 1f)
            value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
            emitHsv(hue, sat, value)
          }
        },
    ) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        drawRect(
          brush = Brush.horizontalGradient(listOf(Color.White, hueColor)),
        )
        drawRect(
          brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
        )
        val cx = sat * size.width
        val cy = (1f - value) * size.height
        drawCircle(
          color = Color.White,
          radius = 9.dp.toPx(),
          center = Offset(cx, cy),
          style = Stroke(width = 2.5.dp.toPx()),
        )
        drawCircle(
          color = Color.Black.copy(alpha = 0.45f),
          radius = 10.5.dp.toPx(),
          center = Offset(cx, cy),
          style = Stroke(width = 1.dp.toPx()),
        )
      }
    }

    // Hue Slider
    Text("Hue", style = MaterialTheme.typography.bodySmall)
    Slider(
      value = hue,
      onValueChange = { h ->
        hue = h
        emitHsv(hue, sat, value)
      },
      valueRange = 0f..360f,
      modifier = Modifier.testTag("color_hue_slider"),
    )

    // Hex Code Text Input + Preview Swatch
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(parseHexColorSafe(currentHex))
          .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
      )
      OutlinedTextField(
        value = hexInput,
        onValueChange = { raw ->
          hexInput = raw.uppercase().take(7)
          val clean = hexInput.removePrefix("#")
          if (clean.matches(Regex("[0-9A-F]{6}"))) {
            onHexSelected("#$clean")
          }
        },
        label = { Text("Hex Color Code") },
        leadingIcon = { Icon(Icons.Outlined.Colorize, contentDescription = null) },
        singleLine = true,
        textStyle = MaterialTheme.typography.labelSmall,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("hex_color_input"),
      )
    }
  }
}

private fun isDarkColor(hex: String): Boolean {
  val c = parseHexColorSafe(hex)
  val lum = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
  return lum < 0.52f
}
