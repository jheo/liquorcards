package com.liquir.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class WebConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `CORS should allow requests from localhost 5173`() {
        mockMvc.perform(
            options("/api/liquors")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
    }

    @Test
    fun `CORS should allow requests from localhost 3000`() {
        mockMvc.perform(
            options("/api/liquors")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
    }

    @Test
    fun `CORS should allow various HTTP methods`() {
        mockMvc.perform(
            options("/api/liquors")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
        )
            .andExpect(status().isOk)
            .andExpect(header().exists("Access-Control-Allow-Methods"))
    }

    @Test
    fun `CORS should reject requests from unknown origins`() {
        mockMvc.perform(
            options("/api/liquors")
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "GET")
        )
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }
}
