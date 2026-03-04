package com.pixelmind.studio.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level state contract for [PixelCanvas] to keep UI stateless and ViewModel-driven.
 */
data class PixelCanvasUiState(
    val gridWidth: Int = 32,
    val gridHeight: Int = 32,
    val pixelSizePx: Float = 16f,
    val zoom: Float = 1f,
    val pan: Offset = Offset.Zero,
    val activeColor: Color = Color.Black,
    val activeTool: PixelTool = PixelTool.Pencil,
    val layers: List<PixelLayer> = emptyList(),
    val activeLayerId: String? = null,
    val showGrid: Boolean = true,
)

enum class PixelTool {
    Pencil,
    Eraser,
    Bucket,
    Eyedropper,
}

data class PixelLayer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val pixels: Map<PixelPoint, Color> = emptyMap(),
)

data class PixelPoint(val x: Int, val y: Int)

/**
 * Intents emitted by the canvas to mutate editor state from a ViewModel/reducer.
 */
sealed interface CanvasAction {
    data class SetPixel(val point: PixelPoint, val color: Color) : CanvasAction
    data class ErasePixel(val point: PixelPoint) : CanvasAction
    data class FillRegion(val point: PixelPoint, val color: Color) : CanvasAction
    data class PickColor(val point: PixelPoint) : CanvasAction
    data class UpdateViewport(val zoom: Float, val pan: Offset) : CanvasAction
}

@Composable
fun PixelCanvas(
    uiStateFlow: StateFlow<PixelCanvasUiState>,
    onAction: (CanvasAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by uiStateFlow.collectAsState()

    LaunchedEffect(uiState.gridWidth, uiState.gridHeight) {
        // Hook for initializing empty layers or migration logic when canvas size changes.
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.activeTool) {
                    detectDragGestures { change, _ ->
                        val point = change.position.toPixelPoint(uiState)
                        when (uiState.activeTool) {
                            PixelTool.Pencil -> onAction(CanvasAction.SetPixel(point, uiState.activeColor))
                            PixelTool.Eraser -> onAction(CanvasAction.ErasePixel(point))
                            PixelTool.Bucket -> onAction(CanvasAction.FillRegion(point, uiState.activeColor))
                            PixelTool.Eyedropper -> onAction(CanvasAction.PickColor(point))
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        val newZoom = (uiState.zoom * zoomChange).coerceIn(0.5f, 32f)
                        val newPan = uiState.pan + panChange
                        onAction(CanvasAction.UpdateViewport(newZoom, newPan))
                    }
                },
        ) {
            drawLayers(uiState)
            if (uiState.showGrid) drawGrid(uiState)
        }
    }
}

private fun DrawScope.drawLayers(uiState: PixelCanvasUiState) {
    val visibleLayers = uiState.layers.filter { it.isVisible }
    visibleLayers.forEach { layer ->
        layer.pixels.forEach { (point, color) ->
            val left = point.x * uiState.pixelSizePx
            val top = point.y * uiState.pixelSizePx
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(uiState.pixelSizePx, uiState.pixelSizePx),
            )
        }
    }
}

private fun DrawScope.drawGrid(uiState: PixelCanvasUiState) {
    val gridColor = Color(0x33FFFFFF)
    repeat(uiState.gridWidth + 1) { x ->
        val xPos = x * uiState.pixelSizePx
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, uiState.gridHeight * uiState.pixelSizePx),
        )
    }

    repeat(uiState.gridHeight + 1) { y ->
        val yPos = y * uiState.pixelSizePx
        drawLine(
            color = gridColor,
            start = Offset(0f, yPos),
            end = Offset(uiState.gridWidth * uiState.pixelSizePx, yPos),
        )
    }
}

private fun Offset.toPixelPoint(uiState: PixelCanvasUiState): PixelPoint {
    val normalizedX = ((x - uiState.pan.x) / (uiState.pixelSizePx * uiState.zoom)).toInt()
    val normalizedY = ((y - uiState.pan.y) / (uiState.pixelSizePx * uiState.zoom)).toInt()

    return PixelPoint(
        x = normalizedX.coerceIn(0, uiState.gridWidth - 1),
        y = normalizedY.coerceIn(0, uiState.gridHeight - 1),
    )
}

fun PixelCanvasUiState.canvasBoundsPx(): Rect {
    val width = gridWidth * pixelSizePx * zoom
    val height = gridHeight * pixelSizePx * zoom
    return Rect(Offset.Zero, androidx.compose.ui.geometry.Size(width, height))
}

fun PixelCanvasUiState.exportSize(scaleFactor: Int): IntSize {
    return IntSize(gridWidth * scaleFactor, gridHeight * scaleFactor)
}
