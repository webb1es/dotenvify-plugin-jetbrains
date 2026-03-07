package dev.webbies.dotenvify.diagnostics

import dev.webbies.dotenvify.core.DotEnvIO
import java.nio.file.Path

/**
 * Runs diagnostics comparing .env keys with source code references.
 */
object EnvDiagnostics {

    data class DiagnosticResult(
        val envKeys: Set<String>,
        val referencedKeys: Set<String>,
        val missingKeys: List<MissingKey>,
        val unusedKeys: List<String>,
    )

    data class MissingKey(
        val key: String,
        val references: List<EnvKeyScanner.KeyReference>,
    )

    /**
     * Runs full diagnostics for a project.
     */
    fun analyze(projectRoot: Path, envFilePath: Path): DiagnosticResult {
        val envEntries = DotEnvIO.readEnvFile(envFilePath)
        val envKeys = envEntries.map { it.key }.toSet()

        val references = EnvKeyScanner.scanProject(projectRoot)
        val referencedKeys = references.map { it.key }.toSet()

        // Missing: referenced in code but not in .env
        val missingKeyNames = referencedKeys - envKeys
        val missingKeys = missingKeyNames.map { key ->
            MissingKey(
                key = key,
                references = references.filter { it.key == key },
            )
        }.sortedBy { it.key }

        // Unused: in .env but not referenced in code
        val unusedKeys = (envKeys - referencedKeys).sorted()

        return DiagnosticResult(
            envKeys = envKeys,
            referencedKeys = referencedKeys,
            missingKeys = missingKeys,
            unusedKeys = unusedKeys,
        )
    }
}
