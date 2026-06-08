package com.shonkware.droidmodloader.engine.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NexusUrlParserTest {

    @Test
    fun parse_readsGameDomainAndModIdFromNexusModUrl() {
        val result = NexusUrlParser.parse(
            "https://www.nexusmods.com/newvegas/mods/12345"
        )

        requireNotNull(result)

        assertEquals("newvegas", result.gameDomain)
        assertEquals(12345L, result.modId)
        assertNull(result.fileId)
    }

    @Test
    fun parse_readsFileIdWhenPresent() {
        val result = NexusUrlParser.parse(
            "https://www.nexusmods.com/skyrimspecialedition/mods/777?tab=files&file_id=888"
        )

        requireNotNull(result)

        assertEquals("skyrimspecialedition", result.gameDomain)
        assertEquals(777L, result.modId)
        assertEquals(888L, result.fileId)
    }

    @Test
    fun parse_returnsNullForNonNexusUrl() {
        assertNull(
            NexusUrlParser.parse("https://example.com/downloads/mod.zip")
        )
    }
}