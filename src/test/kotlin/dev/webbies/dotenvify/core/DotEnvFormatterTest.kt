package dev.webbies.dotenvify.core

import org.junit.Assert.*
import org.junit.Test

class DotEnvFormatterTest {

    @Test
    fun `format empty list`() {
        assertEquals("", DotEnvFormatter.format(emptyList()))
    }

    @Test
    fun `format basic entries sorted`() {
        val entries = listOf(
            EnvEntry("ZULU", "last"),
            EnvEntry("ALPHA", "first"),
            EnvEntry("BRAVO", "second"),
        )
        val result = DotEnvFormatter.format(entries)
        assertEquals("ALPHA=first\nBRAVO=second\nZULU=last\n", result)
    }

    @Test
    fun `format with export prefix`() {
        val entries = listOf(EnvEntry("API_KEY", "test123"))
        val result = DotEnvFormatter.format(entries, FormatOptions(exportPrefix = true))
        assertEquals("export API_KEY=test123\n", result)
    }

    @Test
    fun `format without sorting`() {
        val entries = listOf(
            EnvEntry("ZULU", "last"),
            EnvEntry("ALPHA", "first"),
        )
        val result = DotEnvFormatter.format(entries, FormatOptions(sort = false))
        assertEquals("ZULU=last\nALPHA=first\n", result)
    }

    @Test
    fun `format with ignore lowercase filter`() {
        val entries = listOf(
            EnvEntry("API_KEY", "test123"),
            EnvEntry("lowercase", "should-be-skipped"),
            EnvEntry("UPPERCASE", "included"),
            EnvEntry("MixedCase", "included"),
        )
        val result = DotEnvFormatter.format(entries, FormatOptions(ignoreLowercase = true))
        assertTrue(result.contains("API_KEY="))
        assertTrue(result.contains("UPPERCASE="))
        assertTrue(result.contains("MixedCase="))
        assertFalse(result.contains("lowercase="))
    }

    @Test
    fun `format with url-only filter`() {
        val entries = listOf(
            EnvEntry("DB_URL", "https://example.com/db"),
            EnvEntry("API_URL", "http://api.example.com"),
            EnvEntry("REGULAR", "not-a-url"),
            EnvEntry("PG", "postgres://localhost:5432/db"),
        )
        val result = DotEnvFormatter.format(entries, FormatOptions(urlOnly = true))
        assertTrue(result.contains("DB_URL="))
        assertTrue(result.contains("API_URL="))
        assertFalse(result.contains("REGULAR="))
        assertFalse(result.contains("PG="))  // postgres:// is not HTTP
    }

    @Test
    fun `smart quoting for URLs`() {
        val entries = listOf(EnvEntry("URL", "https://example.com"))
        val result = DotEnvFormatter.format(entries, FormatOptions(sort = false))
        assertEquals("URL=\"https://example.com\"\n", result)
    }

    @Test
    fun `smart quoting for values with spaces`() {
        val entries = listOf(EnvEntry("MSG", "hello world"))
        val result = DotEnvFormatter.format(entries, FormatOptions(sort = false))
        assertEquals("MSG=\"hello world\"\n", result)
    }

    @Test
    fun `no double-quoting already quoted values`() {
        val entries = listOf(EnvEntry("QUOTED", "\"already-quoted\""))
        val result = DotEnvFormatter.format(entries, FormatOptions(sort = false))
        assertEquals("QUOTED=\"already-quoted\"\n", result)
    }

    @Test
    fun `no quoting simple values`() {
        val entries = listOf(EnvEntry("SIMPLE", "value123"))
        val result = DotEnvFormatter.format(entries, FormatOptions(sort = false))
        assertEquals("SIMPLE=value123\n", result)
    }

    @Test
    fun `isURL detects various protocols`() {
        assertTrue(DotEnvFormatter.isURL("http://example.com"))
        assertTrue(DotEnvFormatter.isURL("https://example.com"))
        assertTrue(DotEnvFormatter.isURL("postgres://localhost"))
        assertTrue(DotEnvFormatter.isURL("mysql://localhost"))
        assertTrue(DotEnvFormatter.isURL("mongodb://localhost"))
        assertTrue(DotEnvFormatter.isURL("redis://localhost"))
        assertTrue(DotEnvFormatter.isURL("ftp://example.com"))
        assertTrue(DotEnvFormatter.isURL("ssh://example.com"))
        assertTrue(DotEnvFormatter.isURL("git://github.com"))
        assertTrue(DotEnvFormatter.isURL("mailto:test@example.com"))
        assertFalse(DotEnvFormatter.isURL("just-a-string"))
        assertFalse(DotEnvFormatter.isURL(""))
    }

    @Test
    fun `isHTTPURL only matches http and https`() {
        assertTrue(DotEnvFormatter.isHTTPURL("http://example.com"))
        assertTrue(DotEnvFormatter.isHTTPURL("https://example.com"))
        assertFalse(DotEnvFormatter.isHTTPURL("postgres://localhost"))
        assertFalse(DotEnvFormatter.isHTTPURL("ftp://example.com"))
        assertFalse(DotEnvFormatter.isHTTPURL("not-a-url"))
    }

    @Test
    fun `isQuoted detects matching quotes`() {
        assertTrue(DotEnvFormatter.isQuoted("\"quoted\""))
        assertTrue(DotEnvFormatter.isQuoted("'quoted'"))
        assertTrue(DotEnvFormatter.isQuoted("\"\""))
        assertTrue(DotEnvFormatter.isQuoted("''"))
        assertFalse(DotEnvFormatter.isQuoted("not quoted"))
        assertFalse(DotEnvFormatter.isQuoted("\"mismatched'"))
        assertFalse(DotEnvFormatter.isQuoted(""))
    }
}
