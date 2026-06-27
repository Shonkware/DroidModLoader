package com.shonkware.droidmodloader.engine.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ArchiveMetadataReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun cleanDisplayName_removesArchiveExtensionAndUnderscores() {
        assertEquals(
            "Example Mod",
            ArchiveMetadataReader.cleanDisplayName("Example_Mod.zip")
        )
    }

    @Test
    fun buildFingerprint_changesWhenArchiveMetadataChanges() {
        val first = tempFolder.newFile("first.zip")
        first.writeText("same content")
        first.setLastModified(1000L)

        val second = tempFolder.newFile("second.zip")
        second.writeText("same content")
        second.setLastModified(2000L)

        assertNotEquals(
            ArchiveMetadataReader.buildFingerprint(first),
            ArchiveMetadataReader.buildFingerprint(second)
        )
    }
}
