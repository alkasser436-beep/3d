# RoomCraft 3D

**RoomCraft 3D** is a trilingual (**العربية / English / Français**) Android interior & kitchen design application built with **Kotlin**, **Jetpack Compose (Material 3)**, an embedded **WebGL2** real-time 3D architectural renderer, and **Firebase AI Logic** for photorealistic DSLR-style interior rendering.

## Key Features

- **Measurement-to-3D Room Auto-Generator**
  - Configure room dimensions in meters (`m`) or centimeters (`cm`): Width, Length, and Wall Height.
  - Supports **Kitchen**, **Bedroom**, and **Living Room** layouts with preset room templates.
  - Live procedural generation of walls, floors, ceilings, baseboards, crown molding, recessed downlights, and interactive doors/windows.
- **Modular Cabinet Boxes, Doors & Drawers**
  - Customize cabinet width, depth, height, elevation, position, and rotation.
  - Toggle doors/shutters (open shelving vs closed cabinets), drawers, and metal/profile handles, or switch between base, wall, tall pantry, corner, and glass-front shapes.
- **Lebanese Market Kitchen Specifications**
  - **Aluminum & Profile Handles**: Gola C/J/L channels, edge pulls, anodized finishes, waterproof aluminum plinth, under-cabinet 3000K LED strip, and hydraulic soft-close hinges.
  - **Granite, Marble & Quartz Slabs**: Lebanese market stone slabs with 2cm/3cm/4cm thickness and Polished / Honed finishes.
  - **Sinks, Faucets & Pull-Outs**: Inox 304 and Black Granite composite single/double bowls, pull-down mixers, spice pull-outs, tall pantry columns, magic corners, and dishwashers.
- **Starwood MFC & Real-World Materials**
  - Official **Starwood MFC (P2 Regular & P7 Moisture Resistant)** Uni, Wood, and Fantasy decors alongside procedural PBR Paint, Wood, Stone, Tile, Fabric, and Metal finishes plus an interactive HSV + Hex color matcher.
- **Realistic View & AI Photorealistic Render**
  - **Client-Side WebGL2 Realistic View**: 2048px soft PCF shadows, depth-based SSAO, box-projected reflections, bloom, ACES filmic tone mapping, and Daylight / Golden Hour / Indoor Spotlights presets.
  - **AI Real Photo Render**: Transforms the active 3D viewport into a photorealistic interior photo via Firebase AI Logic (`gemini-nano-banana-2.1`) with Before/After toggle and PNG save/share.

## Building on GitHub Actions

Every push or pull request to `main` / `master` runs the **Android CI** workflow (`.github/workflows/main.yml`) to execute unit tests and assemble the signed Debug APK (`roomcraft3d-debug-apk` artifact).

You can also trigger a manual build anytime from the repository **Actions** tab (**Build & Test Debug APK -> Run workflow**) and download the resulting `.apk` from the workflow summary page.

## Local Build & Testing

1. Clone the repository and open the root directory in **Android Studio**.
2. Ensure `app/google-services.json` matches your Firebase project with **Firebase AI Logic** and **App Check** configured (image generation requires the Firebase Blaze plan).
3. Run unit tests locally:
   ```bash
   gradle :app:testDebugUnitTest --tests "com.example.DesignUnitTests" --tests "com.example.ExampleUnitTest"
   ```
4. Assemble the Debug APK locally:
   ```bash
   gradle :app:assembleDebug
   ```
