package com.pixelmind.studio.io

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.pixelmind.studio.editor.EditorUiState
import com.pixelmind.studio.model.CanvasSize
import com.pixelmind.studio.model.PaletteColor
import com.pixelmind.studio.model.PixelLayer
import com.pixelmind.studio.model.PixelPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ProjectSerialization {

    fun saveProjectJson(state: EditorUiState, file: File) {
        val root = JSONObject().apply {
            put("width", state.canvasSize.width)
            put("height", state.canvasSize.height)
            put("activeLayerId", state.activeLayerId)
            put("palette", JSONArray().apply {
                state.palette.forEach { color -> put(color.color.toHexArgb()) }
            })
            put("layers", JSONArray().apply {
                state.layers.forEach { layer ->
                    put(JSONObject().apply {
                        put("id", layer.id)
                        put("name", layer.name)
                        put("isVisible", layer.isVisible)
                        put("pixels", JSONArray().apply {
                            layer.pixels.forEach { (point, color) ->
                                put(JSONObject().apply {
                                    put("x", point.x)
                                    put("y", point.y)
                                    put("argb", color.toHexArgb())
                                })
                            }
                        })
                    })
                }
            })
        }

        file.writeText(root.toString(2))
    }

    fun loadProjectJson(file: File): EditorUiState {
        val json = JSONObject(file.readText())
        val width = json.getInt("width")
        val height = json.getInt("height")

        val palette = json.getJSONArray("palette").toStringList().map {
            PaletteColor(color = Color(AndroidColor.parseColor(it)))
        }

        val layers = json.getJSONArray("layers").let { arr ->
            (0 until arr.length()).map { idx ->
                val layerObj = arr.getJSONObject(idx)
                val pixelsArray = layerObj.getJSONArray("pixels")
                val pixels = mutableMapOf<PixelPoint, Color>()

                for (pixelIdx in 0 until pixelsArray.length()) {
                    val p = pixelsArray.getJSONObject(pixelIdx)
                    val point = PixelPoint(p.getInt("x"), p.getInt("y"))
                    pixels[point] = Color(AndroidColor.parseColor(p.getString("argb")))
                }

                PixelLayer(
                    id = layerObj.getString("id"),
                    name = layerObj.getString("name"),
                    isVisible = layerObj.getBoolean("isVisible"),
                    pixels = pixels,
                )
            }
        }

        return EditorUiState(
            canvasSize = CanvasSize(width, height),
            layers = layers,
            activeLayerId = json.optString("activeLayerId"),
            palette = palette,
        )
    }

    fun exportPng(state: EditorUiState, outputFile: File, scaleFactor: Int = 8) {
        val src = Bitmap.createBitmap(state.canvasSize.width, state.canvasSize.height, Bitmap.Config.ARGB_8888)

        state.layers.filter { it.isVisible }.forEach { layer ->
            layer.pixels.forEach { (point, color) ->
                src.setPixel(point.x, point.y, color.toAndroidColor())
            }
        }

        val scaled = Bitmap.createScaledBitmap(
            src,
            state.canvasSize.width * scaleFactor,
            state.canvasSize.height * scaleFactor,
            false,
        )

        FileOutputStream(outputFile).use {
            scaled.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

private fun JSONArray.toStringList(): List<String> = buildList {
    for (i in 0 until length()) add(getString(i))
}

private fun Color.toHexArgb(): String =
    "#%08X".format(
        ((alpha * 255).toInt() shl 24) or
            ((red * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8) or
            (blue * 255).toInt(),
    )

private fun Color.toAndroidColor(): Int = AndroidColor.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
