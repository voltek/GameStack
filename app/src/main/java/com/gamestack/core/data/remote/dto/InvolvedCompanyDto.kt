package com.gamestack.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvolvedCompanyDto(
    val developer: Boolean = false,
    val company: GameCompanyDto? = null
)
