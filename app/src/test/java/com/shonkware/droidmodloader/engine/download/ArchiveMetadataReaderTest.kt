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
    fun detectArchiveFormat_recognizesSupportedArchiveTypes() {
        assertEquals("zip", ArchiveMetadataReader.detectArchiveFormat("Example Mod.zip"))
        assertEquals("7z", ArchiveMetadataReader.detectArchiveFormat("Example Mod.7z"))
        assertEquals("rar", ArchiveMetadataReader.detectArchiveFormat("Example Mod.rar"))
        assertEquals("tar", ArchiveMetadataReader.detectArchiveFormat("Example Mod.tar"))
        assertEquals("tar.gz", ArchiveMetadataReader.detectArchiveFormat("Example Mod.tar.gz"))
        assertEquals("tgz", ArchiveMetadataReader.detectArchiveFormat("Example Mod.tgz"))
        assertEquals("tar.bz2", ArchiveMetadataReader.detectArchiveFormat("Example Mod.tar.bz2"))
        assertEquals("tbz2", ArchiveMetadataReader.detectArchiveFormat("Example Mod.tbz2"))
    }

    @Test
    fun detectArchiveFormat_returnsUnknownForUnsupportedExtension() {
        assertEquals("unknown", ArchiveMetadataReader.detectArchiveFormat("readme.txt"))
    }

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
