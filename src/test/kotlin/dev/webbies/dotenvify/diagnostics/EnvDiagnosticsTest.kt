package dev.webbies.dotenvify.diagnostics

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class EnvDiagnosticsTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private fun writeFile(name: String, content: String) {
        val file = tmpDir.root.toPath().resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    @Test
    fun `detects missing keys`() {
        writeFile(".env", "API_URL=https://example.com\n")
        writeFile("app.js", """
            const url = process.env.API_URL;
            const key = process.env.SECRET_KEY;
        """.trimIndent())

        val result = EnvDiagnostics.analyze(tmpDir.root.toPath(), tmpDir.root.toPath().resolve(".env"))

        assertEquals(1, result.missingKeys.size)
        assertEquals("SECRET_KEY", result.missingKeys[0].key)
    }

    @Test
    fun `detects unused keys`() {
        writeFile(".env", "API_URL=https://example.com\nUNUSED_KEY=value\n")
        writeFile("app.js", """
            const url = process.env.API_URL;
        """.trimIndent())

        val result = EnvDiagnostics.analyze(tmpDir.root.toPath(), tmpDir.root.toPath().resolve(".env"))

        assertEquals(1, result.unusedKeys.size)
        assertEquals("UNUSED_KEY", result.unusedKeys[0])
    }

    @Test
    fun `no issues when everything matches`() {
        writeFile(".env", "API_URL=https://example.com\nDB_HOST=localhost\n")
        writeFile("app.js", """
            const url = process.env.API_URL;
            const db = process.env.DB_HOST;
        """.trimIndent())

        val result = EnvDiagnostics.analyze(tmpDir.root.toPath(), tmpDir.root.toPath().resolve(".env"))

        assertTrue(result.missingKeys.isEmpty())
        assertTrue(result.unusedKeys.isEmpty())
    }

    @Test
    fun `missing keys include reference details`() {
        writeFile(".env", "EXISTING=value\n")
        writeFile("src/app.js", """
            const a = process.env.MISSING_KEY;
        """.trimIndent())

        val result = EnvDiagnostics.analyze(tmpDir.root.toPath(), tmpDir.root.toPath().resolve(".env"))

        assertEquals(1, result.missingKeys.size)
        val missing = result.missingKeys[0]
        assertEquals("MISSING_KEY", missing.key)
        assertEquals(1, missing.references.size)
        assertTrue(missing.references[0].file.toString().contains("app.js"))
    }
}
