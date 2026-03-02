package com.liquir.service

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Web crawler for scraping liquor information from category-specific websites.
 * Each category has dedicated crawling logic for its best data sources.
 */
interface WebCrawlerService {
    fun crawl(name: String, category: String, searchQueries: List<String>): List<ExternalLookupData>
}

@Service
class WebCrawlerServiceImpl : WebCrawlerService {

    private val log = LoggerFactory.getLogger(WebCrawlerServiceImpl::class.java)

    private val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override fun crawl(name: String, category: String, searchQueries: List<String>): List<ExternalLookupData> {
        val results = mutableListOf<ExternalLookupData>()

        for (query in searchQueries) {
            try {
                val categoryResults = when (category.lowercase()) {
                    "whisky" -> crawlWhiskyCom(query)
                    "wine" -> crawlVivinoApi(query)
                    "beer" -> crawlBeerAdvocate(query)
                    "sake", "other" -> crawlSakenomy(query)
                    "gin", "vodka", "rum", "tequila", "brandy", "liqueur" -> crawlWhiskyCom(query)
                    else -> null
                }
                if (categoryResults != null) {
                    results.add(categoryResults)
                    break
                }
            } catch (e: Exception) {
                log.warn("Crawl failed for '{}' on category '{}': {}", query, category, e.message)
            }
        }

        return results
    }

    // ========== Whisky.com ==========

    private fun crawlWhiskyCom(query: String): ExternalLookupData? {
        val searchUrl = "https://www.whisky.com/whisky-database/bottle-search.html" +
                "?w_text=${java.net.URLEncoder.encode(query, "UTF-8")}"

        val searchDoc = fetchDocument(searchUrl) ?: return null

        val productLink = searchDoc.select("a[href*=/whisky-database/details/]").firstOrNull()
            ?: searchDoc.select("a[href*=/whisky-database/]").firstOrNull()
            ?: return null

        val productUrl = if (productLink.attr("href").startsWith("http")) {
            productLink.attr("href")
        } else {
            "https://www.whisky.com${productLink.attr("href")}"
        }

        val doc = fetchDocument(productUrl) ?: return null

        val name = doc.selectFirst("h1")?.text()?.trim()
        val bodyText = doc.body().text()

        val abv = extractPattern(bodyText, """(?:ABV|Alcohol)[:\s]*(\d+(?:\.\d+)?)\s*%""")?.toDoubleOrNull()
        val age = extractPattern(bodyText, """(?:Age|Alter)[:\s]*(\d+\s*(?:Years?|Jahre))""")
        val origin = extractPattern(bodyText, """(?:Country|Land)[,\s]*(?:Region)?[:\s]*([A-Za-z\s]+?)(?:\s*,|\s*Type|\s*$)""")
        val type = extractPattern(bodyText, """Type[:\s]*([A-Za-z\s]+?)(?:\s*Age|\s*ABV|\s*Bottle|\s*$)""")
        val volume = extractPattern(bodyText, """(?:Bottle size|Flaschengröße)[:\s]*([\d.]+\s*l)""")
        // Get product image, excluding logos and SVGs
        val imageUrl = doc.select("img[src*=fileadmin]")
            .filter { img ->
                val src = img.absUrl("src").lowercase()
                !src.contains("logo") && !src.endsWith(".svg") && !src.contains("/icons/")
            }
            .firstOrNull()?.absUrl("src")

        // Extract tasting notes and description
        val tastingSection = doc.select(".tasting-notes, .whisky-tasting, [class*=tasting]")
        val nose = doc.select("*:containsOwn(Nose) + *, *:containsOwn(Aroma) + *").firstOrNull()?.text()?.trim()
        val palate = doc.select("*:containsOwn(Palate) + *, *:containsOwn(Taste) + *").firstOrNull()?.text()?.trim()
        val finish = doc.select("*:containsOwn(Finish) + *").firstOrNull()?.text()?.trim()

        val tastingParts = listOfNotNull(
            nose?.let { "Nose: $it" },
            palate?.let { "Palate: $it" },
            finish?.let { "Finish: $it" }
        )
        val tastingText = tastingParts.joinToString(". ").ifEmpty { null }

        // Try to get description paragraphs
        val descriptionEl = doc.select(".description, .whisky-description, .about, article p").firstOrNull()
        val description = descriptionEl?.text()?.trim()?.takeIf { it.length > 30 }
            ?: tastingSection.text().trim().takeIf { it.length > 30 }

        if (name == null && abv == null) return null

        return ExternalLookupData(
            name = name,
            category = "whisky",
            abv = abv,
            volume = volume?.let { formatVolume(it) },
            origin = origin?.trim(),
            style = type?.trim(),
            description = description ?: tastingText,
            imageUrl = imageUrl,
            source = "whisky.com",
            extra = buildMap {
                put("age", age)
                put("product_url", productUrl)
                if (nose != null) put("nose", nose)
                if (palate != null) put("palate", palate)
                if (finish != null) put("finish", finish)
            }
        )
    }

    // ========== Vivino Internal API ==========

