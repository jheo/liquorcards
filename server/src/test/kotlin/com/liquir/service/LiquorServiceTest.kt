package com.liquir.service

import com.liquir.dto.CreateLiquorRequest
import com.liquir.dto.UpdateLiquorRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import com.liquir.repository.LiquorRepository

@SpringBootTest
class LiquorServiceTest {

    @Autowired
    private lateinit var liquorService: LiquorService

    @Autowired
    private lateinit var liquorRepository: LiquorRepository

    @BeforeEach
    fun setUp() {
        liquorRepository.deleteAll()
    }

    private fun createKoreanLiquorRequest(
        name: String = "Macallan 12 Year Old Sherry Oak",
        nameKo: String? = "맥캘란 12년 셰리 오크",
        typeKo: String? = "싱글 몰트 스카치 위스키",
        aboutKo: String? = "맥캘란 12년 셰리 오크는 스페인 헤레즈에서 엄선한 셰리 시즈닝 오크 캐스크에서 숙성됩니다.",
        heritageKo: String? = "1824년 알렉산더 리드에 의해 설립된 맥캘란 증류소입니다.",
        tastingNotesKo: List<String>? = listOf("건과일", "셰리", "바닐라", "생강", "오크", "초콜릿")
    ) = CreateLiquorRequest(
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
        about = "The Macallan 12 Year Old Sherry Oak is a rich single malt.",
        heritage = "Founded in 1824 by Alexander Reid.",
        profile = mapOf("sweetness" to 70, "body" to 80, "richness" to 85),
        tastingNotes = listOf("dried fruit", "sherry", "vanilla", "ginger", "oak", "chocolate"),
        suggestedImageKeyword = "macallan whisky bottle",
        status = "active",
        nameKo = nameKo,
        typeKo = typeKo,
        aboutKo = aboutKo,
        heritageKo = heritageKo,
        tastingNotesKo = tastingNotesKo
    )

