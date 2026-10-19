package com.mrdastbaaf.mdbapp.features.appcontent.data.remote.dto

import kotlinx.serialization.Serializable

/** `GET /app-ui` — everything the home screen needs except the courses themselves. */
@Serializable
data class AppUiDto(
    val hero: HeroDto? = null,
    val sections: List<SectionDto> = emptyList(),
    val menu: MenuDto? = null,
    val featuredCourseIds: List<Int> = emptyList(),
)

@Serializable
data class HeroDto(
    val badgeText: String? = null,
    val headline: String? = null,
    val subheadline: String? = null,
    val ctaPrimaryText: String? = null,
    val ctaSecondaryText: String? = null,
    val stat1Value: String? = null,
    val stat1Label: String? = null,
    val stat2Value: String? = null,
    val stat2Label: String? = null,
    val stat3Value: String? = null,
    val stat3Label: String? = null,
)

@Serializable
data class SectionDto(
    val key: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Int? = null,
)

@Serializable
data class MenuDto(
    val dock: List<MenuItemDto> = emptyList(),
    val guides: List<MenuItemDto> = emptyList(),
)

@Serializable
data class MenuItemDto(
    val key: String? = null,
    val label: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Int? = null,
)

/** `GET /app-pages` — a bare array. */
@Serializable
data class AppPageDto(
    val id: String? = null,
    val title: String? = null,
    /** `all | members | admins` */
    val access: String? = null,
    val sortOrder: Int? = null,
    val url: String? = null,
)

/** `GET /app-pages/{id}/content`. */
@Serializable
data class AppPageContentDto(
    val id: String? = null,
    val title: String? = null,
    /** Rendered `the_content` — HTML. */
    val html: String? = null,
    val access: String? = null,
    val updatedAtJalali: String? = null,
    val url: String? = null,
)
