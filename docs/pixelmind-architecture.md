# PixelMind Studio Architecture

## 1. Core Pixel Editor Engine

### Canvas + Grid
- `PixelCanvas` renders a zoomable/pannable pixel surface with checkerboard transparency backdrop.
- Supported canvas sizes are represented by `CanvasSize` and include `16x16`, `32x32`, `64x64`, plus any custom dimensions.
- Grid lines can be toggled and are drawn over composited visible layers.

### Tooling
- `PixelTool` supports:
  - Pencil (with view-model controlled pixel-perfect mode flag),
  - Eraser,
  - Paint Bucket (4-neighbor flood fill),
  - Eye-dropper color picker.
- Palette manager stores persistent swatches (`PaletteColor`) and can add custom colors.

### Layers
- Layer model is `PixelLayer(id, name, isVisible, pixels)`.
- Layer actions supported in `EditorViewModel`:
  - Add layer,
  - Delete layer,
  - Select active layer,
  - Toggle visibility.

### View Controls
- Pinch gesture updates zoom (`0.5x..32x`).
- Two-finger pan offsets canvas in viewport.
- Viewport state (`zoom`, `pan`) is retained in `EditorUiState` via `StateFlow`.

### Project Import/Export
- `ProjectSerialization.saveProjectJson/loadProjectJson` handles full project persistence (canvas size, palette, layers, pixels).
- `ProjectSerialization.exportPng` exports composited visible layers into scaled PNG output with nearest-neighbor scaling.

## 2. AI Chat and Image Workflow

### Provider Configuration
- Provider-agnostic networking is defined in `ApiService` + `AiGateway`.
- Supported providers in model/config:
  - OpenRouter,
  - Gemini,
  - Mistral,
  - HuggingChat.
- `ProviderSettings.defaultConfigs(...)` centralizes endpoint/model defaults and API key mapping.

### Canvas Context Awareness
- `AiAssistantOrchestrator.layerStackToBase64` composites visible canvas layers to PNG then Base64-encodes it.
- Chat requests include both prompt text and image context (vision model compatible).

### AI Command Processing
- `sendPromptWithCanvasContext()` pushes user prompt + canvas snapshot and appends AI response to chat history.
- If AI image output is returned (`generateSpriteSheet`), Base64 PNG is decoded and converted back into pixel map data for direct layer insertion.

## 3. Advanced AI Sprite Power Tools

### Sprite-to-Sheet Generation
- `runDirectionalSpriteGenerator()` creates 4x4 walk/action sheet request.
- `generateSpriteSheet(...)` builds targeted prompt text and submits image-edit generation request.

### Directional Modes
- `DirectionMode` supports:
  - `FourWay` (up/down/left/right),
  - `EightWay` (diagonal/iso-ready).

### Prompt Engineering Guardrails
- System prompt enforces:
  1. hard pixel edges,
  2. no anti-aliasing/blur,
  3. palette consistency,
  4. silhouette consistency,
  5. transparent PNG-compatible outputs.

## 4. Compose UI Layout
- `PixelMindStudioScreen` is split into:
  - Left: tools, grid presets, palette swatches.
  - Center: drawing canvas.
  - Right: AI panel (prompt input, 4-way/8-way generation buttons, layer controls, chat transcript).
- Reactive architecture uses `EditorViewModel` + `StateFlow` to synchronize editor state and AI operations.
