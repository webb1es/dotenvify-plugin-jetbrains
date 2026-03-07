package dev.webbies.dotenvify.core

import java.nio.file.Path

/**
 * Handles reading and writing .env files with backup support.
 */
object DotEnvIO {

    fun readEnvFile(path: Path): List<EnvEntry> {
        // TODO: Read and parse existing .env file
        return emptyList()
    }

    fun writeEnvFile(path: Path, content: String, backup: Boolean = true) {
        // TODO: Write with optional incremental backup
    }
}
