package com.shonkware.droidmodloader.engine.download

data class NexusUrlInfo(
    val gameDomain: String,
    val modId: Long?,
    val fileId: Long?
)

object NexusUrlParser {
    private val modUrlRegex = Regex(
        pattern = """nexusmods\.com/([^/\s]+)/mods/(\d+)""",
        option = RegexOption.IGNORE_CASE
    )

    private val fileIdRegex = Regex(
        pattern = """[?&]file_id=(\d+)""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(url: String): NexusUrlInfo? {
        val modMatch = modUrlRegex.find(url) ?: return null

        val gameDomain = modMatch.groupValues[1]
        val modId = modMatch.groupValues[2].toLongOrNull()
        val fileId = fileIdRegex.find(url)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()

        return NexusUrlInfo(
            gameDomain = gameDomain,
            modId = modId,
            fileId = fileId
        )
    }
}