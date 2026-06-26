package com.shonkware.droidmodloader.engine.install

import java.io.File
import java.io.IOException

class RarArchiveReader internal constructor(
    private val openSession: (File) -> RarArchiveSession
) : ArchiveReader {

    constructor() : this(
        openSession = ::JunrarArchiveSession
    )

    override fun read(
        archive: File,
        writer: ArchiveEntryWriter
    ) {
        try {
            openSession(archive).use { session ->
                if (session.encrypted) {
                    throw ArchiveReadException(
                        code =
                            ArchiveReadFailureCode
                                .PASSWORD_PROTECTED_OR_ENCRYPTED,
                        message =
                            "Password-protected or encrypted RAR " +
                                    "archives are not supported: " +
                                    archive.name
                    )
                }

                while (true) {
                    val entry = session.nextEntry() ?: break

                    if (entry.name.isBlank()) {
                        continue
                    }

                    if (entry.encrypted) {
                        throw ArchiveReadException(
                            code =
                                ArchiveReadFailureCode
                                    .PASSWORD_PROTECTED_OR_ENCRYPTED,
                            message =
                                "The RAR archive contains an encrypted " +
                                        "entry: ${entry.name}"
                        )
                    }

                    if (entry.splitBefore || entry.splitAfter) {
                        throw ArchiveReadException(
                            code =
                                ArchiveReadFailureCode.MULTIPART_ARCHIVE,
                            message =
                                "Multipart RAR archives are not supported " +
                                        "by this DML release: ${archive.name}"
                        )
                    }

                    if (entry.directory) {
                        writer.writeDirectory(entry.name)
                    } else {
                        writer.writeFile(entry.name) { output ->
                            entry.writeContent(output)
                        }
                    }
                }
            }
        } catch (exception: ArchiveReadException) {
            throw exception
        } catch (exception: IOException) {
            throw ArchiveReadException(
                code = ArchiveReadFailureCode.IO_FAILURE,
                message =
                    "DML could not read or write RAR archive data: " +
                            archive.name,
                cause = exception
            )
        } catch (
            exception: InstallCancelledException
        ) {
            throw exception
        } catch (exception: Exception) {
            throw ArchiveReadException(
                code =
                    ArchiveReadFailureCode.CORRUPT_OR_UNSUPPORTED,
                message =
                    "The RAR archive is corrupt or uses an unsupported " +
                            "RAR4 compression feature: ${archive.name}",
                cause = exception
            )
        }
    }
}