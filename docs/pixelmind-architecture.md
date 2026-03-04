# PixelMind Studio (Phase 1) High-Level Architecture

## PixelCanvas Composable
- `PixelCanvas` is stateless and receives `StateFlow<PixelCanvasUiState>` from `EditorViewModel`.
- Gesture handlers emit `CanvasAction` intents:
  - Drag for pencil/eraser/bucket/eyedropper.
  - Pinch and two-finger pan for viewport transforms.
- `PixelCanvasUiState` stores grid size, active tool/color, layers, and viewport transforms.
- Rendering pipeline:
  1. Draw visible layers in order.
  2. Overlay grid lines if enabled.
  3. Export helpers produce deterministic PNG dimensions.

## Multi-provider AI Networking
- `ApiService` defines provider-agnostic `chatCompletion` and `imageEdit` endpoints.
- `ProviderConfig` stores selected provider, endpoints, API key, and model metadata.
- Chat payloads include text + Base64 image blocks (`MessageContent.ImageBase64`) so vision models can inspect the current canvas.
- `AiGateway` abstracts orchestration for:
  - conversational sprite edits,
  - sprite-to-sheet generation (4x4 etc),
  - directional frame generation with prompt-engineering presets.
