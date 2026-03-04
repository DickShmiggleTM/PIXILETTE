package com.pixelmind.studio.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class PixelPoint(val x: Int, val y: Int)

enum class PixelTool {
    Pencil,
    Eraser,
    Bucket,
    Eyedropper,
}

data class PixelLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val pixels: Map<PixelPoint, Color> = emptyMap(),
)

data class PaletteColor(
    val id: String = UUID.randomUUID().toString(),
    val color: Color,
)

data class CanvasSize(
    val width: Int,
    val height: Int,
) {
    fun supportsPreset(): Boolean =
        (width == 16 && height == 16) ||
            (width == 32 && height == 32) ||
            (width == 64 && height == 64)
}

data class SpriteSheetRequest(
    val directionMode: DirectionMode,
    val framesAcross: Int,
    val framesDown: Int,
    val actionDescription: String,
)

enum class DirectionMode {
    FourWay,
    EightWay,
}
