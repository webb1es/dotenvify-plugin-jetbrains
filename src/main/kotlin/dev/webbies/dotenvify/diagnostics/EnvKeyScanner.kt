package dev.webbies.dotenvify.diagnostics

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

/** Scans source files for environment variable references across multiple languages. */
object EnvKeyScanner {

    data class KeyReference(val key: String, val file: Path, val line: Int, val snippet: String)

    /** Skip files larger than 1MB — unlikely to contain env references. */
    private const val MAX_FILE_SIZE = 1_048_576L

    private val SOURCE_EXTENSIONS = setOf(
        "js", "jsx", "ts", "tsx", "mjs", "cjs",
        "py", "pyw",
        "go",
        "java", "kt", "kts",
        "rb",
        "php",
        "rs",
        "cs",
        "swift",
        "dart",
        "ex", "exs",
        "yml", "yaml",
        "toml",
        "json",
    )

    private val SKIP_DIRS = setOf(
        "node_modules", ".git", ".idea", "build", "dist", "out",
        "target", "__pycache__", ".gradle", "vendor", ".next",
        "venv", ".venv", "env", ".env",
    )

    private val KEY_PATTERN = "[A-Z][A-Z0-9_]+"

    private val PATTERNS = listOf(
        // JS/TS
        Regex("""process\.env\.($KEY_PATTERN)"""),
        Regex("""process\.env\[['"]($KEY_PATTERN)['"]\]"""),
        Regex("""import\.meta\.env\.($KEY_PATTERN)"""),
        // Python
        Regex("""os\.environ\[['"]($KEY_PATTERN)['"]\]"""),
        Regex("""os\.environ\.get\(\s*['"]($KEY_PATTERN)['"]"""),
        Regex("""os\.getenv\(\s*['"]($KEY_PATTERN)['"]"""),
        // Go
        Regex("""os\.Getenv\(\s*"($KEY_PATTERN)""""),
        // JVM
        Regex("""System\.getenv\(\s*"($KEY_PATTERN)""""),
        // dotenv libraries
        Regex("""(?:dotenv|Env|env)\.(?:get|fetch|require)\(\s*['"]($KEY_PATTERN)['"]"""),
        Regex("""\benv\(\s*['"]($KEY_PATTERN)['"]"""),
        // Ruby
        Regex("""ENV\[['"]($KEY_PATTERN)['"]\]"""),
        Regex("""ENV\.fetch\(\s*['"]($KEY_PATTERN)['"]"""),
        // PHP
        Regex("""getenv\(\s*['"]($KEY_PATTERN)['"]"""),
        Regex("""\${'$'}_ENV\[['"]($KEY_PATTERN)['"]\]"""),
        // Rust
        Regex("""env::var\(\s*"($KEY_PATTERN)""""),
        // C#
        Regex("""Environment\.GetEnvironmentVariable\(\s*"($KEY_PATTERN)""""),
        // Variable substitution in config/YAML
        Regex("""\${'$'}\{($KEY_PATTERN)\}"""),
    )

    fun scanProject(projectRoot: Path): List<KeyReference> {
        val references = mutableListOf<KeyReference>()
        for (file in collectSourceFiles(projectRoot)) {
            try {
                // Use lazy line stream instead of reading entire file into memory
                Files.lines(file).use { lines ->
                    var lineNum = 0
                    lines.forEach { line ->
                        lineNum++
                        for (pattern in PATTERNS) {
                            for (match in pattern.findAll(line)) {
                                references.add(KeyReference(match.groupValues[1], file, lineNum, line.trim()))
                            }
                        }
                    }
                }
            } catch (_: IOException) {
                // Skip unreadable files
            } catch (_: java.io.UncheckedIOException) {
                // Skip files with encoding issues (thrown by Files.lines)
            }
        }
        return references
    }

    fun scanProjectKeys(projectRoot: Path): Set<String> =
        scanProject(projectRoot).mapTo(mutableSetOf()) { it.key }

    private fun collectSourceFiles(root: Path): List<Path> {
        if (!Files.isDirectory(root)) return emptyList()
        return Files.walk(root).filter { path ->
            path.isRegularFile() &&
                path.extension in SOURCE_EXTENSIONS &&
                path.fileSize() <= MAX_FILE_SIZE &&
                SKIP_DIRS.none { dir ->
                    val rel = root.relativize(path).toString()
                    rel.startsWith("$dir/") || rel.startsWith("$dir\\")
                }
        }.toList()
    }
}
