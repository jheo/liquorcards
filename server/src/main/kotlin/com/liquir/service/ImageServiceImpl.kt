package com.liquir.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import jakarta.annotation.PostConstruct

@Service
class ImageServiceImpl(
    @Value("\${app.upload-dir:uploads}") private val uploadDir: String
) : ImageService {

    private lateinit var uploadPath: Path

    @PostConstruct
    fun init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize()
        Files.createDirectories(uploadPath)
    }

    override fun storeImage(file: MultipartFile): String {
        val originalFilename = file.originalFilename ?: "image"
        val extension = originalFilename.substringAfterLast('.', "jpg")
        val filename = "${UUID.randomUUID()}.$extension"

        val targetLocation = uploadPath.resolve(filename)
        Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)

        return filename
    }

    override fun getImagePath(filename: String): Path {
        return uploadPath.resolve(filename).normalize()
    }
}
