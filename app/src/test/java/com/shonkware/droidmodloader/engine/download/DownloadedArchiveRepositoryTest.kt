package com.shonkware.droidmodloader.engine.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadedArchiveRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun registerArchive_savesAndReloadsArchiveMetadata() {
        val archiveLibraryDir = tempFolder.newFolder("archive_library")
        val archiveListFile = File(tempFolder.newFolder("state"), "downloaded_archives.json")
        val repository = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        )

        val archiveFile = File(archiveLibraryDir, "Example_Mod.zip")
        archiveFile.writeSignature(
            0x50, 0x4B, 0x03, 0x04
        )
        archiveFile.setLastModified(1000L)

        val record = repository.registerArchive(
            archiveFile = archiveFile,
            originalDisplayName = "Example_Mod.zip",
            sourcePath = "/archives/example.zip",
            sourceUrl = "https://www.nexusmods.com/newvegas/mods/12345?tab=files&file_id=67890"
        )

        val reloaded = requireNotNull(repository.findById(record.archiveId))

        assertEquals(record.archiveId, reloaded.archiveId)
        assertEquals("Example Mod", reloaded.displayName)
        assertEquals("Example_Mod.zip", reloaded.fileName)
        assertEquals("zip", reloaded.archiveFormat)
        assertEquals("Example_Mod.zip", reloaded.relativePath)
        assertEquals(archiveFile.length(), reloaded.sizeBytes)
        assertEquals("/archives/example.zip", reloaded.sourcePath)
        assertEquals("newvegas", reloaded.nexusGameDomain)
        assertEquals(12345L, reloaded.nexusModId)
        assertEquals(67890L, reloaded.nexusFileId)
    }

    @Test
    fun registerArchive_replacesExistingRecordWithSameArchiveId() {
        val archiveLibraryDir = tempFolder.newFolder("archive_library")
        val archiveListFile = File(tempFolder.newFolder("state"), "downloaded_archives.json")
        val repository = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        )

        val archiveFile = File(archiveLibraryDir, "Same_Mod.zip")
        archiveFile.writeSignature(
            0x50, 0x4B, 0x03, 0x04
        )
        archiveFile.setLastModified(1000L)

        repository.registerArchive(
            archiveFile = archiveFile,
            originalDisplayName = "First Name.zip"
        )

        repository.registerArchive(
            archiveFile = archiveFile,
            originalDisplayName = "Second Name.zip"
        )

        val records = repository.load()

        assertEquals(1, records.size)
        assertEquals("Second Name", records.single().displayName)
    }

    @Test
    fun markInstalled_persistsInstalledModLink() {
        val archiveLibraryDir = tempFolder.newFolder("archive_library")
        val archiveListFile = File(tempFolder.newFolder("state"), "downloaded_archives.json")
        val repository = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        )

        val archiveFile = File(archiveLibraryDir, "Install_Me.7z")
        archiveFile.writeSignature(
            0x37, 0x7A, 0xBC, 0xAF,
            0x27, 0x1C, 0x00, 0x04
        )
        archiveFile.setLastModified(1000L)

        val record = repository.registerArchive(
            archiveFile = archiveFile,
            originalDisplayName = "Install_Me.7z"
        )

        repository.markInstalled(
            archiveId = record.archiveId,
            installedModId = "mod-example-123"
        )

        val reloaded = requireNotNull(repository.findById(record.archiveId))

        assertEquals("mod-example-123", reloaded.installedModId)
        assertTrue(reloaded.installedAtMillis != null)
    }

    @Test
    fun buildSummary_includesArchiveCountAndInstallState() {
        val archiveLibraryDir = tempFolder.newFolder("archive_library")
        val archiveListFile = File(tempFolder.newFolder("state"), "downloaded_archives.json")
        val repository = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        )

        val archiveFile = File(archiveLibraryDir, "Summary_Mod.rar")
        archiveFile.writeSignature(
            0x52, 0x61, 0x72, 0x21,
            0x1A, 0x07, 0x00
        )
        archiveFile.setLastModified(1000L)

        val record = repository.registerArchive(
            archiveFile = archiveFile,
            originalDisplayName = "Summary_Mod.rar"
        )

        repository.markInstalled(
            archiveId = record.archiveId,
            installedModId = "summary-mod-id"
        )

        val summary = repository.buildSummary()

        assertTrue(summary.contains("Archive count: 1"))
        assertTrue(summary.contains("Summary Mod"))
        assertTrue(summary.contains("Format: rar"))
        assertTrue(summary.contains("Installed mod: summary-mod-id"))
    }

    @Test
    fun buildSummary_handlesEmptyArchiveLibrary() {
        val archiveLibraryDir = tempFolder.newFolder("archive_library")
        val archiveListFile = File(tempFolder.newFolder("state"), "downloaded_archives.json")
        val repository = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        )

        assertEquals(
            "No archives are currently saved in the archive library.",
            repository.buildSummary()
        )
    }
    @Test
    fun loadMigratesLegacySourceUriIntoSourcePath() {
        val archiveLibraryDir = tempFolder.newFolder("legacy_archive_library")
        val archiveListFile = File(tempFolder.newFolder("legacy_state"), "downloaded_archives.json")
        archiveListFile.writeText(
            """
            [
              {
                "archiveId": "legacy",
                "displayName": "Legacy",
                "fileName": "Legacy.zip",
                "archiveFormat": "zip",
                "relativePath": "Legacy.zip",
                "sizeBytes": 1,
                "modifiedAtMillis": 1,
                "fingerprint": "legacy",
                "sourceUri": "/archives/Legacy.zip",
                "createdAtMillis": 1
              }
            ]
            """.trimIndent()
        )

        val record = DownloadedArchiveRepository(
            archiveLibraryDir = archiveLibraryDir,
            archiveListFile = archiveListFile
        ).load().single()

        assertEquals("/archives/Legacy.zip", record.sourcePath)
    }

    @Test
    fun registerArchive_usesDetectedFormatInsteadOfExtension() {
        val archiveLibraryDir =
            tempFolder.newFolder(
                "mismatch_archive_library"
            )

        val archiveListFile =
            File(
                tempFolder.newFolder("mismatch_state"),
                "downloaded_archives.json"
            )

        val repository =
            DownloadedArchiveRepository(
                archiveLibraryDir = archiveLibraryDir,
                archiveListFile = archiveListFile
            )

        val archiveFile =
            File(
                archiveLibraryDir,
                "Actually_Seven_Zip.zip"
            ).apply {
                writeSignature(
                    0x37, 0x7A, 0xBC, 0xAF,
                    0x27, 0x1C, 0x00, 0x04
                )
            }

        val record =
            repository.registerArchive(
                archiveFile = archiveFile,
                originalDisplayName =
                    "Actually_Seven_Zip.zip"
            )

        assertEquals(
            "7z",
            record.archiveFormat
        )
    }
    private fun File.writeSignature(
        vararg values: Int
    ) {
        writeBytes(
            values.map {
                it.toByte()
            }.toByteArray()
        )
    }

}
