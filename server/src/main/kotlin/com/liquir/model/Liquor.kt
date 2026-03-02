package com.liquir.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "liquors")
class Liquor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    var nameKo: String? = null,

    var type: String? = null,

    var typeKo: String? = null,

    var category: String? = null,

    var abv: Double? = null,

    var age: String? = null,

    var score: Int? = null,

    var price: String? = null,

    var origin: String? = null,

    var region: String? = null,

    var volume: String? = null,

    @Column(length = 2000)
    var about: String? = null,

    @Column(length = 2000)
    var aboutKo: String? = null,

    @Column(length = 2000)
    var heritage: String? = null,

    @Column(length = 2000)
    var heritageKo: String? = null,

    @Column(length = 2000)
    var profileJson: String? = null,

    @Column(length = 2000)
    var tastingNotesJson: String? = null,

    @Column(length = 2000)
    var tastingNotesKoJson: String? = null,

    var imageUrl: String? = null,

    var suggestedImageKeyword: String? = null,

    @Column(nullable = false)
    var status: String = "active",

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
