package com.liquir.dto

import com.liquir.model.Liquor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LiquorDtosTest {

    @Nested
    inner class CreateLiquorRequestToEntityTests {

        @Test
        fun `toEntity should map all Korean fields`() {
            val request = CreateLiquorRequest(
                name = "Macallan 12",
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
                imageUrl = "https://example.com/image.jpg",
                suggestedImageKeyword = "macallan whisky bottle",
                status = "active",
                nameKo = "맥캘란 12년",
                typeKo = "싱글 몰트 스카치 위스키",
                aboutKo = "풍부한 싱글 몰트 위스키.",
                heritageKo = "1824년에 설립됨.",
                tastingNotesKo = listOf("건과일", "셰리", "바닐라")
            )

            val entity = request.toEntity()

            assertEquals("Macallan 12", entity.name)
            assertEquals("Single Malt Scotch Whisky", entity.type)
            assertEquals("whisky", entity.category)
            assertEquals(43.0, entity.abv)
            assertEquals("12 Years", entity.age)
            assertEquals(88, entity.score)
            assertEquals("$75", entity.price)
            assertEquals("Scotland", entity.origin)
            assertEquals("Speyside", entity.region)
            assertEquals("750ml", entity.volume)
            assertEquals("A rich single malt whisky.", entity.about)
            assertEquals("Founded in 1824.", entity.heritage)
            assertEquals("https://example.com/image.jpg", entity.imageUrl)
            assertEquals("macallan whisky bottle", entity.suggestedImageKeyword)
            assertEquals("active", entity.status)

            // Korean fields
            assertEquals("맥캘란 12년", entity.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", entity.typeKo)
            assertEquals("풍부한 싱글 몰트 위스키.", entity.aboutKo)
            assertEquals("1824년에 설립됨.", entity.heritageKo)

            // JSON serialized fields
            assertNotNull(entity.profileJson)
            assertTrue(entity.profileJson!!.contains("sweetness"))
            assertTrue(entity.profileJson!!.contains("70"))

            assertNotNull(entity.tastingNotesJson)
            assertTrue(entity.tastingNotesJson!!.contains("dried fruit"))
            assertTrue(entity.tastingNotesJson!!.contains("sherry"))

            assertNotNull(entity.tastingNotesKoJson)
            assertTrue(entity.tastingNotesKoJson!!.contains("건과일"))
            assertTrue(entity.tastingNotesKoJson!!.contains("셰리"))
            assertTrue(entity.tastingNotesKoJson!!.contains("바닐라"))

            // Timestamps should be set
            assertNotNull(entity.createdAt)
            assertNotNull(entity.updatedAt)
        }

        @Test
        fun `toEntity should handle null Korean fields`() {
            val request = CreateLiquorRequest(
                name = "Generic Whisky"
            )

            val entity = request.toEntity()

            assertEquals("Generic Whisky", entity.name)
            assertNull(entity.nameKo)
            assertNull(entity.typeKo)
            assertNull(entity.aboutKo)
            assertNull(entity.heritageKo)
            assertNull(entity.tastingNotesKoJson)
        }

        @Test
        fun `toEntity should handle null profile and tastingNotes`() {
            val request = CreateLiquorRequest(
                name = "Simple Liquor",
                profile = null,
                tastingNotes = null,
                tastingNotesKo = null
            )

            val entity = request.toEntity()

            assertNull(entity.profileJson)
            assertNull(entity.tastingNotesJson)
            assertNull(entity.tastingNotesKoJson)
        }

        @Test
        fun `toEntity should serialize empty Korean tasting notes list`() {
            val request = CreateLiquorRequest(
                name = "Test Liquor",
                tastingNotesKo = emptyList()
            )

            val entity = request.toEntity()

            assertNotNull(entity.tastingNotesKoJson)
            assertEquals("[]", entity.tastingNotesKoJson)
        }

        @Test
        fun `toEntity should default status to active`() {
            val request = CreateLiquorRequest(name = "Test Liquor")

            val entity = request.toEntity()

            assertEquals("active", entity.status)
        }
    }

    @Nested
    inner class LiquorToResponseTests {

        private fun createTestLiquor(
            id: Long = 1L,
            name: String = "Macallan 12",
            nameKo: String? = "맥캘란 12년",
            type: String? = "Single Malt Scotch Whisky",
            typeKo: String? = "싱글 몰트 스카치 위스키",
            about: String? = "A rich whisky.",
            aboutKo: String? = "풍부한 위스키.",
            heritage: String? = "Founded in 1824.",
            heritageKo: String? = "1824년에 설립됨.",
            profileJson: String? = """{"sweetness":70,"body":80}""",
            tastingNotesJson: String? = """["dried fruit","sherry","vanilla"]""",
            tastingNotesKoJson: String? = """["건과일","셰리","바닐라"]"""
        ): Liquor {
            val now = LocalDateTime.now()
            return Liquor(
                id = id,
                name = name,
                nameKo = nameKo,
                type = type,
                typeKo = typeKo,
                category = "whisky",
                abv = 43.0,
                age = "12 Years",
                score = 88,
                price = "$75",
                origin = "Scotland",
                region = "Speyside",
                volume = "750ml",
                about = about,
                aboutKo = aboutKo,
                heritage = heritage,
                heritageKo = heritageKo,
                profileJson = profileJson,
                tastingNotesJson = tastingNotesJson,
                tastingNotesKoJson = tastingNotesKoJson,
                imageUrl = "https://example.com/image.jpg",
                suggestedImageKeyword = "macallan",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
        }

        @Test
        fun `toResponse should map all Korean fields`() {
            val liquor = createTestLiquor()
            val response = liquor.toResponse()

            assertEquals(1L, response.id)
            assertEquals("Macallan 12", response.name)
            assertEquals("맥캘란 12년", response.nameKo)
            assertEquals("Single Malt Scotch Whisky", response.type)
            assertEquals("싱글 몰트 스카치 위스키", response.typeKo)
            assertEquals("A rich whisky.", response.about)
            assertEquals("풍부한 위스키.", response.aboutKo)
            assertEquals("Founded in 1824.", response.heritage)
            assertEquals("1824년에 설립됨.", response.heritageKo)
        }

        @Test
        fun `toResponse should parse tastingNotesKoJson to list`() {
            val liquor = createTestLiquor(
                tastingNotesKoJson = """["건과일","셰리","바닐라"]"""
            )
            val response = liquor.toResponse()

            assertNotNull(response.tastingNotesKo)
            assertEquals(3, response.tastingNotesKo!!.size)
            assertEquals("건과일", response.tastingNotesKo!![0])
            assertEquals("셰리", response.tastingNotesKo!![1])
            assertEquals("바닐라", response.tastingNotesKo!![2])
        }

        @Test
        fun `toResponse should parse tastingNotesJson to list`() {
            val liquor = createTestLiquor(
                tastingNotesJson = """["dried fruit","sherry","vanilla"]"""
            )
            val response = liquor.toResponse()

            assertNotNull(response.tastingNotes)
            assertEquals(3, response.tastingNotes!!.size)
            assertEquals("dried fruit", response.tastingNotes!![0])
            assertEquals("sherry", response.tastingNotes!![1])
            assertEquals("vanilla", response.tastingNotes!![2])
        }

        @Test
        fun `toResponse should parse profileJson to map`() {
            val liquor = createTestLiquor(
                profileJson = """{"sweetness":70,"body":80,"richness":85}"""
            )
            val response = liquor.toResponse()

            assertNotNull(response.profile)
            assertEquals(70, response.profile!!["sweetness"])
            assertEquals(80, response.profile!!["body"])
            assertEquals(85, response.profile!!["richness"])
        }

        @Test
        fun `toResponse should handle null Korean fields`() {
            val liquor = createTestLiquor(
                nameKo = null,
                typeKo = null,
                aboutKo = null,
                heritageKo = null,
                tastingNotesKoJson = null
            )
            val response = liquor.toResponse()

            assertNull(response.nameKo)
            assertNull(response.typeKo)
            assertNull(response.aboutKo)
            assertNull(response.heritageKo)
            assertNull(response.tastingNotesKo)
        }

        @Test
        fun `toResponse should handle null JSON fields`() {
            val liquor = createTestLiquor(
                profileJson = null,
                tastingNotesJson = null,
                tastingNotesKoJson = null
            )
            val response = liquor.toResponse()

            assertNull(response.profile)
            assertNull(response.tastingNotes)
            assertNull(response.tastingNotesKo)
        }

        @Test
        fun `toResponse should handle invalid JSON gracefully`() {
            val liquor = createTestLiquor(
                profileJson = "not valid json",
                tastingNotesJson = "also not valid",
                tastingNotesKoJson = "{bad json"
            )
            val response = liquor.toResponse()

            assertNull(response.profile)
            assertNull(response.tastingNotes)
            assertNull(response.tastingNotesKo)
        }

        @Test
        fun `toResponse should handle empty JSON arrays`() {
            val liquor = createTestLiquor(
                tastingNotesJson = "[]",
                tastingNotesKoJson = "[]"
            )
            val response = liquor.toResponse()

            assertNotNull(response.tastingNotes)
            assertTrue(response.tastingNotes!!.isEmpty())
            assertNotNull(response.tastingNotesKo)
            assertTrue(response.tastingNotesKo!!.isEmpty())
        }

        @Test
        fun `toResponse should preserve timestamps`() {
            val now = LocalDateTime.of(2025, 1, 15, 10, 30, 0)
            val liquor = Liquor(
                id = 1L,
                name = "Test",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
            val response = liquor.toResponse()

            assertEquals(now, response.createdAt)
            assertEquals(now, response.updatedAt)
        }
    }
}