    private fun crawlVivinoApi(query: String): ExternalLookupData? {
        try {
            val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

            val url = "https://www.vivino.com/api/explore/explore" +
                    "?q=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                    "&page=1&per_page=1"

            val connection = Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "application/json")
                .ignoreContentType(true)
                .timeout(10000)

            val response = connection.execute()
            val json = mapper.readTree(response.body())

            val matches = json.path("explore_vintage").path("matches")
            if (!matches.isArray || matches.size() == 0) return null

            val match = matches[0]
            val vintage = match.path("vintage")
            val wine = vintage.path("wine")

            val name = wine.path("name").asText(null)
            val winery = wine.path("winery").path("name").asText(null)
            val regionName = wine.path("region").path("name").asText(null)
            val country = wine.path("region").path("country").path("name").asText(null)
            val rating = vintage.path("statistics").path("wine_ratings_average").asDouble(0.0)
            val ratingsCount = vintage.path("statistics").path("ratings_count").asInt(0)
            val price = match.path("price").path("amount").asDouble(0.0)
            val imageUrl = vintage.path("image").path("location").asText(null)

            // Extract taste info from Vivino API
            val taste = wine.path("taste")
            val structure = taste.path("structure")
            val acidity = structure.path("acidity").asDouble(0.0)
            val fizziness = structure.path("fizziness").asDouble(0.0)
            val intensity = structure.path("intensity").asDouble(0.0)
            val sweetness = structure.path("sweetness").asDouble(0.0)
            val tannin = structure.path("tannin").asDouble(0.0)

            // Flavor groups
            val flavorGroups = taste.path("flavor")
            val flavors = mutableListOf<String>()
            if (flavorGroups.isArray) {
                for (fg in flavorGroups) {
                    fg.path("group").asText(null)?.let { flavors.add(it) }
                }
            }

            val wineDescription = wine.path("description").asText(null)
                ?: wine.path("review").asText(null)

            if (name == null) return null

            return ExternalLookupData(
                name = if (winery != null) "$winery $name" else name,
                brand = winery,
                category = "wine",
                origin = country,
                region = regionName,
                price = if (price > 0) "$${String.format("%.0f", price)}" else null,
                description = wineDescription,
                imageUrl = imageUrl,
                source = "vivino",
                extra = buildMap {
                    if (rating > 0) put("rating", rating)
                    if (ratingsCount > 0) put("ratings_count", ratingsCount)
                    put("vintage_year", vintage.path("year").asText(null))
                    if (flavors.isNotEmpty()) put("flavor_notes", flavors.joinToString(", "))
                    if (acidity > 0) put("acidity", acidity)
                    if (sweetness > 0) put("sweetness", sweetness)
                    if (tannin > 0) put("tannin", tannin)
                    if (intensity > 0) put("intensity", intensity)
                    if (fizziness > 0) put("fizziness", fizziness)
                }
            )
        } catch (e: Exception) {
            log.debug("Vivino API crawl failed for '{}': {}", query, e.message)
            return null
        }
    }

    // ========== BeerAdvocate ==========

    private fun crawlBeerAdvocate(query: String): ExternalLookupData? {
        val searchUrl = "https://www.beeradvocate.com/search/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val searchDoc = fetchDocument(searchUrl) ?: return null

        val beerLink = searchDoc.select("a[href*=/beer/profile/]").firstOrNull() ?: return null
        val beerUrl = "https://www.beeradvocate.com${beerLink.attr("href")}"

        val doc = fetchDocument(beerUrl) ?: return null

        val titleEl = doc.selectFirst("#ba-content h1")
        val beerName = titleEl?.ownText()?.trim()
        val brewery = titleEl?.selectFirst("span")?.text()?.trim()

        val infoBox = doc.selectFirst("#info_box")?.text() ?: ""

        val abv = extractPattern(infoBox, """(?:ABV|Alcohol)[:\s]*(\d+(?:\.\d+)?)%""")?.toDoubleOrNull()
        val style = extractPattern(infoBox, """Style[:\s]*([A-Za-z\s\-/]+?)(?:\s*ABV|\s*Alcohol|\s*Avail)""")

        val scoreEl = doc.selectFirst(".ba-ravg")
        val score = scoreEl?.text()?.trim()

        // Extract description and tasting notes
        val descEl = doc.selectFirst("#ba-content .break")
            ?: doc.selectFirst("#ba-content p")
        val description = descEl?.text()?.trim()?.takeIf { it.length > 20 }

        // Look for review text / notes
        val reviews = doc.select("#rating_fullview_container .muted_mid")
        val topReview = reviews.firstOrNull()?.text()?.trim()?.takeIf { it.length > 30 }

        if (beerName == null) return null

        return ExternalLookupData(
            name = beerName,
            brand = brewery,
            category = "beer",
            abv = abv,
            style = style?.trim(),
            description = description ?: topReview,
            source = "beeradvocate",
            extra = buildMap {
                put("ba_score", score)
                put("product_url", beerUrl)
                if (topReview != null && description != null) put("top_review", topReview)
            }
        )
    }

    // ========== Sakenomy ==========

    private fun crawlSakenomy(query: String): ExternalLookupData? {
        try {
            val searchUrl = "https://www.sakenomy.jp/en/search/sake/?keyword=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val searchDoc = fetchDocument(searchUrl) ?: return null

            val nextDataScript = searchDoc.selectFirst("script#__NEXT_DATA__")
            if (nextDataScript != null) {
                val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                val jsonData = mapper.readTree(nextDataScript.data())
                val pageProps = jsonData.path("props").path("pageProps")

                val sakeList = pageProps.path("sakeList")
                    .takeIf { it.isArray && it.size() > 0 }
                    ?: pageProps.path("sake")
                        .takeIf { it.isArray && it.size() > 0 }

                if (sakeList != null && sakeList.size() > 0) {
                    val sake = sakeList[0]
                    return extractSakenomyData(sake)
                }
            }

            val productLinks = searchDoc.select("a[href*=/en/sake/]")
            if (productLinks.isNotEmpty()) {
                val href = productLinks.first()!!.attr("href")
                val productUrl = if (href.startsWith("http")) href else "https://www.sakenomy.jp$href"
                return crawlSakenomyProduct(productUrl)
            }

            return null
        } catch (e: Exception) {
            log.debug("Sakenomy crawl failed for '{}': {}", query, e.message)
            return null
        }
    }

    private fun crawlSakenomyProduct(url: String): ExternalLookupData? {
        val doc = fetchDocument(url) ?: return null
        val nextDataScript = doc.selectFirst("script#__NEXT_DATA__") ?: return null

        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val jsonData = mapper.readTree(nextDataScript.data())
        val pageProps = jsonData.path("props").path("pageProps")

        val sake = pageProps.path("sake").takeIf { !it.isMissingNode }
            ?: pageProps.takeIf { pageProps.has("name") || pageProps.has("title") }
            ?: return null

        return extractSakenomyData(sake)
    }

    private fun extractSakenomyData(sake: com.fasterxml.jackson.databind.JsonNode): ExternalLookupData? {
        val name = sake.path("name").asText(null)
            ?: sake.path("title").asText(null)
            ?: return null

        val brewery = sake.path("brewery").path("name").asText(null)
            ?: sake.path("breweryName").asText(null)
        val prefecture = sake.path("brewery").path("prefecture").asText(null)
            ?: sake.path("prefecture").asText(null)
        val abv = sake.path("alcoholContent").asDouble(0.0).takeIf { it > 0 }
            ?: sake.path("alcohol").asDouble(0.0).takeIf { it > 0 }

        val imageUrls = sake.path("imageUrls")
        val imageUrl = if (imageUrls.isArray && imageUrls.size() > 0) {
            imageUrls[0].asText(null)
        } else {
            sake.path("imageUrl").asText(null)
        }

        // Flavor profile
        val flavors = sake.path("flavors")
        val flavorMap = if (!flavors.isMissingNode) {
            mapOf(
                "fragrance" to flavors.path("fragrance").asDouble(0.0),
                "sweetness" to flavors.path("sweetness").asDouble(0.0),
                "acidity" to flavors.path("acidity").asDouble(0.0),
                "taste" to flavors.path("taste").asDouble(0.0),
                "bitterness" to flavors.path("bitterness").asDouble(0.0)
            ).filterValues { it > 0 }
        } else emptyMap()

        val sweetnessLevel = sake.path("sweetnessLevel").path("name").asText(null)
        val classification = sake.path("classification").asText(null)
            ?: sake.path("type").asText(null)

        // Description and tasting notes
        val description = sake.path("description").asText(null)
            ?: sake.path("about").asText(null)
        val tastingNote = sake.path("tastingNote").asText(null)
            ?: sake.path("tasting_note").asText(null)

        return ExternalLookupData(
            name = name,
            brand = brewery,
            category = "other", // sake
            abv = abv,
            origin = "Japan",
            region = prefecture,
            description = description ?: tastingNote,
            imageUrl = imageUrl,
            style = classification,
            source = "sakenomy",
            extra = buildMap {
                if (flavorMap.isNotEmpty()) put("flavor_profile", flavorMap)
                if (sweetnessLevel != null) put("sweetness_level", sweetnessLevel)
                if (classification != null) put("classification", classification)
                if (tastingNote != null && description != null) put("tasting_note", tastingNote)
                put("sake", true)
            }
        )
    }

    // ========== Utility Methods ==========

    private fun fetchDocument(url: String): Document? {
        return try {
            Jsoup.connect(url)
                .userAgent(userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9,ja;q=0.8,ko;q=0.7")
                .timeout(10000)
                .followRedirects(true)
                .get()
        } catch (e: Exception) {
            log.debug("Failed to fetch '{}': {}", url, e.message)
            null
        }
    }

    private fun extractPattern(text: String, pattern: String): String? {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun formatVolume(raw: String): String {
        val liters = Regex("""([\d.]+)\s*l""", RegexOption.IGNORE_CASE).find(raw)
        if (liters != null) {
            val value = liters.groupValues[1].toDoubleOrNull() ?: return raw
            return if (value < 10) "${(value * 1000).toInt()}ml" else raw
        }
        return raw
    }
}
