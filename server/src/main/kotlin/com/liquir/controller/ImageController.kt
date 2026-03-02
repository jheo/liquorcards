package com.liquir.controller

import com.liquir.service.ImageService
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

@RestController
@RequestMapping("/api/images")
class ImageController(
    private val imageService: ImageService
) {

    @PostMapping("/upload")
    fun uploadImage(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "File is empty"))
        }

        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "File must be an image"))
        }

        val filename = imageService.storeImage(file)
        val imageUrl = "/api/images/$filename"

        return ResponseEntity.ok(mapOf(
            "filename" to filename,
            "url" to imageUrl
        ))
    }

    @GetMapping("/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<Resource> {
        val filePath = imageService.getImagePath(filename)

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build()
        }

        val resource = UrlResource(filePath.toUri())
        val contentType = Files.probeContentType(filePath) ?: "application/octet-stream"

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"$filename\"")
            .body(resource)
    }
}
