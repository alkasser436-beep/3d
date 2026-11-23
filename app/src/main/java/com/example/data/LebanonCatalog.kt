package com.example.data

import com.example.model.FurnitureKind
import com.example.model.MaterialFinish

data class LebaneseGraniteSlab(
  val id: String,
  val name: String,
  val arabicOriginNote: String,
  val finish: MaterialFinish,
  val hexColor: String,
  val categoryTag: String,
)

data class CabinetBoxPreset(
  val id: String,
  val title: String,
  val sizeNote: String,
  val kind: FurnitureKind,
  val widthM: Float,
  val depthM: Float,
  val heightM: Float,
  val yM: Float = 0f,
)

object LebanonCatalog {

  val graniteSlabs: List<LebaneseGraniteSlab> = listOf(
    LebaneseGraniteSlab(
      id = "black_galaxy",
      name = "Black Galaxy",
      arabicOriginNote = "Indian Granite • Gold flecks — Very popular in Lebanon",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#1B1D22",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "nero_zimbabwe",
      name = "Nero Zimbabwe",
      arabicOriginNote = "Absolute Black Granite • Deep satin/matte crystal",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#27292E",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "kashmir_white",
      name = "Kashmir White",
      arabicOriginNote = "Light Cream & Grey Granite • Bright kitchen staple",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#E6E3DC",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "bianco_sardo",
      name = "Bianco Sardo",
      arabicOriginNote = "Italian Grey Speckled Granite • Classic heavy-duty",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#CFD2D5",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "tan_brown",
      name = "Tan Brown",
      arabicOriginNote = "Warm Coffee & Copper Granite • Pairs with oak/walnut",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#4C362C",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "blue_pearl",
      name = "Blue Pearl",
      arabicOriginNote = "Norwegian Metallic Blue-Grey Granite",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#404E5C",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "verde_ubatuba",
      name = "Verde Ubatuba",
      arabicOriginNote = "Deep Emerald-Charcoal Granite",
      finish = MaterialFinish.GRANITE_SPECKLED,
      hexColor = "#24352D",
      categoryTag = "Granite",
    ),
    LebaneseGraniteSlab(
      id = "lebanese_basalt",
      name = "Lebanese Basalt / Charcoal",
      arabicOriginNote = "Honed Architectural Dark Stone",
      finish = MaterialFinish.DARK_STONE,
      hexColor = "#3A3E44",
      categoryTag = "Basalt",
    ),
    LebaneseGraniteSlab(
      id = "carrara_italy",
      name = "Carrara White (Italian)",
      arabicOriginNote = "Soft Grey Veined Marble / Quartz look",
      finish = MaterialFinish.MARBLE_WHITE,
      hexColor = "#F2EFE9",
      categoryTag = "Marble",
    ),
    LebaneseGraniteSlab(
      id = "calacatta_gold",
      name = "Calacatta Gold Quartz",
      arabicOriginNote = "Engineered Quartz • Warm golden veining",
      finish = MaterialFinish.QUARTZ_CREAM,
      hexColor = "#EFE8D8",
      categoryTag = "Quartz",
    ),
  )

  val cabinetBoxPresets: List<CabinetBoxPreset> = listOf(
    CabinetBoxPreset(
      id = "base_60",
      title = "60cm Base Unit",
      sizeNote = "60 × 60 × 90 cm",
      kind = FurnitureKind.STRAIGHT_COUNTER,
      widthM = 0.60f,
      depthM = 0.60f,
      heightM = 0.90f,
    ),
    CabinetBoxPreset(
      id = "sink_base_80",
      title = "80cm Sink Base",
      sizeNote = "80 × 60 × 90 cm",
      kind = FurnitureKind.STRAIGHT_COUNTER,
      widthM = 0.80f,
      depthM = 0.60f,
      heightM = 0.90f,
    ),
    CabinetBoxPreset(
      id = "double_counter_180",
      title = "180cm Full Counter",
      sizeNote = "180 × 62 × 90 cm",
      kind = FurnitureKind.STRAIGHT_COUNTER,
      widthM = 1.80f,
      depthM = 0.62f,
      heightM = 0.90f,
    ),
    CabinetBoxPreset(
      id = "upper_80",
      title = "80cm Upper Box",
      sizeNote = "80 × 35 × 78 cm (H 148)",
      kind = FurnitureKind.UPPER_CABINET,
      widthM = 0.80f,
      depthM = 0.35f,
      heightM = 0.78f,
      yM = 1.48f,
    ),
    CabinetBoxPreset(
      id = "tall_pantry_60",
      title = "60cm Tall Column",
      sizeNote = "60 × 60 × 215 cm",
      kind = FurnitureKind.TALL_PANTRY,
      widthM = 0.60f,
      depthM = 0.60f,
      heightM = 2.15f,
    ),
    CabinetBoxPreset(
      id = "island_180",
      title = "180cm Island Box",
      sizeNote = "180 × 90 × 92 cm",
      kind = FurnitureKind.KITCHEN_ISLAND,
      widthM = 1.80f,
      depthM = 0.90f,
      heightM = 0.92f,
    ),
  )
}
