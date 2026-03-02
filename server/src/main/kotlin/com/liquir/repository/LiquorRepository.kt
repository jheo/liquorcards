package com.liquir.repository

import com.liquir.model.Liquor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LiquorRepository : JpaRepository<Liquor, Long> {

    fun findByCategory(category: String): List<Liquor>

    fun findByStatus(status: String): List<Liquor>

    fun findByCategoryAndStatus(category: String, status: String): List<Liquor>

    @Query("SELECT l FROM Liquor l WHERE LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    fun searchByName(search: String): List<Liquor>

    @Query(
        "SELECT l FROM Liquor l WHERE " +
        "(:category IS NULL OR l.category = :category) AND " +
        "(:status IS NULL OR l.status = :status) AND " +
        "(:search IS NULL OR " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.type) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.typeKo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.origin) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.region) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.age) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.about) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.aboutKo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.heritage) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.heritageKo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.tastingNotesJson) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.tastingNotesKoJson) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.profileJson) LIKE LOWER(CONCAT('%', :search, '%'))" +
        ")"
    )
    fun findByFilters(category: String?, status: String?, search: String?): List<Liquor>
}
