package com.liquir.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleNotFound should return 404 with error message`() {
        val response = handler.handleNotFound(NoSuchElementException("Liquor not found"))
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Liquor not found", response.body!!["error"])
    }

    @Test
    fun `handleNotFound should return default message when exception has no message`() {
        val response = handler.handleNotFound(NoSuchElementException())
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Resource not found", response.body!!["error"])
    }

    @Test
    fun `handleIllegalState should return 503 with error message`() {
        val response = handler.handleIllegalState(IllegalStateException("API key not configured"))
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("API key not configured", response.body!!["error"])
    }

    @Test
    fun `handleIllegalState should return default message when exception has no message`() {
        val response = handler.handleIllegalState(IllegalStateException())
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("Service unavailable", response.body!!["error"])
    }

    @Test
    fun `handleMaxUploadSize should return 413`() {
        val response = handler.handleMaxUploadSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException(10485760)
        )
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.statusCode)
        assertTrue(response.body!!["error"]!!.contains("10MB"))
    }

    @Test
    fun `handleGeneral should return 500`() {
        val response = handler.handleGeneral(RuntimeException("Unexpected"))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("An unexpected error occurred", response.body!!["error"])
    }
}
