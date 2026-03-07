package dev.webbies.dotenvify.core

/**
 * A single environment variable entry.
 */
data class EnvEntry(
    val key: String,
    val value: String,
)

/**
 * Result of parsing raw input text.
 */
data class ParseResult(
    val entries: List<EnvEntry>,
    val warnings: List<String> = emptyList(),
)

/**
 * Options controlling how output is formatted.
 */
data class FormatOptions(
    val exportPrefix: Boolean = false,
    val sort: Boolean = true,
    val ignoreLowercase: Boolean = false,
    val urlOnly: Boolean = false,
    val preserveKeys: Set<String> = emptySet(),
)
