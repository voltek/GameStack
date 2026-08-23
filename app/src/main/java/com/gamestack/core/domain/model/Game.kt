package com.gamestack.core.domain.model

data class Game(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val genres: List<String>,
    val developer: String?
)
