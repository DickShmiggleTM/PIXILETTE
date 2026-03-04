package com.pixelmind.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixelmind.studio.editor.EditorViewModel
import com.pixelmind.studio.editor.PixelCanvas
import com.pixelmind.studio.model.CanvasSize
import com.pixelmind.studio.model.PixelTool

@Composable
fun PixelMindStudioScreen(viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    Row(modifier = modifier.fillMaxSize().background(Color(0xFF111111))) {
        Column(
            modifier = Modifier
                .width(112.dp)
                .fillMaxHeight()
                .background(Color(0xFF191919))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolButton("✏️") { viewModel.setTool(PixelTool.Pencil) }
            ToolButton("🩹") { viewModel.setTool(PixelTool.Eraser) }
            ToolButton("🪣") { viewModel.setTool(PixelTool.Bucket) }
            ToolButton("🎯") { viewModel.setTool(PixelTool.Eyedropper) }

            HorizontalDivider()
            Text("Grid", color = Color.White)
            Button(onClick = { viewModel.setCanvasSize(CanvasSize(16, 16)) }) { Text("16") }
            Button(onClick = { viewModel.setCanvasSize(CanvasSize(32, 32)) }) { Text("32") }
            Button(onClick = { viewModel.setCanvasSize(CanvasSize(64, 64)) }) { Text("64") }

            HorizontalDivider()
            Text("Palette", color = Color.White)
            state.palette.forEach {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(it.color, CircleShape)
                        .clickable { viewModel.setColor(it.color) },
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            PixelCanvas(
                state = state,
                onDrawPixel = viewModel::drawPixel,
                onViewport = viewModel::updateViewport,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .background(Color(0xFF181B26))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("AI Assistant", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text("Providers: OpenRouter · Gemini · Mistral · HuggingChat", color = Color.LightGray)

            OutlinedTextField(
                value = state.userPrompt,
                onValueChange = viewModel::setPrompt,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prompt") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::sendPromptWithCanvasContext) { Text("Apply Edit") }
                Button(onClick = { viewModel.runDirectionalSpriteGenerator(false) }) { Text("4-way") }
                Button(onClick = { viewModel.runDirectionalSpriteGenerator(true) }) { Text("8-way") }
            }

            Text("Layers", color = Color.White)
            state.layers.forEach { layer ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(layer.name, color = if (layer.id == state.activeLayerId) Color.Cyan else Color.White)
                    Text(if (layer.isVisible) "👁" else "🚫", modifier = Modifier.clickable { viewModel.toggleLayerVisibility(layer.id) })
                    Text("✖", modifier = Modifier.clickable { viewModel.deleteLayer(layer.id) })
                }
            }
            Button(onClick = viewModel::addLayer) { Text("Add Layer") }

            HorizontalDivider()
            Text("Chat", color = Color.White)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.chatHistory) { message ->
                    Text("${message.role.uppercase()}: ${message.text}", color = Color.LightGray)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
