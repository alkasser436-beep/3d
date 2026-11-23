package com.example.model

enum class RoomType(val displayName: String) {
  KITCHEN("Kitchen"),
  BEDROOM("Bedroom"),
  LIVING_ROOM("Living Room"),
}

enum class LengthUnit(val suffix: String) {
  METERS("m"),
  CENTIMETERS("cm"),
}

enum class AppLanguage(val shortCode: String, val nativeName: String, val isRtl: Boolean) {
  ARABIC("AR", "العربية", true),
  ENGLISH("EN", "English", false),
  FRENCH("FR", "Français", false),
}

enum class MaterialCategory(val label: String) {
  PAINT("Paint"),
  STARWOOD_MFC("Starwood MFC"),
  WOOD("Wood"),
  STONE("Stone"),
  TILE("Tile"),
  FABRIC("Fabric"),
  METAL("Metal"),
}

enum class MaterialFinish(
  val label: String,
  val category: MaterialCategory,
  val defaultHex: String,
) {
  MATTE_PAINT("Matte Paint", MaterialCategory.PAINT, "#F4F1EA"),
  SATIN_PAINT("Satin Paint", MaterialCategory.PAINT, "#E6E0D4"),
  GLOSSY_PAINT("Glossy Lacquer", MaterialCategory.PAINT, "#FAF8F5"),
  MFC_MELAMINE("Starwood Uni MFC", MaterialCategory.STARWOOD_MFC, "#EAE6DF"),
  MFC_WOOD("Starwood Wood MFC", MaterialCategory.STARWOOD_MFC, "#C5A379"),
  WOOD_OAK("White Oak", MaterialCategory.WOOD, "#C8A57B"),
  WOOD_WALNUT("American Walnut", MaterialCategory.WOOD, "#5C3D28"),
  WOOD_BEECH("Scandi Beech", MaterialCategory.WOOD, "#DFC29B"),
  MARBLE_WHITE("Carrara Marble", MaterialCategory.STONE, "#F2EFE9"),
  QUARTZ_CREAM("Calacatta Quartz", MaterialCategory.STONE, "#EBE4D6"),
  DARK_STONE("Nero Slate", MaterialCategory.STONE, "#323539"),
  GRANITE_SPECKLED("Natural Granite", MaterialCategory.STONE, "#1E2024"),
  CERAMIC_TILE("Ceramic Tile", MaterialCategory.TILE, "#E9E5DC"),
  FABRIC_WOVEN("Woven Fabric", MaterialCategory.FABRIC, "#D5CFC4"),
  BRUSHED_METAL("Brushed Metal", MaterialCategory.METAL, "#C3C8CE"),
}

data class SurfaceMaterial(
  val finish: MaterialFinish,
  val hexColor: String,
)

enum class WallId(val label: String) {
  NORTH("Back Wall"),
  SOUTH("Front Wall"),
  WEST("Left Wall"),
  EAST("Right Wall"),
}

enum class OpeningType(val label: String) {
  DOOR("Door"),
  WINDOW("Window"),
}

data class WallOpening(
  val id: String,
  val type: OpeningType,
  val wall: WallId,
  val offsetMeters: Float,
  val widthMeters: Float,
  val heightMeters: Float,
  val sillHeightMeters: Float,
)

enum class CabinetStyle(val label: String) {
  HANDLELESS_GOLA("Handleless"),
  SHAKER_FRAME("Shaker Frame"),
  GLASS_DISPLAY("Glass Display"),
  OPEN_SHELVING("Open Shelf"),
}

enum class FurnitureCategory(val label: String) {
  KITCHEN("Kitchen"),
  BEDROOM("Bedroom"),
  LIVING("Living"),
}

