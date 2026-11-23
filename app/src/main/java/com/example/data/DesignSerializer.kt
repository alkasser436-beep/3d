package com.example.data

import com.example.model.FurnitureItem
import com.example.model.FurnitureKind
import com.example.model.LengthUnit
import com.example.model.LightingPreset
import com.example.model.MaterialFinish
import com.example.model.OpeningType
import com.example.model.RoomDesign
import com.example.model.RoomType
import com.example.model.SelectionTarget
import com.example.model.SurfaceMaterial
import com.example.model.WallId
import com.example.model.WallOpening
import org.json.JSONArray
import org.json.JSONObject

object DesignSerializer {

  fun toViewportPayload(design: RoomDesign, selectedTarget: SelectionTarget): String {
    val root = JSONObject()
    root.put("design", designToJson(design))
    val sel = JSONObject()
    when (selectedTarget) {
      SelectionTarget.Walls -> {
        sel.put("type", "WALLS")
        sel.put("id", "")
      }
      SelectionTarget.Floor -> {
        sel.put("type", "FLOOR")
        sel.put("id", "")
      }
      SelectionTarget.Ceiling -> {
        sel.put("type", "CEILING")
        sel.put("id", "")
      }
      SelectionTarget.AllCountertops -> {
        sel.put("type", "COUNTERTOPS")
        sel.put("id", "")
      }
      is SelectionTarget.Furniture -> {
        sel.put("type", "FURNITURE")
        sel.put("id", selectedTarget.id)
      }
    }
    root.put("selectedTarget", sel)
    return root.toString()
  }

  fun designToJsonString(design: RoomDesign): String = designToJson(design).toString(2)

  fun designToJson(d: RoomDesign): JSONObject {
    val obj = JSONObject()
    obj.put("id", d.id)
    obj.put("name", d.name)
    obj.put("roomType", d.roomType.name)
    obj.put("widthMeters", d.widthMeters.toDouble())
    obj.put("lengthMeters", d.lengthMeters.toDouble())
    obj.put("wallHeightMeters", d.wallHeightMeters.toDouble())
    obj.put("unit", d.unit.name)
    obj.put("wallMaterial", materialToJson(d.wallMaterial))
    obj.put("floorMaterial", materialToJson(d.floorMaterial))
    obj.put("ceilingMaterial", materialToJson(d.ceilingMaterial))
    obj.put("countertopMaterial", materialToJson(d.countertopMaterial))
    obj.put("realisticView", d.realisticView)
    obj.put("lightingPreset", d.lightingPreset.name)
    obj.put("sunAzimuthDeg", d.sunAzimuthDeg.toDouble())
    obj.put("exposure", d.exposure.toDouble())

    val opsArr = JSONArray()
    for (op in d.openings) {
      val o = JSONObject()
      o.put("id", op.id)
      o.put("type", op.type.name)
      o.put("wall", op.wall.name)
      o.put("offsetMeters", op.offsetMeters.toDouble())
      o.put("widthMeters", op.widthMeters.toDouble())
      o.put("heightMeters", op.heightMeters.toDouble())
      o.put("sillHeightMeters", op.sillHeightMeters.toDouble())
      opsArr.put(o)
    }
    obj.put("openings", opsArr)

    val furnArr = JSONArray()
    for (item in d.furniture) {
      val f = JSONObject()
      f.put("id", item.id)
      f.put("kind", item.kind.name)
      f.put("label", item.label)
      f.put("widthMeters", item.widthMeters.toDouble())
      f.put("depthMeters", item.depthMeters.toDouble())
      f.put("heightMeters", item.heightMeters.toDouble())
      f.put("xMeters", item.xMeters.toDouble())
      f.put("zMeters", item.zMeters.toDouble())
      f.put("yMeters", item.yMeters.toDouble())
      f.put("rotationDeg", item.rotationDeg.toDouble())
      f.put("material", materialToJson(item.material))
      if (item.secondaryMaterial != null) {
        f.put("secondaryMaterial", materialToJson(item.secondaryMaterial))
      }
      furnArr.put(f)
    }
    obj.put("furniture", furnArr)
    return obj
  }

