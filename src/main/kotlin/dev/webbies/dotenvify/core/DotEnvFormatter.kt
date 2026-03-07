package dev.webbies.dotenvify.core

/** Formats [EnvEntry] lists into .env file content with filtering, sorting, and smart quoting. */
object DotEnvFormatter {

    private val URL_PREFIXES = listOf(
        "http://", "https://", "ftp://", "sftp://", "ssh://", "git://",
        "file://", "mailto:", "postgres://", "mysql://", "mongodb://", "redis://",
    )

    fun format(entries: List<EnvEntry>, options: FormatOptions = FormatOptions()): String {
        var filtered = entries.toList()

        if (options.ignoreLowercase) {
            filtered = filtered.filter { e -> !(e.key == e.key.lowercase() && e.key != e.key.uppercase()) }
        }
        if (options.urlOnly) {
            filtered = filtered.filter { isHTTPURL(it.value) }
        }
        if (options.sort) {
            filtered = filtered.sortedBy { it.key }
        }

        val prefix = if (options.exportPrefix) "export " else ""
        val lines = filtered.map { "$prefix${it.key}=${smartQuote(it.value)}" }
        return if (lines.isEmpty()) "" else lines.joinToString("\n") + "\n"
    }

    fun isURL(value: String): Boolean = URL_PREFIXES.any { value.startsWith(it) }

    fun isHTTPURL(value: String): Boolean =
        value.startsWith("http://") || value.startsWith("https://")

    fun isQuoted(value: String): Boolean =
        value.length >= 2 &&
            ((value.startsWith('"') && value.endsWith('"')) ||
                (value.startsWith('\'') && value.endsWith('\'')))

    private fun smartQuote(value: String): String {
        if (isQuoted(value)) return value
        if (isURL(value) || value.contains(' ')) return "\"$value\""
        return value
    }
}
