# RoomCraft 3D

**RoomCraft 3D** is an Android interior design application built with **Kotlin**, **Jetpack Compose (Material 3)**, an embedded **WebGL2** real-time 3D architectural renderer, and **Firebase AI Logic** for photorealistic DSLR-style interior rendering.

## Key Features

- **Measurement-to-3D Room Auto-Generator**
  - Configure room dimensions in meters (`m`) or centimeters (`cm`): Width, Length, and Wall Height.
  - Supports **Kitchen**, **Bedroom**, and **Living Room** layouts with preset room templates.
  - Live procedural generation of walls, floors, ceilings, baseboards, crown molding, recessed downlights, and interactive doors/windows.
- **Interactive Furniture & Cabinetry**
  - Place and customize L-shape and straight kitchen counters, kitchen islands, handleless upper cabinets with dark glass strips, tall pantries/fridges, beds, wardrobes, nightstands, dressers, sofas, coffee tables, TV consoles, dining tables, rugs, and lamps.
  - Edit dimensions (`W / D / H`), `X / Y / Z` coordinates, and rotation, or tap elements directly in 3D.
- **Real-World Colors & Materials**
  - Procedural PBR surface textures with height-map bump normals across Paint (Matte, Satin, Glossy), Wood (Oak, Walnut, Beech), Stone (Carrara Marble, Calacatta Quartz, Nero Slate), Ceramic Subway Tiles, Woven Fabric, and Brushed Metal, plus custom Hex color matching.
- **Realistic View & AI Photorealistic Render**
  - **Client-Side WebGL2 Realistic View**: 2048px soft PCF shadows, depth-based SSAO, box-projected reflections, bloom, ACES filmic tone mapping, and Daylight / Golden Hour / Indoor Spotlights presets.
  - **AI Real Photo Render**: Transforms any 3D viewport camera angle into a photorealistic interior photograph using Firebase AI Logic (`gemini-nano-banana-2.1`) with Before/After comparison and high-resolution PNG save/share.
- **Undo/Redo & Local Persistence**
  - 40-step undo/redo history, local project slots, automatic draft saving, and JSON import/export via Android Storage Access Framework.

## Tech Stack

- **UI**: Kotlin, Jetpack Compose, Material Design 3
- **3D Engine**: Custom WebGL2 / GLSL ES 3.00 engine (`app/src/main/assets/`)
- **AI**: Firebase AI Logic (`com.google.firebase:firebase-ai`) + Firebase App Check
- **Architecture**: MVVM with `StateFlow` and `SharedFlow`

## Building & Setup

1. Clone the repository and open the root directory in **Android Studio**.
2. Ensure `app/google-services.json` matches your Firebase project with **Firebase AI Logic** and **App Check** configured (image generation requires the Firebase Blaze plan).
3. Sync Gradle and run the `app` configuration on Android 7.0+ (API 24+, targetSdk 36).
4. Run unit tests locally (via Android Studio or Gradle CLI):
   ```bash
   gradle :app:testDebugUnitTest --tests "com.example.DesignUnitTests" --tests "com.example.ExampleUnitTest"
   ```
