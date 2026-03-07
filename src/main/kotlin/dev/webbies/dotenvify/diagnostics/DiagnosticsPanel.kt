package dev.webbies.dotenvify.diagnostics

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.webbies.dotenvify.ui.MONO_FONT
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*

class DiagnosticsPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val scanButton = JButton("Run Diagnostics")
    private val autoWatchCheckbox = JCheckBox("Auto-watch .env").apply {
        toolTipText = "Automatically re-run diagnostics when .env file changes"
    }
    private val statusLabel = JLabel("Click 'Run Diagnostics' to scan your project")
    private val resultArea = JBTextArea().apply { isEditable = false; font = MONO_FONT }

    private val watcherListener = EnvFileWatcher.EnvChangeListener {
        ApplicationManager.getApplication().invokeLater {
            if (autoWatchCheckbox.isSelected) runDiagnostics()
        }
    }

    init {
        border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(scanButton); add(Box.createHorizontalStrut(12))
            add(autoWatchCheckbox); add(Box.createHorizontalGlue())
            add(statusLabel)
        }

        add(buttonPanel, BorderLayout.NORTH)
        add(JBScrollPane(resultArea).apply {
            preferredSize = Dimension(600, 400)
            border = BorderFactory.createTitledBorder("Results")
        }, BorderLayout.CENTER)

        scanButton.addActionListener { runDiagnostics() }
        autoWatchCheckbox.addItemListener {
            val watcher = project.service<EnvFileWatcher>()
            if (autoWatchCheckbox.isSelected) { watcher.addListener(watcherListener); runDiagnostics() }
            else watcher.removeListener(watcherListener)
        }
    }

    private fun runDiagnostics() {
        val projectRoot = Path.of(project.basePath ?: return)
        val envFile = projectRoot.resolve(".env")

        if (!Files.exists(envFile)) {
            resultArea.text = "No .env file found in project root.\nCreate a .env file first, then run diagnostics."
            statusLabel.text = "No .env file"
            return
        }

        scanButton.isEnabled = false
        statusLabel.text = "Scanning..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning project for env references...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Scanning source files..."
                    val result = EnvDiagnostics.analyze(projectRoot, envFile)
                    ApplicationManager.getApplication().invokeLater {
                        displayResult(result, projectRoot)
                        scanButton.isEnabled = true
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        resultArea.text = "Scan failed: ${e.message}"
                        statusLabel.text = "Error"
                        scanButton.isEnabled = true
                    }
                }
            }
        })
    }

    private fun displayResult(result: EnvDiagnostics.DiagnosticResult, projectRoot: Path) {
        val sb = StringBuilder().apply {
            appendLine("=== DIAGNOSTICS SUMMARY ===")
            appendLine("Keys in .env: ${result.envKeys.size} | Keys referenced in code: ${result.referencedKeys.size}")
            appendLine()

            if (result.missingKeys.isNotEmpty()) {
                appendLine("--- MISSING FROM .env (${result.missingKeys.size}) ---")
                for (missing in result.missingKeys) {
                    appendLine("  ${missing.key}")
                    missing.references.take(3).forEach { ref ->
                        appendLine("    -> ${projectRoot.relativize(ref.file)}:${ref.line}")
                        appendLine("       ${ref.snippet}")
                    }
                    if (missing.references.size > 3) appendLine("    ... and ${missing.references.size - 3} more")
                    appendLine()
                }
            } else {
                appendLine("No missing keys — all referenced keys are defined in .env.")
                appendLine()
            }

            if (result.unusedKeys.isNotEmpty()) {
                appendLine("--- UNUSED IN .env (${result.unusedKeys.size}) ---")
                result.unusedKeys.forEach { appendLine("  $it") }
                appendLine()
                appendLine("Note: Some keys may be used via dynamic access not detectable by static analysis.")
            } else {
                appendLine("No unused keys — all .env keys are referenced in code.")
            }
        }

        resultArea.text = sb.toString()
        resultArea.caretPosition = 0
        val issues = result.missingKeys.size + result.unusedKeys.size
        statusLabel.text = if (issues == 0) "No issues found" else "$issues issue(s) found"
    }
}
