package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SavedDesignSummary
import com.example.viewmodel.AiRenderState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiRenderResultDialog(
  state: AiRenderState,
  onCancelOrDismiss: () -> Unit,
  onRetry: () -> Unit,
  onSaveToPhotos: () -> Unit,
  onShare: () -> Unit,
) {
  if (state is AiRenderState.Idle) return

  var showOriginal3D by remember(state) { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onCancelOrDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .padding(12.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              Icons.Outlined.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "Photorealistic AI Interior",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = when (state) {
                  is AiRenderState.Loading -> "Rendering realistic materials & lighting (30–60s)..."
                  is AiRenderState.Success -> "Generated directly from your 3D room layout"
                  is AiRenderState.Error -> "Could not complete AI render"
                  AiRenderState.Idle -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(onClick = onCancelOrDismiss, modifier = Modifier.testTag("ai_dialog_close")) {
            Icon(Icons.Outlined.Close, contentDescription = "Close AI render dialog")
          }
        }

        when (state) {
          is AiRenderState.Loading -> {
            val shimmerTransition = rememberInfiniteTransition(label = "ai_shimmer")
            val shift by shimmerTransition.animateFloat(
              initialValue = -0.3f,
              targetValue = 1.3f,
              animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
              ),
              label = "shimmer_shift",
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center,
            ) {
              if (state.sourceBitmap != null) {
                Image(
                  bitmap = state.sourceBitmap.asImageBitmap(),
                  contentDescription = "Captured 3D room layout",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.linearGradient(
                        colors = listOf(
                          Color.Black.copy(alpha = 0.45f),
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                          Color.Black.copy(alpha = 0.45f),
                        ),
                        start = Offset(shift * 600f - 200f, 0f),
                        end = Offset(shift * 600f + 200f, 400f),
                      ),
                    ),
                )
              }
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
              ) {
                Column(
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(34.dp))
                  Text(
                    text = "Transforming 3D layout to real photo...",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                  )
                }
              }
            }

            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            OutlinedButton(
              onClick = onCancelOrDismiss,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ai_cancel_button"),
            ) {
              Text("Cancel Render")
            }
          }

          is AiRenderState.Success -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              FilterChip(
                selected = !showOriginal3D,
                onClick = { showOriginal3D = false },
                label = { Text("AI Real Photo") },
                leadingIcon = {
                  Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("ai_tab_photo"),
              )
              FilterChip(
                selected = showOriginal3D,
                onClick = { showOriginal3D = true },
                label = { Text("3D Plan") },
                leadingIcon = {
                  Icon(Icons.Outlined.Compare, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("ai_tab_3d"),
              )
            }

            val displayBitmap = if (showOriginal3D) state.sourceBitmap else state.renderedBitmap
            val ratio = (displayBitmap.width.toFloat() / displayBitmap.height.coerceAtLeast(1)).coerceIn(0.65f, 2.0f)
            Image(
              bitmap = displayBitmap.asImageBitmap(),
              contentDescription = if (showOriginal3D) "Original 3D viewport layout" else "AI photorealistic room photograph",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp)),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = onSaveToPhotos,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_save_button"),
              ) {
                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save")
              }
              OutlinedButton(
                onClick = onShare,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_share_button"),
              ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Share")
              }
              OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("ai_rerender_button"),
              ) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Regenerate AI render", modifier = Modifier.size(18.dp))
              }
            }
          }

          is AiRenderState.Error -> {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
              shape = RoundedCornerShape(14.dp),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Icon(
                  Icons.Outlined.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = state.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("ai_retry_button"),
              ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Try Again")
              }
              OutlinedButton(
                onClick = onCancelOrDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp),
              ) {
                Text("Close")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SnapshotPreviewDialog(
  pngBytes: ByteArray,
  onSaveToPhotos: () -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit,
) {
  val bitmap = remember(pngBytes) {
    BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)?.asImageBitmap()
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text("High-Resolution Snapshot", style = MaterialTheme.typography.titleMedium)
            Text(
              "1920px wide PNG render",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "Close preview")
          }
        }

        if (bitmap != null) {
          val ratio = (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)).coerceIn(0.6f, 2.2f)
          Image(
            bitmap = bitmap,
            contentDescription = "Rendered 3D room snapshot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .aspectRatio(ratio)
              .clip(RoundedCornerShape(14.dp)),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onSaveToPhotos,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("save_snapshot_button"),
          ) {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save PNG")
          }
          OutlinedButton(
            onClick = onShare,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("share_snapshot_button"),
          ) {
            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Share")
          }
        }
      }
    }
  }
}

@Composable
fun ProjectsDialog(
  currentName: String,
  savedDesigns: List<SavedDesignSummary>,
  onSaveCurrent: (String) -> Unit,
  onSaveAsNew: (String) -> Unit,
  onLoadDesign: (String) -> Unit,
  onDeleteDesign: (String) -> Unit,
  onExportJson: () -> Unit,
  onImportJson: () -> Unit,
  onDismiss: () -> Unit,
) {
  var nameInput by remember(currentName) { mutableStateOf(currentName) }
  val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(16.dp),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Saved Designs & Backup", style = MaterialTheme.typography.titleMedium)
          IconButton(onClick = onDismiss) {
            Icon(Icons.Outlined.Close, contentDescription = "Close projects dialog")
          }
        }

        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it.take(40) },
          label = { Text("Design Name") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("project_name_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onSaveCurrent(nameInput) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("save_project_button"),
          ) {
            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save")
          }
          OutlinedButton(
            onClick = { onSaveAsNew(nameInput) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("save_as_new_project_button"),
          ) {
            Text("Save As Copy")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onExportJson,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("export_json_button"),
          ) {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Export JSON")
          }
          OutlinedButton(
            onClick = onImportJson,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("import_json_button"),
          ) {
            Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Import JSON")
          }
        }

        HorizontalDivider()

        Text(
          text = "Local Projects (${savedDesigns.size})",
          style = MaterialTheme.typography.titleSmall,
        )

        if (savedDesigns.isEmpty()) {
          Text(
            text = "No saved projects yet. Tap Save above to store your current design.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(savedDesigns, key = { it.id }) { item ->
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                onClick = { onLoadDesign(item.id) },
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.name,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = "${item.roomType.displayName} • ${item.dimensionsSummary} • ${dateFmt.format(Date(item.updatedAtMillis))}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  IconButton(onClick = { onDeleteDesign(item.id) }) {
                    Icon(
                      Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete saved project",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
