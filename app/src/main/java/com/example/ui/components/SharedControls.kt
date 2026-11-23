package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.LengthUnit
import java.util.Locale
import kotlin.math.roundToInt

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFFD8D4CC)): Color {
  return runCatching {
    val clean = hex.trim().removePrefix("#")
    Color(android.graphics.Color.parseColor("#$clean"))
  }.getOrDefault(fallback)
}

fun formatDimension(meters: Float, unit: LengthUnit): String = when (unit) {
  LengthUnit.METERS -> String.format(Locale.US, "%.2f", meters)
  LengthUnit.CENTIMETERS -> "${(meters * 100f).roundToInt()}"
}

@Composable
fun SectionTitle(
  title: String,
  subtitle: String? = null,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    if (subtitle != null) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(top = 6.dp),
      color = MaterialTheme.colorScheme.outlineVariant,
    )
  }
}

@Composable
fun DimensionSliderInput(
  label: String,
  valueMeters: Float,
  rangeMeters: ClosedFloatingPointRange<Float>,
  unit: LengthUnit,
  onValueMetersChange: (Float) -> Unit,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  var text by remember(unit) { mutableStateOf(formatDimension(valueMeters, unit)) }

  LaunchedEffect(valueMeters, unit) {
    val parsed = text.toFloatOrNull()?.let {
      if (unit == LengthUnit.CENTIMETERS) it / 100f else it
    }
    if (parsed == null || kotlin.math.abs(parsed - valueMeters) > 0.015f) {
      text = formatDimension(valueMeters, unit)
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      OutlinedTextField(
        value = text,
        onValueChange = { raw ->
          text = raw
          val num = raw.toFloatOrNull()
          if (num != null) {
            val meters = if (unit == LengthUnit.CENTIMETERS) num / 100f else num
            if (meters in rangeMeters) {
              onValueMetersChange(meters)
            }
          }
        },
        singleLine = true,
        suffix = {
          Text(
            text = unit.suffix,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        textStyle = MaterialTheme.typography.labelSmall,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Decimal,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            val num = text.toFloatOrNull()
            if (num != null) {
              val meters = (if (unit == LengthUnit.CENTIMETERS) num / 100f else num)
                .coerceIn(rangeMeters.start, rangeMeters.endInclusive)
              onValueMetersChange(meters)
              text = formatDimension(meters, unit)
            } else {
              text = formatDimension(valueMeters, unit)
            }
            focusManager.clearFocus()
          },
        ),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
          .width(104.dp)
          .testTag("${testTagPrefix}_input"),
      )
    }
    Slider(
      value = valueMeters.coerceIn(rangeMeters.start, rangeMeters.endInclusive),
      onValueChange = onValueMetersChange,
      valueRange = rangeMeters,
      colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("${testTagPrefix}_slider"),
    )
  }
}
