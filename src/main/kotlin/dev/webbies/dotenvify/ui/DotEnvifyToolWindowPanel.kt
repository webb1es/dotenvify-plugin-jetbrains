package dev.webbies.dotenvify.ui

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.webbies.dotenvify.core.*
import dev.webbies.dotenvify.diagnostics.EnvFileWatcher
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.dnd.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DotEnvifyToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val inputArea = JBTextArea().apply {
        font = MONO_FONT; lineWrap = true
        emptyText.text = "Paste raw key-value data here, or drag and drop a file..."
    }
    private val outputArea = JBTextArea().apply { font = MONO_FONT; isEditable = false; lineWrap = true }
    private val optionsPanel = FormatOptionsPanel(project)
    private val statusLabel = JLabel(" ")

    private val autoWatchCheckbox = JCheckBox("Auto-watch .env").apply {
        toolTipText = "Automatically refresh preview when .env file changes"
    }

    private var currentEntries: List<EnvEntry> = emptyList()
    private val debounceTimer = Timer(200) { updatePreview() }.apply { isRepeats = false }

    private val watcherListener = EnvFileWatcher.EnvChangeListener {
        ApplicationManager.getApplication().invokeLater {
            if (autoWatchCheckbox.isSelected) updatePreview()
        }
    }

    init {
        border = JBUI.Borders.empty(8)

        // === TOP: Format options + auto-watch ===
        val optionsRow = JPanel(BorderLayout()).apply {
            add(optionsPanel, BorderLayout.CENTER)
            add(autoWatchCheckbox, BorderLayout.EAST)
        }

        // === CENTER: Side-by-side split ===
        val splitter = JBSplitter(false, 0.5f).apply {
            firstComponent = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("Input")
                add(JBScrollPane(inputArea), BorderLayout.CENTER)
            }
            secondComponent = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("Preview")
                add(JBScrollPane(outputArea), BorderLayout.CENTER)
            }
        }

        // === BOTTOM: Action buttons + status ===
        val bottomRow = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(4)
            val actions = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                add(JButton("Paste").apply {
                    icon = AllIcons.Actions.MenuPaste
                    addActionListener { pasteFromClipboard() }
                })
                add(JButton("Clear").apply {
                    icon = AllIcons.Actions.GC
                    addActionListener {
                        inputArea.text = ""; outputArea.text = ""
                        statusLabel.text = " "; currentEntries = emptyList()
                    }
                })
                add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = java.awt.Dimension(2, 24) })
                add(JButton("Apply to .env").apply {
                    icon = AllIcons.Actions.MenuSaveall
                    addActionListener { applyToFile() }
                })
                add(JButton("Copy to Clipboard").apply {
                    icon = AllIcons.Actions.Copy
                    addActionListener { copyToClipboard() }
                })
            }
            add(actions, BorderLayout.WEST)
            add(statusLabel, BorderLayout.EAST)
        }

        add(optionsRow, BorderLayout.NORTH)
        add(splitter, BorderLayout.CENTER)
        add(bottomRow, BorderLayout.SOUTH)

        // Debounced preview
        inputArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = debounceTimer.restart()
            override fun removeUpdate(e: DocumentEvent) = debounceTimer.restart()
            override fun changedUpdate(e: DocumentEvent) = debounceTimer.restart()
        })
        optionsPanel.onChange { updatePreview() }

        setupDragAndDrop()

        autoWatchCheckbox.addItemListener {
            val watcher = project.service<EnvFileWatcher>()
            if (autoWatchCheckbox.isSelected) watcher.addListener(watcherListener)
            else watcher.removeListener(watcherListener)
        }
    }

    private fun updatePreview() {
        val input = inputArea.text
        if (input.isBlank()) {
            outputArea.text = ""; statusLabel.text = " "; currentEntries = emptyList(); return
        }

        val result = DotEnvParser.parse(input)
        currentEntries = result.entries
        if (result.entries.isEmpty()) {
            outputArea.text = ""; statusLabel.text = "No entries found"; return
        }

        outputArea.text = DotEnvFormatter.format(result.entries, optionsPanel.options())
        val warnings = if (result.warnings.isNotEmpty()) " | ${result.warnings.size} warning(s)" else ""
        val formatted = if (result.alreadyFormatted) " | Already .env format" else ""
        statusLabel.text = "${result.entries.size} entries$warnings$formatted"
    }

    private fun applyToFile() {
        if (currentEntries.isEmpty()) {
            EnvFileApplicator.notify(project, "Nothing to save. Paste input first.", NotificationType.WARNING)
            return
        }

        val basePath = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(FileSaverDescriptor("Save .env File", "Choose where to save the .env file"), project)
            .save(basePath, ".env") ?: return

        val targetPath = Path.of(wrapper.file.absolutePath)
        EnvFileApplicator.apply(project, currentEntries, targetPath, "New Input", optionsPanel.options())
    }

    private fun copyToClipboard() {
        val output = outputArea.text
        if (output.isBlank()) return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output), null)
        statusLabel.text = "Copied to clipboard"
    }

    private fun pasteFromClipboard() {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val text = clipboard.getData(DataFlavor.stringFlavor) as? String ?: return
            inputArea.text = text
        } catch (_: java.awt.datatransfer.UnsupportedFlavorException) {
            // No text on clipboard
        } catch (_: java.io.IOException) {
            // Clipboard unavailable
        }
    }

    private fun setupDragAndDrop() {
        DropTarget(inputArea, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
            override fun drop(dtde: DropTargetDropEvent) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                try {
                    val transferable = dtde.transferable
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<java.io.File>
                        val file = files.firstOrNull() ?: return
                        inputArea.text = Files.readString(file.toPath())
                    } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        inputArea.text = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    }
                    dtde.dropComplete(true)
                } catch (_: java.io.IOException) {
                    dtde.dropComplete(false)
                } catch (_: java.awt.datatransfer.UnsupportedFlavorException) {
                    dtde.dropComplete(false)
                }
            }
        })
    }
}