enum class FurnitureKind(
  val displayName: String,
  val category: FurnitureCategory,
  val defaultW: Float,
  val defaultD: Float,
  val defaultH: Float,
  val defaultY: Float = 0f,
  val defaultMaterial: SurfaceMaterial,
  val defaultSecondaryMaterial: SurfaceMaterial? = null,
  val defaultStyle: CabinetStyle = CabinetStyle.HANDLELESS_GOLA,
) {
  STRAIGHT_COUNTER(
    displayName = "Straight Counter",
    category = FurnitureCategory.KITCHEN,
    defaultW = 2.4f,
    defaultD = 0.65f,
    defaultH = 0.90f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.SATIN_PAINT, "#EAE5DC"),
    defaultSecondaryMaterial = SurfaceMaterial(MaterialFinish.MARBLE_WHITE, "#F2EFE9"),
  ),
  L_COUNTER(
    displayName = "L-Shape Counter",
    category = FurnitureCategory.KITCHEN,
    defaultW = 2.8f,
    defaultD = 2.2f,
    defaultH = 0.90f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.SATIN_PAINT, "#4B6B5A"),
    defaultSecondaryMaterial = SurfaceMaterial(MaterialFinish.QUARTZ_CREAM, "#EBE4D6"),
  ),
  KITCHEN_ISLAND(
    displayName = "Kitchen Island",
    category = FurnitureCategory.KITCHEN,
    defaultW = 1.9f,
    defaultD = 0.95f,
    defaultH = 0.92f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_OAK, "#C8A57B"),
    defaultSecondaryMaterial = SurfaceMaterial(MaterialFinish.MARBLE_WHITE, "#F2EFE9"),
  ),
  UPPER_CABINET(
    displayName = "Upper Cabinet",
    category = FurnitureCategory.KITCHEN,
    defaultW = 1.6f,
    defaultD = 0.36f,
    defaultH = 0.78f,
    defaultY = 1.45f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.SATIN_PAINT, "#F2EFE9"),
    defaultStyle = CabinetStyle.GLASS_DISPLAY,
  ),
  TALL_PANTRY(
    displayName = "Tall Pantry / Fridge",
    category = FurnitureCategory.KITCHEN,
    defaultW = 0.90f,
    defaultD = 0.68f,
    defaultH = 2.15f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.BRUSHED_METAL, "#C3C8CE"),
  ),
  DINING_TABLE(
    displayName = "Dining Table",
    category = FurnitureCategory.KITCHEN,
    defaultW = 1.6f,
    defaultD = 0.90f,
    defaultH = 0.76f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_OAK, "#C8A57B"),
  ),
  BED(
    displayName = "Queen Bed",
    category = FurnitureCategory.BEDROOM,
    defaultW = 1.75f,
    defaultD = 2.15f,
    defaultH = 1.05f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_OAK, "#C8A57B"),
    defaultSecondaryMaterial = SurfaceMaterial(MaterialFinish.FABRIC_WOVEN, "#D96B43"),
  ),
  WARDROBE(
    displayName = "Wardrobe",
    category = FurnitureCategory.BEDROOM,
    defaultW = 1.6f,
    defaultD = 0.60f,
    defaultH = 2.15f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.MATTE_PAINT, "#EDEAE2"),
  ),
  NIGHTSTAND(
    displayName = "Nightstand + Lamp",
    category = FurnitureCategory.BEDROOM,
    defaultW = 0.52f,
    defaultD = 0.44f,
    defaultH = 0.56f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_WALNUT, "#6E4C36"),
  ),
  DRESSER(
    displayName = "6-Drawer Dresser",
    category = FurnitureCategory.BEDROOM,
    defaultW = 1.35f,
    defaultD = 0.48f,
    defaultH = 0.82f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_BEECH, "#DFC29B"),
  ),
  SOFA(
    displayName = "3-Seater Sofa",
    category = FurnitureCategory.LIVING,
    defaultW = 2.25f,
    defaultD = 0.95f,
    defaultH = 0.84f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.FABRIC_WOVEN, "#C96A4B"),
  ),
  COFFEE_TABLE(
    displayName = "Coffee Table",
    category = FurnitureCategory.LIVING,
    defaultW = 1.20f,
    defaultD = 0.65f,
    defaultH = 0.42f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_WALNUT, "#5C3D28"),
  ),
  TV_CONSOLE(
    displayName = "Media Console + TV",
    category = FurnitureCategory.LIVING,
    defaultW = 1.80f,
    defaultD = 0.45f,
    defaultH = 0.54f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.WOOD_OAK, "#C8A57B"),
  ),
  RUG(
    displayName = "Area Rug",
    category = FurnitureCategory.LIVING,
    defaultW = 2.40f,
    defaultD = 1.70f,
    defaultH = 0.02f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.FABRIC_WOVEN, "#E5DFD3"),
  ),
  FLOOR_LAMP(
    displayName = "Floor Lamp",
    category = FurnitureCategory.LIVING,
    defaultW = 0.42f,
    defaultD = 0.42f,
    defaultH = 1.65f,
    defaultMaterial = SurfaceMaterial(MaterialFinish.BRUSHED_METAL, "#C5A15B"),
  ),
}

