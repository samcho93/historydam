package com.samdori93.yeoksadam.core.network.catalog

import kotlinx.serialization.Serializable

/**
 * 역사담 웹앱(arHeritage)이 GitHub Pages 에 게시하는 데이터 형식.
 * https://samcho93.github.io/arHeritage/data/{index.json, detail/<id>.json, figures.json}
 */
@Serializable
data class IndexDto(
    val count: Int = 0,
    val items: List<SiteDto> = emptyList(),
)

@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val designation: String = "",
    val category: String = "",
    val era: String = "",
    val city: String = "",
    val lat: Double,
    val lng: Double,
    val thumb: String? = null,
    val local: Boolean = false,
    val tour: Boolean = false,
)

@Serializable
data class DetailDto(
    val id: String,
    val name: String,
    val nameHanja: String = "",
    val designation: String = "",
    val category: String = "",
    val era: String = "",
    val city: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val image: String? = null,
    val images: List<ImageDto> = emptyList(),
    val description: String = "",
    val sourceUrl: String? = null,
    val local: Boolean = false,
    val tour: Boolean = false,
    val tel: String? = null,
)

@Serializable
data class ImageDto(val url: String, val desc: String = "")

@Serializable
data class FiguresDto(val figures: List<CatalogFigureDto> = emptyList())

@Serializable
data class CatalogFigureDto(
    val id: String,
    val name: String,
    val hanja: String = "",
    val title: String = "",
    val years: String = "",
    val seal: String = "",
    val style: String = "scholar",
    val bio: String = "",
    val sites: List<FigureSiteDto> = emptyList(),
    val portrait: String? = null,
    val cutout: String? = null,
    val portraitCredit: String? = null,
    val voice: String? = null,
    val fullBody: String? = null,
    val died: Int? = null,
)

@Serializable
data class FigureSiteDto(val id: String, val note: String = "")
