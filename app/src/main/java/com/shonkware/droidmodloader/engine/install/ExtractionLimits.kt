package com.shonkware.droidmodloader.engine.install

data class ExtractionLimits(
    val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    val maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES,
    val maxRelativePathCharacters: Int =
        DEFAULT_MAX_RELATIVE_PATH_CHARACTERS
) {
    init {
        require(maxEntries > 0) {
            "Maximum archive entry count must be positive."
        }
        require(maxFileBytes > 0) {
            "Maximum extracted file size must be positive."
        }
        require(maxTotalBytes > 0) {
            "Maximum total extracted size must be positive."
        }
        require(maxRelativePathCharacters > 0) {
            "Maximum relative path length must be positive."
        }
        require(maxFileBytes <= maxTotalBytes) {
            "Maximum file size cannot exceed total extraction size."
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 250_000
        const val DEFAULT_MAX_FILE_BYTES =
            32L * 1024L * 1024L * 1024L
        const val DEFAULT_MAX_TOTAL_BYTES =
            256L * 1024L * 1024L * 1024L
        const val DEFAULT_MAX_RELATIVE_PATH_CHARACTERS = 1_024
    }
}