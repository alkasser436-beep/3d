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

import com.example.data.LebanonCatalog
import com.example.model.AluminumFinish
import com.example.model.AppLanguage
import com.example.model.CabinetStyle
import com.example.model.FaucetStyle
import com.example.model.KitchenSpecs
import com.example.model.LebaneseOrganizer
import com.example.model.MfcBoardGrade
import com.example.model.ProfileHandleType
import com.example.model.SinkType
import com.example.viewmodel.StudioTab
import org.junit.Assert.assertFalse

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
      assertEquals(kind.defaultStyle, item.style)
      assertTrue(item.showDoors)
      assertTrue(item.showDrawers)
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

  @Test
  fun cabinetStyleAndDoorsDrawersCustomizationUpdatesCleanly() {
    assertEquals(CabinetStyle.GLASS_DISPLAY, FurnitureKind.UPPER_CABINET.defaultStyle)
    assertEquals(4, CabinetStyle.entries.size)

    val base = DesignTemplates.defaultForRoomType(RoomType.KITCHEN)
    val customized = base.copy(
      furniture = base.furniture.mapIndexed { idx, item ->
        if (idx == 0) {
          item.copy(
            style = CabinetStyle.SHAKER_FRAME,
            showDoors = false,
            showDrawers = true,
            showHandles = false,
          )
        } else {
          item.copy(style = CabinetStyle.OPEN_SHELVING, showDoors = false, showDrawers = false)
        }
      },
    )

    assertEquals(CabinetStyle.SHAKER_FRAME, customized.furniture[0].style)
    assertFalse(customized.furniture[0].showDoors)
    assertTrue(customized.furniture[0].showDrawers)
    assertFalse(customized.furniture[0].showHandles)
    assertEquals(CabinetStyle.OPEN_SHELVING, customized.furniture[1].style)
    assertFalse(customized.furniture[1].showDoors)
    assertFalse(customized.furniture[1].showDrawers)
  }

  @Test
  fun lebanonCatalogGraniteSlabsAndBoxPresetsAreValid() {
    assertEquals(10, LebanonCatalog.graniteSlabs.size)
    LebanonCatalog.graniteSlabs.forEach { slab ->
      assertTrue("Invalid granite hex ${slab.hexColor}", slab.hexColor.matches(Regex("#[0-9A-Fa-f]{6}")))
      assertTrue(slab.name.isNotBlank())
    }
    assertEquals(6, LebanonCatalog.cabinetBoxPresets.size)
    LebanonCatalog.cabinetBoxPresets.forEach { preset ->
      assertTrue(preset.widthM > 0f && preset.depthM > 0f && preset.heightM > 0f)
    }
  }

  @Test
  fun kitchenSpecsAndStudioTabsAreComplete() {
    assertEquals(8, StudioTab.entries.size)
    val specs = KitchenSpecs()
    assertEquals(ProfileHandleType.PROFILE_EDGE, specs.handleType)
    assertEquals(AluminumFinish.MATTE_BLACK, specs.aluminumFinish)
    assertEquals(SinkType.SINGLE_STEEL_60, specs.sinkType)
    assertEquals(FaucetStyle.SPRING_CHEF_STEEL, specs.faucetStyle)
    assertEquals(MfcBoardGrade.P7_MOISTURE_RESISTANT, specs.mfcBoardGrade)
    assertTrue(specs.aluminumPlinth)
    assertTrue(specs.underCabinetLed)
    assertTrue(LebaneseOrganizer.PULLOUT_SPICE_20 in specs.organizers)
  }

  @Test
  fun starwoodMfcAndAppLanguagesAreConfigured() {
    val mfcSwatches = DesignTemplates.swatchesByCategory[MaterialCategory.STARWOOD_MFC]
    assertNotNull(mfcSwatches)
    assertTrue(mfcSwatches!!.size >= 14)
    assertTrue(mfcSwatches.any { it.decorTag == "Uni" })
    assertTrue(mfcSwatches.any { it.decorTag == "Wood" })
    assertTrue(mfcSwatches.any { it.decorTag == "Fantasy" })
    val paintSwatches = DesignTemplates.swatchesByCategory[MaterialCategory.PAINT]
    assertTrue((paintSwatches?.size ?: 0) >= 14)
    assertEquals(3, AppLanguage.entries.size)
    assertTrue(AppLanguage.ARABIC.isRtl)
    assertFalse(AppLanguage.ENGLISH.isRtl)
  }
}
