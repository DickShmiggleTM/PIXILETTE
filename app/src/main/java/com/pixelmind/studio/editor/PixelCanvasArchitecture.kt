package com.pixelmind.studio.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import com.pixelmind.studio.model.PixelPoint

@Composable
fun PixelCanvas(
    state: EditorUiState,
    onDrawPixel: (PixelPoint) -> Unit,
    onViewport: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color(0xFF1A1A1A))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.selectedTool, state.zoom, state.pan) {
                    detectDragGestures { change, _ ->
                        onDrawPixel(change.position.toPixelPoint(state))
                    }
                }
                .pointerInput(state.zoom, state.pan) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        val newZoom = (state.zoom * zoomChange).coerceIn(0.5f, 32f)
                        onViewport(newZoom, state.pan + panChange)
                    }
                },
        ) {
            drawPixelContent(state)
        }
    }
}

private fun DrawScope.drawPixelContent(state: EditorUiState) {
    translate(state.pan.x, state.pan.y) {
        scale(state.zoom, state.zoom, pivot = Offset.Zero) {
            drawCheckerboard(state)
            state.layers.filter { it.isVisible }.forEach { layer ->
                layer.pixels.forEach { (point, color) ->
                    drawRect(
                        color = color,
                        topLeft = Offset(point.x * state.pixelSizePx, point.y * state.pixelSizePx),
                        size = androidx.compose.ui.geometry.Size(state.pixelSizePx, state.pixelSizePx),
                    )
                }
            }
            if (state.showGrid) drawGrid(state)
        }
    }
}

private fun DrawScope.drawCheckerboard(state: EditorUiState) {
    val a = Color(0xFF2A2A2A)
    val b = Color(0xFF333333)
    for (y in 0 until state.canvasSize.height) {
        for (x in 0 until state.canvasSize.width) {
            drawRect(
                color = if ((x + y) % 2 == 0) a else b,
                topLeft = Offset(x * state.pixelSizePx, y * state.pixelSizePx),
                size = androidx.compose.ui.geometry.Size(state.pixelSizePx, state.pixelSizePx),
            )
        }
    }
}

private fun DrawScope.drawGrid(state: EditorUiState) {
    val gridColor = Color(0x30FFFFFF)
    repeat(state.canvasSize.width + 1) { x ->
        val xPos = x * state.pixelSizePx
        drawLine(
            color = gridColor,
            start = Offset(xPos, 0f),
            end = Offset(xPos, state.canvasSize.height * state.pixelSizePx),
        )
    }
    repeat(state.canvasSize.height + 1) { y ->
        val yPos = y * state.pixelSizePx
        drawLine(
            color = gridColor,
            start = Offset(0f, yPos),
            end = Offset(state.canvasSize.width * state.pixelSizePx, yPos),
        )
    }
}

private fun Offset.toPixelPoint(state: EditorUiState): PixelPoint {
    val x = ((x - state.pan.x) / (state.pixelSizePx * state.zoom)).toInt()
    val y = ((y - state.pan.y) / (state.pixelSizePx * state.zoom)).toInt()
    return PixelPoint(
        x = x.coerceIn(0, state.canvasSize.width - 1),
        y = y.coerceIn(0, state.canvasSize.height - 1),
    )
}
