package dev.webbies.dotenvify.core

data class EnvEntry(
    val key: String,
    val value: String,
)

data class ParseResult(
    val entries: List<EnvEntry>,
    val warnings: List<String> = emptyList(),
    val alreadyFormatted: Boolean = false,
)

data class FormatOptions(
    val exportPrefix: Boolean = false,
    val sort: Boolean = true,
    val ignoreLowercase: Boolean = true,
    val urlOnly: Boolean = false,
    val preserveKeys: Set<String> = emptySet(),
)