data class FurnitureItem(
  val id: String,
  val kind: FurnitureKind,
  val label: String,
  val widthMeters: Float,
  val depthMeters: Float,
  val heightMeters: Float,
  val xMeters: Float,
  val zMeters: Float,
  val yMeters: Float = 0f,
  val rotationDeg: Float = 0f,
  val material: SurfaceMaterial,
  val secondaryMaterial: SurfaceMaterial? = null,
  val style: CabinetStyle = kind.defaultStyle,
  val showDoors: Boolean = true,
  val showDrawers: Boolean = true,
  val showHandles: Boolean = false,
)

enum class LightingPreset(val displayName: String, val subtitle: String) {
  DAYLIGHT("Natural Daylight", "Crisp sunbeams & soft sky fill"),
  GOLDEN_HOUR("Golden Hour", "Warm late-afternoon sun glow"),
  INDOOR_SPOTLIGHTS("Indoor Spotlights", "Evening ceiling spots & warm lamps"),
}

enum class CameraPreset(val label: String) {
  PERSPECTIVE_ORBIT("Orbit"),
  TOP_FLOORPLAN("Top Plan"),
  CORNER_EYE_LEVEL("Eye-Level"),
}

sealed interface SelectionTarget {
  data object Walls : SelectionTarget
  data object Floor : SelectionTarget
  data object Ceiling : SelectionTarget
  data object AllCountertops : SelectionTarget
  data class Furniture(val id: String) : SelectionTarget
}

enum class ProfileHandleType(val label: String, val subtitle: String) {
  GOLA_CHANNEL("Gola C/J Aluminum Channel", "Recessed hidden aluminum finger channel"),
  PROFILE_EDGE("Top-Lip Profile Handle", "Aluminum profile bar on door/drawer edge"),
  SLIM_BAR("Slim Aluminum Bar Pull", "Surface-mounted brushed metal bar"),
  PUSH_OPEN("Push-to-Open Latch", "Handleless clean front with touch latch"),
}

enum class AluminumFinish(val label: String, val hexColor: String) {
  MATTE_BLACK("Matte Black Anodized", "#22252A"),
  ANODIZED_SILVER("Anodized Natural Silver", "#C8CDD4"),
  CHAMPAGNE_GOLD("Champagne Gold", "#C9A86A"),
  DARK_BRONZE("Dark Smoked Bronze", "#5C4A3D"),
  BRUSHED_INOX("Brushed Inox Steel", "#D6DBE0"),
}

enum class SinkType(
  val label: String,
  val subtitle: String,
  val widthMeters: Float,
  val doubleBowl: Boolean,
  val isBlackGranite: Boolean,
) {
  SINGLE_STEEL_60("Single Bowl Inox 304 (60cm)", "Undermount / Topmount deep stainless", 0.58f, false, false),
  DOUBLE_STEEL_116("Double Bowl + Drainer (116cm)", "Twin stainless basins with side drainer", 0.96f, true, false),
  GRANITE_BLACK_80("Black Granite Composite (80cm)", "Scratch-resistant quartz-granite sink", 0.74f, false, true),
  GRANITE_DOUBLE_100("Black Granite Double (100cm)", "Twin matte granite bowls — Lebanon popular", 0.92f, true, true),
  COMPACT_45("Compact Single Bowl (45cm)", "Space-saving utility / small kitchen sink", 0.44f, false, false),
}

