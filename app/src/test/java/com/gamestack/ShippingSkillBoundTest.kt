package com.gamestack

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShippingSkillBoundTest {

    @Test
    fun `should keep each shipping Skill within its byte bound`() {
        val skills = System.getProperty("gamestack.shippingSkills")
            ?: error("Run through Gradle: app/build.gradle.kts names the shipping Skills.")
        skills.split(",").forEach { name ->
            val bytes = File("../.claude/skills/$name/SKILL.md").readBytes().size
            assertTrue(
                "$name/SKILL.md is $bytes bytes, over $MAX_BYTES: past it, compaction stops " +
                    "re-attaching it whole, so move a rare case to references/ (ADR 0060).",
                bytes <= MAX_BYTES
            )
        }
    }

    private companion object {
        // Bytes over-count characters outside ASCII, so the bound only errs safe.
        const val MAX_BYTES = 15_000
    }
}
