package com.liquir.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.liquir.dto.AiLookupRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val mapper = jacksonObjectMapper()

    @Test
    fun `POST ai search endpoint should exist and respond`() {
        // The AI service will fail because no API key is configured in tests,
        // but the endpoint itself should exist and return a service-unavailable
        // response rather than a 404.
        val request = AiLookupRequest(name = "Macallan 12", provider = "claude")
        val json = mapper.writeValueAsString(request)

        val result = mockMvc.perform(
            post("/api/ai/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andReturn()

        // The endpoint exists - it should NOT be 404
        val statusCode = result.response.status
        assert(statusCode != 404) {
            "Expected /api/ai/search endpoint to exist (not 404), but got $statusCode"
        }
        // Without API keys configured, we expect 503 (SERVICE_UNAVAILABLE)
        assert(statusCode == 503) {
            "Expected 503 SERVICE_UNAVAILABLE without API key, but got $statusCode"
        }
    }

    @Test
    fun `POST ai-lookup endpoint on liquors controller should exist and respond`() {
        // Verifies the /api/liquors/ai-lookup endpoint also exists
        val request = AiLookupRequest(name = "Hendricks Gin", provider = "claude")
        val json = mapper.writeValueAsString(request)

        val result = mockMvc.perform(
            post("/api/liquors/ai-lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andReturn()

        val statusCode = result.response.status
        assert(statusCode != 404) {
            "Expected /api/liquors/ai-lookup endpoint to exist (not 404), but got $statusCode"
        }
        // Without API keys configured, we expect 503 (SERVICE_UNAVAILABLE)
        assert(statusCode == 503) {
            "Expected 503 SERVICE_UNAVAILABLE without API key, but got $statusCode"
        }
    }

    @Test
    fun `POST ai search endpoint with openai provider should exist and respond`() {
        val request = AiLookupRequest(name = "Opus One 2019", provider = "openai")
        val json = mapper.writeValueAsString(request)

        val result = mockMvc.perform(
            post("/api/ai/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andReturn()

        val statusCode = result.response.status
        assert(statusCode != 404) {
            "Expected /api/ai/search endpoint to exist (not 404), but got $statusCode"
        }
        // Without API keys configured, we expect 503 (SERVICE_UNAVAILABLE)
        assert(statusCode == 503) {
            "Expected 503 SERVICE_UNAVAILABLE without API key, but got $statusCode"
        }
    }
}
