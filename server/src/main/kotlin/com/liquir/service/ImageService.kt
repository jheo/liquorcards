package com.liquir.service

import org.springframework.web.multipart.MultipartFile

interface ImageService {
    fun storeImage(file: MultipartFile): String
    fun getImagePath(filename: String): java.nio.file.Path
}
