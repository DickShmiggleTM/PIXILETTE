package com.pixelmind.studio.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelmind.studio.ai.AiAssistantOrchestrator
import com.pixelmind.studio.model.CanvasSize
import com.pixelmind.studio.model.DirectionMode
import com.pixelmind.studio.model.PaletteColor
import com.pixelmind.studio.model.PixelLayer
import com.pixelmind.studio.model.PixelPoint
import com.pixelmind.studio.model.PixelTool
import com.pixelmind.studio.model.SpriteSheetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessageUi(
    val role: String,
    val text: String,
)

data class EditorUiState(
    val canvasSize: CanvasSize = CanvasSize(32, 32),
    val pixelSizePx: Float = 22f,
    val zoom: Float = 1f,
    val pan: Offset = Offset.Zero,
    val selectedTool: PixelTool = PixelTool.Pencil,
    val selectedColor: Color = Color.Black,
    val palette: List<PaletteColor> = listOf(
        PaletteColor(color = Color.Black),
        PaletteColor(color = Color.White),
        PaletteColor(color = Color.Red),
        PaletteColor(color = Color.Blue),
        PaletteColor(color = Color(0xFF2ECC71)),
    ),
    val layers: List<PixelLayer> = listOf(PixelLayer(name = "Layer 1")),
    val activeLayerId: String? = null,
    val showGrid: Boolean = true,
    val chatHistory: List<ChatMessageUi> = emptyList(),
    val userPrompt: String = "",
    val isAiLoading: Boolean = false,
    val pixelPerfectMode: Boolean = true,
) {
    val activeLayer: PixelLayer?
        get() = layers.firstOrNull { it.id == activeLayerId } ?: layers.firstOrNull()
}

class EditorViewModel(
    private val aiOrchestrator: AiAssistantOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { state ->
            state.copy(activeLayerId = state.layers.firstOrNull()?.id)
        }
    }

    fun setTool(tool: PixelTool) = _uiState.update { it.copy(selectedTool = tool) }

    fun setColor(color: Color) = _uiState.update { it.copy(selectedColor = color) }

    fun setPrompt(prompt: String) = _uiState.update { it.copy(userPrompt = prompt) }

    fun setCanvasSize(size: CanvasSize) {
        _uiState.update { state ->
            state.copy(
                canvasSize = size,
                layers = state.layers.map { layer ->
                    val clipped = layer.pixels.filterKeys { point ->
                        point.x in 0 until size.width && point.y in 0 until size.height
                    }
                    layer.copy(pixels = clipped)
                },
            )
        }
    }

    fun addLayer() {
        _uiState.update { state ->
            val layer = PixelLayer(name = "Layer ${state.layers.size + 1}")
            state.copy(layers = state.layers + layer, activeLayerId = layer.id)
        }
    }

    fun deleteLayer(layerId: String) {
        _uiState.update { state ->
            if (state.layers.size <= 1) return@update state
            val updated = state.layers.filterNot { it.id == layerId }
            state.copy(layers = updated, activeLayerId = updated.last().id)
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        _uiState.update { state ->
            val updated = state.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            }
            state.copy(layers = updated)
        }
    }

    fun selectLayer(layerId: String) = _uiState.update { it.copy(activeLayerId = layerId) }

    fun addPaletteColor(color: Color) {
        _uiState.update { state -> state.copy(palette = state.palette + PaletteColor(color = color)) }
    }

    fun updateViewport(zoom: Float, pan: Offset) {
        _uiState.update { it.copy(zoom = zoom.coerceIn(0.5f, 30f), pan = pan) }
    }

    fun drawPixel(point: PixelPoint) {
        val state = _uiState.value
        when (state.selectedTool) {
            PixelTool.Pencil -> applyToActiveLayer(point, state.selectedColor)
            PixelTool.Eraser -> applyToActiveLayer(point, Color.Transparent, erase = true)
            PixelTool.Bucket -> floodFill(point, state.selectedColor)
            PixelTool.Eyedropper -> pickColor(point)
        }
    }

    private fun applyToActiveLayer(point: PixelPoint, color: Color, erase: Boolean = false) {
        _uiState.update { state ->
            val activeId = state.activeLayerId ?: return@update state
            val updatedLayers = state.layers.map { layer ->
                if (layer.id != activeId) {
                    layer
                } else {
                    val updatedPixels = layer.pixels.toMutableMap()
                    if (erase) {
                        updatedPixels.remove(point)
                    } else {
                        updatedPixels[point] = color
                    }
                    layer.copy(pixels = updatedPixels)
                }
            }
            state.copy(layers = updatedLayers)
        }
    }

    private fun pickColor(point: PixelPoint) {
        val picked = _uiState.value.layers
            .asReversed()
            .firstNotNullOfOrNull { layer -> layer.pixels[point] }
            ?: return
        _uiState.update { it.copy(selectedColor = picked) }
    }

    private fun floodFill(start: PixelPoint, replacement: Color) {
        _uiState.update { state ->
            val activeId = state.activeLayerId ?: return@update state
            val layer = state.layers.firstOrNull { it.id == activeId } ?: return@update state
            val target = layer.pixels[start] ?: Color.Transparent
            if (target == replacement) return@update state

            val pixels = layer.pixels.toMutableMap()
            val queue = ArrayDeque<PixelPoint>()
            val visited = mutableSetOf<PixelPoint>()
            queue.add(start)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (!visited.add(current)) continue
                if (current.x !in 0 until state.canvasSize.width || current.y !in 0 until state.canvasSize.height) continue

                val color = pixels[current] ?: Color.Transparent
                if (color != target) continue

                if (replacement == Color.Transparent) pixels.remove(current) else pixels[current] = replacement
                queue.add(PixelPoint(current.x + 1, current.y))
                queue.add(PixelPoint(current.x - 1, current.y))
                queue.add(PixelPoint(current.x, current.y + 1))
                queue.add(PixelPoint(current.x, current.y - 1))
            }

            val updatedLayers = state.layers.map { if (it.id == activeId) it.copy(pixels = pixels) else it }
            state.copy(layers = updatedLayers)
        }
    }

    fun sendPromptWithCanvasContext() {
        val snapshot = _uiState.value
        if (snapshot.userPrompt.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiLoading = true,
                    chatHistory = it.chatHistory + ChatMessageUi("user", snapshot.userPrompt),
                )
            }

            val response = aiOrchestrator.generateEdit(snapshot.userPrompt, snapshot)

            _uiState.update { state ->
                state.copy(
                    isAiLoading = false,
                    userPrompt = "",
                    chatHistory = state.chatHistory + ChatMessageUi("assistant", response.summary),
                    layers = response.updatedLayers ?: state.layers,
                )
            }
        }
    }

    fun runDirectionalSpriteGenerator(isEightWay: Boolean) {
        val req = SpriteSheetRequest(
            directionMode = if (isEightWay) DirectionMode.EightWay else DirectionMode.FourWay,
            framesAcross = 4,
            framesDown = 4,
            actionDescription = "walk cycle",
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val result = aiOrchestrator.generateSpriteSheet(req, _uiState.value)
            _uiState.update { state ->
                state.copy(
                    isAiLoading = false,
                    chatHistory = state.chatHistory + ChatMessageUi("assistant", result.summary),
                    layers = result.updatedLayers ?: state.layers,
                )
            }
        }
    }
}
