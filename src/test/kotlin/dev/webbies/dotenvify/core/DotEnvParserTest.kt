package dev.webbies.dotenvify.core

import org.junit.Assert.*
import org.junit.Test

class DotEnvParserTest {

    @Test
    fun `parse empty input`() {
        val result = DotEnvParser.parse("")
        assertTrue(result.entries.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `parse blank input`() {
        val result = DotEnvParser.parse("   \n  \n  ")
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun `parse KEY=VALUE format`() {
        val result = DotEnvParser.parse("API_KEY=test123\nDATABASE_URL=postgres://localhost")
        assertEquals(2, result.entries.size)
        assertEquals(EnvEntry("API_KEY", "test123"), result.entries[0])
        assertEquals(EnvEntry("DATABASE_URL", "postgres://localhost"), result.entries[1])
    }

    @Test
    fun `parse KEY=VALUE with export prefix`() {
        val result = DotEnvParser.parse("export API_KEY=test123\nexport DB=localhost")
        assertEquals(2, result.entries.size)
        assertEquals("API_KEY", result.entries[0].key)
        assertEquals("test123", result.entries[0].value)
        assertEquals("DB", result.entries[1].key)
    }

    @Test
    fun `parse quoted values (double quotes)`() {
        val result = DotEnvParser.parse("API_KEY=\"test with spaces\"")
        assertEquals(1, result.entries.size)
        assertEquals("test with spaces", result.entries[0].value)
    }

    @Test
    fun `parse quoted values (single quotes)`() {
        val result = DotEnvParser.parse("API_KEY='single quoted'")
        assertEquals(1, result.entries.size)
        assertEquals("single quoted", result.entries[0].value)
    }

    @Test
    fun `parse KEY VALUE space-separated format`() {
        val result = DotEnvParser.parse("API_KEY abc123\nSECRET xyz789")
        assertEquals(2, result.entries.size)
        assertEquals(EnvEntry("API_KEY", "abc123"), result.entries[0])
        assertEquals(EnvEntry("SECRET", "xyz789"), result.entries[1])
    }

    @Test
    fun `parse line-pair format`() {
        val input = """
            API_KEY
            a1b2c3d4e5f6
            DATABASE_URL
            postgres://localhost:5432/db
        """.trimIndent()
        val result = DotEnvParser.parse(input)
        assertEquals(2, result.entries.size)
        assertEquals(EnvEntry("API_KEY", "a1b2c3d4e5f6"), result.entries[0])
        assertEquals(EnvEntry("DATABASE_URL", "postgres://localhost:5432/db"), result.entries[1])
    }

    @Test
    fun `parse multiple KEY VALUE pairs on one line`() {
        val result = DotEnvParser.parse("KEY1 val1 KEY2 val2")
        assertEquals(2, result.entries.size)
        assertEquals(EnvEntry("KEY1", "val1"), result.entries[0])
        assertEquals(EnvEntry("KEY2", "val2"), result.entries[1])
    }

    @Test
    fun `skip comments`() {
        val input = """
            # This is a comment
            API_KEY=test123
            # Another comment
            SECRET=value
        """.trimIndent()
        val result = DotEnvParser.parse(input)
        assertEquals(2, result.entries.size)
    }

    @Test
    fun `skip blank lines`() {
        val input = "API_KEY=test123\n\n\nSECRET=value\n\n"
        val result = DotEnvParser.parse(input)
        assertEquals(2, result.entries.size)
    }

    @Test
    fun `parse value with equals sign`() {
        val result = DotEnvParser.parse("CONNECTION_STRING=user=admin;password=secret")
        assertEquals(1, result.entries.size)
        assertEquals("user=admin;password=secret", result.entries[0].value)
    }

    @Test
    fun `parse empty value`() {
        val result = DotEnvParser.parse("EMPTY_VAR=")
        assertEquals(1, result.entries.size)
        assertEquals("", result.entries[0].value)
    }

    @Test
    fun `parse whitespace around key and value`() {
        val result = DotEnvParser.parse("  API_KEY  =  test123  ")
        assertEquals(1, result.entries.size)
        assertEquals("API_KEY", result.entries[0].key)
        assertEquals("test123", result.entries[0].value)
    }

    @Test
    fun `parse mixed formats`() {
        val input = """
            # Comment
            export KEY1=value1
            KEY2="value2"
            KEY3='value3'
            KEY4=value4
        """.trimIndent()
        val result = DotEnvParser.parse(input)
        assertEquals(4, result.entries.size)
        assertEquals("value1", result.entries[0].value)
        assertEquals("value2", result.entries[1].value)
        assertEquals("value3", result.entries[2].value)
        assertEquals("value4", result.entries[3].value)
    }

    @Test
    fun `warning on key with no value at end of input`() {
        val result = DotEnvParser.parse("ORPHAN_KEY")
        assertTrue(result.warnings.isNotEmpty())
        assertTrue(result.warnings[0].contains("ORPHAN_KEY"))
    }
}
