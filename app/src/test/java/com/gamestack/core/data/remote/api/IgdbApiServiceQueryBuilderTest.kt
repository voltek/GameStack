package com.gamestack.core.data.remote.api

import okio.Buffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IgdbApiServiceQueryBuilderTest {

    private fun bodyText(query: String): String {
        val buffer = Buffer()
        buildSearchGamesQuery(query).writeTo(buffer)
        return buffer.readUtf8()
    }

    // Spec: the user's query is embedded as the Apicalypse `search` clause.
    @Test
    fun `buildSearchGamesQuery should embed the query in a search clause`() {
        val text = bodyText("Elden Ring")

        assertTrue(text.contains("search \"Elden Ring\";"))
    }

    // Spec: a double quote in the user's query must not break out of the Apicalypse string literal.
    @Test
    fun `buildSearchGamesQuery should escape double quotes in the query`() {
        val text = bodyText("The \"Best\" Game")

        assertTrue(text.contains("search \"The \\\"Best\\\" Game\";"))
    }

    // Spec: IGDB rejects `search` combined with `sort` — the query must never add one.
    @Test
    fun `buildSearchGamesQuery should never include a sort clause`() {
        val text = bodyText("zelda")

        assertFalse(text.contains("sort"))
    }
}