  fun designFromJsonString(json: String): RoomDesign? = runCatching {
    val obj = JSONObject(json)
    val roomType = enumOrDefault(obj.optString("roomType"), RoomType.KITCHEN)
    val unit = enumOrDefault(obj.optString("unit"), LengthUnit.METERS)
    val lighting = enumOrDefault(obj.optString("lightingPreset"), LightingPreset.DAYLIGHT)

    val ops = mutableListOf<WallOpening>()
    val opsArr = obj.optJSONArray("openings") ?: JSONArray()
    for (i in 0 until opsArr.length()) {
      val o = opsArr.getJSONObject(i)
      ops.add(
        WallOpening(
          id = o.optString("id", "op_$i"),
          type = enumOrDefault(o.optString("type"), OpeningType.WINDOW),
          wall = enumOrDefault(o.optString("wall"), WallId.NORTH),
          offsetMeters = o.optDouble("offsetMeters", 0.0).toFloat(),
          widthMeters = o.optDouble("widthMeters", 1.2).toFloat().coerceIn(0.5f, 3.0f),
          heightMeters = o.optDouble("heightMeters", 1.2).toFloat().coerceIn(0.5f, 2.6f),
          sillHeightMeters = o.optDouble("sillHeightMeters", 0.9).toFloat().coerceIn(0f, 1.8f),
        )
      )
    }

    val furn = mutableListOf<FurnitureItem>()
    val furnArr = obj.optJSONArray("furniture") ?: JSONArray()
    for (i in 0 until furnArr.length()) {
      val f = furnArr.getJSONObject(i)
      val kind = enumOrDefault(f.optString("kind"), FurnitureKind.SOFA)
      furn.add(
        FurnitureItem(
          id = f.optString("id", "f_$i"),
          kind = kind,
          label = f.optString("label", kind.displayName),
          widthMeters = f.optDouble("widthMeters", kind.defaultW.toDouble()).toFloat().coerceIn(0.25f, 6.0f),
          depthMeters = f.optDouble("depthMeters", kind.defaultD.toDouble()).toFloat().coerceIn(0.25f, 6.0f),
          heightMeters = f.optDouble("heightMeters", kind.defaultH.toDouble()).toFloat().coerceIn(0.02f, 3.2f),
          xMeters = f.optDouble("xMeters", 0.0).toFloat(),
          zMeters = f.optDouble("zMeters", 0.0).toFloat(),
          yMeters = f.optDouble("yMeters", 0.0).toFloat(),
          rotationDeg = f.optDouble("rotationDeg", 0.0).toFloat(),
          material = materialFromJson(f.optJSONObject("material"), kind.defaultMaterial),
          secondaryMaterial = if (f.has("secondaryMaterial")) {
            materialFromJson(f.optJSONObject("secondaryMaterial"), kind.defaultSecondaryMaterial ?: kind.defaultMaterial)
          } else null,
        )
      )
    }

    RoomDesign(
      id = obj.optString("id", "imported_design"),
      name = obj.optString("name", "Imported Room"),
      roomType = roomType,
      widthMeters = obj.optDouble("widthMeters", 4.5).toFloat().coerceIn(2.0f, 12.0f),
      lengthMeters = obj.optDouble("lengthMeters", 4.0).toFloat().coerceIn(2.0f, 12.0f),
      wallHeightMeters = obj.optDouble("wallHeightMeters", 2.75).toFloat().coerceIn(2.2f, 4.5f),
      unit = unit,
      wallMaterial = materialFromJson(obj.optJSONObject("wallMaterial"), SurfaceMaterial(MaterialFinish.MATTE_PAINT, "#F4F1EA")),
      floorMaterial = materialFromJson(obj.optJSONObject("floorMaterial"), SurfaceMaterial(MaterialFinish.WOOD_OAK, "#C8A57B")),
      ceilingMaterial = materialFromJson(obj.optJSONObject("ceilingMaterial"), SurfaceMaterial(MaterialFinish.MATTE_PAINT, "#FAF8F5")),
      countertopMaterial = materialFromJson(obj.optJSONObject("countertopMaterial"), SurfaceMaterial(MaterialFinish.MARBLE_WHITE, "#F2EFE9")),
      openings = ops,
      furniture = furn,
      realisticView = obj.optBoolean("realisticView", false),
      lightingPreset = lighting,
      sunAzimuthDeg = obj.optDouble("sunAzimuthDeg", 42.0).toFloat(),
      exposure = obj.optDouble("exposure", 1.08).toFloat().coerceIn(0.6f, 1.8f),
    )
  }.getOrNull()

  private fun materialToJson(m: SurfaceMaterial): JSONObject = JSONObject().apply {
    put("finish", m.finish.name)
    put("hexColor", m.hexColor)
  }

  private fun materialFromJson(obj: JSONObject?, fallback: SurfaceMaterial): SurfaceMaterial {
    if (obj == null) return fallback
    val finish = enumOrDefault(obj.optString("finish"), fallback.finish)
    val hex = obj.optString("hexColor", fallback.hexColor)
    return SurfaceMaterial(finish, hex)
  }

  private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
}
