package com.pixelmind.studio.ai

import com.pixelmind.studio.ai.network.AiProvider
import com.pixelmind.studio.ai.network.ProviderConfig

object ProviderSettings {
    fun defaultConfigs(apiKeys: Map<AiProvider, String>): List<ProviderConfig> = listOf(
        ProviderConfig(
            provider = AiProvider.OPEN_ROUTER,
            baseUrl = "https://openrouter.ai/api/v1/",
            chatEndpoint = "chat/completions",
            imageEndpoint = "images/edits",
            apiKey = apiKeys[AiProvider.OPEN_ROUTER].orEmpty(),
            model = "openai/gpt-4o-mini",
            supportsVision = true,
        ),
        ProviderConfig(
            provider = AiProvider.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/",
            chatEndpoint = "models/gemini-1.5-pro:generateContent",
            imageEndpoint = "models/gemini-1.5-pro:generateContent",
            apiKey = apiKeys[AiProvider.GEMINI].orEmpty(),
            model = "gemini-1.5-pro",
            supportsVision = true,
        ),
        ProviderConfig(
            provider = AiProvider.MISTRAL,
            baseUrl = "https://api.mistral.ai/v1/",
            chatEndpoint = "chat/completions",
            imageEndpoint = "images/edits",
            apiKey = apiKeys[AiProvider.MISTRAL].orEmpty(),
            model = "pixtral-large-latest",
            supportsVision = true,
        ),
        ProviderConfig(
            provider = AiProvider.HUGGING_CHAT,
            baseUrl = "https://api-inference.huggingface.co/",
            chatEndpoint = "v1/chat/completions",
            imageEndpoint = "v1/images/edits",
            apiKey = apiKeys[AiProvider.HUGGING_CHAT].orEmpty(),
            model = "Qwen/Qwen2.5-VL-72B-Instruct",
            supportsVision = true,
        ),
    )
}
