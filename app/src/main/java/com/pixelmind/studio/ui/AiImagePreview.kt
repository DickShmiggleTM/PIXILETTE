package com.pixelmind.studio.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun AiImagePreview(base64Png: String, modifier: Modifier = Modifier) {
    val dataUri = "data:image/png;base64,$base64Png"
    AsyncImage(
        model = dataUri,
        contentDescription = "AI generated sprite preview",
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    )
}
