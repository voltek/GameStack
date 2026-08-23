package com.gamestack.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDto(
    val id: Int,
    val name: String,
    val cover: GameCoverDto? = null,
    val genres: List<GameGenreDto>? = null,
    @SerialName("involved_companies")
    val involvedCompanies: List<InvolvedCompanyDto>? = null
)
