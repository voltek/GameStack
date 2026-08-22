package com.gamestack.core.data.mapper

import com.gamestack.core.data.remote.dto.GameCompanyDto
import com.gamestack.core.data.remote.dto.GameCoverDto
import com.gamestack.core.data.remote.dto.GameDto
import com.gamestack.core.data.remote.dto.GameGenreDto
import com.gamestack.core.data.remote.dto.InvolvedCompanyDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDtoMapperTest {

    // Spec: every field maps 1:1, and the cover URL is upgraded to a display size with a real scheme.
    @Test
    fun `toDomain should map all fields when dto is fully populated`() {
        val dto = GameDto(
            id = 1,
            name = "Elden Ring",
            cover = GameCoverDto(url = "//images.igdb.com/igdb/image/upload/t_thumb/co1.jpg"),
            genres = listOf(GameGenreDto("RPG"), GameGenreDto("Adventure")),
            involvedCompanies = listOf(
                InvolvedCompanyDto(developer = false, company = GameCompanyDto("Bandai Namco")),
                InvolvedCompanyDto(developer = true, company = GameCompanyDto("FromSoftware"))
            )
        )

        val game = dto.toDomain()

        assertEquals(1, game.id)
        assertEquals("Elden Ring", game.name)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/co1.jpg", game.coverUrl)
        assertEquals(listOf("RPG", "Adventure"), game.genres)
        assertEquals("FromSoftware", game.developer)
    }

    // Spec: a missing cover maps to a null coverUrl, never a crash or a broken URL.
    @Test
    fun `toDomain should map null coverUrl when cover is null`() {
        val dto = GameDto(id = 2, name = "Untitled")

        val game = dto.toDomain()

        assertNull(game.coverUrl)
    }

    // Spec: no genres means an empty list, not null — UI never has to null-check it.
    @Test
    fun `toDomain should map empty genres list when genres is null`() {
        val dto = GameDto(id = 3, name = "Untitled")

        val game = dto.toDomain()

        assertTrue(game.genres.isEmpty())
    }

    // Spec: no company flagged as developer means a null developer, not the publisher's name.
    @Test
    fun `toDomain should map null developer when no involved company is flagged as developer`() {
        val dto = GameDto(
            id = 4,
            name = "Untitled",
            involvedCompanies = listOf(
                InvolvedCompanyDto(developer = false, company = GameCompanyDto("Some Publisher"))
            )
        )

        val game = dto.toDomain()

        assertNull(game.developer)
    }

    // Spec: with multiple involved companies, the one flagged developer=true wins, not just the first entry.
    @Test
    fun `toDomain should pick the company flagged developer when multiple involved companies exist`() {
        val dto = GameDto(
            id = 5,
            name = "Untitled",
            involvedCompanies = listOf(
                InvolvedCompanyDto(developer = false, company = GameCompanyDto("Publisher")),
                InvolvedCompanyDto(developer = true, company = GameCompanyDto("Actual Developer"))
            )
        )

        val game = dto.toDomain()

        assertEquals("Actual Developer", game.developer)
    }
}
