package com.liquir.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Intermediate data class holding factual data from external databases.
 */
data class ExternalLookupData(
    val name: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val abv: Double? = null,
    val volume: String? = null,
    val origin: String? = null,
    val region: String? = null,
    val price: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val ingredients: String? = null,
    val style: String? = null,
    val source: String = "unknown",
    val extra: Map<String, Any?> = emptyMap()
)

interface ExternalDatabaseService {
    fun collectAll(searchQueries: List<String>, category: String): ExternalLookupResult
    fun collectAll(searchQueries: List<String>, category: String, onProgress: (String, String) -> Unit): ExternalLookupResult
}

data class ExternalLookupResult(
    val found: Boolean,
    val data: List<ExternalLookupData> = emptyList(),
    val sources: List<String> = emptyList()
)

@Service
class ExternalDatabaseServiceImpl(
    @Value("\${app.ai.untappd-client-id:}") private val untappdClientId: String,
    @Value("\${app.ai.untappd-client-secret:}") private val untappdClientSecret: String,
    private val webCrawlerService: WebCrawlerService
) : ExternalDatabaseService {

    private val log = LoggerFactory.getLogger(ExternalDatabaseServiceImpl::class.java)
    private val restTemplate = RestTemplate()
    private val mapper = jacksonObjectMapper()
    private val executor = Executors.newFixedThreadPool(6)

    /**
     * Check if an external result is relevant to the search query.
     * Extracts the brand name (first significant word) from the canonical query
     * and ensures it appears in the result name/brand.
     */
    private fun isRelevantResult(result: ExternalLookupData, searchQueries: List<String>): Boolean {
        val resultName = ((result.name ?: "") + " " + (result.brand ?: "")).lowercase().trim()
        if (resultName.isBlank()) return true // No name to check, allow through

        // Extract brand keywords from the first (canonical) search query
        // e.g., "Talisker 10 Year Old" → brand is "talisker"
        val canonical = searchQueries.firstOrNull()?.lowercase() ?: return true
        val brandWords = canonical.split(" ", "-")
            .filter { it.length > 2 }
            .filter { it !in setOf("year", "years", "old", "single", "malt", "double", "triple",
                "blend", "blended", "cask", "reserve", "special", "edition", "limited",
                "vintage", "aged", "barrel", "strength", "proof", "bottle", "bottled",
                "the", "and", "for", "with") }

        if (brandWords.isEmpty()) return true

        // The primary brand word (first significant word) MUST be present in the result
        val primaryBrand = brandWords.first()
        val found = resultName.contains(primaryBrand)
        if (!found) {
            log.info("Rejecting irrelevant result '{}' - brand '{}' not found (query: '{}')",
                resultName.trim(), primaryBrand, canonical)
        }
        return found
    }

    override fun collectAll(searchQueries: List<String>, category: String): ExternalLookupResult {
        return collectAll(searchQueries, category) { _, _ -> }
    }

    override fun collectAll(searchQueries: List<String>, category: String, onProgress: (String, String) -> Unit): ExternalLookupResult {
        val allData = mutableListOf<ExternalLookupData>()
        val sources = mutableListOf<String>()

        // Build all search tasks to run in parallel
        data class SearchTask(
            val name: String,
            val nameKo: String,
            val callable: Callable<ExternalLookupData?>
        )

        val tasks = mutableListOf<SearchTask>()

        // 1. Open Food Facts (all categories)
        tasks.add(SearchTask("Open Food Facts", "Open Food Facts", Callable {
            for (query in searchQueries) {
                try {
                    val result = searchOpenFoodFacts(query)
                    if (result != null) {
                        log.info("Found '{}' in Open Food Facts", query)
                        return@Callable result
                    }
                } catch (e: Exception) {
                    log.warn("Open Food Facts lookup failed for '{}': {}", query, e.message)
                }
            }
            null
        }))

        // 2. Category-specific APIs
        when (category.lowercase()) {
            "whisky" -> {
                tasks.add(SearchTask("WhiskyHunter", "WhiskyHunter", Callable {
                    for (query in searchQueries) {
                        try {
                            val result = searchWhiskyHunter(query)
                            if (result != null) return@Callable result
                        } catch (e: Exception) {
                            log.warn("WhiskyHunter lookup failed: {}", e.message)
                        }
                    }
                    null
                }))
            }
            "liqueur" -> {
                tasks.add(SearchTask("TheCocktailDB", "TheCocktailDB", Callable {
                    for (query in searchQueries) {
                        try {
                            val result = searchCocktailDb(query)
                            if (result != null) return@Callable result
                        } catch (e: Exception) {
                            log.warn("CocktailDB lookup failed: {}", e.message)
                        }
                    }
                    null
                }))
            }
            "beer" -> {
                if (untappdClientId.isNotBlank() && untappdClientSecret.isNotBlank()) {
                    tasks.add(SearchTask("Untappd", "Untappd", Callable {
                        for (query in searchQueries) {
                            try {
                                val result = searchUntappd(query)
                                if (result != null) return@Callable result
                            } catch (e: Exception) {
                                log.warn("Untappd lookup failed: {}", e.message)
                            }
                        }
                        null
                    }))
                }
            }
        }

        // 3. Web crawling (all categories)
        val crawlSourceName = when (category.lowercase()) {
            "whisky" -> "whisky.com"
            "wine" -> "Vivino"
            "beer" -> "BeerAdvocate"
            "sake" -> "Sakenomy"
            else -> "Web crawl"
        }
        tasks.add(SearchTask(crawlSourceName, crawlSourceName, Callable {
            try {
                val crawlResults = webCrawlerService.crawl(searchQueries.first(), category, searchQueries)
                crawlResults.firstOrNull()
            } catch (e: Exception) {
                log.warn("Web crawling failed for '{}': {}", searchQueries.firstOrNull(), e.message)
                null
            }
        }))

        // Report all sources being searched
        val sourceNames = tasks.map { it.name }
        onProgress(
            sourceNames.joinToString(", "),
            sourceNames.joinToString(", ")
        )

        // Submit all tasks in parallel
        val futures = tasks.map { task ->
            task to executor.submit(task.callable)
        }

        // Collect results with timeout, filtering irrelevant matches
        for ((task, future) in futures) {
            try {
                val result = future.get(15, TimeUnit.SECONDS)
                if (result != null) {
                    if (isRelevantResult(result, searchQueries)) {
                        allData.add(result)
                        sources.add(result.source)
                        log.info("Got relevant data from {}: '{}'", task.name, result.name ?: result.brand)
                    } else {
                        log.info("Discarded irrelevant result from {}: '{}'", task.name, result.name ?: result.brand)
                    }
                }
            } catch (e: Exception) {
                log.warn("{} timed out or failed: {}", task.name, e.message)
            }
        }

        return ExternalLookupResult(
            found = allData.isNotEmpty(),
            data = allData,
            sources = sources.distinct()
        )
    }

    // ========== Open Food Facts ==========

    private fun searchOpenFoodFacts(name: String): ExternalLookupData? {
        val url = "https://world.openfoodfacts.org/cgi/search.pl" +
                "?search_terms=${java.net.URLEncoder.encode(name, "UTF-8")}" +
                "&search_simple=1&action=process&json=1&page_size=5"

        val response = restTemplate.getForEntity(url, Map::class.java)
        val body = response.body ?: return null
        val products = body["products"] as? List<*> ?: return null
        if (products.isEmpty()) return null

        val product = findBestMatch(products, name) ?: return null
        val p = product as Map<*, *>

        val productName = p["product_name"] as? String
        val brands = p["brands"] as? String
        val categories = p["categories"] as? String
        val alcohol = extractAlcohol(p)
        val quantity = p["quantity"] as? String
        val countries = p["countries"] as? String
        val origins = p["origins"] as? String
        val imageUrl = p["image_front_url"] as? String ?: p["image_url"] as? String
        val ingredientsText = p["ingredients_text"] as? String

        val category = mapOpenFoodFactsCategory(categories)

        return ExternalLookupData(
            name = productName,
            brand = brands,
            category = category,
            abv = alcohol,
            volume = quantity,
            origin = countries ?: origins,
            imageUrl = imageUrl,
            ingredients = ingredientsText,
            source = "openfoodfacts",
            extra = mapOf("categories_raw" to categories, "origins" to origins)
        )
    }

    private fun findBestMatch(products: List<*>, query: String): Any? {
        val queryLower = query.lowercase()
        val queryWords = queryLower.split(" ", "-").filter { it.length > 2 }

        return products.filterNotNull()
            .maxByOrNull { product ->
                val p = product as Map<*, *>
                val name = (p["product_name"] as? String ?: "").lowercase()
                val brands = (p["brands"] as? String ?: "").lowercase()
                val combined = "$name $brands"
                queryWords.count { combined.contains(it) }
            }
            ?.takeIf { product ->
                val p = product as Map<*, *>
                val name = (p["product_name"] as? String ?: "").lowercase()
                val brands = (p["brands"] as? String ?: "").lowercase()
                val combined = "$name $brands"
                queryWords.any { combined.contains(it) }
            }
    }

    private fun extractAlcohol(product: Map<*, *>): Double? {
        val nutriments = product["nutriments"] as? Map<*, *>
        if (nutriments != null) {
            val alcohol = nutriments["alcohol_100g"]
            if (alcohol != null) {
                return when (alcohol) {
                    is Number -> alcohol.toDouble()
                    is String -> alcohol.toDoubleOrNull()
                    else -> null
                }
            }
        }
        val alcoholValue = product["alcohol_value"]
        if (alcoholValue != null) {
            return when (alcoholValue) {
                is Number -> alcoholValue.toDouble()
                is String -> alcoholValue.toDoubleOrNull()
                else -> null
            }
        }
        return null
    }

    private fun mapOpenFoodFactsCategory(categories: String?): String {
        if (categories == null) return "other"
        val lower = categories.lowercase()
        return when {
            lower.contains("whisk") || lower.contains("bourbon") || lower.contains("scotch") -> "whisky"
            lower.contains("wine") || lower.contains("vin") -> "wine"
            lower.contains("beer") || lower.contains("ale") || lower.contains("lager") || lower.contains("bière") -> "beer"
            lower.contains("gin") -> "gin"
            lower.contains("vodka") -> "vodka"
            lower.contains("rum") || lower.contains("rhum") -> "rum"
            lower.contains("tequila") || lower.contains("mezcal") -> "tequila"
            lower.contains("brandy") || lower.contains("cognac") || lower.contains("armagnac") -> "brandy"
            lower.contains("liqueur") || lower.contains("liquor") -> "liqueur"
            lower.contains("sake") || lower.contains("nihonshu") -> "sake"
            else -> "other"
        }
    }

    // ========== WhiskyHunter ==========

    private fun searchWhiskyHunter(name: String): ExternalLookupData? {
        val url = "https://whiskyhunter.net/api/distilleries_info/"
        val response = restTemplate.getForEntity(url, List::class.java)
        val distilleries = response.body ?: return null

        val queryLower = name.lowercase()
        val queryWords = queryLower.split(" ", "-").filter { it.length > 2 }

        // Find the best matching distillery (most words matched), not just the first
        val match = distilleries.filterNotNull()
            .map { entry ->
                val e = entry as Map<*, *>
                val distName = (e["name"] as? String ?: "").lowercase()
                val slug = (e["slug"] as? String ?: "").lowercase()
                val matchCount = queryWords.count { distName.contains(it) || slug.contains(it) }
                e to matchCount
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first as? Map<*, *> ?: return null

        return ExternalLookupData(
            brand = match["name"] as? String,
            category = "whisky",
            origin = match["country"] as? String,
            source = "whiskyhunter",
            extra = mapOf("slug" to match["slug"], "whisky_data" to match)
        )
    }

    // ========== TheCocktailDB ==========

    private fun searchCocktailDb(name: String): ExternalLookupData? {
        val url = "https://www.thecocktaildb.com/api/json/v1/1/search.php" +
                "?s=${java.net.URLEncoder.encode(name, "UTF-8")}"
        val response = restTemplate.getForEntity(url, Map::class.java)
        val body = response.body ?: return null
        val drinks = body["drinks"] as? List<*> ?: return null
        if (drinks.isEmpty()) return null

        val drink = drinks.first() as? Map<*, *> ?: return null
        val ingredients = (1..15).mapNotNull { i ->
            val ingredient = drink["strIngredient$i"] as? String
            val measure = drink["strMeasure$i"] as? String
            if (!ingredient.isNullOrBlank()) {
                if (!measure.isNullOrBlank()) "$measure $ingredient".trim() else ingredient
            } else null
        }

        return ExternalLookupData(
            name = drink["strDrink"] as? String,
            category = if (drink["strAlcoholic"] == "Alcoholic") "liqueur" else "other",
            description = drink["strInstructions"] as? String,
            imageUrl = drink["strDrinkThumb"] as? String,
            ingredients = ingredients.joinToString(", "),
            source = "thecocktaildb",
            extra = mapOf("glass" to drink["strGlass"], "ingredients_list" to ingredients)
        )
    }

    // ========== Untappd ==========

    private fun searchUntappd(name: String): ExternalLookupData? {
        if (untappdClientId.isBlank() || untappdClientSecret.isBlank()) return null

        val url = "https://api.untappd.com/v4/search/beer" +
                "?q=${java.net.URLEncoder.encode(name, "UTF-8")}" +
                "&client_id=$untappdClientId&client_secret=$untappdClientSecret"

        val response = restTemplate.getForEntity(url, Map::class.java)
        val body = response.body ?: return null
        val responseData = body["response"] as? Map<*, *> ?: return null
        val beers = responseData["beers"] as? Map<*, *> ?: return null
        val items = beers["items"] as? List<*> ?: return null
        if (items.isEmpty()) return null

        val item = items.first() as Map<*, *>
        val beer = item["beer"] as? Map<*, *> ?: return null
        val brewery = item["brewery"] as? Map<*, *>

        return ExternalLookupData(
            name = beer["beer_name"] as? String,
            brand = brewery?.get("brewery_name") as? String,
            category = "beer",
            abv = (beer["beer_abv"] as? Number)?.toDouble(),
            origin = brewery?.get("country_name") as? String,
            description = beer["beer_description"] as? String,
            imageUrl = beer["beer_label"] as? String,
            style = beer["beer_style"] as? String,
            source = "untappd",
            extra = mapOf("ibu" to (beer["beer_ibu"] as? Number)?.toDouble())
        )
    }
}
