package com.shonkware.droidmodloader.engine.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class InstallReplacementRecoveryTest {
    private val store =
        InstallReplacementTransactionStore()

    @Test
    fun `prepared first install discards staged content`() {
        withFixture("first-prepared") { fixture ->
            val transaction = transaction(
                state = InstallReplacementState.PREPARED,
                hadExistingInstall = false
            )
            val stagedDir = fixture.directory(
                transaction.stagedDirectoryName,
                "new"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()

            assertEquals(
                InstallReplacementRecoveryAction
                    .DISCARDED_STAGED_INSTALL,
                result.action
            )
            assertFalse(stagedDir.exists())
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `prepared first install recognizes completed promotion`() {
        withFixture("first-promoted") { fixture ->
            val transaction = transaction(
                state = InstallReplacementState.PREPARED,
                hadExistingInstall = false
            )
            val finalDir = fixture.directory(
                transaction.finalDirectoryName,
                "new"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()

            assertEquals(
                InstallReplacementRecoveryAction
                    .COMPLETED_PROMOTED_INSTALL,
                result.action
            )
            assertEquals(
                "new",
                File(finalDir, "marker.txt").readText()
            )
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `prepared replacement restores a backup moved before state update`() {
        withFixture("prepared-backup") { fixture ->
            val transaction = transaction(
                state = InstallReplacementState.PREPARED,
                hadExistingInstall = true
            )
            val backupDir = fixture.directory(
                transaction.backupDirectoryName,
                "old"
            )
            val stagedDir = fixture.directory(
                transaction.stagedDirectoryName,
                "new"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()
            val finalDir = File(
                fixture.modsDir,
                transaction.finalDirectoryName
            )

            assertEquals(
                InstallReplacementRecoveryAction
                    .RESTORED_PREVIOUS_INSTALL,
                result.action
            )
            assertEquals(
                "old",
                File(finalDir, "marker.txt").readText()
            )
            assertFalse(backupDir.exists())
            assertFalse(stagedDir.exists())
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `backup created state restores the previous installation`() {
        withFixture("backup-created") { fixture ->
            val transaction = transaction(
                state =
                    InstallReplacementState.BACKUP_CREATED,
                hadExistingInstall = true
            )
            fixture.directory(
                transaction.backupDirectoryName,
                "old"
            )
            fixture.directory(
                transaction.stagedDirectoryName,
                "new"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()
            val finalDir = File(
                fixture.modsDir,
                transaction.finalDirectoryName
            )

            assertEquals(
                InstallReplacementRecoveryAction
                    .RESTORED_PREVIOUS_INSTALL,
                result.action
            )
            assertEquals(
                "old",
                File(finalDir, "marker.txt").readText()
            )
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `backup created state completes promotion that already moved`() {
        withFixture("promotion-before-state") { fixture ->
            val transaction = transaction(
                state =
                    InstallReplacementState.BACKUP_CREATED,
                hadExistingInstall = true
            )
            val finalDir = fixture.directory(
                transaction.finalDirectoryName,
                "new"
            )
            val backupDir = fixture.directory(
                transaction.backupDirectoryName,
                "old"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()

            assertEquals(
                InstallReplacementRecoveryAction
                    .COMPLETED_PROMOTED_INSTALL,
                result.action
            )
            assertEquals(
                "new",
                File(finalDir, "marker.txt").readText()
            )
            assertFalse(backupDir.exists())
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `promoted state removes the stale backup`() {
        withFixture("promoted") { fixture ->
            val transaction = transaction(
                state = InstallReplacementState.PROMOTED,
                hadExistingInstall = true
            )
            val finalDir = fixture.directory(
                transaction.finalDirectoryName,
                "new"
            )
            val backupDir = fixture.directory(
                transaction.backupDirectoryName,
                "old"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()

            assertEquals(
                InstallReplacementRecoveryAction
                    .COMPLETED_PROMOTED_INSTALL,
                result.action
            )
            assertEquals(
                "new",
                File(finalDir, "marker.txt").readText()
            )
            assertFalse(backupDir.exists())
            assertTrue(fixture.transactions().isEmpty())
        }
    }

    @Test
    fun `ambiguous state is retained for manual recovery`() {
        withFixture("ambiguous") { fixture ->
            val transaction = transaction(
                state = InstallReplacementState.PREPARED,
                hadExistingInstall = false
            )
            val finalDir = fixture.directory(
                transaction.finalDirectoryName,
                "final"
            )
            val stagedDir = fixture.directory(
                transaction.stagedDirectoryName,
                "staged"
            )
            fixture.persist(transaction)

            val result = fixture.recover().single()

            assertFalse(result.recovered)
            assertTrue(
                result.failureMessage.orEmpty().contains(
                    "manual recovery"
                )
            )
            assertTrue(finalDir.exists())
            assertTrue(stagedDir.exists())
            assertEquals(1, fixture.transactions().size)
        }
    }

    private fun transaction(
        state: InstallReplacementState,
        hadExistingInstall: Boolean
    ): InstallReplacementTransaction {
        return InstallReplacementTransaction(
            id = "test",
            state = state,
            finalDirectoryName = "ExampleMod",
            stagedDirectoryName =
                "_installing_ExampleMod_test",
            backupDirectoryName =
                "_dml_backup_ExampleMod_test",
            hadExistingInstall = hadExistingInstall
        )
    }

    private fun withFixture(
        name: String,
        action: (Fixture) -> Unit
    ) {
        val root = Files.createTempDirectory(
            "dml-recovery-$name"
        ).toFile()
        val modsDir = File(root, "mods").apply {
            check(mkdirs())
        }

        try {
            action(
                Fixture(
                    modsDir = modsDir,
                    store = store
                )
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private data class Fixture(
        val modsDir: File,
        val store: InstallReplacementTransactionStore
    ) {
        fun directory(
            name: String,
            marker: String
        ): File {
            return File(modsDir, name).apply {
                check(mkdirs())
                File(this, "marker.txt").writeText(marker)
            }
        }

        fun persist(
            transaction: InstallReplacementTransaction
        ) {
            store.create(
                modsDir = modsDir,
                transaction = transaction
            )
        }

        fun recover():
                List<InstallReplacementRecoveryResult> {
            return InstallReplacementRecovery()
                .recoverAll(modsDir)
        }

        fun transactions(): List<File> {
            return store.listTransactionFiles(modsDir)
        }
    }
}