enum class FaucetStyle(
  val label: String,
  val isBlack: Boolean,
  val isSpringChef: Boolean,
) {
  SPRING_CHEF_STEEL("Pull-Down Spring Chef Mixer", false, true),
  L_SPOUT_CHROME("Modern L-Spout Chrome Mixer", false, false),
  MATTE_BLACK_ARC("Matte Black High-Arc Gooseneck", true, false),
}

enum class LebaneseOrganizer(val label: String, val sizeSpec: String, val category: String) {
  PULLOUT_SPICE_20("Pull-Out Spice & Oil Basket", "15–20 cm", "Pull-Out"),
  PULLOUT_PANTRY_40("Tall Pantry Pull-Out Column", "30–40 cm", "Pull-Out"),
  MAGIC_CORNER("Magic Corner Pull-Out System", "90–100 cm Corner", "Corner"),
  PULLOUT_BIN("Built-In Dual Trash Pull-Out", "30–45 cm", "Pull-Out"),
  DISH_RACK_80("Upper Stainless Dish Rack", "60–90 cm", "Sink Zone"),
  CUTLERY_TRAY("Soft-Close Cutlery Organizer", "60–90 cm Drawer", "Drawer"),
  BUILT_IN_HOB_HOOD("Built-in Gas Hob + Chimney Hood", "60–90 cm", "Appliances"),
  WATER_FILTER_TAP("Under-Sink RO Water Filter", "3-Stage Compact", "Sink Zone"),
}

enum class MfcBoardGrade(val code: String, val title: String, val specNote: String) {
  P2_REGULAR("MFC / P2", "Regular Chipboard (P2)", "620–700 kg/m³ • Bedrooms, wardrobes & dry cabinets"),
  P7_MOISTURE_RESISTANT("MFC / P7", "Moisture Resistant (P7)", "670–810 kg/m³ • 9% 24h swelling — Sink & kitchen boxes"),
}

data class KitchenSpecs(
  val handleType: ProfileHandleType = ProfileHandleType.PROFILE_EDGE,
  val aluminumFinish: AluminumFinish = AluminumFinish.MATTE_BLACK,
  val aluminumPlinth: Boolean = true,
  val underCabinetLed: Boolean = true,
  val softCloseHardware: Boolean = true,
  val countertopThicknessCm: Int = 3,
  val countertopPolished: Boolean = true,
  val sinkType: SinkType = SinkType.SINGLE_STEEL_60,
  val faucetStyle: FaucetStyle = FaucetStyle.SPRING_CHEF_STEEL,
  val mfcBoardGrade: MfcBoardGrade = MfcBoardGrade.P7_MOISTURE_RESISTANT,
  val organizers: Set<LebaneseOrganizer> = setOf(
    LebaneseOrganizer.PULLOUT_SPICE_20,
    LebaneseOrganizer.DISH_RACK_80,
    LebaneseOrganizer.BUILT_IN_HOB_HOOD,
  ),
)

data class RoomDesign(
  val id: String,
  val name: String,
  val roomType: RoomType,
  val widthMeters: Float,
  val lengthMeters: Float,
  val wallHeightMeters: Float,
  val unit: LengthUnit = LengthUnit.METERS,
  val wallMaterial: SurfaceMaterial,
  val floorMaterial: SurfaceMaterial,
  val ceilingMaterial: SurfaceMaterial,
  val countertopMaterial: SurfaceMaterial,
  val openings: List<WallOpening>,
  val furniture: List<FurnitureItem>,
  val realisticView: Boolean = false,
  val lightingPreset: LightingPreset = LightingPreset.DAYLIGHT,
  val sunAzimuthDeg: Float = 42f,
  val exposure: Float = 1.08f,
  val kitchenSpecs: KitchenSpecs = KitchenSpecs(),
)

data class SavedDesignSummary(
  val id: String,
  val name: String,
  val roomType: RoomType,
  val dimensionsSummary: String,
  val updatedAtMillis: Long,
)
