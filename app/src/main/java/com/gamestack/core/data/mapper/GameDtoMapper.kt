package com.gamestack.core.data.mapper

import com.gamestack.core.data.remote.dto.GameDto
import com.gamestack.core.domain.model.Game

private const val IgdbCoverSizeThumb = "t_thumb"
private const val IgdbCoverSizeDisplay = "t_cover_big"

fun GameDto.toDomain(): Game = Game(
    id = id,
    name = name,
    coverUrl = cover?.url?.toDisplayCoverUrl(),
    genres = genres?.map { it.name } ?: emptyList(),
    developer = involvedCompanies
        ?.firstOrNull { it.developer }
        ?.company
        ?.name
)

// IGDB returns protocol-relative thumbnail URLs (e.g. "//images.igdb.com/.../t_thumb/xyz.jpg") —
// upgrade to a display-sized image and a real scheme before this ever reaches Coil.
private fun String.toDisplayCoverUrl(): String {
    val withScheme = if (startsWith("//")) "https:$this" else this
    return withScheme.replace(IgdbCoverSizeThumb, IgdbCoverSizeDisplay)
}
