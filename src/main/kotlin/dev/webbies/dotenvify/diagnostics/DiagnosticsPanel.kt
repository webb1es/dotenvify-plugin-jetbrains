package dev.webbies.dotenvify.diagnostics

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*

class DiagnosticsPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val scanButton = JButton("Run Diagnostics").apply { icon = AllIcons.Actions.Find }
    private val autoWatchCheckbox = JCheckBox("Auto-watch .env", true).apply {
        toolTipText = "Automatically re-run diagnostics when .env file changes"
    }
    private val statusLabel = JLabel("Click 'Run Diagnostics' to scan your project")

    /** Each item is a navigable diagnostic entry. */
    private data class DiagnosticItem(
        val label: String,
        val detail: String?,
        val file: Path? = null,
        val line: Int = 0,
        val isHeader: Boolean = false,
    )

    private val listModel = DefaultListModel<DiagnosticItem>()
    private val resultList = JBList(listModel).apply {
        cellRenderer = object : ColoredListCellRenderer<DiagnosticItem>() {
            override fun customizeCellRenderer(
                list: JList<out DiagnosticItem>, value: DiagnosticItem?,
                index: Int, selected: Boolean, hasFocus: Boolean,
            ) {
                val item = value ?: return
                if (item.isHeader) {
                    append(item.label, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                } else if (item.file != null) {
                    append("  ${item.label}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    item.detail?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
                } else {
                    append("  ${item.label}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
    }

    private val watcherListener = EnvFileWatcher.EnvChangeListener {
        ApplicationManager.getApplication().invokeLater {
            if (autoWatchCheckbox.isSelected) runDiagnostics()
        }
    }

    init {
        border = JBUI.Borders.empty(8)

        // === TOP: Auto-watch ===
        val optionsRow = JPanel(BorderLayout()).apply {
            add(autoWatchCheckbox, BorderLayout.WEST)
        }

        // === CENTER: Results ===
        val resultsPanel = JBScrollPane(resultList).apply {
            border = BorderFactory.createTitledBorder("Results")
        }

        // === BOTTOM: Action buttons + status ===
        val bottomRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            val actions = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(scanButton)
            }
            add(actions, BorderLayout.WEST)
            add(statusLabel, BorderLayout.EAST)
        }

        add(optionsRow, BorderLayout.NORTH)
        add(resultsPanel, BorderLayout.CENTER)
        add(bottomRow, BorderLayout.SOUTH)

        scanButton.addActionListener { runDiagnostics() }

        // Auto-watch enabled by default — register listener immediately
        project.service<EnvFileWatcher>().addListener(watcherListener)
        autoWatchCheckbox.addItemListener {
            val watcher = project.service<EnvFileWatcher>()
            if (autoWatchCheckbox.isSelected) watcher.addListener(watcherListener)
            else watcher.removeListener(watcherListener)
        }

        // Double-click to navigate to source
        resultList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val item = resultList.selectedValue ?: return
                    navigateTo(item)
                }
            }
        })
    }

    private fun navigateTo(item: DiagnosticItem) {
        val filePath = item.file ?: return
        val vf = LocalFileSystem.getInstance().findFileByNioFile(filePath) ?: return
        OpenFileDescriptor(project, vf, item.line - 1, 0).navigate(true)
    }

    private fun runDiagnostics() {
        val projectRoot = Path.of(project.basePath ?: return)
        val envFile = projectRoot.resolve(".env")

        if (!Files.exists(envFile)) {
            listModel.clear()
            listModel.addElement(DiagnosticItem("No .env file found in project root.", null, isHeader = true))
            listModel.addElement(DiagnosticItem("Create a .env file first, then run diagnostics.", null))
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
                } catch (e: java.io.IOException) {
                    ApplicationManager.getApplication().invokeLater {
                        listModel.clear()
                        listModel.addElement(DiagnosticItem("Scan failed: ${e.message}", null, isHeader = true))
                        statusLabel.text = "Error"
                        scanButton.isEnabled = true
                    }
                } catch (e: SecurityException) {
                    ApplicationManager.getApplication().invokeLater {
                        listModel.clear()
                        listModel.addElement(DiagnosticItem("Permission denied: ${e.message}", null, isHeader = true))
                        statusLabel.text = "Error"
                        scanButton.isEnabled = true
                    }
                }
            }
        })
    }

    private fun displayResult(result: EnvDiagnostics.DiagnosticResult, projectRoot: Path) {
        listModel.clear()

        listModel.addElement(DiagnosticItem(
            "Keys in .env: ${result.envKeys.size} | Keys referenced in code: ${result.referencedKeys.size}",
            null, isHeader = true
        ))

        if (result.missingKeys.isNotEmpty()) {
            listModel.addElement(DiagnosticItem("", null)) // spacer
            listModel.addElement(DiagnosticItem("MISSING FROM .env (${result.missingKeys.size})", null, isHeader = true))
            for (missing in result.missingKeys) {
                for (ref in missing.references.take(3)) {
                    val relPath = projectRoot.relativize(ref.file)
                    listModel.addElement(DiagnosticItem(
                        "${missing.key}",
                        "$relPath:${ref.line}  ${ref.snippet}",
                        file = ref.file,
                        line = ref.line,
                    ))
                }
                if (missing.references.size > 3) {
                    listModel.addElement(DiagnosticItem(
                        "  ... and ${missing.references.size - 3} more references", null
                    ))
                }
            }
        } else {
            listModel.addElement(DiagnosticItem("", null))
            listModel.addElement(DiagnosticItem("No missing keys — all referenced keys are defined in .env.", null))
        }

        if (result.unusedKeys.isNotEmpty()) {
            listModel.addElement(DiagnosticItem("", null))
            listModel.addElement(DiagnosticItem("UNUSED IN .env (${result.unusedKeys.size})", null, isHeader = true))
            result.unusedKeys.forEach {
                listModel.addElement(DiagnosticItem(it, "Defined in .env but not referenced in code"))
            }
            listModel.addElement(DiagnosticItem("", null))
            listModel.addElement(DiagnosticItem("Note: Some keys may be used via dynamic access not detectable by static analysis.", null))
        } else {
            listModel.addElement(DiagnosticItem("", null))
            listModel.addElement(DiagnosticItem("No unused keys — all .env keys are referenced in code.", null))
        }

        val issues = result.missingKeys.size + result.unusedKeys.size
        statusLabel.text = if (issues == 0) "No issues found" else "$issues issue(s) found"
    }
}
