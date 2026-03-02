package com.liquir.service

interface ImageGenerationService {
    fun generateImage(keyword: String, bottleVisualDescription: String? = null): String?

    /**
     * Generate image with additional external reference image URLs that were collected from databases.
     */
    fun generateImage(keyword: String, bottleVisualDescription: String?, externalImageUrls: List<String>): String?
}
