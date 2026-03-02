package com.liquir.config

import com.liquir.model.Liquor
import com.liquir.repository.LiquorRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DataSeeder(
    private val liquorRepository: LiquorRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        if (liquorRepository.count() > 0L) return

        val now = LocalDateTime.now()

        liquorRepository.saveAll(listOf(
            Liquor(
                name = "Macallan 12 Year Old Sherry Oak",
                nameKo = "맥캘란 12년 셰리 오크",
                type = "Single Malt Scotch Whisky",
                typeKo = "싱글 몰트 스카치 위스키",
                category = "whisky",
                abv = 43.0,
                age = "12 Years",
                score = 88,
                price = "$75",
                origin = "Scotland",
                region = "Speyside",
                volume = "750ml",
                about = "The Macallan 12 Year Old Sherry Oak is matured exclusively in hand-picked sherry seasoned oak casks from Jerez, Spain. It delivers a rich and complex flavor profile that has made it one of the most recognized single malts in the world.",
                aboutKo = "맥캘란 12년 셰리 오크는 스페인 헤레즈에서 엄선한 셰리 시즈닝 오크 캐스크에서 숙성됩니다. 풍부하고 복잡한 풍미 프로필로 세계에서 가장 인정받는 싱글 몰트 중 하나가 되었습니다.",
                heritage = "Founded in 1824 by Alexander Reid, The Macallan distillery sits on an estate overlooking the River Spey. It has long been considered one of the top single malt whiskies and is frequently the benchmark against which others are measured.",
                heritageKo = "1824년 알렉산더 리드에 의해 설립된 맥캘란 증류소는 스페이 강이 내려다보이는 부지에 자리잡고 있습니다. 오랫동안 최고의 싱글 몰트 위스키 중 하나로 여겨져 왔으며 다른 위스키의 기준이 되는 벤치마크입니다.",
                profileJson = """{"sweetness":70,"body":80,"richness":85,"smokiness":10,"finish":78,"complexity":82}""",
                tastingNotesJson = """["dried fruit","sherry","vanilla","ginger","oak","chocolate"]""",
                tastingNotesKoJson = """["건과일","셰리","바닐라","생강","오크","초콜릿"]""",
                suggestedImageKeyword = "macallan whisky bottle",
                status = "active",
                createdAt = now,
                updatedAt = now
            ),
            Liquor(
                name = "Hendricks Gin",
                nameKo = "헨드릭스 진",
                type = "Premium Gin",
                typeKo = "프리미엄 진",
                category = "gin",
                abv = 41.4,
                age = null,
                score = 85,
                price = "$35",
                origin = "Scotland",
                region = "Girvan",
                volume = "750ml",
                about = "Hendricks is a unique gin infused with cucumber and rose petals, creating a wonderfully refreshing spirit. Distilled in Scotland using a combination of a Carter-Head still and a Bennett still.",
                aboutKo = "헨드릭스는 오이와 장미 꽃잎을 첨가하여 만든 독특한 진으로, 놀랍도록 상쾌한 스피릿을 제공합니다. 스코틀랜드에서 카터-헤드 스틸과 베넷 스틸을 조합하여 증류됩니다.",
                heritage = "First released in 1999 by William Grant & Sons, Hendricks quickly became a cult favorite. The distinctive apothecary-style bottle and unconventional botanicals set it apart from traditional London Dry gins.",
                heritageKo = "1999년 윌리엄 그랜트 앤 선즈에 의해 처음 출시된 헨드릭스는 빠르게 컬트적 인기를 얻었습니다. 독특한 약국 스타일의 병과 비전통적인 보타니컬이 전통적인 런던 드라이 진과 차별화됩니다.",
                profileJson = """{"juniper":55,"citrus":45,"floral":80,"herbal":60,"spice":35,"complexity":72}""",
                tastingNotesJson = """["cucumber","rose","citrus","juniper","black pepper"]""",
                tastingNotesKoJson = """["오이","장미","시트러스","주니퍼","흑후추"]""",
                suggestedImageKeyword = "hendricks gin bottle",
                status = "active",
                createdAt = now,
                updatedAt = now
            ),
            Liquor(
                name = "Opus One 2019",
                nameKo = "오퍼스 원 2019",
                type = "Red Blend",
                typeKo = "레드 블렌드",
                category = "wine",
                abv = 14.5,
                age = "2019 Vintage",
                score = 95,
                price = "$400",
                origin = "United States",
                region = "Napa Valley",
                volume = "750ml",
                about = "Opus One is a Bordeaux-style blend from Napa Valley, born of a partnership between Robert Mondavi and Baron Philippe de Rothschild. The 2019 vintage showcases exceptional depth with layers of dark fruit and refined tannins.",
                aboutKo = "오퍼스 원은 로버트 몬다비와 바론 필립 드 로칠드의 파트너십에서 탄생한 나파 밸리의 보르도 스타일 블렌드입니다. 2019년 빈티지는 다크 프루트의 깊이와 세련된 타닌이 돋보입니다.",
                heritage = "Established in 1979, Opus One represents the first ultra-premium joint venture between a Napa Valley and a Bordeaux winery. Each vintage is a testament to the unique terroir of the Oakville appellation.",
                heritageKo = "1979년에 설립된 오퍼스 원은 나파 밸리와 보르도 와이너리 간의 최초의 울트라 프리미엄 합작 투자를 대표합니다. 각 빈티지는 오크빌 아펠라시옹의 독특한 테루아르를 증명합니다.",
                profileJson = """{"sweetness":25,"acidity":65,"tannin":75,"body":88,"fruitiness":82,"complexity":92}""",
                tastingNotesJson = """["blackcurrant","dark cherry","violet","cedar","mocha","graphite"]""",
                tastingNotesKoJson = """["블랙커런트","다크체리","바이올렛","시더","모카","흑연"]""",
                suggestedImageKeyword = "opus one wine bottle",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
        ))
    }
}
