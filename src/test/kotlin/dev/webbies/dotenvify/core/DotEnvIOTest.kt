package dev.webbies.dotenvify.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class DotEnvIOTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `readEnvFile returns empty list for non-existent file`() {
        val path = tempDir.root.toPath().resolve("nonexistent.env")
        val result = DotEnvIO.readEnvFile(path)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `readEnvFile parses existing file`() {
        val file = tempDir.newFile("test.env").toPath()
        Files.writeString(file, "API_KEY=test123\nDATABASE_URL=postgres://localhost\n")

        val result = DotEnvIO.readEnvFile(file)
        assertEquals(2, result.size)
        assertEquals("API_KEY", result[0].key)
        assertEquals("test123", result[0].value)
    }

    @Test
    fun `writeEnvFile creates file with content`() {
        val path = tempDir.root.toPath().resolve("output.env")
        DotEnvIO.writeEnvFile(path, "API_KEY=test123\n", backup = false)

        assertTrue(Files.exists(path))
        assertEquals("API_KEY=test123\n", Files.readString(path))
    }

    @Test
    fun `writeEnvFile creates backup before overwriting`() {
        val path = tempDir.root.toPath().resolve("output.env")
        Files.writeString(path, "OLD=value\n")

        DotEnvIO.writeEnvFile(path, "NEW=value\n", backup = true)

        val backupPath = path.resolveSibling("output.env.backup.1")
        assertTrue(Files.exists(backupPath))
        assertEquals("OLD=value\n", Files.readString(backupPath))
        assertEquals("NEW=value\n", Files.readString(path))
    }

    @Test
    fun `writeEnvFile skips backup when disabled`() {
        val path = tempDir.root.toPath().resolve("output.env")
        Files.writeString(path, "OLD=value\n")

        DotEnvIO.writeEnvFile(path, "NEW=value\n", backup = false)

        val backupPath = path.resolveSibling("output.env.backup.1")
        assertFalse(Files.exists(backupPath))
    }

    @Test
    fun `backupFile creates incremental backups`() {
        val path = tempDir.root.toPath().resolve("test.env")
        Files.writeString(path, "TEST=value")

        DotEnvIO.backupFile(path)
        assertTrue(Files.exists(path.resolveSibling("test.env.backup.1")))

        DotEnvIO.backupFile(path)
        assertTrue(Files.exists(path.resolveSibling("test.env.backup.2")))
    }

    @Test
    fun `backupFile does nothing for non-existent file`() {
        val path = tempDir.root.toPath().resolve("nonexistent.env")
        DotEnvIO.backupFile(path) // should not throw
    }

    @Test
    fun `backupFile preserves content`() {
        val path = tempDir.root.toPath().resolve("test.env")
        val content = "API_KEY=secret\nDB=localhost\n"
        Files.writeString(path, content)

        DotEnvIO.backupFile(path)

        val backupPath = path.resolveSibling("test.env.backup.1")
        assertEquals(content, Files.readString(backupPath))
    }

    @Test
    fun `applyPreserve keeps existing values for preserved keys`() {
        val newEntries = listOf(
            EnvEntry("DATABASE_URL", "new-url"),
            EnvEntry("API_KEY", "new-key"),
        )
        val existingEntries = listOf(
            EnvEntry("DATABASE_URL", "keep-this"),
            EnvEntry("API_KEY", "old-key"),
        )
        val result = DotEnvIO.applyPreserve(newEntries, existingEntries, setOf("DATABASE_URL"))
        assertEquals("keep-this", result.find { it.key == "DATABASE_URL" }!!.value)
        assertEquals("new-key", result.find { it.key == "API_KEY" }!!.value)
    }

    @Test
    fun `applyPreserve uses new value when key not in existing`() {
        val newEntries = listOf(EnvEntry("NEW_KEY", "new-value"))
        val existingEntries = emptyList<EnvEntry>()
        val result = DotEnvIO.applyPreserve(newEntries, existingEntries, setOf("NEW_KEY"))
        assertEquals("new-value", result[0].value)
    }

    @Test
    fun `applyPreserve with empty preserve set returns original`() {
        val entries = listOf(EnvEntry("KEY", "value"))
        val result = DotEnvIO.applyPreserve(entries, emptyList(), emptySet())
        assertEquals(entries, result)
    }
}
