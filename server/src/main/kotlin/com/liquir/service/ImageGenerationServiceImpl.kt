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
import java.nio.file.Paths
import java.time.Duration
import java.util.*

@Service
class ImageGenerationServiceImpl(
    @Value("\${app.ai.google-api-key:}") private val googleApiKey: String,
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) : ImageGenerationService {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    override fun generateImage(keyword: String, bottleVisualDescription: String?): String? {
        return generateImage(keyword, bottleVisualDescription, emptyList())
    }

    override fun generateImage(keyword: String, bottleVisualDescription: String?, externalImageUrls: List<String>): String? {
        if (googleApiKey.isBlank()) {
            logger.warn("Google API key not configured, skipping image generation")
            return null
        }

        return try {
            val referenceImages = mutableListOf<String>()

            for (url in externalImageUrls.take(4)) {
                val base64 = downloadImageAsBase64(url)
                if (base64 != null) {
                    referenceImages.add(base64)
                    logger.info("Downloaded external reference image from: {}", url.take(80))
                }
            }

            logger.info("Total {} reference images for '{}'", referenceImages.size, keyword)

            if (referenceImages.isNotEmpty()) {
                generateWithMultipleReferences(referenceImages, keyword, bottleVisualDescription)
            } else {
                generateFromDescription(keyword, bottleVisualDescription)
            }
        } catch (e: Exception) {
            logger.error("Image generation failed: {}", e.message, e)
            null
        }
    }

    /**
     * Download an image URL and return as base64 string. Returns null if failed.
     */
    fun downloadImageAsBase64(imageUrl: String): String? {
        // Skip SVGs, logos, icons
        val urlLower = imageUrl.lowercase()
        if (urlLower.endsWith(".svg") || urlLower.contains("logo") || urlLower.contains("/icon")) {
            logger.debug("Skipping non-photo image: {}", imageUrl)
            return null
        }

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .timeout(Duration.ofSeconds(8))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

            // Must be a real image (>5KB to filter tiny icons) and <5MB
            if (response.statusCode() == 200 && response.body().size in 5_000..5_000_000) {
                Base64.getEncoder().encodeToString(response.body())
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Failed to download image from '{}': {}", imageUrl, e.message)
            null
        }
    }

    /**
     * Generate image using multiple reference images + Gemini.
     */
    private fun generateWithMultipleReferences(
        referenceImages: List<String>,
        keyword: String,
        bottleVisualDescription: String?
    ): String? {
        val descSection = if (!bottleVisualDescription.isNullOrBlank()) {
            "\nBottle details: $bottleVisualDescription"
        } else ""

        val prompt = """Using these reference images of "$keyword", recreate this exact liquor bottle as a premium product photograph.
$descSection
CRITICAL Requirements:
- The BOTTLE SHAPE, LABEL DESIGN, and BRAND MARKINGS must be faithfully reproduced from the reference images
- The label text, logo, colors, and typography must be clearly legible and accurate
- Show the FULL bottle from cap/cork to base, nothing cropped
- Clean dark gradient background (charcoal to near-black)
- Centered composition, bottle occupying 60-70% of frame height
- Professional studio lighting with soft highlights on glass, showing liquid color through glass
- Photorealistic, high resolution quality
- No text overlays, no watermarks, no additional objects
- The bottle must look like an actual product photo you'd see on a retailer website"""

        val parts = mutableListOf<Map<String, Any>>()
        parts.add(mapOf("text" to prompt))

        // Add reference images (up to 4 to keep request size reasonable)
        for (imgBase64 in referenceImages.take(4)) {
            parts.add(mapOf(
                "inlineData" to mapOf(
                    "mimeType" to "image/jpeg",
                    "data" to imgBase64
                )
            ))
        }

        val requestBody = mapper.writeValueAsString(mapOf(
            "contents" to listOf(mapOf("parts" to parts)),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("TEXT", "IMAGE")
            )
        ))

        return callGeminiAndSave(requestBody)
    }

    private fun generateFromDescription(keyword: String, bottleVisualDescription: String?): String? {
        val prompt = if (!bottleVisualDescription.isNullOrBlank()) {
            """Professional product photograph of "$keyword" bottle with label clearly visible.
$bottleVisualDescription
- Show the FULL bottle from cap/cork to base with the label facing the camera
- The label, brand name, and all text on the bottle must be clearly readable
- Clean dark gradient background (charcoal to near-black)
- Centered composition, bottle occupying 60-70% of frame height
- Professional studio lighting showing liquid color through glass
- Photorealistic, high resolution quality
- No text overlays, no watermarks, no additional objects"""
        } else {
            """Professional product photograph of "$keyword" liquor bottle with label clearly visible.
- Show the FULL bottle from cap/cork to base with the label facing the camera
- The brand name and label must be clearly readable
- Clean dark gradient background
- Centered composition, studio lighting, photorealistic, high quality
- No additional text or objects"""
        }

        val requestBody = mapper.writeValueAsString(mapOf(
            "contents" to listOf(mapOf(
                "parts" to listOf(mapOf("text" to prompt))
            )),
            "generationConfig" to mapOf(
                "responseModalities" to listOf("TEXT", "IMAGE")
            )
        ))

        return callGeminiAndSave(requestBody)
    }

    private fun callGeminiAndSave(requestBody: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-pro-image-preview:generateContent?key=$googleApiKey"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(60))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            logger.error("Gemini image generation failed with status {}: {}", response.statusCode(),
                response.body().take(500))
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

            logger.info("Generated image saved: {} (from {} reference images)", filename,
                parts.count { (it as? Map<*, *>)?.containsKey("inlineData") == true })
            return "/api/images/$filename"
        }

        logger.warn("No image data found in Gemini response")
        return null
    }
}
