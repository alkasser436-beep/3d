package com.example

import com.example.data.DesignTemplates
import com.example.model.FurnitureKind
import com.example.model.LengthUnit
import com.example.model.LightingPreset
import com.example.model.MaterialCategory
import com.example.model.MaterialFinish
import com.example.model.RoomType
import com.example.model.SurfaceMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignUnitTests {

  @Test
  fun templatesCoverAllRoomTypesWithValidDimensions() {
    RoomType.entries.forEach { type ->
      val tpl = DesignTemplates.defaultForRoomType(type)
      assertTrue(tpl.widthMeters in 2.0f..12.0f)
      assertTrue(tpl.lengthMeters in 2.0f..12.0f)
      assertTrue(tpl.wallHeightMeters in 2.2f..4.5f)
      assertTrue(tpl.furniture.isNotEmpty())
      assertTrue(tpl.openings.isNotEmpty())
    }
    assertEquals(6, DesignTemplates.templates.size)
  }

  @Test
  fun materialSwatchesCoverAllCategoriesWithValidHex() {
    MaterialCategory.entries.forEach { category ->
      val swatches = DesignTemplates.swatchesByCategory[category]
      assertNotNull(swatches)
      assertTrue(swatches!!.isNotEmpty())
      swatches.forEach { sw ->
        assertTrue("Invalid hex ${sw.hexColor}", sw.hexColor.matches(Regex("#[0-9A-Fa-f]{6}")))
      }
    }
  }

  @Test
  fun furnitureFactoryCreatesValidDefaultItems() {
    FurnitureKind.entries.forEachIndexed { idx, kind ->
      val item = DesignTemplates.createFurnitureItem(kind, idx)
      assertEquals(kind, item.kind)
      assertTrue(item.widthMeters > 0f)
      assertTrue(item.depthMeters > 0f)
      assertTrue(item.heightMeters > 0f)
      assertEquals(kind.defaultMaterial, item.material)
    }
  }

  @Test
  fun designCopyPreservesRealisticAndUnitSettings() {
    val base = DesignTemplates.defaultForRoomType(RoomType.KITCHEN)
    val updated = base.copy(
      unit = LengthUnit.CENTIMETERS,
      realisticView = true,
      lightingPreset = LightingPreset.INDOOR_SPOTLIGHTS,
      wallMaterial = SurfaceMaterial(MaterialFinish.SATIN_PAINT, "#9FB3A2"),
    )
    assertEquals(LengthUnit.CENTIMETERS, updated.unit)
    assertTrue(updated.realisticView)
    assertEquals(LightingPreset.INDOOR_SPOTLIGHTS, updated.lightingPreset)
    assertEquals("#9FB3A2", updated.wallMaterial.hexColor)
  }
}
