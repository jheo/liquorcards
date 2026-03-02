package com.liquir.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.liquir.dto.CreateLiquorRequest
import com.liquir.dto.LiquorResponse
import com.liquir.repository.LiquorRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class StatusEndpointTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var liquorRepository: LiquorRepository

    private val mapper = jacksonObjectMapper().apply { findAndRegisterModules() }

    @BeforeEach
    fun setUp() {
        liquorRepository.deleteAll()
    }

    private fun createLiquor(): LiquorResponse {
        val request = CreateLiquorRequest(
            name = "Test Liquor",
            category = "whisky",
            status = "active"
        )
        val result = mockMvc.perform(
            post("/api/liquors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn()
        return mapper.readValue(result.response.contentAsString)
    }

    @Test
    fun `PATCH status should update status to archived`() {
        val created = createLiquor()

        mockMvc.perform(
            patch("/api/liquors/${created.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "archived"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("archived"))
            .andExpect(jsonPath("$.name").value("Test Liquor"))
    }

    @Test
    fun `PATCH status should return 400 when status field is missing`() {
        val created = createLiquor()

        mockMvc.perform(
            patch("/api/liquors/${created.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"other": "value"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH status should return 404 for non-existent id`() {
        mockMvc.perform(
            patch("/api/liquors/99999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "archived"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE should return 404 for non-existent id`() {
        mockMvc.perform(delete("/api/liquors/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH status should update from archived back to active`() {
        val created = createLiquor()

        // Archive it first
        mockMvc.perform(
            patch("/api/liquors/${created.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "archived"}""")
        ).andExpect(status().isOk)

        // Restore it
        mockMvc.perform(
            patch("/api/liquors/${created.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status": "active"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("active"))
    }
}
