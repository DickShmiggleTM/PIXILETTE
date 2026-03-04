package com.pixelmind.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelmind.studio.ai.AiAssistantOrchestrator
import com.pixelmind.studio.ai.network.AiGateway
import com.pixelmind.studio.ai.network.MultiProviderChatResponse
import com.pixelmind.studio.ai.network.MultiProviderImageResponse
import com.pixelmind.studio.ai.network.ProviderConfig
import com.pixelmind.studio.editor.EditorViewModel
import com.pixelmind.studio.ui.PixelMindStudioScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: EditorViewModel = viewModel(factory = EditorViewModelFactory())
            PixelMindStudioScreen(viewModel = vm)
        }
    }
}

private class EditorViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val orchestrator = AiAssistantOrchestrator(
            gateway = LocalDebugAiGateway,
            selectedProvider = AiAssistantOrchestrator.defaultProvider(apiKey = ""),
        )
        return EditorViewModel(orchestrator) as T
    }
}

private object LocalDebugAiGateway : AiGateway {
    override suspend fun runSpriteAssistant(
        providerConfig: ProviderConfig,
        systemPrompt: String,
        userPrompt: String,
        canvasBase64: String,
    ): MultiProviderChatResponse = MultiProviderChatResponse()

    override suspend fun runSpriteSheetGenerator(
        providerConfig: ProviderConfig,
        prompt: String,
        spriteBase64: String,
        framesAcross: Int,
        framesDown: Int,
    ): MultiProviderImageResponse = MultiProviderImageResponse()
}
