package dev.webbies.dotenvify.diagnostics

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class EnvKeyScannerTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    private fun writeFile(name: String, content: String) {
        val file = tmpDir.root.toPath().resolve(name)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    @Test
    fun `detects process_env_KEY in JavaScript`() {
        writeFile("app.js", """
            const url = process.env.API_URL;
            const key = process.env.SECRET_KEY;
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("API_URL"))
        assertTrue(keys.contains("SECRET_KEY"))
    }

    @Test
    fun `detects process_env bracket notation`() {
        writeFile("app.ts", """
            const url = process.env['API_URL'];
            const key = process.env["DB_HOST"];
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("API_URL"))
        assertTrue(keys.contains("DB_HOST"))
    }

    @Test
    fun `detects import_meta_env for Vite`() {
        writeFile("main.ts", """
            const url = import.meta.env.VITE_API_URL;
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("VITE_API_URL"))
    }

    @Test
    fun `detects Python os_environ and os_getenv`() {
        writeFile("app.py", """
            import os
            db = os.environ['DATABASE_URL']
            secret = os.environ.get('SECRET_KEY')
            port = os.getenv('PORT')
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DATABASE_URL"))
        assertTrue(keys.contains("SECRET_KEY"))
        assertTrue(keys.contains("PORT"))
    }

    @Test
    fun `detects Go os_Getenv`() {
        writeFile("main.go", """
            package main
            import "os"
            func main() {
                host := os.Getenv("DB_HOST")
            }
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DB_HOST"))
    }

    @Test
    fun `detects Java System_getenv`() {
        writeFile("App.java", """
            public class App {
                String url = System.getenv("API_URL");
            }
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("API_URL"))
    }

    @Test
    fun `detects Ruby ENV`() {
        writeFile("config.rb", """
            db = ENV['DATABASE_URL']
            secret = ENV.fetch('SECRET_KEY')
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DATABASE_URL"))
        assertTrue(keys.contains("SECRET_KEY"))
    }

    @Test
    fun `detects PHP getenv and _ENV`() {
        writeFile("config.php", """
            <?php
            ${'$'}db = getenv('DATABASE_URL');
            ${'$'}key = ${'$'}_ENV['API_KEY'];
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DATABASE_URL"))
        assertTrue(keys.contains("API_KEY"))
    }

    @Test
    fun `detects Rust env_var`() {
        writeFile("main.rs", """
            let url = env::var("DATABASE_URL").unwrap();
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DATABASE_URL"))
    }

    @Test
    fun `detects dollar-brace in YAML`() {
        writeFile("docker-compose.yml", """
            services:
              app:
                environment:
                  - DB_HOST=${'$'}{DB_HOST}
                  - API_KEY=${'$'}{API_KEY}
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DB_HOST"))
        assertTrue(keys.contains("API_KEY"))
    }

    @Test
    fun `skips node_modules`() {
        writeFile("node_modules/lib/index.js", """
            const x = process.env.SHOULD_SKIP;
        """.trimIndent())
        writeFile("src/app.js", """
            const x = process.env.SHOULD_FIND;
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertFalse(keys.contains("SHOULD_SKIP"))
        assertTrue(keys.contains("SHOULD_FIND"))
    }

    @Test
    fun `returns references with file and line info`() {
        writeFile("app.js", """
            const a = process.env.API_URL;
            const b = "hello";
            const c = process.env.API_URL;
        """.trimIndent())

        val refs = EnvKeyScanner.scanProject(tmpDir.root.toPath())
        val apiRefs = refs.filter { it.key == "API_URL" }
        assertEquals(2, apiRefs.size)
        assertEquals(1, apiRefs[0].line)
        assertEquals(3, apiRefs[1].line)
    }

    @Test
    fun `ignores lowercase keys`() {
        writeFile("app.js", """
            const a = process.env.lowercase_key;
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertFalse(keys.contains("lowercase_key"))
    }

    @Test
    fun `detects dotenv library pattern`() {
        writeFile("app.kt", """
            val url = dotenv.get("DATABASE_URL")
            val key = env("API_KEY")
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("DATABASE_URL"))
        assertTrue(keys.contains("API_KEY"))
    }

    @Test
    fun `detects CSharp Environment_GetEnvironmentVariable`() {
        writeFile("Program.cs", """
            var url = Environment.GetEnvironmentVariable("CONNECTION_STRING");
        """.trimIndent())

        val keys = EnvKeyScanner.scanProjectKeys(tmpDir.root.toPath())
        assertTrue(keys.contains("CONNECTION_STRING"))
    }
}
