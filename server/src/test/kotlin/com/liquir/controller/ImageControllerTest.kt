package com.liquir.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class ImageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `POST upload should accept a valid image file`() {
        val file = MockMultipartFile(
            "file",
            "test-image.jpg",
            "image/jpeg",
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) // JPEG magic bytes
        )

        mockMvc.perform(multipart("/api/images/upload").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.filename").exists())
            .andExpect(jsonPath("$.url").exists())
    }

    @Test
    fun `POST upload should reject an empty file`() {
        val file = MockMultipartFile(
            "file",
            "empty.jpg",
            "image/jpeg",
            byteArrayOf()
        )

        mockMvc.perform(multipart("/api/images/upload").file(file))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("File is empty"))
    }

    @Test
    fun `POST upload should reject a non-image file`() {
        val file = MockMultipartFile(
            "file",
            "document.pdf",
            "application/pdf",
            byteArrayOf(0x25, 0x50, 0x44, 0x46) // PDF magic bytes
        )

        mockMvc.perform(multipart("/api/images/upload").file(file))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("File must be an image"))
    }

    @Test
    fun `GET image should return 404 for non-existent file`() {
        mockMvc.perform(get("/api/images/nonexistent-file.jpg"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST upload and GET should round-trip successfully`() {
        val imageData = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        )
        val file = MockMultipartFile(
            "file",
            "roundtrip.jpg",
            "image/jpeg",
            imageData
        )

        val uploadResult = mockMvc.perform(multipart("/api/images/upload").file(file))
            .andExpect(status().isOk)
            .andReturn()

        val response = uploadResult.response.contentAsString
        val urlRegex = """"url":"(/api/images/[^"]+)"""".toRegex()
        val url = urlRegex.find(response)?.groupValues?.get(1)
            ?: throw AssertionError("No url in response: $response")

        mockMvc.perform(get(url))
            .andExpect(status().isOk)
    }

    @Test
    fun `POST upload with png should work`() {
        val file = MockMultipartFile(
            "file",
            "test.png",
            "image/png",
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
        )

        mockMvc.perform(multipart("/api/images/upload").file(file))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.filename").exists())
    }
}
