package com.liquir.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.liquir.dto.CreateLiquorRequest
import com.liquir.dto.LiquorResponse
import com.liquir.dto.UpdateLiquorRequest
import com.liquir.repository.LiquorRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
class LiquorControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var liquorRepository: LiquorRepository

    private val mapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    @BeforeEach
    fun setUp() {
        liquorRepository.deleteAll()
    }

    private fun createRequestJson(
        name: String = "Macallan 12 Year Old Sherry Oak",
        nameKo: String? = "맥캘란 12년 셰리 오크",
        typeKo: String? = "싱글 몰트 스카치 위스키",
        aboutKo: String? = "풍부한 싱글 몰트 위스키입니다.",
        heritageKo: String? = "1824년에 설립되었습니다.",
        tastingNotesKo: List<String>? = listOf("건과일", "셰리", "바닐라")
    ): String {
        val request = CreateLiquorRequest(
            name = name,
            type = "Single Malt Scotch Whisky",
            category = "whisky",
            abv = 43.0,
            age = "12 Years",
            score = 88,
            price = "$75",
            origin = "Scotland",
            region = "Speyside",
            volume = "750ml",
            about = "A rich single malt whisky.",
            heritage = "Founded in 1824.",
            profile = mapOf("sweetness" to 70, "body" to 80),
            tastingNotes = listOf("dried fruit", "sherry", "vanilla"),
            suggestedImageKeyword = "macallan whisky bottle",
            status = "active",
            nameKo = nameKo,
            typeKo = typeKo,
            aboutKo = aboutKo,
            heritageKo = heritageKo,
            tastingNotesKo = tastingNotesKo
        )
        return mapper.writeValueAsString(request)
    }

    private fun postLiquor(json: String = createRequestJson()): LiquorResponse {
        val result = mockMvc.perform(
            post("/api/liquors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        )
            .andExpect(status().isCreated)
            .andReturn()

        return mapper.readValue(result.response.contentAsString)
    }

    @Nested
    inner class PostLiquorTests {

        @Test
        fun `POST should create liquor with Korean fields and return 201`() {
            val json = createRequestJson()

            mockMvc.perform(
                post("/api/liquors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Macallan 12 Year Old Sherry Oak"))
                .andExpect(jsonPath("$.nameKo").value("맥캘란 12년 셰리 오크"))
                .andExpect(jsonPath("$.typeKo").value("싱글 몰트 스카치 위스키"))
                .andExpect(jsonPath("$.aboutKo").value("풍부한 싱글 몰트 위스키입니다."))
                .andExpect(jsonPath("$.heritageKo").value("1824년에 설립되었습니다."))
                .andExpect(jsonPath("$.tastingNotesKo[0]").value("건과일"))
                .andExpect(jsonPath("$.tastingNotesKo[1]").value("셰리"))
                .andExpect(jsonPath("$.tastingNotesKo[2]").value("바닐라"))
                .andExpect(jsonPath("$.id").isNumber)
        }

        @Test
        fun `POST should create liquor without Korean fields`() {
            val request = CreateLiquorRequest(
                name = "Simple Whisky",
                type = "Blended Whisky",
                category = "whisky"
            )
            val json = mapper.writeValueAsString(request)

            mockMvc.perform(
                post("/api/liquors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Simple Whisky"))
                .andExpect(jsonPath("$.nameKo").doesNotExist())
                .andExpect(jsonPath("$.typeKo").doesNotExist())
                .andExpect(jsonPath("$.aboutKo").doesNotExist())
                .andExpect(jsonPath("$.heritageKo").doesNotExist())
                .andExpect(jsonPath("$.tastingNotesKo").doesNotExist())
        }

        @Test
        fun `POST should persist Korean fields to database`() {
            val created = postLiquor()

            val fromDb = liquorRepository.findById(created.id).orElseThrow()
            assertEquals("맥캘란 12년 셰리 오크", fromDb.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", fromDb.typeKo)
            assertEquals("풍부한 싱글 몰트 위스키입니다.", fromDb.aboutKo)
            assertEquals("1824년에 설립되었습니다.", fromDb.heritageKo)
            assertNotNull(fromDb.tastingNotesKoJson)
            assertTrue(fromDb.tastingNotesKoJson!!.contains("건과일"))
        }

        @Test
        fun `POST should handle Korean characters in all text fields`() {
            val json = createRequestJson(
                name = "테스트 위스키",
                nameKo = "테스트 위스키 한국어",
                typeKo = "블렌디드 위스키",
                aboutKo = "이것은 테스트 설명입니다. 한국어 문자가 정확히 저장되어야 합니다.",
                heritageKo = "이 위스키는 오랜 역사를 가지고 있습니다.",
                tastingNotesKo = listOf("꿀", "바닐라", "오크", "과일", "향신료")
            )

            mockMvc.perform(
                post("/api/liquors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("테스트 위스키"))
                .andExpect(jsonPath("$.nameKo").value("테스트 위스키 한국어"))
                .andExpect(jsonPath("$.typeKo").value("블렌디드 위스키"))
                .andExpect(jsonPath("$.tastingNotesKo").isArray)
                .andExpect(jsonPath("$.tastingNotesKo.length()").value(5))
        }
    }

    @Nested
    inner class GetLiquorsTests {

        @Test
        fun `GET all should return Korean fields`() {
            postLiquor()

            mockMvc.perform(get("/api/liquors"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].nameKo").value("맥캘란 12년 셰리 오크"))
                .andExpect(jsonPath("$[0].typeKo").value("싱글 몰트 스카치 위스키"))
                .andExpect(jsonPath("$[0].aboutKo").value("풍부한 싱글 몰트 위스키입니다."))
                .andExpect(jsonPath("$[0].heritageKo").value("1824년에 설립되었습니다."))
                .andExpect(jsonPath("$[0].tastingNotesKo[0]").value("건과일"))
        }

        @Test
        fun `GET all should return multiple items with Korean fields`() {
            postLiquor(createRequestJson(name = "Whisky 1", nameKo = "위스키 1"))
            postLiquor(createRequestJson(name = "Whisky 2", nameKo = "위스키 2"))

            mockMvc.perform(get("/api/liquors"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nameKo").exists())
                .andExpect(jsonPath("$[1].nameKo").exists())
        }

        @Test
        fun `GET all should return empty array when no liquors exist`() {
            mockMvc.perform(get("/api/liquors"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        fun `GET by id should return Korean fields`() {
            val created = postLiquor()

            mockMvc.perform(get("/api/liquors/${created.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(created.id))
                .andExpect(jsonPath("$.nameKo").value("맥캘란 12년 셰리 오크"))
                .andExpect(jsonPath("$.typeKo").value("싱글 몰트 스카치 위스키"))
                .andExpect(jsonPath("$.aboutKo").value("풍부한 싱글 몰트 위스키입니다."))
                .andExpect(jsonPath("$.heritageKo").value("1824년에 설립되었습니다."))
                .andExpect(jsonPath("$.tastingNotesKo[0]").value("건과일"))
                .andExpect(jsonPath("$.tastingNotesKo[1]").value("셰리"))
                .andExpect(jsonPath("$.tastingNotesKo[2]").value("바닐라"))
        }

        @Test
        fun `GET by id should return 404 for non-existent id`() {
            mockMvc.perform(get("/api/liquors/99999"))
                .andExpect(status().isNotFound)
        }

        @Test
        fun `GET by id should return null Korean fields when not set`() {
            val request = CreateLiquorRequest(name = "No Korean", category = "whisky")
            val json = mapper.writeValueAsString(request)
            val created = postLiquor(json)

            mockMvc.perform(get("/api/liquors/${created.id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("No Korean"))
                .andExpect(jsonPath("$.nameKo").doesNotExist())
                .andExpect(jsonPath("$.typeKo").doesNotExist())
                .andExpect(jsonPath("$.tastingNotesKo").doesNotExist())
        }
    }

    @Nested
    inner class PutLiquorTests {

        @Test
        fun `PUT should update Korean fields`() {
            val created = postLiquor()

            val updateRequest = UpdateLiquorRequest(
                nameKo = "맥캘란 12년 (수정됨)",
                typeKo = "싱글 몰트 (수정됨)",
                aboutKo = "수정된 설명입니다.",
                heritageKo = "수정된 역사입니다.",
                tastingNotesKo = listOf("사과", "꿀")
            )
            val updateJson = mapper.writeValueAsString(updateRequest)

            mockMvc.perform(
                put("/api/liquors/${created.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.nameKo").value("맥캘란 12년 (수정됨)"))
                .andExpect(jsonPath("$.typeKo").value("싱글 몰트 (수정됨)"))
                .andExpect(jsonPath("$.aboutKo").value("수정된 설명입니다."))
                .andExpect(jsonPath("$.heritageKo").value("수정된 역사입니다."))
                .andExpect(jsonPath("$.tastingNotesKo[0]").value("사과"))
                .andExpect(jsonPath("$.tastingNotesKo[1]").value("꿀"))
                .andExpect(jsonPath("$.tastingNotesKo.length()").value(2))
        }

        @Test
        fun `PUT should preserve Korean fields when not included in update`() {
            val created = postLiquor()

            val updateRequest = UpdateLiquorRequest(name = "Updated English Name")
            val updateJson = mapper.writeValueAsString(updateRequest)

            mockMvc.perform(
                put("/api/liquors/${created.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated English Name"))
                .andExpect(jsonPath("$.nameKo").value("맥캘란 12년 셰리 오크"))
                .andExpect(jsonPath("$.typeKo").value("싱글 몰트 스카치 위스키"))
                .andExpect(jsonPath("$.aboutKo").value("풍부한 싱글 몰트 위스키입니다."))
                .andExpect(jsonPath("$.heritageKo").value("1824년에 설립되었습니다."))
        }

        @Test
        fun `PUT should update both English and Korean fields simultaneously`() {
            val created = postLiquor()

            val updateRequest = UpdateLiquorRequest(
                name = "Updated Name",
                nameKo = "업데이트된 이름",
                score = 95
            )
            val updateJson = mapper.writeValueAsString(updateRequest)

            mockMvc.perform(
                put("/api/liquors/${created.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.nameKo").value("업데이트된 이름"))
                .andExpect(jsonPath("$.score").value(95))
        }

        @Test
        fun `PUT should return 404 for non-existent id`() {
            val updateRequest = UpdateLiquorRequest(nameKo = "테스트")
            val updateJson = mapper.writeValueAsString(updateRequest)

            mockMvc.perform(
                put("/api/liquors/99999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `PUT should add Korean fields to item that had none`() {
            val request = CreateLiquorRequest(name = "No Korean Yet", category = "whisky")
            val json = mapper.writeValueAsString(request)
            val created = postLiquor(json)

            val updateRequest = UpdateLiquorRequest(
                nameKo = "이제 한국어 있음",
                typeKo = "위스키",
                tastingNotesKo = listOf("오크", "바닐라")
            )
            val updateJson = mapper.writeValueAsString(updateRequest)

            mockMvc.perform(
                put("/api/liquors/${created.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.nameKo").value("이제 한국어 있음"))
                .andExpect(jsonPath("$.typeKo").value("위스키"))
                .andExpect(jsonPath("$.tastingNotesKo[0]").value("오크"))
                .andExpect(jsonPath("$.tastingNotesKo[1]").value("바닐라"))
        }
    }

    @Nested
    inner class DeleteLiquorTests {

        @Test
        fun `DELETE should remove liquor with Korean fields`() {
            val created = postLiquor()

            mockMvc.perform(delete("/api/liquors/${created.id}"))
                .andExpect(status().isNoContent)

            mockMvc.perform(get("/api/liquors/${created.id}"))
                .andExpect(status().isNotFound)
        }
    }
}
