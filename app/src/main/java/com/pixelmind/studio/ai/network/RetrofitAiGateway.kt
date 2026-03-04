package com.pixelmind.studio.ai.network

class RetrofitAiGateway(
    private val apiService: ApiService,
) : AiGateway {

    override suspend fun runSpriteAssistant(
        providerConfig: ProviderConfig,
        systemPrompt: String,
        userPrompt: String,
        canvasBase64: String,
    ): MultiProviderChatResponse {
        val request = MultiProviderChatRequest(
            model = providerConfig.model,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = listOf(MessageContent.Text(systemPrompt)),
                ),
                ChatMessage(
                    role = "user",
                    content = listOf(
                        MessageContent.Text(userPrompt),
                        MessageContent.ImageBase64(
                            mediaType = "image/png",
                            base64Data = canvasBase64,
                        ),
                    ),
                ),
            ),
        )

        return apiService.chatCompletion(
            endpoint = providerConfig.chatEndpoint,
            authorization = "Bearer ${providerConfig.apiKey}",
            referer = "https://pixelmind.studio",
            appTitle = "PixelMind Studio",
            request = request,
        )
    }

    override suspend fun runSpriteSheetGenerator(
        providerConfig: ProviderConfig,
        prompt: String,
        spriteBase64: String,
        framesAcross: Int,
        framesDown: Int,
    ): MultiProviderImageResponse {
        val request = MultiProviderImageRequest(
            model = providerConfig.model,
            prompt = "$prompt Return a ${framesAcross}x${framesDown} sheet.",
            imageBase64 = spriteBase64,
        )

        return apiService.imageEdit(
            endpoint = providerConfig.imageEndpoint,
            authorization = "Bearer ${providerConfig.apiKey}",
            request = request,
        )
    }
}
