package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.model.LightingPreset
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "AiPhotoRender"

class PhotorealisticRenderRepository {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-nano-banana-2.1",
      generationConfig = generationConfig {
        responseModalities = listOf(ResponseModality.IMAGE)
      },
    )
  }

  suspend fun decodeAndPrepareInputBitmap(pngBytes: ByteArray): Bitmap = withContext(Dispatchers.Default) {
    require(pngBytes.size > 128) { "3D snapshot was empty." }
    val options = BitmapFactory.Options().apply {
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val raw = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, options)
      ?: error("Could not decode 3D viewport snapshot.")
    require(raw.width >= 16 && raw.height >= 16) { "3D viewport snapshot dimensions were invalid." }

    val maxDim = max(raw.width, raw.height)
    val scaled = if (maxDim <= 1024) {
      raw
    } else {
      val scale = 1024f / maxDim
      val targetW = (raw.width * scale).roundToInt().coerceAtLeast(1)
      val targetH = (raw.height * scale).roundToInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(raw, targetW, targetH, true).also {
        if (it !== raw) raw.recycle()
      }
    }
    if (scaled.config == Bitmap.Config.ARGB_8888) {
      scaled
    } else {
      scaled.copy(Bitmap.Config.ARGB_8888, false).also {
        if (it !== scaled) scaled.recycle()
      }
    }
  }

  suspend fun renderPhotorealisticPhoto(
    viewportBitmap: Bitmap,
    design: RoomDesign,
  ): Bitmap {
    val prompt = buildArchitecturalPrompt(design)
    return withTimeout(60_000L) {
      val response = model.generateContent(
        content {
          image(viewportBitmap)
          text(prompt)
        },
      )
      val candidate = response.candidates.firstOrNull()
      val imagePart = candidate?.content?.parts?.filterIsInstance<ImagePart>()?.firstOrNull()
      if (imagePart?.image != null) {
        return@withTimeout imagePart.image
      }

      val blockReason = response.promptFeedback?.blockReason?.name
      val finishReason = candidate?.finishReason?.name
      val textHint = candidate?.content?.parts
        ?.filterIsInstance<TextPart>()
        ?.joinToString(" ") { it.text }
        ?.take(160)
        ?.trim()
      val diagnostic = listOfNotNull(
        blockReason?.let { "Blocked: $it" },
        finishReason?.let { "Finish: $it" },
        textHint?.takeIf { it.isNotEmpty() },
      ).joinToString(" | ")
      Log.e(TAG, "AI image response missing ImagePart. $diagnostic")
      error(
        if (diagnostic.isNotBlank()) {
          "AI model returned no image ($diagnostic)."
        } else {
          "The AI model returned no image. Try switching camera to Eye-Level and retry."
        },
      )
    }
  }

  suspend fun bitmapToPngBytes(bitmap: Bitmap): ByteArray = withContext(Dispatchers.IO) {
    ByteArrayOutputStream().use { stream ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)
      stream.toByteArray()
    }
  }

  private fun buildArchitecturalPrompt(design: RoomDesign): String {
    val lightingDesc = when (design.lightingPreset) {
      LightingPreset.DAYLIGHT ->
        "crisp natural morning daylight streaming softly through any windows, balanced bounce light, realistic ambient shadows"
      LightingPreset.GOLDEN_HOUR ->
        "warm late-afternoon golden sunlight rays entering through the window, soft warm architectural bounce shadows"
      LightingPreset.INDOOR_SPOTLIGHTS ->
        "warm 3000K architectural recessed ceiling spotlights and warm glowing under-cabinet LED strips casting dramatic soft light onto walls and counters"
    }

    val roomSpecificDetails = when (design.roomType) {
      RoomType.KITCHEN ->
        "Ultra-realistic modern architectural kitchen interior: realistic running-bond ceramic subway tile backsplash reflecting warm under-cabinet LED strips, sleek handleless cabinetry with subtle bevels and real hardware, realistic stone quartz/marble veining on countertops, authentic stainless faucet and sink basin, matte ceramic floor tiles with natural daylight reflection, and genuine outdoor sky/courtyard depth visible through window glass."
      RoomType.BEDROOM ->
        "Ultra-realistic modern architectural bedroom interior: soft natural linen duvet wrinkles and plush pillows on the bed, authentic oak/walnut wood grain with subtle satin sheen on furniture and floorboards, warm bedside lamp glow, realistic fabric rug pile, and natural daylight from windows."
      RoomType.LIVING_ROOM ->
        "Ultra-realistic modern architectural living room interior: tactile bouclé or linen sofa cushions with natural folds, authentic hardwood floor planks with realistic specular sheen, recessed ceiling spotlights, natural indoor shadows, and subtle styled tabletop realism."
    }

    val itemsSummary = design.furniture.joinToString(", ") { item ->
      val mods = buildList {
        add(item.style.label)
        if (!item.showDoors) add("no doors / open shelves")
        if (!item.showDrawers) add("no drawers")
        if (item.showHandles) add("with brushed metal handles") else add("handleless")
      }.joinToString(", ")
      "${item.label} (${item.material.finish.label} ${item.material.hexColor}, $mods)"
    }

    val ks = design.kitchenSpecs
    val kitchenHardwarePrompt = if (design.roomType == RoomType.KITCHEN) {
      val orgText = ks.organizers.joinToString(", ") { it.label }.ifEmpty { "clean minimalist counters" }
      "Kitchen Hardware & Fit-Out Specs (Lebanon standard): Starwood ${ks.mfcBoardGrade.code} (${ks.mfcBoardGrade.title}) melamine cabinet boards, " +
        "${ks.handleType.label} in ${ks.aluminumFinish.label}, " +
        "${ks.countertopThicknessCm}cm ${if (ks.countertopPolished) "polished" else "honed matte"} granite/stone slab edge, " +
        "${ks.sinkType.label} with ${ks.faucetStyle.label}, " +
        (if (ks.aluminumPlinth) "${ks.aluminumFinish.label} waterproof aluminum plinth toe-kick, " else "") +
        (if (ks.underCabinetLed) "warm 3000K aluminum-channel under-cabinet LED strip lighting, " else "") +
        "equipped with: $orgText."
    } else ""

    return String.format(
      Locale.US,
      """
      Transform this 3D architectural room viewport image into a genuine, indistinguishable-from-real-life interior photograph captured on a full-frame DSLR with a 24mm architectural tilt-shift lens.
      Strictly preserve the exact camera angle, perspective, room geometry (%.1fm x %.1fm, %.1fm ceiling), wall/window/door positions, and exact furniture/cabinet layout and proportions shown in the input image. Do NOT add or move walls, windows, or major furniture pieces.
      Materials specified:
      - Walls: %s (%s)
      - Floor: %s (%s)
      - Ceiling: %s (%s)
      - Countertops/Granite: %s (%s)
      - Furniture/Cabinets: %s
      %s
      Lighting & Atmosphere: %s.
      Details: %s
      Replace all flat 3D polygons, smooth shading, and grid artifacts with real-world micro-textures, physically accurate soft global illumination, crisp contact shadows, subtle glass and metal reflections, and authentic photographic dynamic range. No CGI look, no text, no watermarks.
      """.trimIndent(),
      design.widthMeters,
      design.lengthMeters,
      design.wallHeightMeters,
      design.wallMaterial.finish.label,
      design.wallMaterial.hexColor,
      design.floorMaterial.finish.label,
      design.floorMaterial.hexColor,
      design.ceilingMaterial.finish.label,
      design.ceilingMaterial.hexColor,
      design.countertopMaterial.finish.label,
      design.countertopMaterial.hexColor,
      itemsSummary,
      kitchenHardwarePrompt,
      lightingDesc,
      roomSpecificDetails,
    )
  }
}