    @Nested
    inner class CreateTests {

        @Test
        fun `create should persist and return Korean fields`() {
            val request = createKoreanLiquorRequest()
            val response = liquorService.create(request)

            assertNotNull(response.id)
            assertEquals("Macallan 12 Year Old Sherry Oak", response.name)
            assertEquals("맥캘란 12년 셰리 오크", response.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", response.typeKo)
            assertEquals("맥캘란 12년 셰리 오크는 스페인 헤레즈에서 엄선한 셰리 시즈닝 오크 캐스크에서 숙성됩니다.", response.aboutKo)
            assertEquals("1824년 알렉산더 리드에 의해 설립된 맥캘란 증류소입니다.", response.heritageKo)
            assertNotNull(response.tastingNotesKo)
            assertEquals(6, response.tastingNotesKo!!.size)
            assertEquals("건과일", response.tastingNotesKo!![0])
            assertEquals("초콜릿", response.tastingNotesKo!![5])
        }

        @Test
        fun `create should persist English fields alongside Korean fields`() {
            val request = createKoreanLiquorRequest()
            val response = liquorService.create(request)

            assertEquals("Single Malt Scotch Whisky", response.type)
            assertEquals("whisky", response.category)
            assertEquals(43.0, response.abv)
            assertEquals("12 Years", response.age)
            assertEquals(88, response.score)
            assertEquals("$75", response.price)
            assertEquals("Scotland", response.origin)
            assertEquals("Speyside", response.region)
            assertEquals("750ml", response.volume)
            assertNotNull(response.tastingNotes)
            assertEquals(6, response.tastingNotes!!.size)
            assertNotNull(response.profile)
            assertEquals(70, response.profile!!["sweetness"])
        }

        @Test
        fun `create should handle null Korean fields`() {
            val request = CreateLiquorRequest(
                name = "Generic Whisky",
                type = "Blended Whisky",
                category = "whisky"
            )
            val response = liquorService.create(request)

            assertNotNull(response.id)
            assertEquals("Generic Whisky", response.name)
            assertNull(response.nameKo)
            assertNull(response.typeKo)
            assertNull(response.aboutKo)
            assertNull(response.heritageKo)
            assertNull(response.tastingNotesKo)
        }

        @Test
        fun `create should handle empty Korean tasting notes`() {
            val request = createKoreanLiquorRequest(
                tastingNotesKo = emptyList()
            )
            val response = liquorService.create(request)

            assertNotNull(response.tastingNotesKo)
            assertTrue(response.tastingNotesKo!!.isEmpty())
        }
    }

    @Nested
    inner class UpdateTests {

        @Test
        fun `update should modify Korean fields`() {
            val created = liquorService.create(createKoreanLiquorRequest())

            val updateRequest = UpdateLiquorRequest(
                nameKo = "맥캘란 12년 (업데이트됨)",
                typeKo = "싱글 몰트 위스키 (업데이트됨)",
                aboutKo = "업데이트된 설명입니다.",
                heritageKo = "업데이트된 역사입니다.",
                tastingNotesKo = listOf("사과", "꿀", "계피")
            )
            val updated = liquorService.update(created.id, updateRequest)

            assertEquals(created.id, updated.id)
            assertEquals("맥캘란 12년 (업데이트됨)", updated.nameKo)
            assertEquals("싱글 몰트 위스키 (업데이트됨)", updated.typeKo)
            assertEquals("업데이트된 설명입니다.", updated.aboutKo)
            assertEquals("업데이트된 역사입니다.", updated.heritageKo)
            assertNotNull(updated.tastingNotesKo)
            assertEquals(3, updated.tastingNotesKo!!.size)
            assertEquals("사과", updated.tastingNotesKo!![0])
            assertEquals("꿀", updated.tastingNotesKo!![1])
            assertEquals("계피", updated.tastingNotesKo!![2])
        }

        @Test
        fun `update should not clear Korean fields when not provided`() {
            val created = liquorService.create(createKoreanLiquorRequest())

            // Update only name (English) - Korean fields should be preserved
            val updateRequest = UpdateLiquorRequest(
                name = "Macallan 12 Updated"
            )
            val updated = liquorService.update(created.id, updateRequest)

            assertEquals("Macallan 12 Updated", updated.name)
            // Korean fields should remain unchanged
            assertEquals("맥캘란 12년 셰리 오크", updated.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", updated.typeKo)
            assertNotNull(updated.aboutKo)
            assertNotNull(updated.heritageKo)
            assertNotNull(updated.tastingNotesKo)
            assertEquals(6, updated.tastingNotesKo!!.size)
        }

        @Test
        fun `update should allow updating Korean and English fields together`() {
            val created = liquorService.create(createKoreanLiquorRequest())

            val updateRequest = UpdateLiquorRequest(
                name = "Updated Name",
                nameKo = "업데이트된 이름",
                score = 95
            )
            val updated = liquorService.update(created.id, updateRequest)

            assertEquals("Updated Name", updated.name)
            assertEquals("업데이트된 이름", updated.nameKo)
            assertEquals(95, updated.score)
        }

        @Test
        fun `update should throw for non-existent id`() {
            assertThrows<NoSuchElementException> {
                liquorService.update(99999L, UpdateLiquorRequest(nameKo = "테스트"))
            }
        }
    }

    @Nested
    inner class FindByIdTests {

        @Test
        fun `findById should return Korean fields`() {
            val created = liquorService.create(createKoreanLiquorRequest())
            val found = liquorService.findById(created.id)

            assertEquals(created.id, found.id)
            assertEquals("맥캘란 12년 셰리 오크", found.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", found.typeKo)
            assertEquals("맥캘란 12년 셰리 오크는 스페인 헤레즈에서 엄선한 셰리 시즈닝 오크 캐스크에서 숙성됩니다.", found.aboutKo)
            assertEquals("1824년 알렉산더 리드에 의해 설립된 맥캘란 증류소입니다.", found.heritageKo)
            assertNotNull(found.tastingNotesKo)
            assertEquals(6, found.tastingNotesKo!!.size)
        }

        @Test
        fun `findById should return null Korean fields when not set`() {
            val created = liquorService.create(
                CreateLiquorRequest(name = "No Korean Data", category = "whisky")
            )
            val found = liquorService.findById(created.id)

            assertEquals("No Korean Data", found.name)
            assertNull(found.nameKo)
            assertNull(found.typeKo)
            assertNull(found.aboutKo)
            assertNull(found.heritageKo)
            assertNull(found.tastingNotesKo)
        }

        @Test
        fun `findById should throw for non-existent id`() {
            assertThrows<NoSuchElementException> {
                liquorService.findById(99999L)
            }
        }
    }

    @Nested
    inner class FindAllTests {

        @Test
        fun `findAll should return Korean fields for all items`() {
            liquorService.create(createKoreanLiquorRequest(name = "Macallan 12", nameKo = "맥캘란 12년"))
            liquorService.create(createKoreanLiquorRequest(name = "Lagavulin 16", nameKo = "라가불린 16년"))

            val all = liquorService.findAll(null, null, null, null)

            assertEquals(2, all.size)
            assertTrue(all.all { it.nameKo != null })
            assertTrue(all.all { it.typeKo != null })
            assertTrue(all.all { it.aboutKo != null })
            assertTrue(all.all { it.tastingNotesKo != null })
        }

        @Test
        fun `findAll should return mixed Korean and non-Korean items`() {
            liquorService.create(createKoreanLiquorRequest(name = "With Korean", nameKo = "한국어 있음"))
            liquorService.create(CreateLiquorRequest(name = "Without Korean", category = "whisky"))

            val all = liquorService.findAll(null, null, null, null)

            assertEquals(2, all.size)
            val withKorean = all.find { it.name == "With Korean" }
            val withoutKorean = all.find { it.name == "Without Korean" }

            assertNotNull(withKorean)
            assertNotNull(withoutKorean)
            assertEquals("한국어 있음", withKorean!!.nameKo)
            assertNull(withoutKorean!!.nameKo)
        }

        @Test
        fun `findAll with category filter should return Korean fields`() {
            liquorService.create(createKoreanLiquorRequest(name = "Whisky 1"))
            liquorService.create(
                CreateLiquorRequest(name = "Gin 1", category = "gin", nameKo = "진 1")
            )

            val whiskyOnly = liquorService.findAll("whisky", null, null, null)
            assertEquals(1, whiskyOnly.size)
            assertEquals("Whisky 1", whiskyOnly[0].name)
            assertNotNull(whiskyOnly[0].nameKo)
        }

        @Test
        fun `findAll with search filter should return Korean fields`() {
            liquorService.create(createKoreanLiquorRequest(name = "Macallan 12"))
            liquorService.create(createKoreanLiquorRequest(name = "Lagavulin 16", nameKo = "라가불린 16년"))

            val results = liquorService.findAll(null, null, "Macallan", null)
            assertEquals(1, results.size)
            assertEquals("Macallan 12", results[0].name)
            assertNotNull(results[0].nameKo)
        }

        @Test
        fun `findAll should return empty list when no items exist`() {
            val all = liquorService.findAll(null, null, null, null)
            assertTrue(all.isEmpty())
        }

        @Test
        fun `findAll should sort by name alphabetically`() {
            liquorService.create(createKoreanLiquorRequest(name = "Zacapa"))
            liquorService.create(createKoreanLiquorRequest(name = "Ardbeg"))
            liquorService.create(createKoreanLiquorRequest(name = "Macallan"))

            val sorted = liquorService.findAll(null, null, null, "name")
            assertEquals("Ardbeg", sorted[0].name)
            assertEquals("Macallan", sorted[1].name)
            assertEquals("Zacapa", sorted[2].name)
        }

        @Test
        fun `findAll should sort by score descending`() {
            liquorService.create(CreateLiquorRequest(name = "Low", category = "whisky", score = 70))
            liquorService.create(CreateLiquorRequest(name = "High", category = "whisky", score = 95))
            liquorService.create(CreateLiquorRequest(name = "Mid", category = "whisky", score = 85))

            val sorted = liquorService.findAll(null, null, null, "score")
            assertEquals("High", sorted[0].name)
            assertEquals("Mid", sorted[1].name)
            assertEquals("Low", sorted[2].name)
        }

        @Test
        fun `findAll should sort by createdAt descending by default`() {
            liquorService.create(createKoreanLiquorRequest(name = "First"))
            liquorService.create(createKoreanLiquorRequest(name = "Second"))

            val sorted = liquorService.findAll(null, null, null, null)
            // Most recently created should be first
            assertEquals(2, sorted.size)
        }

        @Test
        fun `findAll should sort by createdAt when sort is 'createdat'`() {
            liquorService.create(createKoreanLiquorRequest(name = "First"))
            liquorService.create(createKoreanLiquorRequest(name = "Second"))

            val sorted = liquorService.findAll(null, null, null, "createdat")
            assertEquals(2, sorted.size)
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `delete should remove an existing liquor`() {
            val created = liquorService.create(createKoreanLiquorRequest())
            liquorService.delete(created.id)

            assertThrows<NoSuchElementException> {
                liquorService.findById(created.id)
            }
        }

        @Test
        fun `delete should throw for non-existent id`() {
            assertThrows<NoSuchElementException> {
                liquorService.delete(99999L)
            }
        }
    }
}
