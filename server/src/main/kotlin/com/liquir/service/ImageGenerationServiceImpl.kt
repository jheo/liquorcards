package com.liquir.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Service
class ImageGenerationServiceImpl(
    @Value("\${app.ai.google-api-key:}") private val googleApiKey: String,
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) : ImageGenerationService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newHttpClient()

    override fun generateImage(keyword: String): String? {
        if (googleApiKey.isBlank()) {
            logger.warn("Google API key not configured, skipping image generation")
            return null
        }

        return try {
            val prompt = "Professional product photograph of $keyword on a clean dark background, studio lighting, high quality, photorealistic, no text or labels"

            val requestBody = mapper.writeValueAsString(mapOf(
                "contents" to listOf(mapOf(
                    "parts" to listOf(mapOf("text" to prompt))
                )),
                "generationConfig" to mapOf(
                    "responseModalities" to listOf("TEXT", "IMAGE")
                )
            ))

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-preview-image-generation:generateContent?key=$googleApiKey"

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                logger.error("Gemini image generation failed with status {}: {}", response.statusCode(), response.body())
                return null
            }

            val responseJson: Map<String, Any> = mapper.readValue(response.body())
            val candidates = responseJson["candidates"] as? List<*> ?: return null
            val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return null
            val content = firstCandidate["content"] as? Map<*, *> ?: return null
            val parts = content["parts"] as? List<*> ?: return null

            for (part in parts) {
                val partMap = part as? Map<*, *> ?: continue
                val inlineData = partMap["inlineData"] as? Map<*, *> ?: continue
                val base64Data = inlineData["data"] as? String ?: continue
                val mimeType = inlineData["mimeType"] as? String ?: "image/png"

                val extension = when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("webp") -> "webp"
                    else -> "png"
                }

                val filename = "${UUID.randomUUID()}.$extension"
                val uploadPath = Paths.get(uploadDir)
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath)
                }

                val filePath = uploadPath.resolve(filename)
                Files.write(filePath, Base64.getDecoder().decode(base64Data))

                logger.info("Generated image saved: {}", filename)
                return "/api/images/$filename"
            }

            logger.warn("No image data found in Gemini response")
            null
        } catch (e: Exception) {
            logger.error("Image generation failed: {}", e.message, e)
            null
        }
    }
}
