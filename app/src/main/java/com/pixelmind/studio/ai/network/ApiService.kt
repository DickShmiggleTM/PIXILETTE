package com.pixelmind.studio.ai.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Provider-agnostic Retrofit contract. Each provider descriptor can supply base URL, headers,
 * model id, and endpoint while sharing the same internal request model.
 */
interface ApiService {

    @POST
    suspend fun chatCompletion(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String? = null,
        @Header("X-Title") appTitle: String? = null,
        @Body request: MultiProviderChatRequest,
    ): MultiProviderChatResponse

    @POST
    suspend fun imageEdit(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Body request: MultiProviderImageRequest,
    ): MultiProviderImageResponse
}

enum class AiProvider {
    OPEN_ROUTER,
    GEMINI,
    MISTRAL,
    HUGGING_CHAT,
}

data class ProviderConfig(
    val provider: AiProvider,
    val baseUrl: String,
    val chatEndpoint: String,
    val imageEndpoint: String,
    val apiKey: String,
    val model: String,
    val supportsVision: Boolean,
)

data class MultiProviderChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.35f,
    val maxTokens: Int = 1200,
)

data class ChatMessage(
    val role: String,
    val content: List<MessageContent>,
)

sealed interface MessageContent {
    data class Text(val text: String) : MessageContent
    data class ImageBase64(val mediaType: String, val base64Data: String) : MessageContent
}

data class MultiProviderChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList(),
)

data class ChatChoice(
    val index: Int = 0,
    val message: AssistantMessage,
)

data class AssistantMessage(
    val role: String = "assistant",
    val content: String,
)

data class MultiProviderImageRequest(
    val model: String,
    val prompt: String,
    val imageBase64: String,
    val maskBase64: String? = null,
    val outputFormat: String = "png",
)

data class MultiProviderImageResponse(
    val images: List<GeneratedImage> = emptyList(),
)

data class GeneratedImage(
    val base64Png: String,
    val width: Int,
    val height: Int,
)

/**
 * This contract allows the chat feature to pass bitmap context to vision models and parse both
 * text instructions + image outputs for sprite or sprite sheet updates.
 */
interface AiGateway {
    suspend fun runSpriteAssistant(
        providerConfig: ProviderConfig,
        systemPrompt: String,
        userPrompt: String,
        canvasBase64: String,
    ): MultiProviderChatResponse

    suspend fun runSpriteSheetGenerator(
        providerConfig: ProviderConfig,
        prompt: String,
        spriteBase64: String,
        framesAcross: Int,
        framesDown: Int,
    ): MultiProviderImageResponse
}
