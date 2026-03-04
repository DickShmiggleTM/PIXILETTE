package com.pixelmind.studio.ai

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.Color
import com.pixelmind.studio.ai.network.AiGateway
import com.pixelmind.studio.ai.network.AiProvider
import com.pixelmind.studio.ai.network.ProviderConfig
import com.pixelmind.studio.editor.EditorUiState
import com.pixelmind.studio.model.DirectionMode
import com.pixelmind.studio.model.PixelLayer
import com.pixelmind.studio.model.PixelPoint
import com.pixelmind.studio.model.SpriteSheetRequest
import java.io.ByteArrayOutputStream

data class AiEditResult(
    val summary: String,
    val updatedLayers: List<PixelLayer>? = null,
)

class AiAssistantOrchestrator(
    private val gateway: AiGateway,
    private val selectedProvider: ProviderConfig,
) {

    suspend fun generateEdit(prompt: String, uiState: EditorUiState): AiEditResult {
        val canvasBase64 = layerStackToBase64(uiState)
        val systemPrompt = pixelPerfectSystemPrompt()

        val response = gateway.runSpriteAssistant(
            providerConfig = selectedProvider,
            systemPrompt = systemPrompt,
            userPrompt = prompt,
            canvasBase64 = canvasBase64,
        )

        val assistantText = response.choices.firstOrNull()?.message?.content
            ?: "No response from provider."

        return AiEditResult(summary = assistantText)
    }

    suspend fun generateSpriteSheet(
        request: SpriteSheetRequest,
        uiState: EditorUiState,
    ): AiEditResult {
        val spriteBase64 = layerStackToBase64(uiState)
        val directionText = if (request.directionMode == DirectionMode.EightWay) "8-way" else "4-way"

        val prompt = buildString {
            append("Generate a $directionText pixel art sprite sheet with ${request.framesAcross}x${request.framesDown} frames. ")
            append("Action: ${request.actionDescription}. ")
            append("Keep palette and silhouette consistent with input sprite, pixel-perfect, no anti-aliasing.")
        }

        val response = gateway.runSpriteSheetGenerator(
            providerConfig = selectedProvider,
            prompt = prompt,
            spriteBase64 = spriteBase64,
            framesAcross = request.framesAcross,
            framesDown = request.framesDown,
        )

        val image = response.images.firstOrNull()
            ?: return AiEditResult(summary = "No image returned.")

        val updated = decodeSpriteSheetIntoLayer(image.base64Png)
        return AiEditResult(
            summary = "Generated ${request.framesAcross}x${request.framesDown} sprite sheet (${directionText}).",
            updatedLayers = listOf(updated),
        )
    }

    private fun layerStackToBase64(uiState: EditorUiState): String {
        val bitmap = Bitmap.createBitmap(uiState.canvasSize.width, uiState.canvasSize.height, Bitmap.Config.ARGB_8888)
        for (layer in uiState.layers.filter { it.isVisible }) {
            for ((point, color) in layer.pixels) {
                bitmap.setPixel(point.x, point.y, color.toArgb())
            }
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeSpriteSheetIntoLayer(base64Png: String): PixelLayer {
        val bytes = Base64.decode(base64Png, Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val pixels = mutableMapOf<PixelPoint, Color>()

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val argb = bitmap.getPixel(x, y)
                val color = Color(argb)
                if (color.alpha > 0f) {
                    pixels[PixelPoint(x, y)] = color
                }
            }
        }

        return PixelLayer(name = "AI Sprite Sheet", pixels = pixels)
    }

    private fun pixelPerfectSystemPrompt(): String =
        """
            You are PixelMind Studio's sprite assistant.
            Rules:
            1) Respect exact pixel art style, hard edges only, no anti-aliasing, no blur.
            2) Keep color palette consistent with incoming sprite unless user explicitly asks for recolor.
            3) Maintain character silhouette and proportions.
            4) If outputting instructions, respond as concise numbered pixel edit steps.
            5) If outputting an image, ensure PNG with transparent background.
        """.trimIndent()

    companion object {
        fun defaultProvider(apiKey: String): ProviderConfig = ProviderConfig(
            provider = AiProvider.OPEN_ROUTER,
            baseUrl = "https://openrouter.ai/api/v1/",
            chatEndpoint = "chat/completions",
            imageEndpoint = "images/edits",
            apiKey = apiKey,
            model = "openai/gpt-4o-mini",
            supportsVision = true,
        )
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
