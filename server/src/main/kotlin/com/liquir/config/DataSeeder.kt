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
                tastingDetail = "On the nose, rich aromas of dried fruits and sherry-soaked oak greet you, with hints of ginger spice and vanilla. The palate is full-bodied and warming, delivering waves of Christmas cake, orange peel, and dark chocolate, supported by a robust maltiness. The finish is long and satisfying, with lingering notes of wood spice, dried fruit sweetness, and a subtle nuttiness.",
                tastingDetailKo = "코에서는 건과일과 셰리에 적신 오크의 풍부한 향이 느껴지며, 생강 스파이스와 바닐라의 힌트가 곁들여집니다. 입안에서는 풀바디하고 따뜻한 느낌으로 크리스마스 케이크, 오렌지 껍질, 다크 초콜릿의 풍미가 물결치듯 퍼지며, 탄탄한 몰트 느낌이 뒷받침합니다. 피니시는 길고 만족스러우며, 우드 스파이스, 건과일의 달콤함, 은은한 견과류의 여운이 남습니다.",
                pairingJson = """["Dark Chocolate Truffles","Aged Cheddar","Christmas Pudding","Smoked Salmon","Dried Fruit & Nut Mix","Tiramisu"]""",
                pairingKoJson = """["다크 초콜릿 트러플","숙성 체다 치즈","크리스마스 푸딩","훈제 연어","건과일 & 견과류 믹스","티라미수"]""",
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
                tastingDetail = "The nose opens with a distinctive burst of fresh cucumber and delicate rose petals, layered over a soft juniper base with hints of citrus zest. On the palate, the cucumber freshness takes center stage, complemented by floral notes, a touch of black pepper, and subtle herbal complexity. The finish is clean and crisp, with a lingering coolness and gentle spice that invites another sip.",
                tastingDetailKo = "코에서는 신선한 오이와 섬세한 장미 꽃잎의 독특한 향이 먼저 느껴지며, 부드러운 주니퍼 베이스 위에 시트러스 제스트의 힌트가 겹쳐집니다. 입안에서는 오이의 상쾌함이 중심을 이루고, 꽃향, 블랙 페퍼의 터치, 은은한 허벌 복합미가 어우러집니다. 피니시는 깔끔하고 산뜻하며, 시원한 여운과 부드러운 스파이스가 남아 또 한 모금을 부릅니다.",
                pairingJson = """["Cucumber Sandwiches","Smoked Salmon Canapés","Light Salads","Oysters","Tonic Water with Lime"]""",
                pairingKoJson = """["오이 샌드위치","훈제 연어 카나페","라이트 샐러드","굴","토닉워터 & 라임"]""",
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
                tastingDetail = "The nose reveals an opulent bouquet of blackcurrant, ripe dark cherry, and violet, with undertones of cedar, tobacco leaf, and a hint of graphite. On the palate, the wine is richly textured with velvety tannins, delivering concentrated flavors of black plum, mocha, dark chocolate, and subtle earthy minerality. The finish is exceptionally long, with polished tannins and a persistent echo of cassis and fine oak spice.",
                tastingDetailKo = "코에서는 블랙커런트, 잘 익은 다크 체리, 바이올렛의 풍성한 부케가 느껴지며, 시더, 담뱃잎, 흑연의 은은한 기저가 깔려 있습니다. 입안에서는 벨벳 같은 타닌과 함께 풍부한 질감이 느껴지고, 블랙 플럼, 모카, 다크 초콜릿, 은은한 미네랄리티의 풍미가 집중적으로 전달됩니다. 피니시는 매우 길며, 세련된 타닌과 카시스 및 파인 오크 스파이스의 여운이 오래 지속됩니다.",
                pairingJson = """["Filet Mignon","Lamb Rack","Aged Gruyère","Wild Mushroom Risotto","Dark Chocolate Desserts","Truffle Dishes"]""",
                pairingKoJson = """["필레 미뇽","양갈비 랙","숙성 그뤼예르 치즈","야생 버섯 리조또","다크 초콜릿 디저트","트러플 요리"]""",
                suggestedImageKeyword = "opus one wine bottle",
                status = "active",
                createdAt = now,
                updatedAt = now
            )
        ))
    }
}
